package cn.nulladev.modularmagic.generator;

import cn.nulladev.modularmagic.energy.*;
import cn.nulladev.modularmagic.util.AetherNumbers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import java.util.List;

public final class GeneratorItem extends BlockItem {
    public GeneratorItem(Block block){super(block,new Properties().stacksTo(1));}
    public static String ruleId(ItemStack stack){return EnergyLinkItem.data(stack).getCompound("AetherDevice").getCompound("Generator").getString("Rule");}
    public static ItemStack create(String id){
        var stack=new ItemStack(EnergyContent.ITEMS.get(EnergyContent.Kind.GENERATOR).get());
        var generator=new CompoundTag();generator.putString("Rule",id);
        var device=new CompoundTag();device.put("Generator",generator);
        var tag=new CompoundTag();tag.put("AetherDevice",device);EnergyLinkItem.data(stack,tag);return stack;
    }
    @Override public Component getName(ItemStack stack){var r=GeneratorCatalog.get(ruleId(stack));return r==null?super.getName(stack):Component.translatable("generator.modularmagic.name",Component.translatable(r.translation()));}
}
