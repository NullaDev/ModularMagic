package cn.nulladev.modularmagic.spell.billing;

import cn.nulladev.modularmagic.item.CoreWandItem;
import cn.nulladev.modularmagic.mana.PlayerFocus;
import cn.nulladev.modularmagic.spell.*;
import dev.xkmc.l2magic.content.engine.context.*;
import dev.xkmc.l2magic.init.registrate.EngineRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;

public final class PaidCasting {
    public static boolean execute(ServerPlayer player,ItemStack wand,String json,SpellContext context,SpellCompiler.RuntimeSpell runtime){
        var item=(CoreWandItem)wand.getItem();
        if(item.focusMultiplier(wand)>0&&player.getData(PlayerFocus.TRIPPED))throw new CastAccount.Stop("gui.modularmagic.focus_tripped");
        if(BillingRuntime.count(player.getUUID())>=CostRules.PARALLEL.get())throw new CastAccount.Stop("gui.modularmagic.cast_limit");
        var document=SpellDocument.fromJson(json);final SpellBudget.Plan plan;
        try{plan=SpellBudget.plan(document,context);}catch(RuntimeException e){
            cn.nulladev.modularmagic.ModularMagic.LOGGER.debug("Spell budget rejected: {}",e.getMessage());
            throw new CastAccount.Stop("gui.modularmagic.invalid_budget");
        }
        var account=CastAccount.open(player,wand,context,plan);BillingRuntime.add(account);
        try{
            var program=new MeteredProgram(account,json);
            var engine=program.action(document,runtime.action().action());
            var anchor=player.level().registryAccess().registryOrThrow(EngineRegistry.SPELL).getHolder(RuntimeSpells.ANCHOR).orElseThrow();
            var ctx=new EngineContext(new UserContext(player.level(),player,anchor,new PaidScheduler(account)),
                    LocationContext.of(context.origin(),context.facing()),new SingleThreadedRandomSource(context.seed()),context.defaultArgs());
            if(runtime.action().condition()!=null){
                var condition=program.condition(document,runtime.action().condition());
                var allowed=new boolean[1];BillingRuntime.run(account,()->allowed[0]=condition.test(ctx));
                if(!allowed[0]){player.displayClientMessage(net.minecraft.network.chat.Component.translatable("gui.modularmagic.condition_not_met"),true);return false;}
            }
            account.begin(document.nodes().stream().anyMatch(n->n.schema().category().equals("effect")||n.template.equals("action/arrow")||n.template.equals("action/trident")));
            account.charge(plan.startup());
            var trigger=document.root.children.get("trigger_type").getFirst();account.charge(account.price(trigger.template,CostRules.defaults()));
            BillingRuntime.run(account,()->engine.execute(ctx));ctx.registerScheduler();
            player.inventoryMenu.broadcastChanges();
            return true;
        }catch(RuntimeException e){account.abort(e);if(e instanceof CastAccount.Stop stop)throw stop;throw new CastAccount.Stop("gui.modularmagic.cast_limit");}
        finally{account.rootDone();account.tick();}
    }
    private PaidCasting(){}
}
