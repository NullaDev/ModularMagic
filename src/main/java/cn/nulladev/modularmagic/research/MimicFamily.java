package cn.nulladev.modularmagic.research;

import cn.nulladev.modularmagic.ModularMagic;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;

/** Shared material rules. Tags let packs extend the recognized block families. */
public enum MimicFamily {
    GRAVITY("gravity","gravityGenerationCost","2560",Items.SAND),
    RAILS("rails","railGenerationCost","16",Items.RAIL),
    PLANTS("plants","plantGenerationCost","1200",Items.OAK_LOG),
    CARPETS("carpets","carpetGenerationCost","16",Items.WHITE_CARPET);

    public final String id,configKey,defaultCost;
    public final Item example;
    private final TagKey<Block> tag;
    MimicFamily(String id,String key,String cost,Item example){this.id=id;configKey=key;defaultCost=cost;this.example=example;tag=TagKey.create(Registries.BLOCK,ModularMagic.id("mimic/"+id));}
    public ResourceLocation target(){return ModularMagic.id(id);}
    public ItemStack template(){return switch(this){case GRAVITY->PlaceholderContent.GRAVITY.toStack();case RAILS->PlaceholderContent.RAILS.toStack();case PLANTS->PlaceholderContent.PLANTS.toStack();case CARPETS->PlaceholderContent.CARPETS.toStack();};}
    public boolean matches(Block b){
        if(this==PLANTS&&(b instanceof HugeMushroomBlock||b instanceof FungusBlock))return false;
        if(b.builtInRegistryHolder().is(tag))return true;
        return switch(this){
            case GRAVITY->b instanceof FallingBlock||b instanceof BrushableBlock;
            case RAILS->b instanceof BaseRailBlock;
            case CARPETS->b.builtInRegistryHolder().is(BlockTags.WOOL_CARPETS);
            case PLANTS->b.builtInRegistryHolder().is(BlockTags.LOGS_THAT_BURN)||b.builtInRegistryHolder().is(BlockTags.LEAVES)
                ||b instanceof BushBlock||b instanceof GrowingPlantBlock||b instanceof SugarCaneBlock||b instanceof CactusBlock
                ||b instanceof BambooStalkBlock||b instanceof VineBlock||b instanceof CocoaBlock
                ||b instanceof ChorusFlowerBlock||b instanceof ChorusPlantBlock||b==Blocks.MELON||b==Blocks.PUMPKIN;
        };
    }
    public static MimicFamily fromTarget(ResourceLocation id){for(var f:values())if(f.target().equals(id))return f;return null;}
    public static MimicFamily of(Block block){for(var f:values())if(f.matches(block))return f;return null;}
    public static MimicFamily parse(String id){for(var f:values())if(f.id.equals(id))return f;throw new IllegalArgumentException("Unknown mimic family: "+id);}
}
