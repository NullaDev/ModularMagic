package cn.nulladev.modularmagic.content.init.reg;

import cn.nulladev.modularmagic.content.init.ModularMagic;
import cn.nulladev.modularmagic.content.item.MagicNodeItem;
import cn.nulladev.modularmagic.content.magictree.Node;
import cn.nulladev.modularmagic.content.magictree.Nodes;
import com.tterrag.registrate.util.entry.ItemEntry;
import dev.xkmc.l2core.init.reg.registrate.SimpleEntry;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.client.model.generators.ModelFile;

public class ModItems {
    public static final SimpleEntry<CreativeModeTab> TAB = ModularMagic.REGISTRATE
            .buildL2CreativeTab("modularmagic", "Modular Magic", e ->
                    e.icon(ModItems.NODE_SELECTOR_BALL::asStack));

    public static final ItemEntry<MagicNodeItem> NODE_SELECTOR_BALL, NODE_SELECTOR_CYLINDER;

    static {
        NODE_SELECTOR_BALL = node("selector_ball", Nodes.SELECTOR_BALL);
        NODE_SELECTOR_CYLINDER = node("selector_cylinder", Nodes.SELECTOR_CYLINDER);
    }

    private static ItemEntry<MagicNodeItem> node(String id, Node node) {
        return ModularMagic.REGISTRATE.item(id, p -> new MagicNodeItem(p, node))
                .model((ctx, pvd) -> {
                    pvd.generated(ctx, pvd.modLoc("item/node/" + ctx.getName()));
                    pvd.getBuilder(ctx.getName() + "_node").parent(
                                    new ModelFile.UncheckedModelFile(pvd.modLoc("custom/rune_core")))
                            .texture("all", pvd.modLoc("item/node/" + ctx.getName()))
                            .renderType("cutout");
                }).register();
    }

    public static void register() {

    }
}
