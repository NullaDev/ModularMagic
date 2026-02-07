package cn.nulladev.modularmagic.content.magictree;

import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.xkmc.l2magic.content.engine.spell.SpellAction;
import net.minecraft.util.GsonHelper;

import javax.annotation.Nullable;

public class Tree {
    public Node root;

    public String toJSON() {
        return root.toJSON();
    }

    @Nullable
    public SpellAction parseToAction() {
        JsonElement json = GsonHelper.parse(this.toJSON());
        DataResult<SpellAction> result = SpellAction.CODEC.parse(JsonOps.INSTANCE, json);
        return result.result().orElse(null);
    }
}
