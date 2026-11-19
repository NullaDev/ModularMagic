package cn.nulladev.modularmagic.compat.jade;

import cn.nulladev.modularmagic.ModularMagic;
import cn.nulladev.modularmagic.util.AetherNumbers;
import cn.nulladev.modularmagic.energy.EnergyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.*;

public enum AetherBlockProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;
    @Override public ResourceLocation getUid() { return ModularMagic.id("aether_devices"); }
    @Override public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof EnergyBlockEntity be) data.put(AetherJadeData.KEY,AetherJadeData.device(be));
    }
    @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!accessor.getServerData().contains(AetherJadeData.KEY)) return;
        var data=accessor.getServerData().getCompound(AetherJadeData.KEY);
        if (data.getBoolean("Infinite")) tooltip.add(text("infinite"));
        else bar(tooltip,data,"stored");
        tooltip.add(text("status",text("state."+data.getString("Status"),AetherNumbers.format(data.getLong("StartCost")))));
        if(data.contains("Products"))tooltip.add(text("products",data.getInt("Products")));
        if(data.contains("Upgrades"))tooltip.add(text("resonance_upgrades",data.getInt("Upgrades"),data.getInt("MaxUpgrades")));
        if(data.contains("Upstream"))tooltip.add(text("upstream",BlockPos.of(data.getLong("Upstream")).toShortString()));
        if (data.contains("Battery")) {
            tooltip.add(text("charging_item",Component.translatable(data.getString("BatteryName"))));
            var battery=data.getCompound("Battery");
            if (battery.getBoolean("Infinite")) tooltip.add(Component.translatable("tooltip.modularmagic.mana_infinite"));
            else bar(tooltip,battery,"battery");
            if (accessor.showDetails() && battery.getLong("Reserved")>0) tooltip.add(text("reserved",AetherNumbers.format(battery.getLong("Reserved"))));
        }
        if (!accessor.showDetails()) return;
        if (data.contains("Production")) tooltip.add(text("production",AetherNumbers.format(data.getDouble("Production"))));
        tooltip.add(text("bandwidth",AetherNumbers.format(data.getLong("Bandwidth"))));
        if (data.contains("Range")) tooltip.add(text("range",data.getInt("Range")));
        if(data.contains("LosslessDistance"))tooltip.add(text("lossless_distance",number(data.getDouble("LosslessDistance"))));
        if (data.contains("Target")) tooltip.add(text("target",BlockPos.of(data.getLong("Target")).toShortString()));
        if (data.getLong("InFlight")>0) tooltip.add(text("in_flight",AetherNumbers.format(data.getLong("InFlight"))));
        if (data.contains("Channel")) tooltip.add(text("channel",data.getInt("Channel")));
        if (data.contains("CrossDimension")) {
            tooltip.add(text(data.getBoolean("CrossDimension")?"cross_dimension":"same_dimension",number(data.getDouble(data.getBoolean("CrossDimension")?"CrossFee":"LocalFee"))));
            tooltip.add(text("player_rate",AetherNumbers.format(data.getInt("PlayerRate"))));
        }
        if (data.contains("Chunks")) tooltip.add(text("loading",data.getInt("Chunks"),AetherNumbers.format(data.getLong("Upkeep"))));
    }
    static Component text(String key,Object... args) { return Component.translatable("jade.modularmagic."+key,args); }
    static String number(double value) { return value==Math.rint(value)?Long.toString((long)value):String.format(java.util.Locale.ROOT,"%.2f",value); }
    static void bar(ITooltip tooltip,CompoundTag data,String key) {
        long stored=data.getLong("Stored"),capacity=data.getLong("Capacity");
        var helper=IElementHelper.get();
        tooltip.add(helper.progress((float)Math.clamp((double)stored/Math.max(1,capacity),0,1),text(key,AetherNumbers.format(stored),AetherNumbers.format(capacity)),
                helper.progressStyle().color(0xff245b70,0xff347b91).textColor(0xffeeeeee),BoxStyle.getNestedBox(),true));
    }
}
