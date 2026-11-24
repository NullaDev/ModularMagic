package cn.nulladev.modularmagic.energy;

import cn.nulladev.modularmagic.mana.ManaStorage;
import cn.nulladev.modularmagic.research.PlaceholderItem;
import cn.nulladev.modularmagic.utility.UtilityConfig;
import cn.nulladev.modularmagic.util.AetherNumbers;
import cn.nulladev.modularmagic.spell.billing.ManaEscrow;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.*;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import java.util.UUID;

/** A single stored balance, with independent shared input/output quotas. All amounts are fixed-point units. */
public final class EnergyBlockEntity extends BlockEntity implements WorldlyContainer {
    public long energy;
    public final net.minecraft.world.SimpleContainer enchantInventory=new net.minecraft.world.SimpleContainer(28){@Override public void setChanged(){super.setChanged();EnergyBlockEntity.this.changed();}};
    public final cn.nulladev.modularmagic.generator.GeneratorState generator=new cn.nulladev.modularmagic.generator.GeneratorState(this);
    public UUID identity = UUID.randomUUID(), owner;
    public ItemStack battery = ItemStack.EMPTY;
    public ItemStack product = ItemStack.EMPTY;
    public ItemStack emptyBuckets = ItemStack.EMPTY;
    public BlockPos target;
    public UUID targetIdentity;
    public BlockPos resonanceParent;
    public UUID resonanceParentIdentity;
    public int channel, loaderRadius, resonanceUpgrades;
    public boolean crossDimension, outputPaused, loaderEnabled;
    public long pulseAmount;
    public long pulseDelivered;
    public int pulseAge, pulseDuration;
    public Vec3 pulseStart, pulseEnd;
    private boolean dirty;
    private long quotaWindow = Long.MIN_VALUE, inputUsed, outputUsed;
    public int appliedRadius = -1;
    public long offlineSince = -1;
    public float displayedFill;

    public EnergyBlockEntity(BlockPos pos, BlockState state) { super(EnergyContent.ENTITY.get(),pos,state); }
    public EnergyContent.Kind kind() { return ((EnergyBlock)getBlockState().getBlock()).kind; }
    public long capacity() {
        long mana = switch(kind()) {
            case ENCHANTER -> cn.nulladev.modularmagic.crafting.ArtificeConfig.ENCHANT_CAPACITY.get();
            case MATERIALIZER -> UtilityConfig.MATERIALIZER_CAPACITY.get();
            case GENERATOR -> generator.rule()==null?1:generator.rule().capacity.get();
            case DISRUPTOR -> EnergyConfig.DISRUPTOR_CAPACITY.get();
            case TANK -> EnergyConfig.BASIC_CAPACITY.get();
            case EXPANDED_TANK -> EnergyConfig.EXPANDED_CAPACITY.get();
            case ULTIMATE_TANK -> EnergyConfig.ULTIMATE_CAPACITY.get();
            case CRYSTAL_CHAMBER -> EnergyConfig.CHAMBER_CAPACITY.get();
            case CONDENSER -> EnergyConfig.CONDENSER_CAPACITY.get();
            case CREATIVE, CREATIVE_TANK, CREATIVE_EMITTER -> EnergyConfig.CREATIVE_CAPACITY.get();
            case WORLD_ANCHOR -> EnergyConfig.LOADER_CAPACITY.get();
            case RESONATOR -> EnergyConfig.RESONATOR_CAPACITY.get();
            case ANCHOR -> EnergyConfig.ANCHOR_CAPACITY.get();
            default -> EnergyConfig.EMITTER_CAPACITY.get();
        };
        return mana * ManaStorage.SCALE;
    }
    public long rate() { return switch(kind()) {
        case ENCHANTER -> cn.nulladev.modularmagic.crafting.ArtificeConfig.ENCHANT_RATE.get();
        case MATERIALIZER -> UtilityConfig.MATERIALIZER_RATE.get();
        case GENERATOR -> generator.rule()==null?0:generator.rule().output.get();
        case DISRUPTOR -> EnergyConfig.DISRUPTOR_RATE.get();
        case TANK -> EnergyConfig.BASIC_RATE.get();
        case EXPANDED_TANK -> EnergyConfig.EXPANDED_RATE.get();
        case ULTIMATE_TANK -> EnergyConfig.ULTIMATE_RATE.get();
        case CREATIVE, CREATIVE_TANK -> EnergyConfig.CREATIVE_RATE.get();
        case CRYSTAL_CHAMBER -> EnergyConfig.CHAMBER_RATE.get();
        case CONDENSER -> EnergyConfig.CONDENSER_RATE.get();
        case EMITTER, EXPANDED_EMITTER, ULTIMATE_EMITTER, CREATIVE_EMITTER -> (long)shotSize()*20/EnergyConfig.SHOT_INTERVAL.get();
        case RESONATOR -> resonanceBandwidth();
        case WORLD_ANCHOR -> EnergyConfig.LOADER_INPUT_RATE.get();
        case ANCHOR -> EnergyConfig.ANCHOR_RATE.get();
    }; }
    public int resonanceLevel() {return Math.clamp(resonanceUpgrades,0,EnergyConfig.RESONANCE_UPGRADES.get());}
    public boolean resonanceEnabled() {return resonanceLevel()>0;}
    public long resonanceBandwidth() {return resonanceLevel()*EnergyConfig.RESONANCE_UPGRADE_RATE.get();}
    public int resonanceRange() {return resonanceEnabled()?EnergyConfig.RESONANCE_RANGE.get()+resonanceLevel()*EnergyConfig.RESONANCE_RANGE_PER_SHARD.get():0;}
    public double resonanceLosslessDistance() {return Math.min(resonanceRange(),EnergyConfig.RESONANCE_FREE_DISTANCE.get()+resonanceLevel()*EnergyConfig.RESONANCE_FREE_PER_SHARD.get());}
    public double resonanceEfficiency(double distance) {return resonanceEnabled()?EnergyMath.efficiency(EnergyConfig.RESONANCE_LOSS.get(),distance,resonanceLosslessDistance(),resonanceRange(),resonanceLevel()):0;}
    private void resetQuota() {
        long window = level == null ? 0 : level.getGameTime()/quotaTicks();
        if(window!=quotaWindow) { quotaWindow=window; inputUsed=outputUsed=0; }
    }
    private int quotaTicks() { return kind().emitter()?EnergyConfig.SHOT_INTERVAL.get():5; }
    private long quotaLimit() { return kind().emitter()?(long)shotSize()*ManaStorage.SCALE:EnergyMath.quarterUnits(rate(),quotaWindow); }
    public int shotSize() { return switch(kind()) {case EXPANDED_EMITTER -> EnergyConfig.EXPANDED_SHOT.get();case ULTIMATE_EMITTER -> EnergyConfig.ULTIMATE_SHOT.get();case CREATIVE_EMITTER -> EnergyConfig.CREATIVE_SHOT.get();default -> EnergyConfig.SHOT.get();}; }
    public int shotRange() { return switch(kind()) {case EXPANDED_EMITTER -> EnergyConfig.EXPANDED_RANGE.get();case ULTIMATE_EMITTER -> EnergyConfig.ULTIMATE_RANGE.get();case CREATIVE_EMITTER -> EnergyConfig.CREATIVE_RANGE.get();default -> EnergyConfig.RANGE.get();}; }
    public Vec3 emissionPivot() { return Vec3.atLowerCornerOf(worldPosition).add(.5,.6,.5); }
    public Vec3 emissionDirection() {
        if(target!=null) return Vec3.atCenterOf(target).subtract(emissionPivot()).normalize();
        var facing=getBlockState().getValue(EnergyBlock.FACING);return new Vec3(facing.getStepX(),0,facing.getStepZ());
    }
    public Vec3 emissionStart() {return emissionPivot().add(emissionDirection().scale(.96));}
    public long room() { return kind().infinite()?0:Math.max(0,capacity()-energy-pulseAmount-generator.reserved-EnergyWorld.incoming(this)); }
    public long receive(long requested, boolean simulate) {
        if(kind()==EnergyContent.Kind.GENERATOR)return 0;
        resetQuota();long amount=Math.max(0,Math.min(requested,Math.min(room(),quotaLimit()-inputUsed)));
        if(!simulate&&amount>0) { energy+=amount;inputUsed+=amount;changed(); }return amount;
    }
    public long extract(long requested, boolean simulate) {
        resetQuota();long amount=paused()?0:Math.max(0,Math.min(requested,Math.min(kind().infinite()?Long.MAX_VALUE:energy,quotaLimit()-outputUsed)));
        if(!simulate&&amount>0) { if(!kind().infinite())energy-=amount;outputUsed+=amount;changed(); }return amount;
    }
    public void refund(long amount) {if(amount>0&&!kind().infinite()){energy+=amount;changed();}}
    public long crystalCost() {return (EnergyConfig.SHARD.get()+EnergyConfig.CRYSTAL_FEE.get())*ManaStorage.SCALE;}
    public boolean canReceiveFragments() {return kind().tank()&&!kind().infinite();}
    public boolean allowed(Player player) { return owner==null || owner.equals(player.getUUID()) || player.hasPermissions(2); }
    public boolean paused() { return outputPaused || level!=null&&level.hasNeighborSignal(worldPosition); }
    public boolean sameOwner(EnergyBlockEntity other) { return java.util.Objects.equals(owner,other.owner); }
    public void changed() { setChanged();dirty=true; }
    @Override public void onLoad() { super.onLoad();if(level instanceof ServerLevel server) EnergyWorld.add(server,worldPosition); }
    @Override public void onChunkUnloaded() { if(level instanceof ServerLevel server) EnergyWorld.remove(server,worldPosition);super.onChunkUnloaded(); }
    @Override public void setRemoved() { if(level instanceof ServerLevel server) EnergyWorld.remove(server,worldPosition);super.setRemoved(); }

    public boolean waterStructure() {
        return level!=null&&level.getFluidState(worldPosition.above()).is(FluidTags.WATER)&&canInterceptWater();
    }
    public boolean canInterceptWater() {
        if(level==null||!kind().disrupts()||!level.getGameRules().getBoolean(GameRules.RULE_WATER_SOURCE_CONVERSION))return false;
        BlockPos work=worldPosition.above();
        var fluid=level.getFluidState(work);
        if(fluid.isSource()||(!fluid.isEmpty()&&!fluid.is(FluidTags.WATER)))return false;
        for(Direction direction:java.util.List.of(Direction.NORTH,Direction.EAST)) {
            BlockPos a=work.relative(direction),b=work.relative(direction.getOpposite());
            if(level.hasChunkAt(a)&&level.hasChunkAt(b)&&level.getFluidState(a).is(FluidTags.WATER)&&level.getFluidState(a).isSource()
                    &&level.getFluidState(b).is(FluidTags.WATER)&&level.getFluidState(b).isSource())return true;
        }
        return false;
    }
    public void tick() {
        if(level==null)return;
        if(level.isClientSide) {
            float target=(float)Math.min(1,(double)energy/capacity());
            if(kind()==EnergyContent.Kind.CRYSTAL_CHAMBER)target=Math.max(target,product.getCount()/64F);
            displayedFill += (target-displayedFill)*.2F;
            if(pulseAmount>0)pulseAge=Math.min(pulseAge+1,pulseDuration);
            return;
        }
        long time=level.getGameTime();
        if(kind().infinite() && energy!=capacity()) { energy=capacity();changed(); }
        boolean active=false;
        if(kind()==EnergyContent.Kind.GENERATOR){generator.tick();active=generator.signal>=3&&generator.signal<15;}
        if(kind().disrupts()) {
            active=waterStructure();
            if(active&&time%EnergyConfig.PASSIVE_INTERVAL.get()==0) { energy=Math.min(capacity(),energy+EnergyConfig.PASSIVE.get()*ManaStorage.SCALE);changed(); }
            if(active&&time%10==0)((ServerLevel)level).sendParticles(ParticleTypes.END_ROD,worldPosition.getX()+.5,worldPosition.getY()+1.15,worldPosition.getZ()+.5,1,.12,.12,.12,0);
        }
        if(kind().emitter()) { tickPulse();active=pulseAmount>0; }
        if(time%5==0) {
            if(kind()==EnergyContent.Kind.WORLD_ANCHOR) EnergyWorld.tickLoader(this);
            chargeBattery();
        }
        if(kind()==EnergyContent.Kind.CRYSTAL_CHAMBER && !paused()) {
            if(time%EnergyConfig.CRYSTAL_INTERVAL.get()==0) crystallizeAether();
            if(time%EnergyConfig.STABILIZE_INTERVAL.get()==0) crystalizeNearby();
            active=!product.isEmpty()||energy>=crystalCost();
        }
        if(kind()==EnergyContent.Kind.CONDENSER)active=energy>=EnergyConfig.SHARD.get()*ManaStorage.SCALE;
        if(kind()==EnergyContent.Kind.MATERIALIZER && !paused() && time%UtilityConfig.MATERIALIZER_INTERVAL.get()==0) {
            materialize();
        }
        if(kind()==EnergyContent.Kind.WORLD_ANCHOR)active=appliedRadius>=0;
        if((kind().infinite()||kind()==EnergyContent.Kind.RESONATOR||kind()==EnergyContent.Kind.ANCHOR)&&energy>0)active=!outputPaused;
        if(kind()==EnergyContent.Kind.RESONATOR&&!resonanceEnabled())active=false;
        if(getBlockState().getValue(EnergyBlock.ACTIVE)!=active)level.setBlock(worldPosition,getBlockState().setValue(EnergyBlock.ACTIVE,active),3);
        if(dirty&&time%5==0) { dirty=false;level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);level.updateNeighbourForOutputSignal(worldPosition,getBlockState().getBlock()); }
    }
    private void materialize() {
        var generated = PlaceholderItem.material(battery);
        long price = PlaceholderItem.price(battery);
        if (generated.isEmpty() || price < 0 || price > energy / ManaStorage.SCALE) return;
        boolean bucket = generated.is(Items.WATER_BUCKET) || generated.is(Items.LAVA_BUCKET) || generated.is(Items.POWDER_SNOW_BUCKET);
        if (bucket && emptyBuckets.isEmpty()) return;
        if (!product.isEmpty() && (!ItemStack.isSameItemSameComponents(product, generated)
                || product.getCount() >= product.getMaxStackSize())) return;
        energy -= price * ManaStorage.SCALE;
        if (bucket) emptyBuckets.shrink(1);
        if (product.isEmpty()) product = generated;
        else product.grow(1);
        changed();
    }

    private void chargeBattery() {
        if(!kind().tank()||battery.isEmpty()||!(battery.getItem() instanceof ManaStorage storage)||paused())return;
        ManaEscrow.get(level.getServer()).claim(battery);
        long moved=storage.receiveUnits(battery,extract(Long.MAX_VALUE,true),true);
        if(moved>0) { long taken=extract(moved,false);long accepted=storage.receiveUnits(battery,taken,false);refund(taken-accepted);changed(); }
    }
    private void crystalizeNearby() {
        if(!productRoom())return;
        long fee=EnergyConfig.CRYSTAL_FEE.get()*ManaStorage.SCALE;
        if(energy<fee)return;
        for(var item:level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(worldPosition.above()))) {
            var stack=item.getItem();
            if(!stack.is(EnergyContent.SHARD.get())||AetherShardItem.expired(stack,level))continue;
            var result=AetherShardItem.create(true,level);
            var value=AetherShardItem.value(stack);var tag=EnergyLinkItem.data(result);tag.putLong("AetherValue",value);EnergyLinkItem.data(result,tag);
            if(!product.isEmpty()&&!ItemStack.isSameItemSameComponents(product,result))continue;
            energy-=fee;stack.shrink(1);if(stack.isEmpty())item.discard();else item.setItem(stack.copy());
            storeProduct(result);changed();break;
        }
    }
    private boolean productRoom() {
        return product.isEmpty()||product.getCount()<64&&ItemStack.isSameItemSameComponents(product,AetherShardItem.create(true,level));
    }
    private void storeProduct(ItemStack stack) { if(product.isEmpty())product=stack;else product.grow(stack.getCount());changed(); }
    private void crystallizeAether() {
        if(!productRoom())return;
        long cost=crystalCost();
        int count=(int)Math.min(Math.min(EnergyConfig.CRYSTAL_BATCH.get(),64-product.getCount()),energy/cost);
        if(count<=0)return;
        var stack=AetherShardItem.create(true,level);stack.setCount(count);energy-=cost*count;storeProduct(stack);
    }
    private boolean pathClear() {
        if(target==null||pulseStart==null||pulseEnd==null)return false;
        for(int i=0;i<=Math.ceil(pulseStart.distanceTo(pulseEnd)*2);i++) {
            var p=BlockPos.containing(pulseStart.lerp(pulseEnd,i/Math.max(1,Math.ceil(pulseStart.distanceTo(pulseEnd)*2))));
            if(!level.hasChunkAt(p))return false;
        }
        var hit=level.clip(new ClipContext(pulseStart,pulseEnd,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,net.minecraft.world.phys.shapes.CollisionContext.empty()));
        return hit.getType()==HitResult.Type.MISS||hit.getBlockPos().equals(target);
    }
    public boolean startPulse() {
        if(pulseAmount!=0||target==null||paused()||worldPosition.distSqr(target)>shotRange()*shotRange()||!(level instanceof ServerLevel server))return false;
        var dest=EnergyWorld.device(server,target);
        if(dest==null||!java.util.Objects.equals(targetIdentity,dest.identity)||!sameOwner(dest)||target.equals(worldPosition)||!ResonanceNetwork.accepts(this,dest))return false;
        pulseStart=emissionStart();pulseEnd=Vec3.atCenterOf(target);
        if(!pathClear())return false;
        long amount=Math.min(extract((long)shotSize()*ManaStorage.SCALE,true),dest.receive(Long.MAX_VALUE,true));
        if(amount<=0)return false;
        double efficiency=EnergyMath.efficiency(EnergyConfig.PULSE_LOSS.get(),Math.sqrt(worldPosition.distSqr(target)),EnergyConfig.PULSE_FREE_DISTANCE.get(),shotRange(),0);
        long delivered=EnergyMath.delivered(amount,efficiency);if(delivered<=0)return false;
        pulseAmount=extract(amount,false);pulseDelivered=delivered;pulseAge=0;
        pulseDuration=Math.max(1,(int)Math.ceil(pulseStart.distanceTo(pulseEnd)/EnergyConfig.PULSE_SPEED.get()));changed();return true;
    }
    private void tickPulse() {
        if(pulseAmount>0) {
            if(!pathClear()) { cancelPulse();return; }
            if(++pulseAge>=pulseDuration) {
                var dest=EnergyWorld.device((ServerLevel)level,target);
                long amount=pulseAmount,delivery=pulseDelivered;pulseAmount=pulseDelivered=0;
                long accepted=dest!=null&&java.util.Objects.equals(dest.identity,targetIdentity)&&sameOwner(dest)&&ResonanceNetwork.accepts(this,dest)?dest.receive(delivery,false):0;
                long cost=delivery==0?0:java.math.BigDecimal.valueOf(amount).multiply(java.math.BigDecimal.valueOf(accepted)).divide(java.math.BigDecimal.valueOf(delivery),0,java.math.RoundingMode.CEILING).longValue();
                refund(amount-cost);pulseAge=0;changed();
            }
        } else if(level.getGameTime()%EnergyConfig.SHOT_INTERVAL.get()==0)startPulse();
    }
    public void cancelPulse() { if(pulseAmount>0){refund(pulseAmount);pulseAmount=pulseDelivered=0;pulseAge=0;changed();} }

    public boolean interact(Player player, InteractionHand hand) {
        if(!allowed(player)){player.displayClientMessage(Component.translatable("energy.modularmagic.denied"),true);return true;}
        var held=player.getItemInHand(hand);
        if(kind()==EnergyContent.Kind.GENERATOR&&held.isEmpty()){
            player.displayClientMessage(Component.translatable("generator.modularmagic.comparator",
                    Math.max(0, generator.cooldown), generator.comparator()), true);
            return true;
        }
        if(held.getItem() instanceof ManaStorage) {
            if(!kind().tank()) {player.displayClientMessage(Component.translatable("energy.modularmagic.use_reservoir"),true);return true;}
            if(battery.isEmpty()) { battery=held.copyWithCount(1);held.shrink(1);changed(); }
            else player.displayClientMessage(Component.translatable("energy.modularmagic.battery_busy"),true);
            return true;
        }
        if(held.getItem() instanceof AetherShardItem) {
            if(AetherShardItem.expired(held,level)){held.setCount(0);return true;}
            if(!canReceiveFragments())return false;
            long amount=AetherShardItem.value(held)*ManaStorage.SCALE;
            if(room()>=amount) { energy+=amount;held.shrink(1);changed(); }return true;
        }
        if(held.is(Items.AMETHYST_SHARD)&&kind()==EnergyContent.Kind.RESONATOR) {
            int installed=Math.min(EnergyConfig.RESONANCE_UPGRADES.get()-resonanceUpgrades,player.isShiftKeyDown()?held.getCount():1);
            if(installed>0){resonanceUpgrades+=installed;if(!player.isCreative())held.shrink(installed);changed();}
            player.displayClientMessage(Component.translatable("energy.modularmagic.resonance_upgrades",resonanceUpgrades,EnergyConfig.RESONANCE_UPGRADES.get(),AetherNumbers.format(resonanceBandwidth())),true);return true;
        }
        if(held.is(EnergyContent.CROSS_LENS.get())&&kind()==EnergyContent.Kind.ANCHOR&&!crossDimension) { crossDimension=true;held.shrink(1);changed();return true; }
        if(!held.isEmpty())return false;
        if(player.isShiftKeyDown()&&kind()==EnergyContent.Kind.RESONATOR&&resonanceUpgrades>0) {
            var shards=new ItemStack(Items.AMETHYST_SHARD,resonanceUpgrades);resonanceUpgrades=0;
            if(!player.getInventory().add(shards))player.drop(shards,false);changed();return true;
        }
        if(player.isShiftKeyDown()&&!battery.isEmpty()) {
            var taken=battery;battery=ItemStack.EMPTY;
            if(!player.getInventory().add(taken))player.drop(taken,false);changed();return true;
        }
        if(kind()==EnergyContent.Kind.CRYSTAL_CHAMBER) {
            if(product.isEmpty()&&!paused())crystallizeAether();
            if(!product.isEmpty()) {
                var result=product.split(player.isShiftKeyDown()?product.getCount():1);
                if(!player.getInventory().add(result))player.drop(result,false);changed();
            } else player.displayClientMessage(Component.translatable("energy.modularmagic.crystal_waiting"),true);
        } else if(kind()==EnergyContent.Kind.DISRUPTOR||kind()==EnergyContent.Kind.CONDENSER) {
            long cost=EnergyConfig.SHARD.get()*ManaStorage.SCALE;
            int count=(int)Math.min(player.isShiftKeyDown()?EnergyConfig.BATCH_EXTRACT.get():1,energy/cost);
            if(count>0) { energy-=cost*count;var shard=AetherShardItem.create(false,level);shard.setCount(count);if(!player.getInventory().add(shard))player.drop(shard,false);changed(); }
            else player.displayClientMessage(Component.translatable("energy.modularmagic.need_fragment",AetherNumbers.format(cost/ManaStorage.SCALE)),true);
        } else if(kind()==EnergyContent.Kind.WORLD_ANCHOR) {
            loaderEnabled=!loaderEnabled;changed();if(!loaderEnabled)EnergyWorld.release(this);
            player.displayClientMessage(Component.translatable(loaderEnabled?"energy.modularmagic.loader_enabled":"energy.modularmagic.loader_disabled",AetherNumbers.format(EnergyConfig.LOADER_START.get())),true);
        } else player.displayClientMessage(status(),true);
        return true;
    }
    public Component status() {
        if(!kind().tank())return Component.translatable("energy.modularmagic.device_status",AetherNumbers.format(energy/ManaStorage.SCALE),AetherNumbers.format(capacity()/ManaStorage.SCALE));
        return Component.translatable("energy.modularmagic.status",AetherNumbers.format(energy/ManaStorage.SCALE),AetherNumbers.format(capacity()/ManaStorage.SCALE),battery.isEmpty()?Component.translatable("energy.modularmagic.no_battery"):battery.getHoverName());
    }
    public CompoundTag itemData() {
        var tag=new CompoundTag();tag.put("Generator",generator.save());tag.putLong("Energy",energy+pulseAmount);tag.putUUID("Identity",identity);
        tag.putInt("ResonanceUpgrades",resonanceUpgrades);
        tag.putInt("Channel",channel);tag.putBoolean("CrossDimension",crossDimension);tag.putInt("Radius",loaderRadius);
        if(owner!=null)tag.putUUID("Owner",owner);return tag;
    }
    public void restoreItem(CompoundTag tag) {
        if(tag.contains("Generator"))generator.load(tag.getCompound("Generator"));
        energy=Math.max(0,tag.getLong("Energy"));if(tag.hasUUID("Identity"))identity=tag.getUUID("Identity");
        resonanceUpgrades=Math.clamp(tag.getInt("ResonanceUpgrades"),0,64);
        if(tag.hasUUID("Owner"))owner=tag.getUUID("Owner");channel=Math.clamp(tag.getInt("Channel"),0,15);
        crossDimension=tag.getBoolean("CrossDimension");loaderRadius=Math.clamp(tag.getInt("Radius"),0,1);
    }
    public ItemStack asItem() {
        var stack=new ItemStack(getBlockState().getBlock());var tag=new CompoundTag();var data=itemData();
        if(kind()==EnergyContent.Kind.ENCHANTER||kind().tank()||kind()==EnergyContent.Kind.CRYSTAL_CHAMBER||kind()==EnergyContent.Kind.CONDENSER||kind()==EnergyContent.Kind.GENERATOR)data.putLong("Energy",0);
        if(kind()==EnergyContent.Kind.GENERATOR){var g=data.getCompound("Generator");g.putLong("Reserved",0);if(generator.ruleId.equals("curing"))g.remove("Actor");}
        tag.put("AetherDevice",data);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return stack;
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag,registries);
        if(kind()==EnergyContent.Kind.ENCHANTER){var items=net.minecraft.core.NonNullList.withSize(28,ItemStack.EMPTY);for(int i=0;i<28;i++)items.set(i,enchantInventory.getItem(i));net.minecraft.world.ContainerHelper.saveAllItems(tag,items,registries);}tag.merge(itemData());tag.putLong("Energy",energy);
        if(!battery.isEmpty())tag.put("Battery",battery.save(registries));
        if(!product.isEmpty())tag.put("Product",product.save(registries));
        if(!emptyBuckets.isEmpty())tag.put("EmptyBuckets",emptyBuckets.save(registries));
        tag.putBoolean("OutputPaused",outputPaused);tag.putBoolean("LoaderEnabled",loaderEnabled);
        if(resonanceParent!=null)tag.putLong("ResonanceParent",resonanceParent.asLong());
        if(resonanceParentIdentity!=null)tag.putUUID("ResonanceParentIdentity",resonanceParentIdentity);
        if(target!=null)tag.putLong("Target",target.asLong());if(targetIdentity!=null)tag.putUUID("TargetIdentity",targetIdentity);
        tag.putLong("Pulse",pulseAmount);tag.putLong("PulseDelivered",pulseDelivered);tag.putInt("PulseAge",pulseAge);tag.putInt("PulseDuration",pulseDuration);
        if(pulseStart!=null) {tag.putDouble("SX",pulseStart.x);tag.putDouble("SY",pulseStart.y);tag.putDouble("SZ",pulseStart.z);}
        if(pulseEnd!=null) {tag.putDouble("EX",pulseEnd.x);tag.putDouble("EY",pulseEnd.y);tag.putDouble("EZ",pulseEnd.z);}
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag,registries);restoreItem(tag);
        if(kind()==EnergyContent.Kind.ENCHANTER){var items=net.minecraft.core.NonNullList.withSize(28,ItemStack.EMPTY);net.minecraft.world.ContainerHelper.loadAllItems(tag,items,registries);for(int i=0;i<28;i++)enchantInventory.setItem(i,items.get(i));}
        battery=tag.contains("Battery")?ItemStack.parseOptional(registries,tag.getCompound("Battery")):ItemStack.EMPTY;
        product=tag.contains("Product")?ItemStack.parseOptional(registries,tag.getCompound("Product")):ItemStack.EMPTY;
        emptyBuckets=tag.contains("EmptyBuckets")?ItemStack.parseOptional(registries,tag.getCompound("EmptyBuckets")):ItemStack.EMPTY;
        outputPaused=tag.getBoolean("OutputPaused");loaderEnabled=tag.getBoolean("LoaderEnabled");
        resonanceParent=tag.contains("ResonanceParent")?BlockPos.of(tag.getLong("ResonanceParent")):null;
        resonanceParentIdentity=tag.hasUUID("ResonanceParentIdentity")?tag.getUUID("ResonanceParentIdentity"):null;
        target=tag.contains("Target")?BlockPos.of(tag.getLong("Target")):null;targetIdentity=tag.hasUUID("TargetIdentity")?tag.getUUID("TargetIdentity"):null;
        pulseAmount=Math.max(0,tag.getLong("Pulse"));pulseDelivered=Math.clamp(tag.getLong("PulseDelivered"),0,pulseAmount);pulseAge=tag.getInt("PulseAge");pulseDuration=tag.getInt("PulseDuration");
        pulseStart=tag.contains("SX")?new Vec3(tag.getDouble("SX"),tag.getDouble("SY"),tag.getDouble("SZ")):null;
        pulseEnd=tag.contains("EX")?new Vec3(tag.getDouble("EX"),tag.getDouble("EY"),tag.getDouble("EZ")):null;
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public int getContainerSize() { return kind()==EnergyContent.Kind.MATERIALIZER?3:2; }
    @Override public boolean isEmpty() { return battery.isEmpty()&&product.isEmpty()&&emptyBuckets.isEmpty(); }
    @Override public ItemStack getItem(int slot) { return slot==0?battery:slot==1?product:slot==2?emptyBuckets:ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot,int amount) { var result=getItem(slot).split(amount);if(!result.isEmpty())changed();return result; }
    @Override public ItemStack removeItemNoUpdate(int slot) {var item=getItem(slot);setItem(slot,ItemStack.EMPTY);return item;}
    @Override public void setItem(int slot,ItemStack stack) {if(slot==0)battery=stack;else if(slot==1)product=stack;else if(slot==2)emptyBuckets=stack;changed();}
    @Override public boolean stillValid(Player player) {return !isRemoved()&&player.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5)<=64;}
    @Override public void clearContent() {battery=product=emptyBuckets=ItemStack.EMPTY;changed();}
    @Override public int[] getSlotsForFace(Direction side) {
        if(kind()==EnergyContent.Kind.MATERIALIZER)return new int[]{side==Direction.DOWN?1:2};
        return kind()==EnergyContent.Kind.CRYSTAL_CHAMBER?new int[]{1}:new int[0];
    }
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction side) {
        return kind()==EnergyContent.Kind.MATERIALIZER&&slot==2&&side!=Direction.DOWN&&stack.is(Items.BUCKET);
    }
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side) {
        return slot==1&&(kind()==EnergyContent.Kind.CRYSTAL_CHAMBER||kind()==EnergyContent.Kind.MATERIALIZER&&side==Direction.DOWN);
    }
}
