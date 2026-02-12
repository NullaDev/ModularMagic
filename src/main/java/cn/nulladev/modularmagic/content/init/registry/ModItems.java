package cn.nulladev.modularmagic.content.init.registry;

import cn.nulladev.modularmagic.content.block.*;
import cn.nulladev.modularmagic.content.init.ModularMagic;
import cn.nulladev.modularmagic.content.item.*;
import cn.nulladev.modularmagic.content.magictree.NodeProperty;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;
import dev.xkmc.l2core.init.reg.registrate.SimpleEntry;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.client.model.generators.ModelFile;

public class ModItems {
    public static final SimpleEntry<CreativeModeTab> TAB = ModularMagic.REGISTRATE
            .buildL2CreativeTab("modularmagic", "Modular Magic", e ->
                    e.icon(ModItems.NODE_SELECTOR_BALL::asStack));

    public static final BlockEntry<MagicCompilerBlock> MAGIC_COMPILER_BLOCK;
    public static final ItemEntry<MagicNodeItem> NODE_SELECTOR_BALL, NODE_SELECTOR_CYLINDER;

    static {
        MAGIC_COMPILER_BLOCK = ModularMagic.REGISTRATE.block("magic_compiler", MagicCompilerBlock::new).simpleItem().register();

        NODE_SELECTOR_BALL = node("selector_ball", NodeProperty.SELECTOR_BALL);
        NODE_SELECTOR_CYLINDER = node("selector_cylinder", NodeProperty.SELECTOR_CYLINDER);
    }

    private static ItemEntry<MagicNodeItem> node(String id, NodeProperty property) {
        return ModularMagic.REGISTRATE.item(id, p -> new MagicNodeItem(p, property)).register();
//                .model((ctx, pvd) -> {
//                    pvd.generated(ctx, pvd.modLoc("item/node/" + ctx.getName()));
//                    pvd.getBuilder(ctx.getName() + "_node").parent(
//                                    new ModelFile.UncheckedModelFile(pvd.modLoc("custom/rune_core")))
//                            .texture("all", pvd.modLoc("item/node/" + ctx.getName()));
//                }).register();
    }

    public static void register() {

    }
}
