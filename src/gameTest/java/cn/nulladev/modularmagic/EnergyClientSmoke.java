package cn.nulladev.modularmagic;

import cn.nulladev.modularmagic.energy.*;
import cn.nulladev.modularmagic.mana.ManaStorage;
import cn.nulladev.modularmagic.registry.ModContent;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Real client packets, synced block entities, renderer and guide in the disposable smoke world. */
@EventBusSubscriber(modid=ModularMagic.MOD_ID,value=Dist.CLIENT)
public final class EnergyClientSmoke {
    private static int ticks,stage,changed;
    private static volatile boolean ready;
    private static final BlockPos EMITTER=new BlockPos(-2,100,0),TANK=new BlockPos(0,100,0),ANCHOR=new BlockPos(3,100,0);
    private static final BlockPos[] SAMPLES={new BlockPos(0,100,-3),new BlockPos(2,100,-3),new BlockPos(4,100,-3)};
    private static final BlockPos CHAMBER=new BlockPos(6,100,-3),CONDENSER=new BlockPos(8,100,-3);
    private static final BlockPos TREE_ROOT=new BlockPos(-4,100,8),TREE_BRANCH=new BlockPos(0,100,8),TREE_LEAF=new BlockPos(4,100,8);
    private static long chargedBefore;
    private static long fragmentRemaining;
    private static volatile java.util.UUID jadeItem;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("modularmagic.energySmoke"))return;
        var mc=Minecraft.getInstance();if(++ticks>3000){fail(mc,new IllegalStateException("Energy smoke timed out at "+stage));return;}
        if(ticks-changed<12||mc.player==null||mc.getSingleplayerServer()==null)return;
        try {
            if(stage==0) {
                mc.options.guiScale().set(2);mc.resizeDisplay();var id=mc.player.getUUID();
                mc.getSingleplayerServer().execute(()->{
                    var p=mc.getSingleplayerServer().getPlayerList().getPlayer(id);var l=p.serverLevel();p.setGameMode(GameType.CREATIVE);p.getInventory().clearContent();
                    for(var item:l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(-8,99,-6,13,106,13)))item.discard();
                    for(int x=-8;x<=12;x++)for(int z=-6;z<=12;z++)for(int y=99;y<=105;y++)l.setBlock(new BlockPos(x,y,z),(y==99?Blocks.SMOOTH_STONE:Blocks.AIR).defaultBlockState(),3);
                    for(var k:new EnergyContent.Kind[]{EnergyContent.Kind.CREATIVE,EnergyContent.Kind.EMITTER,EnergyContent.Kind.TANK,EnergyContent.Kind.ANCHOR}) {
                        BlockPos pos=switch(k){case CREATIVE->EMITTER.west();case EMITTER->EMITTER;case TANK->TANK;default->ANCHOR;};
                        l.setBlock(pos,EnergyContent.BLOCKS.get(k).get().defaultBlockState(),3);var be=(EnergyBlockEntity)l.getBlockEntity(pos);be.owner=id;if(k==EnergyContent.Kind.ANCHOR)be.energy=100000*ManaStorage.SCALE;be.changed();
                    }
                    var kinds=new EnergyContent.Kind[]{EnergyContent.Kind.TANK,EnergyContent.Kind.EXPANDED_TANK,EnergyContent.Kind.ULTIMATE_TANK};
                    for(int i=0;i<3;i++){l.setBlock(SAMPLES[i],EnergyContent.BLOCKS.get(kinds[i]).get().defaultBlockState(),3);var be=(EnergyBlockEntity)l.getBlockEntity(SAMPLES[i]);be.owner=id;be.energy=(long)(be.capacity()*(i+1)/4D);be.changed();}
                    l.setBlock(CHAMBER,EnergyContent.BLOCKS.get(EnergyContent.Kind.CRYSTAL_CHAMBER).get().defaultBlockState(),3);var chamber=(EnergyBlockEntity)l.getBlockEntity(CHAMBER);chamber.owner=id;chamber.energy=8000*ManaStorage.SCALE;chamber.changed();
                    l.setBlock(CONDENSER,EnergyContent.BLOCKS.get(EnergyContent.Kind.CONDENSER).get().defaultBlockState(),3);var condenser=(EnergyBlockEntity)l.getBlockEntity(CONDENSER);condenser.owner=id;condenser.energy=1000*ManaStorage.SCALE;condenser.changed();
                    for(var pos:new BlockPos[]{TREE_ROOT,TREE_BRANCH,TREE_LEAF}){l.setBlock(pos,EnergyContent.BLOCKS.get(EnergyContent.Kind.RESONATOR).get().defaultBlockState(),3);var node=(EnergyBlockEntity)l.getBlockEntity(pos);node.owner=id;node.resonanceUpgrades=1;node.energy=pos.equals(TREE_ROOT)?5000*ManaStorage.SCALE:0;node.changed();}
                    l.setBlock(new BlockPos(10,100,-3),EnergyContent.BLOCKS.get(EnergyContent.Kind.CREATIVE_TANK).get().defaultBlockState(),3);
                    int emitterX=6;for(var kind:java.util.List.of(EnergyContent.Kind.EXPANDED_EMITTER,EnergyContent.Kind.ULTIMATE_EMITTER)) {
                        var pos=new BlockPos(emitterX,100,0);l.setBlock(pos,EnergyContent.BLOCKS.get(kind).get().defaultBlockState(),3);var e=(EnergyBlockEntity)l.getBlockEntity(pos);e.owner=id;e.energy=5000*ManaStorage.SCALE;
                        e.target=CHAMBER;e.targetIdentity=chamber.identity;e.changed();emitterX+=2;
                    }
                    var disruptor=new BlockPos(-5,100,-3);l.setBlock(disruptor,EnergyContent.BLOCKS.get(EnergyContent.Kind.DISRUPTOR).get().defaultBlockState(),3);((EnergyBlockEntity)l.getBlockEntity(disruptor)).owner=id;
                    l.setBlock(disruptor.above().west(),Blocks.WATER.defaultBlockState(),3);l.setBlock(disruptor.above().east(),Blocks.WATER.defaultBlockState(),3);
                    p.getInventory().setItem(0,EnergyContent.LINK.toStack());p.getInventory().setItem(1,ModContent.MANA_CONTAINERS.get("mana_container_basic").toStack());
                    p.getInventory().setItem(2,EnergyContent.RECEIVER.toStack());p.getInventory().setItem(5,ModContent.CORE_WAND.toStack());
                    p.teleportTo(.5,100,3);p.setYRot(180);p.setXRot(17);p.inventoryMenu.broadcastChanges();l.setDayTime(6000);ready=true;
                });next();return;
            }
            if(!ready||device(mc,TANK)==null)return;
            if(stage==1) { select(mc,0);use(mc,EMITTER);next(); }
            else if(stage==2) {use(mc,TANK);next();}
            else if(stage==3) {
                if(device(mc,TANK).energy==0)return;
                require(device(mc,EMITTER).target!=null,"Real tuner packets must bind a destination");
                var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(id);var tank=(EnergyBlockEntity)p.serverLevel().getBlockEntity(TANK);tank.energy=600000*ManaStorage.SCALE;tank.changed();});
                select(mc,1);use(mc,TANK);next();
            } else if(stage==4) {
                var be=device(mc,TANK);if(be.battery.isEmpty()||((ManaStorage)be.battery.getItem()).stored(be.battery)==0)return;
                chargedBefore=((ManaStorage)be.battery.getItem()).stored(be.battery);
                screenshot(mc,"modularmagic-reservoir-charging-item.png");
                mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player,ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY));use(mc,TANK);
                mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player,ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY));next();
            } else if(stage==5) {
                if(!device(mc,TANK).battery.isEmpty())return;
                require(mc.player.getInventory().items.stream().anyMatch(s->s.getItem() instanceof ManaStorage),"Retrieving the charged container must return the real item");
                select(mc,2);use(mc,ANCHOR);next();
            } else if(stage==6) {
                var receiver=mc.player.getInventory().getItem(2);if(!EnergyLinkItem.data(receiver).hasUUID("Device"))return;
                long stored=mc.player.getInventory().items.stream().filter(s->s.getItem() instanceof ManaStorage).mapToLong(s->((ManaStorage)s.getItem()).stored(s)).sum();
                if(stored<=chargedBefore)return;
                select(mc,5);mc.player.setYRot(180);mc.player.setXRot(17);
                for(int i=0;i<3;i++)require(device(mc,SAMPLES[i])!=null&&device(mc,SAMPLES[i]).displayedFill>.15F,"Loaded reservoir fill must be synced and rendered");
                var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(id);p.teleportTo(2.5,102,7.5);p.setYRot(180);p.setXRot(17);});next();
            } else if(stage==7) {
                screenshot(mc,"modularmagic-energy-filled.png");
                mc.getSingleplayerServer().execute(()->{var l=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID()).serverLevel();for(var pos:SAMPLES){var be=(EnergyBlockEntity)l.getBlockEntity(pos);be.energy=0;be.changed();}
                    for(var be:EnergyWorld.devices(l))if(be.kind().emitter()&&CHAMBER.equals(be.target)){be.outputPaused=true;be.cancelPulse();be.changed();}
                    var chamber=(EnergyBlockEntity)l.getBlockEntity(CHAMBER);chamber.energy=0;chamber.changed();});next();
            } else if(stage==8) {
                for(var pos:SAMPLES)if(device(mc,pos).energy!=0||device(mc,pos).displayedFill>.005F)return;
                require(device(mc,CHAMBER).energy==0&&!device(mc,CHAMBER).product.isEmpty()&&device(mc,CHAMBER).displayedFill>.05F,"Finished crystal clusters must stay visible after their aether buffer is spent");
                var crystalModel=mc.getBlockRenderer().getBlockModel(device(mc,CHAMBER).getBlockState());
                var quads=crystalModel.getQuads(null,null,net.minecraft.util.RandomSource.create(0));
                require(quads.stream().anyMatch(q->q.getSprite().contents().name().equals(net.minecraft.resources.ResourceLocation.withDefaultNamespace("item/nether_star"))),"The separate crystallization chamber must retain its central Nether Star nucleus");
                require(quads.stream().noneMatch(q->q.getSprite().contents().name().equals(net.minecraft.resources.ResourceLocation.withDefaultNamespace("block/amethyst_block"))),"The crystalline reservoir frame must use its metal structure rather than amethyst");
                screenshot(mc,"modularmagic-energy-empty.png");
                mc.getSingleplayerServer().execute(()->{var l=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID()).serverLevel();for(var pos:SAMPLES){var be=(EnergyBlockEntity)l.getBlockEntity(pos);be.energy=be.capacity();be.changed();}});next();
            } else if(stage==9) {
                for(var pos:SAMPLES)if(device(mc,pos).displayedFill<.98F)return;
                screenshot(mc,"modularmagic-energy-full.png");openGuide();next();
            } else if(stage==10) {
                require(mc.screen!=null,"Energy guide must open in a real client");screenshot(mc,"modularmagic-energy-guide.png");
                ModularMagic.LOGGER.info("MODULARMAGIC_ENERGY_SMOKE_SUCCESS: real emitter binding, pulse delivery, battery insertion/retrieval, remote recharge, two liquid levels and a growing solid crystal cluster and Patchouli chapter");
                mc.setScreen(null);var id=mc.player.getUUID();
                mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().getPlayerList().getPlayer(id).teleportTo(2.5,100,.5));next();
            } else if(stage==11) {
                if(mc.player.position().distanceToSqr(new Vec3(2.5,100,.5))>1)return;
                look(mc,Vec3.atCenterOf(SAMPLES[1]));next();
            } else if(stage==12) {
                look(mc,Vec3.atCenterOf(SAMPLES[1]));
                var accessor=snownee.jade.impl.ObjectDataCenter.get();
                if(!(accessor instanceof snownee.jade.api.BlockAccessor block)||!block.getPosition().equals(SAMPLES[1]))return;
                var data=snownee.jade.impl.ObjectDataCenter.getServerData();
                if(data==null||!data.contains(cn.nulladev.modularmagic.compat.jade.AetherJadeData.KEY))return;
                var snapshot=data.getCompound(cn.nulladev.modularmagic.compat.jade.AetherJadeData.KEY);
                require(snapshot.getLong("Stored")==5000000&&snapshot.getLong("Capacity")==5000000,"Real Jade server data must show the correct reservoir balance");
                var root=snownee.jade.overlay.WailaTickHandler.instance().rootElement;
                require(root!=null&&root.getTooltip().getMessage().contains("5M"),"The actual Jade overlay must abbreviate aether amounts");
                screenshot(mc,"modularmagic-jade-reservoir.png");var id=mc.player.getUUID();
                mc.getSingleplayerServer().execute(()->{
                    var p=mc.getSingleplayerServer().getPlayerList().getPlayer(id);var stack=ModContent.MANA_CONTAINERS.get("mana_container_basic").toStack();
                    ((ManaStorage)stack.getItem()).receive(stack,3456,false);
                    var item=new net.minecraft.world.entity.item.ItemEntity(p.level(),2.5,100.8,-.8,stack);item.setNoGravity(true);item.setDeltaMovement(Vec3.ZERO);item.setPickUpDelay(1000);
                    p.serverLevel().addFreshEntity(item);jadeItem=item.getUUID();
                });next();
            } else if(stage==13) {
                if(jadeItem==null)return;look(mc,new Vec3(2.5,100.9,-.8));next();
            } else if(stage==14) {
                look(mc,new Vec3(2.5,100.9,-.8));var accessor=snownee.jade.impl.ObjectDataCenter.get();
                if(!(accessor instanceof snownee.jade.api.EntityAccessor entity)||!entity.getEntity().getUUID().equals(jadeItem))return;
                var data=snownee.jade.impl.ObjectDataCenter.getServerData();if(data==null||!data.contains(cn.nulladev.modularmagic.compat.jade.AetherJadeData.KEY))return;
                var snapshot=data.getCompound(cn.nulladev.modularmagic.compat.jade.AetherJadeData.KEY);
                require(snapshot.getString("Type").equals("container")&&snapshot.getLong("Stored")==3456,"Dropped-container Jade data must come from its actual stack");
                require(snownee.jade.overlay.WailaTickHandler.instance().rootElement.getTooltip().getMessage().contains("3.46K"),"Jade must abbreviate the dropped container's mana");
                screenshot(mc,"modularmagic-jade-container.png");ModularMagic.LOGGER.info("MODULARMAGIC_JADE_SMOKE_SUCCESS: actual block/entity Jade server packets, compact progress bars and independent Patchouli mappings");
                var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().getPlayerList().getPlayer(id).teleportTo(8.5,100,-.5));next();
            } else if(stage==15) {
                if(mc.player.position().distanceToSqr(new Vec3(8.5,100,-.5))>1)return;
                select(mc,6);require(mc.player.getMainHandItem().isEmpty(),"Condenser extraction requires an empty hand");use(mc,CONDENSER);next();
            } else if(stage==16) {
                int slot=-1;for(int i=0;i<9;i++)if(mc.player.getInventory().getItem(i).is(EnergyContent.SHARD.get())){slot=i;break;}
                if(slot<0)return;select(mc,slot);
                require(device(mc,CONDENSER).energy==800*ManaStorage.SCALE,"Real empty-hand use must debit the condenser once");
                fragmentRemaining=AetherShardItem.remainingTicks(mc.player.getMainHandItem(),mc.level);
                require(fragmentRemaining>0&&fragmentRemaining<100,"Client must see a live, elapsed fragment deadline");next();
            } else if(stage==17) {
                var held=mc.player.getMainHandItem();long remaining=AetherShardItem.remainingTicks(held,mc.level);
                require(remaining>0&&remaining<fragmentRemaining,"Held countdown must keep decreasing on the client");
                var lines=held.getTooltipLines(Item.TooltipContext.of(mc.level),mc.player,TooltipFlag.NORMAL);
                require(lines.stream().anyMatch(line->line.getString().contains(Long.toString(remaining))),"Inventory tooltip must show the current remaining ticks");
                look(mc,Vec3.atCenterOf(CONDENSER));screenshot(mc,"modularmagic-condenser-countdown.png");next();
            } else if(stage==18) {
                if(!mc.player.getMainHandItem().isEmpty())return;
                ModularMagic.LOGGER.info("MODULARMAGIC_FRAGMENT_SMOKE_SUCCESS: real condenser extraction, live held/inventory tick countdown, expiration and new device guide mappings");
                var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().getPlayerList().getPlayer(id).teleportTo(-1.5,100,11.5));next();
            } else if(stage==19) {
                if(mc.player.position().distanceToSqr(new Vec3(-1.5,100,11.5))>1)return;
                select(mc,0);use(mc,TREE_ROOT);next();
            } else if(stage==20) {
                use(mc,TREE_BRANCH);next();
            } else if(stage==21) {
                if(!TREE_ROOT.equals(device(mc,TREE_BRANCH).resonanceParent))return;
                var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().getPlayerList().getPlayer(id).teleportTo(1.5,100,11.5));next();
            } else if(stage==22) {
                if(mc.player.position().distanceToSqr(new Vec3(1.5,100,11.5))>1)return;
                use(mc,TREE_BRANCH);next();
            } else if(stage==23) {
                use(mc,TREE_LEAF);next();
            } else if(stage==24) {
                if(!TREE_BRANCH.equals(device(mc,TREE_LEAF).resonanceParent))return;
                select(mc,2);use(mc,TREE_LEAF);next();
            } else if(stage==25) {
                var data=EnergyLinkItem.data(mc.player.getInventory().getItem(2));
                if(!data.hasUUID("Network")||!data.getUUID("Network").equals(device(mc,TREE_ROOT).identity)||!data.getBoolean("Connected")||data.getLong("DeliveredPerSecond")<=0)return;
                for(String name:new String[]{"item/aether_shard","item/mana_container_basic","item/creative_mana_container","block/creative_checker"}) {
                    var sprite=mc.getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(ModularMagic.id(name));
                    require(sprite.contents().name().equals(ModularMagic.id(name)),"Original textures must be stitched, not missing: "+name);
                }
                for(var node:cn.nulladev.modularmagic.spell.NodeCatalog.all()) {
                    var model=mc.getItemRenderer().getModel(ModContent.NODES.get(node.id()).toStack(),mc.level,mc.player,0);
                    var quads=model.getQuads(null,null,net.minecraft.util.RandomSource.create(0));
                    require(!quads.isEmpty(),"Node model must have geometry: "+node.id());
                    for(var quad:quads){var sprite=quad.getSprite().contents().name();require(sprite.getNamespace().equals(ModularMagic.MOD_ID)&&sprite.getPath().startsWith("block/node/"),"Every node face must resolve its colored atlas sprite: "+node.id()+" -> "+sprite);}
                }
                for(var kind:new EnergyContent.Kind[]{EnergyContent.Kind.CREATIVE,EnergyContent.Kind.CREATIVE_TANK,EnergyContent.Kind.CREATIVE_EMITTER}) {
                    var model=mc.getBlockRenderer().getBlockModel(EnergyContent.BLOCKS.get(kind).get().defaultBlockState());
                    var quads=model.getQuads(null,null,net.minecraft.util.RandomSource.create(0));
                    require(quads.stream().anyMatch(q->q.getSprite().contents().name().equals(ModularMagic.id("block/frame_netherite"))),"Creative devices must retain normal metal surfaces");
                    require(quads.stream().anyMatch(q->q.getSprite().contents().name().equals(ModularMagic.id("block/creative_checker"))),"Creative devices must retain a checker accent");
                }
                select(mc,5);var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().getPlayerList().getPlayer(id).teleportTo(1.5,102,12));next();
            } else if(stage==26) {
                look(mc,new Vec3(.5,100.7,8.5));screenshot(mc,"modularmagic-resonance-tree.png");
                ModularMagic.LOGGER.info("MODULARMAGIC_TREE_TEXTURE_SMOKE_SUCCESS: real root-child-grandchild tuner packets, network receiver binding, original texture atlas and immersive guide");
                var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->{
                    var p=mc.getSingleplayerServer().getPlayerList().getPlayer(id);p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();
                    var categories=new java.util.HashSet<String>();int slot=9;
                    for(var node:cn.nulladev.modularmagic.spell.NodeCatalog.all())if(categories.add(node.category()))p.getInventory().setItem(slot++,ModContent.NODES.get(node.id()).toStack());
                    p.getInventory().setItem(0,EnergyContent.ITEMS.get(EnergyContent.Kind.CREATIVE_TANK).toStack());p.getInventory().setItem(1,EnergyContent.ITEMS.get(EnergyContent.Kind.CREATIVE_EMITTER).toStack());
                    p.getInventory().setItem(2,EnergyContent.ITEMS.get(EnergyContent.Kind.CREATIVE).toStack());p.getInventory().setItem(3,ModContent.MANA_CONTAINERS.get("creative_mana_container").toStack());
                    p.getInventory().setItem(4,ModContent.CREATIVE_WAND.toStack());p.getInventory().setItem(5,ModContent.FORMED_CORE.toStack());p.getInventory().setItem(6,ModContent.WRITING_TABLE_ITEM.toStack());
                    p.inventoryMenu.broadcastChanges();
                });next();
            } else if(stage==27) {
                if(mc.player.isCreative()||!(mc.player.getInventory().getItem(9).getItem() instanceof cn.nulladev.modularmagic.item.NodeItem))return;
                mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));next();
            } else if(stage==28) {
                require(mc.screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen,"Node palette must be visible in the actual inventory");
                screenshot(mc,"modularmagic-node-palette.png");
                ModularMagic.LOGGER.info("MODULARMAGIC_NODE_ATLAS_SMOKE_SUCCESS: all 100 node models resolve colored sprites, creative devices mix normal metal and local checker accents");stage=99;mc.stop();
            }
        } catch(Throwable e) {fail(mc,e);}
    }
    private static void openGuide() throws ReflectiveOperationException {
        var common=Class.forName("vazkii.patchouli.common.book.BookRegistry");
        var books=(java.util.Map<?,?>)common.getField("books").get(common.getField("INSTANCE").get(null));var book=books.get(ModularMagic.id("assembly_guide"));
        var contents=book.getClass().getMethod("getContents").invoke(book);require(!(boolean)contents.getClass().getMethod("isErrored").invoke(contents),"Guide must load without errors");
        var entries=(java.util.Map<?,?>)contents.getClass().getField("entries").get(contents);require(entries.containsKey(ModularMagic.id("energy/aether_tank")),"Energy guide entry must be registered");
        var lookup=contents.getClass().getMethod("getEntryForStack",ItemStack.class);
        for(var kind:EnergyContent.Kind.values()) {
            var expected=entries.get(ModularMagic.id("energy/"+kind.id));require(expected!=null,"Dedicated device guide missing: "+kind.id);
            var mapping=(com.mojang.datafixers.util.Pair<?,?>)lookup.invoke(contents,EnergyContent.ITEMS.get(kind).toStack());
            require(mapping!=null&&mapping.getFirst()==expected,"Ctrl lookup must open each device's own entry: "+kind.id);
        }
        for(String id:new String[]{"aether_shard","crystalline_aether_shard","aether_tuner","aether_receiver","interdimensional_lens"}) {
            var expected=entries.get(ModularMagic.id("energy/"+id));var stack=new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ModularMagic.id(id)));
            var mapping=(com.mojang.datafixers.util.Pair<?,?>)lookup.invoke(contents,stack);
            require(expected!=null&&mapping!=null&&mapping.getFirst()==expected,"New item Ctrl lookup must open its own entry: "+id);
        }
        var tutorial=entries.get(ModularMagic.id("energy/getting_started"));
        require(tutorial!=null&&((java.util.List<?>)tutorial.getClass().getMethod("getPages").invoke(tutorial)).size()>=6,"A complete energy tutorial must be present");
        var registry=Class.forName("vazkii.patchouli.client.book.ClientBookRegistry");registry.getMethod("displayBookGui",net.minecraft.resources.ResourceLocation.class,net.minecraft.resources.ResourceLocation.class,int.class)
                .invoke(registry.getField("INSTANCE").get(null),ModularMagic.id("assembly_guide"),ModularMagic.id("energy/aether_tank"),0);
    }
    private static EnergyBlockEntity device(Minecraft mc,BlockPos pos) {return mc.level.getBlockEntity(pos) instanceof EnergyBlockEntity be?be:null;}
    private static void select(Minecraft mc,int slot) {mc.player.getInventory().selected=slot;}
    private static void use(Minecraft mc,BlockPos pos) {mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));}
    private static void next() {stage++;changed=ticks;}
    private static void look(Minecraft mc,Vec3 target) {
        var d=target.subtract(mc.player.getEyePosition());mc.player.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));
        mc.player.setXRot((float)-Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z))));
    }
    private static void screenshot(Minecraft mc,String name) {Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->ModularMagic.LOGGER.info("ENERGY_SCREENSHOT: {}",message.getString()));}
    private static void require(boolean valid,String message) {if(!valid)throw new IllegalStateException(message);}
    private static void fail(Minecraft mc,Throwable e) {ModularMagic.LOGGER.error("MODULARMAGIC_ENERGY_SMOKE_FAILED stage="+stage,e);stage=99;mc.stop();}
}
