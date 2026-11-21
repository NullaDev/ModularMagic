package cn.nulladev.modularmagic.research;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;

public final class LensItem extends Item{
    public LensItem(){super(new Properties().stacksTo(1));}
    @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand hand){
        if(!l.isClientSide){var hit=getPlayerPOVHitResult(l,p,ClipContext.Fluid.ANY);
            if(hit.getType()==HitResult.Type.BLOCK){var topics=ResearchCatalog.source(BuiltInRegistries.BLOCK.getKey(l.getBlockState(hit.getBlockPos()).getBlock()).toString());
                var next=topics.stream().filter(t->!ResearchKnowledge.completed(p,t)&&ResearchKnowledge.ready(p,t)).findFirst();
                if(next.isPresent()){var t=next.get();if(ResearchContent.spend(p,Items.PAPER,1)){var note=ResearchNoteItem.create(t.id(),t.id().equals("K00")||t.nodes().isEmpty());if(!p.addItem(note))p.drop(note,false);}
                    else p.displayClientMessage(Component.translatable("research.modularmagic.paper"),true);
                }else p.displayClientMessage(Component.translatable(topics.isEmpty()?"research.modularmagic.no_value":topics.stream().allMatch(t->ResearchKnowledge.completed(p,t))?"research.modularmagic.complete":"research.modularmagic.prerequisite"),true);
            }
        }return InteractionResultHolder.sidedSuccess(p.getItemInHand(hand),l.isClientSide);
    }
}
