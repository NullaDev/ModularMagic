package cn.nulladev.modularmagic;

import cn.nulladev.modularmagic.research.*;
import cn.nulladev.modularmagic.spell.NodeCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.*;
import java.util.UUID;

@GameTestHolder(ModularMagic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class KnowledgeCommandGameTests {
    private static ServerPlayer player(GameTestHelper h){var p=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"KnowledgeTester"));p.getInventory().clearContent();p.setGameMode(GameType.CREATIVE);p.setPos(h.absolutePos(new BlockPos(1,2,1)).getCenter());return p;}
    @GameTest(template="empty",batch="knowledge_commands") public static void creativePlayersCanRelockAndPrepareResearch(GameTestHelper h)throws Exception{
        var p=player(h);var manager=h.getLevel().getServer().getCommands();var source=p.createCommandSourceStack().withPermission(2);
        manager.performPrefixedCommand(source,"mm knowledge grant @s all");h.assertTrue(NodeCatalog.all().stream().allMatch(n->ResearchKnowledge.knows(p,n.id())),"Grant all must store real knowledge even in creative mode");
        manager.performPrefixedCommand(source,"mm knowledge revoke @s K01");h.assertTrue(!ResearchKnowledge.knows(p,"effect/push")&&!ResearchKnowledge.completed(p,ResearchCatalog.get("K01")),"Creative mode must not bypass a relock");
        ResearchKnowledge.save(p,"paid:effect/push","yes");ResearchKnowledge.save(p,"draft:effect/push","bad");manager.performPrefixedCommand(source,"mm knowledge prepare @s K01");h.assertTrue(ResearchKnowledge.ready(p,ResearchCatalog.get("K01"))&&!ResearchKnowledge.completed(p,ResearchCatalog.get("K01")),"Prepare must learn prerequisites and lock the target");h.assertTrue(ResearchKnowledge.state(p).getString("paid:effect/push").isEmpty()&&ResearchKnowledge.state(p).getString("draft:effect/push").isEmpty(),"Prepare clears old target progress and ink payment");
        h.assertTrue(p.getInventory().items.stream().anyMatch(s->s.is(ResearchContent.NOTE.get())),"Prepare gives a physical note for the table");
        var pos=h.absolutePos(new BlockPos(1,1,1));h.getLevel().setBlockAndUpdate(pos,ResearchContent.TABLE.get().defaultBlockState());var menu=new ResearchMenu(11,p.getInventory(),pos,false);menu.supplies.setItem(0,ResearchNoteItem.create("K01",false));h.assertTrue(menu.document!=null&&!menu.reading,"Prepared creative players must actually reach the assembly puzzle");menu.removed(p);
        manager.performPrefixedCommand(source,"modularmagic knowledge reset @s");h.assertTrue(ResearchKnowledge.state(p).isEmpty(),"Reset must clear knowledge, drafts and payments");h.succeed();
    }
    @GameTest(template="empty",batch="knowledge_commands") public static void commandPermissionAndUnknownIdsAreChecked(GameTestHelper h)throws Exception{
        var p=player(h);var dispatcher=h.getLevel().getServer().getCommands().getDispatcher();var low=p.createCommandSourceStack().withPermission(0);var parsed=dispatcher.parse("mm knowledge grant @s all",low);h.assertTrue(parsed.getReader().canRead(),"Non-operators must not receive the admin command branch");var source=p.createCommandSourceStack().withPermission(2);boolean rejected=false;try{dispatcher.execute("mm knowledge grant @s no_such_knowledge",source);}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){rejected=true;}h.assertTrue(rejected&&ResearchKnowledge.state(p).isEmpty(),"An invalid id must not partially grant knowledge");dispatcher.execute("mm knowledge grant @s effect/push",source);h.assertTrue(ResearchKnowledge.knows(p,"effect/push"),"Node IDs with a slash must work without quoting");h.succeed();
    }
}
