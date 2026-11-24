package cn.nulladev.modularmagic.utility;

import cn.nulladev.modularmagic.energy.EnergyLinkItem;
import cn.nulladev.modularmagic.mana.ManaStorage;
import cn.nulladev.modularmagic.research.PlaceholderItem;
import cn.nulladev.modularmagic.util.AetherNumbers;
import dev.xkmc.l2magic.content.engine.block.BlockUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = cn.nulladev.modularmagic.ModularMagic.MOD_ID)
public final class AetherUtilityItem extends Item implements ManaStorage {
    public enum Kind { PLACE, FOOD, LAUNCH, BURN, TOOL, CANNON }

    @FunctionalInterface
    public interface LaunchAction {
        boolean launch(ServerPlayer player);
    }

    private static final Map<Item, LaunchAction> PROJECTILES = new HashMap<>();
    static {
        registerProjectile(Items.EGG, player -> throwProjectile(player, new ThrownEgg(player.level(), player)));
        registerProjectile(Items.SNOWBALL, player -> throwProjectile(player, new Snowball(player.level(), player)));
        registerProjectile(Items.ENDER_PEARL, player -> throwProjectile(player, new ThrownEnderpearl(player.level(), player)));
    }

    public static void registerProjectile(Item material, LaunchAction action) {
        PROJECTILES.put(material, action);
    }

    private static boolean throwProjectile(ServerPlayer player, ThrowableItemProjectile entity) {
        entity.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 1.5F, 1);
        return player.level().addFreshEntity(entity);
    }

    final Kind kind;

    public AetherUtilityItem(Kind kind) {
        super(new Properties().stacksTo(1));
        this.kind = kind;
    }

    public static ItemStack sample(ItemStack unit) {
        var sample = unit.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyOne();
        if (sample.isEmpty() && unit.is(UtilityContent.CANNON.get())) {
            return PlaceholderItem.create(ResourceLocation.withDefaultNamespace("tnt"));
        }
        return sample;
    }

    public static void setSample(ItemStack unit, ItemStack sample) {
        if (sample.isEmpty()) unit.remove(DataComponents.CONTAINER);
        else unit.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(sample.copyWithCount(1))));
    }

    private ItemStack material(ItemStack unit) {
        return PlaceholderItem.material(sample(unit));
    }

    public boolean accepts(ItemStack stack, Player player) {
        if (PlaceholderItem.price(stack) < 0 || (!player.isCreative() && stack.is(cn.nulladev.modularmagic.research.PlaceholderContent.CREATIVE.get()))) return false;
        var material = PlaceholderItem.material(stack);
        return switch (kind) {
            case PLACE -> !PlaceholderItem.block(stack).defaultBlockState().isAir();
            case FOOD -> material.getFoodProperties(player) != null;
            case LAUNCH -> material.is(Items.BONE_MEAL) || PROJECTILES.containsKey(material.getItem());
            case BURN -> material.getBurnTime(RecipeType.SMELTING) > 0 && material.getCraftingRemainingItem().isEmpty();
            case TOOL -> material.getItem() instanceof TieredItem || material.getItem() instanceof ShearsItem;
            case CANNON -> false;
        };
    }

    private long cost(ItemStack stack) {
        var sample = sample(stack);
        long price = PlaceholderItem.price(sample);
        if (kind == Kind.TOOL && price >= 0) {
            int durability = Math.max(1, PlaceholderItem.material(sample).getMaxDamage());
            return Math.max(1, (long)Math.ceil((double)price / durability));
        }
        return price;
    }

    private boolean canPay(Player player, ItemStack stack, long fee) {
        return fee >= 0 && (player.isCreative() || stored(stack) >= fee);
    }

    private void pay(Player player, ItemStack stack, long fee) {
        if (!player.isCreative()) extract(stack, fee, false);
        player.getInventory().setChanged();
    }

    @Override
    public long capacity(ItemStack stack) {
        return UtilityConfig.CACHE.get();
    }

    @Override
    public long stored(ItemStack stack) {
        return Math.clamp(EnergyLinkItem.data(stack).getLong("UnitAether"), 0, capacity(stack));
    }

    @Override
    public long receive(ItemStack stack, long requested, boolean simulate) {
        long amount = Math.clamp(requested, 0, capacity(stack) - stored(stack));
        if (!simulate && amount > 0) setStored(stack, stored(stack) + amount);
        return amount;
    }

    @Override
    public long extract(ItemStack stack, long requested, boolean simulate) {
        long amount = Math.clamp(requested, 0, stored(stack));
        if (!simulate && amount > 0) setStored(stack, stored(stack) - amount);
        return amount;
    }

    private void setStored(ItemStack stack, long amount) {
        var tag = EnergyLinkItem.data(stack);
        tag.putLong("UnitAether", amount);
        EnergyLinkItem.data(stack, tag);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && kind != Kind.CANNON) {
            if (player instanceof ServerPlayer server) server.openMenu(new SimpleMenuProvider(
                    (id, inventory, user) -> new UnitSampleMenu(id, inventory, hand),
                    Component.translatable("gui.modularmagic.unit_sample")), buffer -> buffer.writeEnum(hand));
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        long fee = cost(stack);
        if (kind == Kind.FOOD) {
            return canPay(player, stack, fee) ? super.use(level, player, hand) : InteractionResultHolder.fail(stack);
        }
        if (kind != Kind.LAUNCH && kind != Kind.CANNON) return InteractionResultHolder.pass(stack);
        var action = PROJECTILES.get(material(stack).getItem());
        if (kind != Kind.CANNON && action == null) return InteractionResultHolder.pass(stack);
        if (!canPay(player, stack, fee) || player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        if (player instanceof ServerPlayer server) {
            if (kind == Kind.CANNON) propel(server);
            else if (!action.launch(server)) return InteractionResultHolder.fail(stack);
            pay(player, stack, fee);
            player.getCooldowns().addCooldown(this, kind == Kind.CANNON ? UtilityConfig.CANNON_COOLDOWN.get() : 10);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static void propel(ServerPlayer player) {
        var look = player.getLookAngle();
        double speed = UtilityConfig.CANNON_SPEED.get();
        double lift = UtilityConfig.CANNON_LIFT.get();
        player.setDeltaMovement(player.getDeltaMovement().add(look.x * speed, Math.max(lift, look.y * speed + lift), look.z * speed));
        player.setOnGround(false);
        player.hasImpulse = true;
        player.hurtMarked = true;
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
        var level = player.serverLevel();
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, player.getX(), player.getY(), player.getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, .8F, 1.1F);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) return use(context.getLevel(), player, context.getHand()).getResult();
        var unit = context.getItemInHand();
        var material = material(unit);
        var level = context.getLevel();
        if (kind == Kind.BURN) return smelt(context, material);
        if (kind != Kind.PLACE && kind != Kind.TOOL && !(kind == Kind.LAUNCH && material.is(Items.BONE_MEAL))) return InteractionResult.PASS;
        long fee = cost(unit);
        if (!canPay(player, unit, fee)) return InteractionResult.FAIL;
        if (!modifiable(context, kind == Kind.PLACE ? new BlockPlaceContext(context).getClickedPos() : context.getClickedPos())) return InteractionResult.FAIL;
        if (kind == Kind.PLACE && material.isEmpty()) {
            var placement = new BlockPlaceContext(context);
            var target = PlaceholderItem.block(sample(unit));
            if (target != Blocks.FIRE || !placement.canPlace()) return InteractionResult.PASS;
            var state = target.getStateForPlacement(placement);
            if (state == null || !state.canSurvive(level, placement.getClickedPos())) return InteractionResult.FAIL;
            if (!level.isClientSide) {
                if (!level.setBlockAndUpdate(placement.getClickedPos(), state)) return InteractionResult.FAIL;
                pay(player, unit, fee);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (kind == Kind.PLACE && material.getItem() instanceof DispensibleContainerItem bucket) {
            var pos = context.getClickedPos().relative(context.getClickedFace());
            if (!modifiable(context, pos)) return InteractionResult.FAIL;
            if (!level.isClientSide) {
                if (!bucket.emptyContents(player, level, pos, null)) return InteractionResult.FAIL;
                pay(player, unit, fee);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        var virtual = new MaterialUseContext(context, material);
        var result = material.getItem().useOn(virtual);
        if (result.consumesAction() && !level.isClientSide) pay(player, unit, fee);
        return result;
    }

    private InteractionResult smelt(UseOnContext context, ItemStack fuel) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var original = level.getBlockState(pos);
        if (!modifiable(context, pos) || original.hasBlockEntity()) return InteractionResult.FAIL;
        int burn = fuel.getBurnTime(RecipeType.SMELTING);
        if (burn <= 0) return InteractionResult.PASS;
        var input = new SingleRecipeInput(new ItemStack(original.getBlock()));
        var match = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, level);
        if (match.isEmpty()) return InteractionResult.PASS;
        var recipe = match.get().value();
        var product = recipe.assemble(input, level.registryAccess());
        if (product.isEmpty()) return InteractionResult.PASS;
        long fee = (long)Math.ceil((double)cost(context.getItemInHand()) * recipe.getCookingTime() / burn);
        var player = context.getPlayer();
        if (!canPay(player, context.getItemInHand(), fee)) return InteractionResult.FAIL;
        if (!level.isClientSide) {
            BlockState result = Blocks.AIR.defaultBlockState();
            if (product.getCount() == 1 && product.getItem() instanceof BlockItem block
                    && block.getBlock().defaultBlockState().canSurvive(level, pos)) result = block.getBlock().defaultBlockState();
            if (!level.setBlockAndUpdate(pos, result)) return InteractionResult.FAIL;
            if (result.isAir()) Block.popResource(level, pos, product);
            pay(player, context.getItemInHand(), fee);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean modifiable(UseOnContext context, BlockPos pos) {
        var level = context.getLevel();
        var player = context.getPlayer();
        return player != null && player.mayBuild() && level.hasChunkAt(pos) && level.getWorldBorder().isWithinBounds(pos)
                && (!(player instanceof ServerPlayer server) || (!server.getServer().isUnderSpawnProtection(server.serverLevel(), pos, player)
                && BlockUtils.allowModification(level, pos)));
    }

    private static final class MaterialUseContext extends UseOnContext {
        MaterialUseContext(UseOnContext from, ItemStack sample) {
            super(from.getLevel(), from.getPlayer(), from.getHand(), sample,
                    new BlockHitResult(from.getClickLocation(), from.getClickedFace(), from.getClickedPos(), from.isInside()));
        }
    }

    @Override
    public FoodProperties getFoodProperties(ItemStack stack, LivingEntity entity) {
        return kind == Kind.FOOD ? material(stack).getFoodProperties(entity) : null;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return kind == Kind.FOOD ? UseAnim.EAT : super.getUseAnimation(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (kind == Kind.FOOD && entity instanceof ServerPlayer player) {
            var food = getFoodProperties(stack, entity);
            long fee = cost(stack);
            if (food != null && player.canEat(food.canAlwaysEat()) && canPay(player, stack, fee)) {
                var remainder = material(stack).finishUsingItem(level, player);
                if (!player.isCreative() && !remainder.isEmpty() && !player.addItem(remainder)) player.drop(remainder, false);
                pay(player, stack, fee);
            }
        }
        return stack;
    }

    @Override
    public int getBurnTime(ItemStack stack, RecipeType<?> recipeType) {
        if (kind != Kind.BURN || cost(stack) < 0 || stored(stack) < cost(stack)) return 0;
        return material(stack).getBurnTime(recipeType);
    }

    public static boolean consumeFuel(ItemStack stack) {
        if (!(stack.getItem() instanceof AetherUtilityItem unit) || unit.kind != Kind.BURN) return false;
        long fee = unit.cost(stack);
        if (fee >= 0) unit.extract(stack, fee, false);
        return true;
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return kind == Kind.TOOL ? material(stack).getAttributeModifiers() : super.getDefaultAttributeModifiers(stack);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return kind == Kind.TOOL && stored(stack) >= cost(stack) ? material(stack).getDestroySpeed(state) : 1;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return kind == Kind.TOOL && stored(stack) >= cost(stack) && material(stack).isCorrectToolForDrops(state);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (kind != Kind.TOOL || !(attacker instanceof ServerPlayer player) || !canPay(player, stack, cost(stack))) return false;
        pay(player, stack, cost(stack));
        return true;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        if (kind != Kind.TOOL || !(miner instanceof ServerPlayer player) || !canPay(player, stack, cost(stack))) return false;
        pay(player, stack, cost(stack));
        return true;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (kind != Kind.TOOL || !canPay(player, stack, cost(stack))) return InteractionResult.PASS;
        var result = material(stack).interactLivingEntity(player, target, hand);
        if (!player.level().isClientSide && result.consumesAction()) pay(player, stack, cost(stack));
        return result;
    }

    @SubscribeEvent
    public static void checkAttack(AttackEntityEvent event) {
        var player = event.getEntity();
        var stack = player.getMainHandItem();
        if (stack.getItem() instanceof AetherUtilityItem unit && unit.kind == Kind.TOOL
                && !unit.canPay(player, stack, unit.cost(stack))) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void checkBreak(BlockEvent.BreakEvent event) {
        var player = event.getPlayer();
        var stack = player.getMainHandItem();
        if (stack.getItem() instanceof AetherUtilityItem unit && unit.kind == Kind.TOOL
                && !unit.canPay(player, stack, unit.cost(stack))) event.setCanceled(true);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        var sample = sample(stack);
        if (!sample.isEmpty()) {
            lines.add(Component.translatable("tooltip.modularmagic.unit_sample", sample.getHoverName()));
            sample.getItem().appendHoverText(sample, context, lines, flag);
        }
        lines.add(Component.translatable("tooltip.modularmagic.unit_cache", AetherNumbers.format(stored(stack)), AetherNumbers.format(capacity(stack))));
    }
}
