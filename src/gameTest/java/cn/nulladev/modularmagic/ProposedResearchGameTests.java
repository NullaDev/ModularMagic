package cn.nulladev.modularmagic;

import cn.nulladev.modularmagic.spell.*;
import com.google.gson.JsonParser;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
import java.nio.file.*;

/** Optional local authoring check: design drafts are intentionally excluded from distributed source. */
@GameTestHolder(ModularMagic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ProposedResearchGameTests {
    @GameTest(template="empty",batch="research_design",timeoutTicks=200)
    public static void locallyAuthoredChallengeReferencesCompile(GameTestHelper h)throws Exception{
        var folder=Path.of("private-design","研究题库");if(!Files.exists(folder))folder=Path.of("..","..","private-design","研究题库");
        if(!Files.exists(folder.resolve("index.json"))){h.succeed();return;}
        var index=JsonParser.parseString(Files.readString(folder.resolve("index.json"))).getAsJsonObject();var errors=new java.util.ArrayList<String>();int checked=0;
        for(var item:index.getAsJsonArray("cases")){String id=item.getAsJsonObject().get("id").getAsString();var data=JsonParser.parseString(Files.readString(folder.resolve(id+".json"))).getAsJsonObject();var document=SpellDocument.fromJson(data.get("document").toString());var result=SpellCompiler.compile(document,h.getLevel().registryAccess());if(!result.complete())errors.add(id+": "+result.errors());checked++;}
        h.assertTrue(errors.isEmpty(),"Proposed challenges must compile in the actual L2Magic engine: "+String.join("; ",errors));ModularMagic.LOGGER.info("Validated {} proposed research reference spells",checked);h.succeed();
    }
}
