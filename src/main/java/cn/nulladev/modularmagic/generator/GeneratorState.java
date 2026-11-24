package cn.nulladev.modularmagic.generator;

import cn.nulladev.modularmagic.energy.*;
import cn.nulladev.modularmagic.mana.ManaStorage;
import cn.nulladev.modularmagic.network.EnergyVisual;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Server-owned progress. No input inventory: every input is consumed in the world. */
public final class GeneratorState {
    public final EnergyBlockEntity be;
    public String ruleId="", status="empty";
    public int cooldown=-1, signal, retry;
    public UUID actor;
    public BlockPos actorPosition;
    public int missingTicks,cureTotal,cureRemaining;
    public double progress;
    public long reserved, lastTick=Long.MIN_VALUE;
    public final List<BlockPos> musicTargets=new ArrayList<>();
    public GeneratorState(EnergyBlockEntity be){this.be=be;}
    public GeneratorCatalog.Rule rule(){return GeneratorCatalog.get(ruleId);}
    public ServerLevel level(){return (ServerLevel)be.getLevel();}
    public AABB bounds(){
        var r=rule();if(r==null)return new AABB(be.getBlockPos());
        var p=be.getBlockPos();int x=p.getX()-r.width.get()/2,z=p.getZ()-r.depth.get()/2;
        if(ruleId.equals("snow")){var chunk=new net.minecraft.world.level.ChunkPos(p);x=chunk.getMinBlockX();z=chunk.getMinBlockZ();}
        int width=ruleId.equals("snow")?16:r.width.get(),depth=ruleId.equals("snow")?16:r.depth.get();
        return new AABB(x,p.getY()-1,z,x+width,p.getY()-1+r.height.get(),z+depth);
    }
    public void placed(){if(rule()!=null&&rule().defaults.coldStart())cooldown=rule().period.get();retry=0;}
    public void load(CompoundTag tag){
        ruleId=tag.getString("Rule");if(rule()==null)ruleId="";
        cooldown=tag.contains("Cooldown")?Math.max(0,tag.getInt("Cooldown")):-1;
        signal=Math.clamp(tag.getInt("Signal"),0,15);
        status=tag.contains("Status")?tag.getString("Status"):"empty";
        actor=tag.hasUUID("Actor")?tag.getUUID("Actor"):null;reserved=Math.max(0,tag.getLong("Reserved"));
        actorPosition=tag.contains("ActorPosition")?BlockPos.of(tag.getLong("ActorPosition")):null;
        cureTotal=tag.getInt("CureTotal");cureRemaining=Math.clamp(tag.getInt("CureRemaining"),0,Math.max(0,cureTotal));progress=Math.clamp(tag.getDouble("Progress"),0,1);
        musicTargets.clear();for(long p:tag.getLongArray("Jukeboxes"))if(musicTargets.size()<4)musicTargets.add(BlockPos.of(p));
    }
    public CompoundTag save(){
        var t=new CompoundTag();t.putString("Rule",ruleId);t.putInt("Cooldown",cooldown);
        t.putInt("Signal",signal);t.putString("Status",status);if(actor!=null)t.putUUID("Actor",actor);t.putLong("Reserved",reserved);
        if(actorPosition!=null)t.putLong("ActorPosition",actorPosition.asLong());t.putInt("CureTotal",cureTotal);t.putInt("CureRemaining",cureRemaining);t.putDouble("Progress",progress);
        t.putLongArray("Jukeboxes",musicTargets.stream().mapToLong(BlockPos::asLong).toArray());return t;
    }
    public int comparator(){
        var rule = rule();
        if (rule == null || cooldown <= 0) return 0;
        int period = rule.period.get();
        if (period <= 0) return 0;
        return Math.ceilDiv(15 * Math.min(cooldown, period), period);
    }
    public void state(String status,int signal){if(!this.status.equals(status)||this.signal!=signal){this.status=status;this.signal=signal;be.changed();}}
    public long room(){return Math.max(0,be.capacity()-be.energy-reserved);}
    private boolean fits(long displayed){if(displayed<=0)return false;if(displayed>room()/ManaStorage.SCALE){state("full",15);return false;}return true;}
    private void produce(long amount){be.energy+=amount*ManaStorage.SCALE;be.changed();}
    public void showInput(Vec3 source) {
        EnergyVisual.send(level(), EnergyVisual.Kind.HARVEST, source,
                Vec3.atCenterOf(be.getBlockPos()).add(0, .5, 0));
    }
    public void tick(){
        lastTick=level().getGameTime();var r=rule();if(r==null){state("empty",0);return;}
        if(be.paused()){state("paused",0);return;}
        if(ruleId.equals("curing")){CuringGenerator.tick(this);return;}
        if(cooldown<0)cooldown=r.defaults.coldStart()?r.period.get():0;
        if(cooldown>0){cooldown--;be.changed();if(cooldown>0){state("cooldown",3);return;}}
        if(room()<r.yield(0)*ManaStorage.SCALE){state("full",15);return;}
        if(ruleId.equals("tnt")){state("ready",14);return;}
        if(retry-->0)return;
        state("input",1);boolean success=switch(ruleId){
            case "rails","egg" -> items();
            case "experience" -> experience();
            case "wool","equipment","iron_golem","sand" -> entities();
            case "music" -> music();
            default -> blocks();
        };
        if(success){cooldown=r.period.get();state("cooldown",3);be.changed();}else if(ruleId.equals("snow")){cooldown=r.period.get();be.changed();}
        retry=success?0:20;
    }
    private boolean experience(){
        int left=rule().batch.get();long total=0;
        for(var p:level().getEntitiesOfClass(net.minecraft.server.level.ServerPlayer.class,bounds())){
            if(p.isSpectator()||p.isCreative())continue;
            int level=p.experienceLevel;long base=level<=16?(long)level*level+6L*level:level<=31?(long)(2.5*level*level-40.5*level+360):(long)(4.5*level*level-162.5*level+2220);
            long points=base+(long)Math.floor(p.experienceProgress*p.getXpNeededForNextLevel());
            int n=(int)Math.min(Math.min(points,left),(room()/ManaStorage.SCALE-total)/rule().yield(0));
            if(n>0){p.giveExperiencePoints(-n);total+=n*rule().yield(0);left-=n;showInput(p.getEyePosition());}if(left==0)break;
        }
        if(total>0)produce(total);return total>0;
    }
    private boolean items(){
        int left=rule().batch.get(),taken=0;long total=0;
        for(var e:level().getEntitiesOfClass(ItemEntity.class,bounds())){
            var stack=e.getItem();boolean valid=ruleId.equals("egg")?stack.is(Items.EGG):stack.is(Items.RAIL)||stack.is(Items.POWERED_RAIL)||stack.is(Items.DETECTOR_RAIL)||stack.is(Items.ACTIVATOR_RAIL);
            if(!valid||e.isRemoved())continue;
            long price=rule().yield(0);int n=(int)Math.min(Math.min(left,stack.getCount()),(room()/ManaStorage.SCALE-total)/price);
            if(n<=0){if(taken==0)state("full",15);break;}
            showInput(e.position());
            stack.shrink(n);if(stack.isEmpty())e.discard();else e.setItem(stack.copy());left-=n;taken+=n;total+=price*n;if(left==0)break;
        }
        if(total>0)produce(total);return taken>0;
    }
    private boolean entities(){
        int left=rule().batch.get();long total=0;
        for(var e:level().getEntitiesOfClass(Entity.class,bounds())){
            if(e.isRemoved()||!e.isAlive())continue;long value=0;Runnable consume=null;
            if(ruleId.equals("wool")&&e instanceof Sheep sheep&&!sheep.isBaby()&&!sheep.isSheared()){
                value=rule().yield(0);consume=()->sheep.setSheared(true);
            }else if(ruleId.equals("sand")&&e instanceof FallingBlockEntity falling){
                var s=falling.getBlockState();if(s.is(Blocks.SAND)||s.is(Blocks.RED_SAND)||s.is(Blocks.GRAVEL)){value=rule().yield(0);consume=falling::discard;}
            }else if(ruleId.equals("iron_golem")&&e instanceof IronGolem golem&&!golem.hasCustomName()){
                value=rule().yield(0);consume=golem::discard;
            }else if(ruleId.equals("equipment")&&e instanceof ArmorStand stand&&stand.getUUID().equals(actor)){
                for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)){
                    var stack=stand.getItemBySlot(slot);long price=GeneratorCatalog.armorYield(stack);
                    if(price>0){value=price;consume=()->stand.setItemSlot(slot,ItemStack.EMPTY);break;}
                }
            }
            if(consume==null)continue;
            if(value>(room()/ManaStorage.SCALE-total)){if(total==0)state("full",15);break;}
            showInput(e.getBoundingBox().getCenter());consume.run();total+=value;if(--left==0)break;
        }
        if(ruleId.equals("equipment")&&actor==null)state("binding",2);
        if(total>0)produce(total);return total>0;
    }
    private record Change(BlockPos pos,BlockState before,BlockState after,long amount){}
    private boolean blocks(){
        var b=bounds();var changes=new ArrayList<Change>();long total=0;int units=0;
        BlockPos red=null,brown=null;
        for(var scan:BlockPos.betweenClosed((int)b.minX,(int)b.minY,(int)b.minZ,(int)b.maxX-1,(int)b.maxY-1,(int)b.maxZ-1)){
            if(!level().hasChunkAt(scan))continue;var p=scan.immutable();var s=level().getBlockState(p);BlockState after=null;long amount=0;int count=1;
            switch(ruleId){
                case "water" -> {if(s.is(Blocks.WATER)&&s.getFluidState().isSource()){after=Blocks.AIR.defaultBlockState();amount=rule().yield(0);}}
                case "stone" -> {int index=s.is(Blocks.COBBLESTONE)?0:s.is(Blocks.STONE)?1:s.is(Blocks.BASALT)?2:-1;if(index>=0){after=Blocks.AIR.defaultBlockState();amount=rule().yield(index);}}
                case "lava" -> {if(s.is(Blocks.LAVA)&&s.getFluidState().isSource()||s.is(Blocks.LAVA_CAULDRON)){after=s.is(Blocks.LAVA_CAULDRON)?Blocks.CAULDRON.defaultBlockState():Blocks.AIR.defaultBlockState();amount=rule().yield(0);}}
                case "snow" -> {if(s.is(Blocks.SNOW)){int layers=s.getValue(SnowLayerBlock.LAYERS);count=Math.min(layers,rule().batch.get()-units);amount=rule().yield(0)*count;after=count<layers?s.setValue(SnowLayerBlock.LAYERS,layers-count):Blocks.AIR.defaultBlockState();}}
                case "fire" -> {if(s.is(Blocks.FIRE)){after=Blocks.AIR.defaultBlockState();amount=rule().yield(0);}}
                case "crops" -> {if(s.getBlock() instanceof CropBlock crop&&crop.isMaxAge(s)){after=crop.getStateForAge(0);amount=rule().yield(0);}}
                case "ice" -> {if(s.is(Blocks.ICE)&&!level().dimensionType().ultraWarm()){after=Blocks.WATER.defaultBlockState();amount=rule().yield(0);}}
                case "copper" -> {after=WeatheringCopper.getPrevious(s).orElse(null);if(after!=null)amount=rule().yield(0);}
                case "honey" -> {if(s.hasProperty(BeehiveBlock.HONEY_LEVEL)&&s.getValue(BeehiveBlock.HONEY_LEVEL)>0){after=s.setValue(BeehiveBlock.HONEY_LEVEL,s.getValue(BeehiveBlock.HONEY_LEVEL)-1);amount=rule().yield(0);}}
                case "sculk" -> {if(s.is(Blocks.SCULK)){after=Blocks.AIR.defaultBlockState();amount=rule().yield(0);}}
                case "amethyst" -> {if(s.is(Blocks.AMETHYST_CLUSTER)&&level().hasChunkAt(p.relative(s.getValue(AmethystClusterBlock.FACING).getOpposite()))&&level().getBlockState(p.relative(s.getValue(AmethystClusterBlock.FACING).getOpposite())).is(Blocks.BUDDING_AMETHYST)){after=Blocks.LARGE_AMETHYST_BUD.defaultBlockState().setValue(AmethystClusterBlock.FACING,s.getValue(AmethystClusterBlock.FACING)).setValue(AmethystClusterBlock.WATERLOGGED,s.getValue(AmethystClusterBlock.WATERLOGGED));amount=rule().yield(0);}}
                case "chorus" -> {if(s.is(Blocks.CHORUS_FLOWER)&&s.getValue(ChorusFlowerBlock.AGE)==5){after=s.setValue(ChorusFlowerBlock.AGE,0);amount=rule().yield(0);}}
                case "mushrooms" -> {if(s.is(Blocks.RED_MUSHROOM))red=p;if(s.is(Blocks.BROWN_MUSHROOM))brown=p;}
            }
            if(after!=null){changes.add(new Change(p,s,after,amount));total+=amount;units+=count;if(units>=rule().batch.get())break;}
        }
        if(ruleId.equals("mushrooms")&&red!=null&&brown!=null){long half=rule().yield(0)/2;changes.add(new Change(red,level().getBlockState(red),Blocks.AIR.defaultBlockState(),half));changes.add(new Change(brown,level().getBlockState(brown),Blocks.AIR.defaultBlockState(),rule().yield(0)-half));total=rule().yield(0);}
        if(changes.isEmpty()||!fits(total))return false;
        for(var c:changes)if(!level().getBlockState(c.pos).equals(c.before))return false;
        long accepted=0;for(var c:changes)if(level().setBlock(c.pos,c.after,3)){accepted+=c.amount;showInput(Vec3.atCenterOf(c.pos));}
        if(accepted>0)produce(accepted);return accepted>0;
    }
    private boolean music(){
        if(musicTargets.isEmpty()){state("binding",2);return false;}
        long amount=rule().yield(0);if(!fits(amount))return false;
        for(var p:musicTargets)if(bounds().contains(p.getX()+.5,p.getY()+.5,p.getZ()+.5)&&level().hasChunkAt(p)&&level().getBlockEntity(p) instanceof JukeboxBlockEntity box){
            var stack=box.getItem(0);
            if(!stack.isEmpty()&&net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("minecraft")&&stack.has(net.minecraft.core.component.DataComponents.JUKEBOX_PLAYABLE)){
                box.removeItem(0,1);box.setChanged();produce(amount);showInput(Vec3.atCenterOf(p));return true;
            }
        }
        return false;
    }
    public boolean captureTnt(PrimedTnt tnt){
        if(!ruleId.equals("tnt")||be.paused()||cooldown!=0||tnt.isRemoved()||tnt.getFuse()<1||!bounds().contains(tnt.position()))return false;
        long amount=rule().yield(0);if(!fits(amount))return false;
        showInput(tnt.position());tnt.discard();produce(amount);cooldown=rule().period.get();state("cooldown",3);be.changed();return true;
    }
}
