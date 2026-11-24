package cn.nulladev.modularmagic.energy;

import cn.nulladev.modularmagic.ModularMagic;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import java.util.*;

public final class EnergyContent {
    public enum Kind {
        ENCHANTER("aether_enchanter"), MATERIALIZER("aether_materializer"), GENERATOR("aether_generator"), DISRUPTOR("aether_disruptor"), TANK("aether_tank"), EXPANDED_TANK("expanded_aether_tank"),
        ULTIMATE_TANK("ultimate_aether_tank"), CREATIVE_TANK("creative_aether_tank"),
        CONDENSER("aether_condenser"), CRYSTAL_CHAMBER("aether_crystallization_chamber"),
        EMITTER("aether_emitter"), EXPANDED_EMITTER("expanded_aether_emitter"), ULTIMATE_EMITTER("ultimate_aether_emitter"), CREATIVE_EMITTER("creative_aether_emitter"), CREATIVE("creative_aether_generator"),
        RESONATOR("aether_resonator"), ANCHOR("personal_aether_anchor"), WORLD_ANCHOR("world_anchor");
        public final String id;
        Kind(String id) { this.id = id; }
        public boolean tank() { return this == TANK || this == EXPANDED_TANK || this == ULTIMATE_TANK || this == CREATIVE_TANK; }
        public boolean emitter() { return this == EMITTER || this == EXPANDED_EMITTER || this == ULTIMATE_EMITTER || this == CREATIVE_EMITTER; }
        public boolean infinite() { return this==CREATIVE||this==CREATIVE_TANK||this==CREATIVE_EMITTER; }
        public Rarity rarity() {
            if(infinite())return Rarity.EPIC;
            if(this==ULTIMATE_TANK||this==ULTIMATE_EMITTER||this==CRYSTAL_CHAMBER)return Rarity.RARE;
            if(this==EXPANDED_TANK||this==EXPANDED_EMITTER||this==RESONATOR||this==ANCHOR||this==WORLD_ANCHOR)return Rarity.UNCOMMON;
            return Rarity.COMMON;
        }
        public boolean disrupts() { return this == DISRUPTOR; }
    }
    private static final DeferredRegister.Blocks BLOCK_REG = DeferredRegister.createBlocks(ModularMagic.MOD_ID);
    private static final DeferredRegister.Items ITEM_REG = DeferredRegister.createItems(ModularMagic.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BE_REG = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ModularMagic.MOD_ID);
    private static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, ModularMagic.MOD_ID);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RESONANCE_GLOW =
            PARTICLES.register("resonance_glow", () -> new SimpleParticleType(false));
    public static final Map<Kind, DeferredBlock<EnergyBlock>> BLOCKS = new EnumMap<>(Kind.class);
    public static final Map<Kind, DeferredItem<BlockItem>> ITEMS = new EnumMap<>(Kind.class);
    public static final DeferredItem<AetherShardItem> SHARD = ITEM_REG.register("aether_shard", () -> new AetherShardItem(false));
    public static final DeferredItem<AetherShardItem> CRYSTAL = ITEM_REG.register("crystalline_aether_shard", () -> new AetherShardItem(true));
    public static final DeferredItem<EnergyLinkItem> LINK = ITEM_REG.register("aether_tuner", () -> new EnergyLinkItem(false));
    public static final DeferredItem<EnergyLinkItem> RECEIVER = ITEM_REG.register("aether_receiver", () -> new EnergyLinkItem(true));
    public static final DeferredItem<Item> CROSS_LENS = ITEM_REG.register("interdimensional_lens", () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    static {
        for (Kind kind : Kind.values()) {
            var block = BLOCK_REG.register(kind.id, () -> new EnergyBlock(kind,
                    BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.COPPER).noOcclusion().forceSolidOn().lightLevel(s -> s.getValue(EnergyBlock.ACTIVE) ? 8 : 0)));
            BLOCKS.put(kind, block);
            ITEMS.put(kind, ITEM_REG.register(kind.id, () -> kind==Kind.GENERATOR?new cn.nulladev.modularmagic.generator.GeneratorItem(block.get()):new BlockItem(block.get(), new Item.Properties().stacksTo(1).rarity(kind.rarity()))));
        }
    }
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyBlockEntity>> ENTITY = BE_REG.register("aether_device",
            () -> BlockEntityType.Builder.of(EnergyBlockEntity::new, BLOCKS.values().stream().map(DeferredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
    public static void register(IEventBus bus) { BLOCK_REG.register(bus); ITEM_REG.register(bus); BE_REG.register(bus); PARTICLES.register(bus); }
    private EnergyContent() {}
}
