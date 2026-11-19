package cn.nulladev.modularmagic.client;

import cn.nulladev.modularmagic.ModularMagic;
import cn.nulladev.modularmagic.item.CoreWandItem;
import cn.nulladev.modularmagic.mana.*;
import cn.nulladev.modularmagic.config.ResourceConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(modid = ModularMagic.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ResourceHud {
    private static net.minecraft.world.entity.player.Player displayedPlayer;
    private static SmoothedGauge focusGauge;
    @SubscribeEvent public static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ModularMagic.id("resources"), (graphics, tick) -> render(graphics));
    }
    public static boolean visible() {
        var mc = Minecraft.getInstance();
        return mc.player != null && !mc.options.hideGui && !mc.player.isSpectator()
                && (mc.player.getMainHandItem().getItem() instanceof CoreWandItem || mc.player.getOffhandItem().getItem() instanceof CoreWandItem);
    }
    private static void render(GuiGraphics g) {
        if (!visible()) return;
        var mc = Minecraft.getInstance(); var player = mc.player;
        var mana = InventoryMana.total(player);
        int x = 8, y = g.guiHeight() - 48;
        draw(g, x, y, 0xff38b9d5, mana.infinite() ? 1 : mana.capacity() == 0 ? 0 : (double) mana.stored() / mana.capacity(),
                Component.translatable("hud.modularmagic.mana", mana.infinite() ? "∞" : number(mana.stored()) + " / " + number(mana.capacity())));
        double focus = PlayerFocus.value(player), max = ResourceConfig.FOCUS_MAX.get();
        long now=System.nanoTime();
        if(displayedPlayer!=player){displayedPlayer=player;focusGauge=new SmoothedGauge(focus,now);}
        if(focusGauge.target()!=focus)focusGauge.update(focus,now);
        double shown=focusGauge.value(now);boolean debt=shown<0,tripped=player.getData(PlayerFocus.TRIPPED);
        draw(g, x, y + 20, debt?0xffdc6378:tripped?0xffdda34e:0xffb883eb,
                Math.clamp((debt?-shown:shown) / max, 0, 1), Component.translatable(debt?"hud.modularmagic.focus_debt":tripped?"hud.modularmagic.focus_tripped":"hud.modularmagic.focus",
                        debt?String.format(java.util.Locale.ROOT,"%.1f",Math.max(0,-focus)):String.format(java.util.Locale.ROOT, "%.1f / %.1f", focus, max)));
    }
    private static String number(double value){return cn.nulladev.modularmagic.util.AetherNumbers.format((long)Math.floor(value));}
    private static void draw(GuiGraphics g, int x, int y, int color, double fraction, Component label) {
        var font = Minecraft.getInstance().font;
        g.fill(x - 2, y - 2, x + 146, y + 17, 0xb0121722);
        g.drawString(font, label, x + 2, y, 0xffeeeeee, false);
        g.fill(x + 1, y + 11, x + 143, y + 15, 0xff303747);
        g.fill(x + 1, y + 11, x + 1 + (int) (142 * fraction), y + 15, color);
    }
}
