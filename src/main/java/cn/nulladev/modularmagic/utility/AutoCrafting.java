package cn.nulladev.modularmagic.utility;

import cn.nulladev.modularmagic.crafting.ContainerUpgradeRecipe;
import cn.nulladev.modularmagic.mana.ManaStorage;
import cn.nulladev.modularmagic.research.*;
import cn.nulladev.modularmagic.spell.billing.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import java.util.*;

/** Plans against copies, including outputs, wrappers and payment, then commits a complete craft. */
public final class AutoCrafting {
    private static final long INF=Long.MAX_VALUE/16;
    private record Demand(int position,Ingredient ingredient,ItemStack exact){
        boolean accepts(ItemStack stack){return exact==null?ingredient.test(stack):ItemStack.isSameItemSameComponents(exact,stack);}
    }
    private record Source(int slot,ItemStack value,long cost,boolean bucket){}

    public static boolean tryCraft(ServerPlayer player,ItemStack tool){
        var pattern=tool.getOrDefault(UtilityContent.PATTERN.get(),CrafterPattern.EMPTY);
        if(!pattern.enabled()||pattern.recipe().isEmpty())return false;
        var id=ResourceLocation.tryParse(pattern.recipe());if(id==null)return false;
        var holder=player.level().getRecipeManager().byKey(id).orElse(null);
        if(holder==null||!(holder.value() instanceof CraftingRecipe recipe))return false;
        var template=CraftingInput.of(3,3,pattern.samples());if(!recipe.matches(template,player.level()))return false;
        int locked=-1;for(int i=0;i<37;i++)if(item(player,i)==tool)locked=i;if(locked<0)return false;
        var escrow=ManaEscrow.get(player.getServer());for(int i=0;i<37;i++)if(item(player,i).getItem() instanceof cn.nulladev.modularmagic.item.ManaContainerItem)escrow.claim(item(player,i));
        var demands=requirements(recipe,template);if(demands.isEmpty())return false;
        var sources=new ArrayList<Source>();
        for(int i=0;i<37;i++){
            if(i==locked)continue;var stack=item(player,i);if(stack.isEmpty()||demands.stream().noneMatch(d->d.accepts(stack)))continue;
            for(int n=0;n<Math.min(stack.getCount(),demands.size());n++)sources.add(new Source(i,stack.copyWithCount(1),0,false));
        }
        for(int i=0;i<37;i++){
            var mimic=item(player,i);if(mimic.is(PlaceholderContent.CREATIVE.get())&&!player.isCreative())continue;
            var generated=PlaceholderItem.material(mimic);if(generated.isEmpty()||demands.stream().noneMatch(d->d.accepts(generated)))continue;
            long fee=PlaceholderItem.price(mimic);if(fee<0)continue;
            long price=CastAccount.units(fee);if(price>=INF/10)continue;
            boolean bucket=generated.is(Items.WATER_BUCKET)||generated.is(Items.LAVA_BUCKET)||generated.is(Items.POWDER_SNOW_BUCKET);
            for(int n=0;n<demands.size();n++)sources.add(new Source(-1,generated,price,bucket));
        }
        var assignment=assign(demands,sources);if(assignment==null)return false;
        var afterInputs=copyInventory(player);var actual=NonNullList.withSize(template.size(),ItemStack.EMPTY);
        long price=UtilityConfig.CRAFT.get()*ManaStorage.SCALE;int buckets=0;
        for(int i=0;i<demands.size();i++){
            var source=sources.get(assignment[i]);actual.set(demands.get(i).position,source.value.copyWithCount(1));
            if(source.slot>=0)afterInputs.get(source.slot).shrink(1);else{price=Math.addExact(price,source.cost);if(source.bucket)buckets++;}
        }
        for(int i=0;i<37&&buckets>0;i++)if(afterInputs.get(i).is(Items.BUCKET)){int n=Math.min(buckets,afterInputs.get(i).getCount());afterInputs.get(i).shrink(n);buckets-=n;}
        if(buckets>0)return false;
        var input=CraftingInput.of(template.width(),template.height(),actual);if(!recipe.matches(input,player.level()))return false;
        var output=recipe.assemble(input,player.registryAccess());if(output.isEmpty())return false;
        var remains=recipe.getRemainingItems(input);
        var proposed=copies(afterInputs);
        if(!deliver(proposed,output,remains,output.has(DataComponents.MAP_POST_PROCESSING))||!pay(proposed,price,player.isCreative()))return false;
        // Native map recipes finalize their data here, as they do in a vanilla crafter.
        output.onCraftedBySystem(player.level());
        var committed=copies(afterInputs);
        if(!deliver(committed,output,remains,false)||!pay(committed,price,player.isCreative()))return false;
        for(int i=0;i<37;i++)if(!ItemStack.matches(item(player,i),committed.get(i))){var old=item(player,i);if(!old.isEmpty())old.setCount(0);player.getInventory().setItem(i==36?40:i,committed.get(i));}
        player.getInventory().setChanged();player.inventoryMenu.broadcastChanges();return true;
    }
    private static List<Demand> requirements(CraftingRecipe recipe,CraftingInput input){
        var shaped=recipe instanceof ShapedRecipe s?s:recipe instanceof ContainerUpgradeRecipe u?u.delegate():null;
        var result=new ArrayList<Demand>();
        if(shaped!=null){
            for(boolean mirror:new boolean[]{false,true}){
                boolean matches=shaped.getWidth()==input.width()&&shaped.getHeight()==input.height();var current=new ArrayList<Demand>();
                for(int y=0;matches&&y<input.height();y++)for(int x=0;x<input.width();x++){
                    int position=y*input.width()+x,slot=y*input.width()+(mirror?input.width()-x-1:x);var ingredient=shaped.getIngredients().get(slot);var sample=input.getItem(position);
                    if(!ingredient.test(sample)){matches=false;break;}if(!sample.isEmpty())current.add(new Demand(position,ingredient,null));
                }
                if(matches)return current;
            }
        }
        if(recipe instanceof ShapelessRecipe){
            var positions=new ArrayList<Integer>();for(int i=0;i<input.size();i++)if(!input.getItem(i).isEmpty())positions.add(i);
            var ingredients=recipe.getIngredients();int[] mapped=new int[positions.size()];
            if(ingredients.size()==positions.size()&&mapIngredients(input,positions,ingredients,mapped,new boolean[ingredients.size()],0)){
                for(int i=0;i<positions.size();i++)result.add(new Demand(positions.get(i),ingredients.get(mapped[i]),null));return result;
            }
        }
        for(int i=0;i<input.size();i++)if(!input.getItem(i).isEmpty())result.add(new Demand(i,Ingredient.EMPTY,input.getItem(i)));
        return result;
    }
    private static boolean mapIngredients(CraftingInput input,List<Integer> positions,List<Ingredient> ingredients,int[] mapped,boolean[] used,int row){
        if(row==positions.size())return true;
        for(int j=0;j<ingredients.size();j++)if(!used[j]&&ingredients.get(j).test(input.getItem(positions.get(row)))){used[j]=true;mapped[row]=j;if(mapIngredients(input,positions,ingredients,mapped,used,row+1))return true;used[j]=false;}
        return false;
    }
    /** Rectangular Hungarian matching: scarce real ingredients cannot be stolen by broad tag slots. */
    private static int[] assign(List<Demand> rows,List<Source> columns){
        int n=rows.size(),m=columns.size();if(m<n)return null;long[][] cost=new long[n][m];
        for(int i=0;i<n;i++)for(int j=0;j<m;j++)cost[i][j]=rows.get(i).accepts(columns.get(j).value)?columns.get(j).cost:INF;
        long[] u=new long[n+1],v=new long[m+1];int[] p=new int[m+1],way=new int[m+1];
        for(int i=1;i<=n;i++){
            p[0]=i;int j0=0;var min=new long[m+1];Arrays.fill(min,INF);var used=new boolean[m+1];
            do{
                used[j0]=true;int i0=p[j0],j1=0;long delta=INF;
                for(int j=1;j<=m;j++)if(!used[j]){long current=cost[i0-1][j-1]-u[i0]-v[j];if(current<min[j]){min[j]=current;way[j]=j0;}if(min[j]<delta){delta=min[j];j1=j;}}
                if(delta>=INF/2)return null;
                for(int j=0;j<=m;j++)if(used[j]){u[p[j]]+=delta;v[j]-=delta;}else min[j]-=delta;
                j0=j1;
            }while(p[j0]!=0);
            do{int j1=way[j0];p[j0]=p[j1];j0=j1;}while(j0!=0);
        }
        var match=new int[n];Arrays.fill(match,-1);for(int j=1;j<=m;j++)if(p[j]!=0)match[p[j]-1]=j-1;
        for(int i=0;i<n;i++)if(match[i]<0||cost[i][match[i]]>=INF/2)return null;return match;
    }
    private static boolean deliver(List<ItemStack> inventory,ItemStack output,List<ItemStack> remains,boolean separate){
        if(!insert(inventory,output,separate))return false;for(var stack:remains)if(!insert(inventory,stack,false))return false;return true;
    }
    private static boolean insert(List<ItemStack> inventory,ItemStack value,boolean separate){
        var left=value.copy();if(left.isEmpty())return true;
        if(!separate)for(int i=0;i<36;i++){var s=inventory.get(i);if(!s.isEmpty()&&ItemStack.isSameItemSameComponents(s,left)){int n=Math.min(left.getCount(),Math.max(0,Math.min(64,s.getMaxStackSize())-s.getCount()));s.grow(n);left.shrink(n);if(left.isEmpty())return true;}}
        for(int i=0;i<36;i++)if(inventory.get(i).isEmpty()){int n=Math.min(left.getCount(),Math.min(64,left.getMaxStackSize()));inventory.set(i,left.copyWithCount(n));left.shrink(n);if(left.isEmpty())return true;}
        return false;
    }
    private static boolean pay(List<ItemStack> inventory,long amount,boolean creative){
        if(creative)return true;for(var s:inventory)if(s.getItem() instanceof ManaStorage m&&m.infinite(s))return true;long left=amount;for(var stack:inventory)if(stack.getItem() instanceof ManaStorage storage&&stack.getCount()==1){if(storage.infinite(stack))return true;left-=storage.extractUnits(stack,left,false);if(left==0)return true;}return left==0;
    }
    private static ItemStack item(ServerPlayer p,int index){return p.getInventory().getItem(index==36?40:index);}
    private static List<ItemStack> copyInventory(ServerPlayer p){var list=new ArrayList<ItemStack>();for(int i=0;i<37;i++)list.add(item(p,i).copy());return list;}
    private static List<ItemStack> copies(List<ItemStack> input){return new ArrayList<>(input.stream().map(ItemStack::copy).toList());}
    private AutoCrafting(){}
}
