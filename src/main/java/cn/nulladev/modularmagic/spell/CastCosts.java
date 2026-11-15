package cn.nulladev.modularmagic.spell;

import dev.xkmc.l2magic.content.engine.context.SpellContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.Objects;

/** Server-only cost hook. Called once per successful cast attempt before executing effects. */
public final class CastCosts {
    @FunctionalInterface public interface Handler {
        boolean tryConsume(Player player, ItemStack wand, ItemStack core, SpellContext context);
    }
    private static volatile Handler handler = (player, wand, core, context) -> true;
    public static void setHandler(Handler value) { handler = Objects.requireNonNull(value); }
    public static boolean tryConsume(Player player, ItemStack wand, ItemStack core, SpellContext context) {
        return handler.tryConsume(player, wand, core, context);
    }
    private CastCosts() {}
}
