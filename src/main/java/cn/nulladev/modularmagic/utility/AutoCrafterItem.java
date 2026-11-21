package cn.nulladev.modularmagic.utility;

import cn.nulladev.modularmagic.ModularMagic;
import cn.nulladev.modularmagic.util.AetherNumbers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import java.util.List;

public final class AutoCrafterItem extends Item {
    public AutoCrafterItem(){super(new Properties().stacksTo(1));}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player p,InteractionHand hand){
        if(p instanceof ServerPlayer server)server.openMenu(new SimpleMenuProvider((id,inv,user)->new AutoCrafterMenu(id,inv,hand),getName(p.getItemInHand(hand))),buf->buf.writeEnum(hand));
        return InteractionResultHolder.sidedSuccess(p.getItemInHand(hand),level.isClientSide);
    }
    @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean selected){
        if(!(entity instanceof ServerPlayer p)||level.isClientSide||p.containerMenu instanceof AutoCrafterMenu)return;
        var pattern=stack.getOrDefault(UtilityContent.PATTERN.get(),CrafterPattern.EMPTY);if(!pattern.enabled())return;
        try{AutoCrafting.tryCraft(p,stack);}catch(RuntimeException ex){stack.set(UtilityContent.PATTERN.get(),pattern.active(false));ModularMagic.LOGGER.warn("Disabled invalid auto crafting recipe {}",pattern.recipe(),ex);}
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext ctx,List<Component> lines,TooltipFlag flag){
        var pattern=stack.getOrDefault(UtilityContent.PATTERN.get(),CrafterPattern.EMPTY);
        lines.add(Component.translatable(pattern.enabled()?"gui.modularmagic.autocrafter_on":"gui.modularmagic.autocrafter_off"));
        lines.add(Component.translatable("tooltip.modularmagic.crafting_cost",AetherNumbers.format(UtilityConfig.CRAFT.get())));
    }
}
