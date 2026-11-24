package cn.nulladev.modularmagic.research;

import cn.nulladev.modularmagic.energy.EnergyLinkItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class ResearchNoteItem extends Item{
    public final boolean direct;
    public ResearchNoteItem(boolean direct){super(new Properties().stacksTo(1));this.direct=direct;}
    public static ItemStack create(String topic,boolean direct){var stack=(direct?ResearchContent.DIRECT_NOTE:ResearchContent.NOTE).toStack();var tag=EnergyLinkItem.data(stack);tag.putString("Research",topic);EnergyLinkItem.data(stack,tag);return stack;}
    @Override public Component getName(ItemStack stack){try{return Component.translatable(getDescriptionId(),Component.translatable(ResearchCatalog.get(EnergyLinkItem.data(stack).getString("Research")).key()));}catch(RuntimeException e){return Component.translatable("research.modularmagic.blank_note");}}
    private InteractionResult accept(Player p,ItemStack stack){
        if(p.level().isClientSide)return InteractionResult.SUCCESS;
        try{var t=ResearchCatalog.get(EnergyLinkItem.data(stack).getString("Research"));if(!ResearchKnowledge.ready(p,t)){p.displayClientMessage(Component.translatable("research.modularmagic.prerequisite"),true);return InteractionResult.FAIL;}
            if(ResearchKnowledge.completed(p,t)){p.displayClientMessage(Component.translatable("research.modularmagic.complete"),true);return InteractionResult.FAIL;}
            if(direct||t.id().equals("K00")||t.nodes().isEmpty()){ResearchKnowledge.learn(p,t);p.displayClientMessage(Component.translatable("research.modularmagic.learned",Component.translatable(t.key())),false);}
            else{ResearchKnowledge.save(p,"observed:"+t.id(),"yes");ResearchKnowledge.save(p,"active",t.id());}
            if(!p.isCreative()&&(direct||t.id().equals("K00")||t.nodes().isEmpty()))stack.shrink(1);return InteractionResult.SUCCESS;
        }catch(RuntimeException e){return InteractionResult.FAIL;}
    }
    @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand hand){var s=p.getItemInHand(hand);return new InteractionResultHolder<>(accept(p,s),s);}
    @Override public InteractionResult useOn(UseOnContext c){if(c.getPlayer()==null)return InteractionResult.PASS;if(c.getLevel().getBlockState(c.getClickedPos()).is(ResearchContent.TABLE.get())){ResearchContent.open(c.getPlayer(),c.getClickedPos(),false,c.getHand());return InteractionResult.SUCCESS;}return accept(c.getPlayer(),c.getItemInHand());}
}
