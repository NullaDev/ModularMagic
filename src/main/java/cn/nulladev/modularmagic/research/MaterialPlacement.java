package cn.nulladev.modularmagic.research;

import cn.nulladev.modularmagic.mana.*;
import cn.nulladev.modularmagic.item.ManaContainerItem;
import cn.nulladev.modularmagic.spell.billing.*;
import dev.xkmc.l2magic.content.engine.block.*;
import dev.xkmc.l2magic.content.engine.context.EngineContext;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import java.util.*;

/** Ordinary placement consumes inventory material; only a carried placeholder can generate material. */
public final class MaterialPlacement{
    public static double fee(BlockState state){
        var family=MimicFamily.of(state.getBlock());var values=Map.of("hardness",(double)state.getDestroySpeed(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,BlockPos.ZERO));
        try{
            if(family==MimicFamily.RAILS||family==MimicFamily.CARPETS)return CostRules.evaluate(PlaceholderContent.FAMILY_COSTS.get(family).get(),values);
            String id=BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();for(String rule:PlaceholderContent.COSTS.get())if(rule.startsWith(id+"="))return CostRules.evaluate(rule.substring(id.length()+1),values);
            var extra=PlaceholderContent.EXTRA_COSTS.get(id);if(extra!=null)return CostRules.evaluate(extra.get(),values);
            return family==null?Double.NaN:CostRules.evaluate(PlaceholderContent.FAMILY_COSTS.get(family).get(),values);
        }catch(RuntimeException e){return Double.NaN;}
    }
    private static List<ItemStack> inventory(ServerPlayer p){var s=new ArrayList<ItemStack>(p.getInventory().items);s.addAll(p.getInventory().offhand);return s;}
    private static boolean matches(ItemStack s,BlockState state){if(s.isEmpty()||s.has(DataComponents.BLOCK_ENTITY_DATA)||s.has(DataComponents.CONTAINER))return false;return s.getItem() instanceof BlockItem block&&state.is(block.getBlock())||state.is(Blocks.WATER)&&s.is(Items.WATER_BUCKET)||state.is(Blocks.LAVA)&&s.is(Items.LAVA_BUCKET)||state.is(Blocks.POWDER_SNOW)&&s.is(Items.POWDER_SNOW_BUCKET);}
    private static int materialCount(BlockState state){
        int count=1;for(var prop:state.getProperties()){
            if(Set.of("layers","candles","eggs","pickles","flower_amount").contains(prop.getName())&&state.getValue(prop) instanceof Integer value)count=Math.max(count,value);
            if(prop.getName().equals("type")&&state.getValue(prop).toString().equalsIgnoreCase("double"))count=Math.max(count,2);
        }return count;
    }
    private static boolean unearnedState(BlockState state){var defaults=state.getBlock().defaultBlockState();for(var prop:state.getProperties())if(Set.of("age","honey_level","charges","moisture").contains(prop.getName())&&!state.getValue(prop).equals(defaults.getValue(prop)))return true;return false;}
    public static boolean pay(ServerPlayer p,double amount,boolean infinite,boolean simulate){if(p.isCreative()||infinite||amount==0)return true;long need=CastAccount.units(amount),have=0;var escrow=ManaEscrow.get(p.getServer());
        for(var s:inventory(p))if(s.getItem() instanceof ManaContainerItem m&&s.getCount()==1){escrow.claim(s);if(m.infinite(s))return true;have=Math.min(Long.MAX_VALUE/2,have+m.extractUnits(s,Long.MAX_VALUE,true));}if(have<need)return false;
        if(!simulate)for(var s:inventory(p))if(s.getItem() instanceof ManaContainerItem m&&s.getCount()==1){need-=m.extractUnits(s,need,false);if(need==0)break;}return true;}
    public static boolean place(ServerPlayer p,BlockPos pos,BlockState state,double multiplier,boolean infinite,double extraBase,boolean forceGenerate){
        return place(p,pos,state,multiplier,infinite,extraBase,forceGenerate,null);
    }
    public static boolean placeMimic(ServerPlayer p,BlockPos pos,BlockState state,double base,net.minecraft.resources.ResourceLocation material){
        return place(p,pos,state,1,p.isCreative(),base,true,material);
    }
    private static boolean place(ServerPlayer p,BlockPos pos,BlockState state,double multiplier,boolean infinite,double extraBase,boolean forceGenerate,net.minecraft.resources.ResourceLocation requestedMaterial){
        var l=p.serverLevel();if(state.isAir()||!l.hasChunkAt(pos)||!l.getWorldBorder().isWithinBounds(pos)||!p.mayBuild()||p.getServer().isUnderSpawnProtection(l,pos,p)||!BlockUtils.allowModification(l,pos)||!l.getBlockState(pos).canBeReplaced())return false;
        if(l.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER)&&state.hasProperty(BlockStateProperties.WATERLOGGED))state=state.setValue(BlockStateProperties.WATERLOGGED,true);
        if(l.getBlockState(pos).equals(state))return false;
        boolean doublePlant=state.getBlock() instanceof DoublePlantBlock&&state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)==net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER;
        if(doublePlant){var top=pos.above();if(!l.hasChunkAt(top)||l.isOutsideBuildHeight(top)||!l.getWorldBorder().isWithinBounds(top)||p.getServer().isUnderSpawnProtection(l,top,p)||!BlockUtils.allowModification(l,top)||!l.getBlockState(top).canBeReplaced())return false;}
        var materials=new LinkedHashMap<ItemStack,Integer>();boolean generated=forceGenerate;int count=materialCount(state);
        if(!p.isCreative()){
            if(unearnedState(state))return false;
            if(!forceGenerate){int left=count;for(var s:inventory(p))if(matches(s,state)){int n=Math.min(left,s.getCount());materials.put(s,n);left-=n;if(left==0)break;}if(left>0)materials.clear();}
            if(materials.isEmpty()){var id=requestedMaterial==null?BuiltInRegistries.BLOCK.getKey(state.getBlock()):requestedMaterial;boolean found=false;for(var s:inventory(p))if(s.is(PlaceholderContent.ITEM.get())&&id.equals(PlaceholderItem.target(s))){found=true;break;}
                if(!found){p.displayClientMessage(Component.translatable("placeholder.modularmagic.missing"),true);return false;}generated=true;}
        }
        double generation=generated?fee(state)*count:0;if(!Double.isFinite(generation)||generation<0)return false;double cost=(extraBase+generation)*multiplier;
        if(!pay(p,cost,infinite,true)){p.displayClientMessage(Component.translatable("gui.modularmagic.not_enough_mana"),true);return false;}
        if(!l.setBlockAndUpdate(pos,state))return false;
        if(doublePlant)state.getBlock().setPlacedBy(l,pos,state,p,new ItemStack(state.getBlock().asItem()));
        if(!p.isCreative())for(var part:materials.entrySet()){var material=part.getKey();boolean bucket=material.is(Items.WATER_BUCKET)||material.is(Items.LAVA_BUCKET)||material.is(Items.POWDER_SNOW_BUCKET);material.shrink(part.getValue());if(bucket&&!p.addItem(new ItemStack(Items.BUCKET,part.getValue())))p.drop(new ItemStack(Items.BUCKET,part.getValue()),false);}
        if(!pay(p,cost,infinite,false))throw new IllegalStateException("Placement payment changed during one server operation");p.inventoryMenu.broadcastChanges();return true;
    }
    public static boolean execute(Object action,EngineContext ctx,CastAccount account){BlockState state;
        if(action instanceof SetBlock b)state=b.state();else if(action instanceof SetBlockFacing b){state=b.state();if(state.hasProperty(BlockStateProperties.FACING))state=state.setValue(BlockStateProperties.FACING,Direction.getNearest(ctx.loc().dir()));if(state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)){var dir=Direction.getNearest(ctx.loc().dir().multiply(1,0,1));if(dir.getAxis()!=Direction.Axis.Y)state=state.setValue(BlockStateProperties.HORIZONTAL_FACING,dir);}if(state.hasProperty(BlockStateProperties.AXIS))state=state.setValue(BlockStateProperties.AXIS,Direction.getNearest(ctx.loc().dir()).getAxis());}
        else return false;
        if(!place(account.player,BlockPos.containing(ctx.loc().pos()),state,account.multiplier,account.infinite,0,false))throw new CastAccount.Stop("placeholder.modularmagic.failed");return true;
    }
    private MaterialPlacement(){}
}
