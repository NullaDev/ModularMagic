package cn.nulladev.modularmagic.compat.jade;

import cn.nulladev.modularmagic.ModularMagic;
import cn.nulladev.modularmagic.util.AetherNumbers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;
import static cn.nulladev.modularmagic.compat.jade.AetherBlockProvider.*;

public enum AetherItemProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    INSTANCE;
    @Override public ResourceLocation getUid() { return ModularMagic.id("aether_items"); }
    @Override public int getDefaultPriority() { return 5000; }
    @Override public void appendServerData(CompoundTag data,EntityAccessor accessor) {
        if (accessor.getEntity() instanceof ItemEntity entity) {
            var snapshot=AetherJadeData.item(entity.getItem(),accessor.getLevel());
            if (!snapshot.isEmpty()) data.put(AetherJadeData.KEY,snapshot);
        }
    }
    @Override public void appendTooltip(ITooltip tooltip,EntityAccessor accessor,IPluginConfig config) {
        if (!accessor.getServerData().contains(AetherJadeData.KEY))return;
        var data=accessor.getServerData().getCompound(AetherJadeData.KEY);
        tooltip.remove(JadeIds.MC_ITEM_TOOLTIP);
        switch(data.getString("Type")) {
            case "container" -> {
                if(data.getBoolean("Infinite"))tooltip.add(text("infinite"));else bar(tooltip,data,"battery");
                if(accessor.showDetails()&&data.getLong("Reserved")>0)tooltip.add(text("reserved",AetherNumbers.format(data.getLong("Reserved"))));
            }
            case "fragment" -> {
                tooltip.add(text(data.getBoolean("Stable")?"stable":"expires",data.getLong("Remaining")));
                if(data.getLong("Value")>0)tooltip.add(text("fragment_value",AetherNumbers.format(data.getLong("Value"))));
            }
            case "wand" -> {
                tooltip.add(text("wand_rates",number(data.getDouble("ManaMultiplier")),number(data.getDouble("FocusMultiplier"))));
                tooltip.add(text("cores",data.getInt("Cores")));
            }
            case "core" -> {
                if(data.contains("Mana"))tooltip.add(text("core_cost",AetherNumbers.format(data.getLong("Mana")),number(data.getDouble("Focus"))));
                else tooltip.add(text(data.getBoolean("Formed")?"core_unpriced":"core_unformed"));
            }
            case "receiver" -> tooltip.add(text(data.getBoolean("Bound")?"receiver_bound":"receiver_unbound"));
            case "device" -> tooltip.add(data.getBoolean("Infinite")?text("infinite"):text("carried_energy",AetherNumbers.format(data.getLong("Stored"))));
        }
    }
}
