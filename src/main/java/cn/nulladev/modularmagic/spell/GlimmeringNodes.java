package cn.nulladev.modularmagic.spell;

import cn.nulladev.modularmagic.ModularMagic;
import cn.nulladev.modularmagic.spell.billing.CostRules;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.l2magic.content.engine.block.BlockUtils;
import dev.xkmc.l2magic.content.engine.context.EngineContext;
import dev.xkmc.l2magic.content.engine.core.*;
import dev.xkmc.l2magic.content.engine.extension.ExtensionTypeKey;
import dev.xkmc.l2magic.content.engine.processor.SimpleServerProcessor;
import dev.xkmc.l2magic.content.engine.selector.SelectedEntities;
import dev.xkmc.l2magic.content.engine.selector.SelectionType;
import dev.xkmc.l2magic.content.engine.variable.DoubleVariable;
import dev.xkmc.l2magic.content.engine.variable.IntVariable;
import dev.xkmc.l2magic.init.registrate.EngineRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Comparator;
import java.util.function.Supplier;

/** GlimmeringTales spell extensions adapted to the ModularMagic billing and registry setup. LGPL-2.1. */
public final class GlimmeringNodes {
    private static final DeferredRegister<EngineType<?>> ACTIONS = DeferredRegister.create(EngineRegistry.ENGINE.key(), ModularMagic.MOD_ID);
    private static final DeferredRegister<ProcessorType<?>> EFFECTS = DeferredRegister.create(EngineRegistry.PROCESSOR.key(), ModularMagic.MOD_ID);
    private static final DeferredRegister<FilterType<?>> FILTERS = DeferredRegister.create(EngineRegistry.FILTER.key(), ModularMagic.MOD_ID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ModularMagic.MOD_ID);
    static final Supplier<TemporarySpellBlock> TEMPORARY_BLOCK = BLOCKS.register("temporary_spell_block", TemporarySpellBlock::new);

    private static final Supplier<EngineType<TemporaryBlock>> TEMPORARY = action("temporary_block", () -> TemporaryBlock.CODEC);
    private static final Supplier<EngineType<MeltBlock>> MELT = action("melt_block", () -> MeltBlock.CODEC);
    private static final Supplier<EngineType<PotionCloud>> CLOUD = action("effect_cloud", () -> PotionCloud.CODEC);
    private static final Supplier<EngineType<Thunder>> THUNDER = action("thunder", () -> Thunder.CODEC);
    private static final Supplier<EngineType<RemoveLiquid>> REMOVE_LIQUID = action("remove_liquid", () -> RemoveLiquid.CODEC);
    private static final Supplier<ProcessorType<Procreation>> PROCREATION = effect("procreation", () -> Procreation.CODEC);
    private static final Supplier<ProcessorType<HealInterval>> HEAL = effect("heal_interval", () -> HealInterval.CODEC);
    private static final Supplier<ProcessorType<StackingStatus>> STACKING = effect("stacking_status", () -> StackingStatus.CODEC);
    private static final Supplier<FilterType<HurtCooldown>> HURT_COOLDOWN = FILTERS.register("glimmering/invulnerability_frame", () -> () -> HurtCooldown.CODEC);

    public static void register(IEventBus bus) {
        ACTIONS.register(bus);
        EFFECTS.register(bus);
        FILTERS.register(bus);
        BLOCKS.register(bus);
    }

    private static <T extends Record & ConfiguredEngine<T>> Supplier<EngineType<T>> action(String name, Supplier<MapCodec<T>> codec) {
        String path = "glimmering/" + name;
        return ACTIONS.register(path, () -> new EngineType<>(new ExtensionTypeKey(EngineRegistry.ENGINE.key().location(), ModularMagic.id(path)), codec.get()));
    }

    private static <T extends Record & EntityProcessor<T>> Supplier<ProcessorType<T>> effect(String name, Supplier<MapCodec<T>> codec) {
        String path = "glimmering/" + name;
        return EFFECTS.register(path, () -> new ProcessorType<>(new ExtensionTypeKey(EngineRegistry.PROCESSOR.key().location(), ModularMagic.id(path)), codec.get()));
    }

    public record TemporaryBlock(TemporarySpellBlock.Material material, IntVariable tick) implements ConfiguredEngine<TemporaryBlock> {
        static final MapCodec<TemporaryBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                TemporarySpellBlock.Material.CODEC.fieldOf("material").forGetter(TemporaryBlock::material),
                IntVariable.codec("tick", TemporaryBlock::tick)
        ).apply(i, TemporaryBlock::new));

        @Override public EngineType<TemporaryBlock> type() { return TEMPORARY.get(); }

        @Override public void execute(EngineContext ctx) {
            if (!(ctx.user().level() instanceof ServerLevel level)) return;
            var pos = BlockPos.containing(ctx.loc().pos());
            if (material.restore() == Blocks.AIR && level.getBlockState(pos).canBeReplaced())
                placeTemporary(ctx, level, pos, material, tick.eval(ctx));
        }
    }

    public record MeltBlock(IntVariable tick) implements ConfiguredEngine<MeltBlock> {
        static final MapCodec<MeltBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(IntVariable.codec("tick", MeltBlock::tick)).apply(i, MeltBlock::new));

        @Override public EngineType<MeltBlock> type() { return MELT.get(); }

        @Override public void execute(EngineContext ctx) {
            if (!(ctx.user().level() instanceof ServerLevel level)) return;
            var pos = BlockPos.containing(ctx.loc().pos());
            var original = level.getBlockState(pos).getBlock();
            for (var material : TemporarySpellBlock.Material.values()) {
                if (material.restore() != Blocks.AIR && original == material.restore()) {
                    placeTemporary(ctx, level, pos, material, tick.eval(ctx));
                    return;
                }
            }
        }
    }

    private static void placeTemporary(EngineContext ctx, ServerLevel level, BlockPos pos, TemporarySpellBlock.Material material, int delay) {
        var block = TEMPORARY_BLOCK.get();
        if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos) || !level.getFluidState(pos).isEmpty()) return;
        BlockUtils.set(ctx, block.defaultBlockState().setValue(TemporarySpellBlock.MATERIAL, material));
        if (level.getBlockState(pos).is(block)) level.scheduleTick(pos, block, Mth.clamp(delay, 1, 1_200));
    }

    public record PotionCloud(Holder<Potion> effect, DoubleVariable radius, IntVariable duration) implements ConfiguredEngine<PotionCloud> {
        static final MapCodec<PotionCloud> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                BuiltInRegistries.POTION.holderByNameCodec().fieldOf("effect").forGetter(PotionCloud::effect),
                DoubleVariable.codec("radius", PotionCloud::radius), IntVariable.codec("duration", PotionCloud::duration)
        ).apply(i, PotionCloud::new));

        @Override public EngineType<PotionCloud> type() { return CLOUD.get(); }

        @Override public void execute(EngineContext ctx) {
            if (!(ctx.user().level() instanceof ServerLevel level)) return;
            var pos = ctx.loc().pos();
            var cloud = new AreaEffectCloud(level, pos.x, pos.y, pos.z);
            cloud.setOwner(ctx.user().user());
            cloud.setRadius((float) Mth.clamp(radius.eval(ctx), 0.5, 8));
            cloud.setRadiusOnUse(-0.5F);
            cloud.setWaitTime(Mth.clamp(duration.eval(ctx), 0, 200));
            cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
            cloud.setPotionContents(new PotionContents(effect));
            level.addFreshEntity(cloud);
        }
    }

    public record Thunder(DoubleVariable damage) implements ConfiguredEngine<Thunder> {
        static final MapCodec<Thunder> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(DoubleVariable.codec("damage", Thunder::damage)).apply(i, Thunder::new));
        private static final ResourceKey<net.minecraft.world.damagesource.DamageType> DAMAGE =
                ResourceKey.create(Registries.DAMAGE_TYPE, ModularMagic.id("glimmering/lightning"));

        @Override public EngineType<Thunder> type() { return THUNDER.get(); }

        @Override public void execute(EngineContext ctx) {
            if (!(ctx.user().level() instanceof ServerLevel level)) return;
            var pos = BlockPos.containing(ctx.loc().pos());
            if (level.getBlockState(pos).isCollisionShapeFullBlock(level, pos)) pos = pos.above();
            var bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt == null) return;
            Vec3 strike = Vec3.atBottomCenterOf(pos);
            bolt.moveTo(strike);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);

            var caster = ctx.user().user();
            var source = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE), bolt, caster);
            var bounds = new AABB(strike.x - 3, strike.y - 3, strike.z - 3, strike.x + 3, strike.y + 6, strike.z + 3);
            float strength = (float) Mth.clamp(damage.eval(ctx), 0, CostRules.DAMAGE.get());
            level.getEntitiesOfClass(LivingEntity.class, bounds, entity -> entity.isAlive() && SelectionType.ENEMY_NO_FAMILY.test(entity, caster))
                    .stream().sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(strike)))
                    .limit(CostRules.TARGETS.get()).forEach(entity -> entity.hurt(source, strength));
        }
    }

    public record RemoveLiquid(IntVariable depth, IntVariable limit, FluidType fluid) implements ConfiguredEngine<RemoveLiquid> {
        static final MapCodec<RemoveLiquid> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                IntVariable.codec("depth", RemoveLiquid::depth), IntVariable.codec("limit", RemoveLiquid::limit),
                NeoForgeRegistries.FLUID_TYPES.byNameCodec().fieldOf("fluid").forGetter(RemoveLiquid::fluid)
        ).apply(i, RemoveLiquid::new));

        @Override public EngineType<RemoveLiquid> type() { return REMOVE_LIQUID.get(); }

        @Override public void execute(EngineContext ctx) {
            if (!(ctx.user().level() instanceof ServerLevel level)) return;
            var start = BlockPos.containing(ctx.loc().pos());
            BlockPos.breadthFirstTraversal(start, Mth.clamp(depth.eval(ctx), 0, 8), Mth.clamp(limit.eval(ctx), 1, 128),
                    (pos, visit) -> { for (var direction : Direction.values()) visit.accept(pos.relative(direction)); },
                    pos -> remove(level, pos));
        }

        private boolean remove(ServerLevel level, BlockPos pos) {
            if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos) || !BlockUtils.allowModification(level, pos)) return false;
            var state = level.getBlockState(pos);
            if (state.getFluidState().isEmpty() || state.getFluidState().getType().getFluidType() != fluid) return false;
            if (state.getBlock() instanceof BucketPickup bucket && !bucket.pickupBlock(null, level, pos, state).isEmpty()) return true;
            if (!(state.getBlock() instanceof LiquidBlock)) return level.destroyBlock(pos, true);
            return level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }

    public record Procreation(IntVariable num) implements SimpleServerProcessor<Procreation> {
        static final MapCodec<Procreation> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(IntVariable.codec("num", Procreation::num)).apply(i, Procreation::new));

        @Override public ProcessorType<Procreation> type() { return PROCREATION.get(); }

        @Override public void process(SelectedEntities selection, EngineContext ctx) {
            var breeder = ctx.user().user() instanceof Player player ? player : null;
            int remaining = Math.max(0, num.eval(ctx));
            for (var entity : selection.living()) {
                if (remaining == 0) break;
                if (!(entity instanceof Animal animal)) continue;
                if (animal.isBaby()) animal.ageUp(Animal.getSpeedUpSecondsWhenFeeding(-animal.getAge()), true);
                else if (animal.canFallInLove()) animal.setInLove(breeder);
                else continue;
                remaining--;
            }
        }
    }

    public record HealInterval(IntVariable interval, DoubleVariable heal) implements SimpleServerProcessor<HealInterval> {
        static final MapCodec<HealInterval> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                IntVariable.codec("interval", HealInterval::interval), DoubleVariable.codec("heal", HealInterval::heal)
        ).apply(i, HealInterval::new));

        @Override public ProcessorType<HealInterval> type() { return HEAL.get(); }

        @Override public void process(SelectedEntities selection, EngineContext ctx) {
            int spacing = Math.max(1, interval.eval(ctx));
            float amount = (float) Mth.clamp(heal.eval(ctx), 0, CostRules.DAMAGE.get());
            for (var entity : selection.living()) if (entity.tickCount % spacing == 0) entity.heal(amount);
        }
    }

    public record StackingStatus(Holder<MobEffect> effect, IntVariable duration, IntVariable max) implements SimpleServerProcessor<StackingStatus> {
        static final MapCodec<StackingStatus> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("effect").forGetter(StackingStatus::effect),
                IntVariable.codec("duration", StackingStatus::duration), IntVariable.codec("max", StackingStatus::max)
        ).apply(i, StackingStatus::new));

        @Override public ProcessorType<StackingStatus> type() { return STACKING.get(); }

        @Override public void process(SelectedEntities selection, EngineContext ctx) {
            int cap = Mth.clamp(max.eval(ctx), 0, 10);
            int time = Mth.clamp(duration.eval(ctx), 0, CostRules.DURATION.get().intValue());
            if (cap == 0 || time == 0) return;
            for (var entity : selection.living()) {
                var current = entity.getEffect(effect);
                int level = current == null ? 0 : Math.min(cap - 1, current.getAmplifier() + 1);
                entity.addEffect(new MobEffectInstance(effect, time, level), ctx.user().user());
            }
        }
    }

    public record HurtCooldown(IntVariable frame) implements EntityFilter<HurtCooldown> {
        static final MapCodec<HurtCooldown> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(IntVariable.codec("frame", HurtCooldown::frame)).apply(i, HurtCooldown::new));

        @Override public FilterType<HurtCooldown> type() { return HURT_COOLDOWN.get(); }
        @Override public boolean test(LivingEntity entity, EngineContext ctx) { return entity.hurtTime <= frame.eval(ctx); }
    }

    private GlimmeringNodes() {}
}
