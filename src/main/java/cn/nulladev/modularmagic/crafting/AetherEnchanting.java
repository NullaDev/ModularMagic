package cn.nulladev.modularmagic.crafting;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;

public final class AetherEnchanting {
    public record Quote(ItemStack result,long aether,int changed){}
    public static ItemEnchantments enchantments(ItemStack stack){return stack.getOrDefault(stack.is(Items.ENCHANTED_BOOK)?DataComponents.STORED_ENCHANTMENTS:DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY);}
    public static Quote quote(ItemStack target,ItemStack source){
        if(target.isEmpty()||target.getCount()!=1||source.isEmpty())return new Quote(ItemStack.EMPTY,0,0);
        var existing=enchantments(target);var incoming=enchantments(source);var merged=new ItemEnchantments.Mutable(existing);
        long levels=0;int changed=0;
        var ordered=incoming.entrySet().stream().sorted(java.util.Comparator.comparing(e->e.getKey().unwrapKey().map(k->k.location().toString()).orElse(""))).toList();
        for(var entry:ordered){
            var enchantment=entry.getKey();int old=merged.getLevel(enchantment);
            int next=Math.min(enchantment.value().getMaxLevel(),old+entry.getIntValue());
            if(next<=old||!target.supportsEnchantment(enchantment))continue;
            boolean compatible=true;for(var other:merged.keySet())if(!other.equals(enchantment)&&!Enchantment.areCompatible(other,enchantment)){compatible=false;break;}
            if(!compatible)continue;
            int multiplier=enchantment.value().getAnvilCost();if(source.is(Items.ENCHANTED_BOOK))multiplier=Math.max(1,multiplier/2);
            levels+=Math.max(1,multiplier)*next;merged.set(enchantment,next);changed++;
        }
        if(changed==0)return new Quote(ItemStack.EMPTY,0,0);
        levels+=Math.max(0,target.getOrDefault(DataComponents.REPAIR_COST,0))+Math.max(0,source.getOrDefault(DataComponents.REPAIR_COST,0));
        long fee=Math.min(Long.MAX_VALUE/cn.nulladev.modularmagic.mana.ManaStorage.SCALE,levels*ArtificeConfig.AETHER_PER_ANVIL_LEVEL.get());
        var result=target.copy();result.set(DataComponents.ENCHANTMENTS,merged.toImmutable());return new Quote(result,fee,changed);
    }
    private AetherEnchanting(){}
}
