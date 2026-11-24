package cn.nulladev.modularmagic;

import cn.nulladev.modularmagic.client.AutoCrafterScreen;
import cn.nulladev.modularmagic.compat.jei.ArtificeJeiPlugin;
import cn.nulladev.modularmagic.mana.ManaStorage;
import cn.nulladev.modularmagic.registry.ModContent;
import cn.nulladev.modularmagic.utility.*;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.util.*;

@EventBusSubscriber(modid=ModularMagic.MOD_ID,value=Dist.CLIENT)
public final class UtilityClientSmoke {
    private static int ticks,stage,changed;private static boolean savedPause;
    private static final List<String> IDS=List.of("placement_unit","eating_unit","launch_unit","burning_unit","tool_unit","handheld_aether_crafting_table","aether_materializer","portable_tnt_cannon","auto_crafter","advanced_wand");
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(!Boolean.getBoolean("modularmagic.utilitiesSmoke"))return;var mc=Minecraft.getInstance();
        if(++ticks>2400){ModularMagic.LOGGER.error("UTILITIES_SMOKE_TIMEOUT stage={}",stage);mc.stop();return;}
        if(mc.player==null||mc.getSingleplayerServer()==null||ticks-changed<20)return;
        try{switch(stage){
            case 0->{savedPause=mc.options.pauseOnLostFocus;mc.options.pauseOnLostFocus=false;mc.player.getInventory().selected=0;mc.options.guiScale().set(2);mc.resizeDisplay();server(mc,()->{var p=player(mc);p.setGameMode(GameType.SURVIVAL);p.setInvulnerable(true);p.setHealth(p.getMaxHealth());p.fallDistance=0;for(int x=12;x<=20;x++)for(int z=12;z<=20;z++)p.serverLevel().setBlockAndUpdate(new BlockPos(x,99,z),Blocks.SMOOTH_STONE.defaultBlockState());p.teleportTo(16.5,101,18.5);p.getInventory().clearContent();p.getInventory().selected=0;p.setItemInHand(InteractionHand.MAIN_HAND,UtilityContent.CRAFTER.toStack());p.getInventory().setItem(1,new ItemStack(Items.OAK_LOG,2));var b=ModContent.MANA_CONTAINERS.get("mana_container_basic").toStack();((ManaStorage)b.getItem()).receive(b,10000,false);p.getInventory().setItem(2,b);});next();}
            case 1->{if(!mc.player.getMainHandItem().is(UtilityContent.CRAFTER.get()))return;mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);next();}
            case 2->{if(!(mc.screen instanceof AutoCrafterScreen)||!(mc.player.containerMenu instanceof AutoCrafterMenu m))return;mc.gameMode.handleInventoryMouseClick(m.containerId,38,0,ClickType.QUICK_MOVE,mc.player);next();}
            case 3->{if(!(mc.player.containerMenu instanceof AutoCrafterMenu m)||!m.preview.getItem(0).is(Items.OAK_PLANKS))return;require(mc.player.getInventory().getItem(1).getCount()==2,"Reference selection consumed logs");mc.gameMode.handleInventoryMouseClick(m.containerId,0,1,ClickType.PICKUP,mc.player);next();}
            case 4->{if(!(mc.player.containerMenu instanceof AutoCrafterMenu m)||!m.pattern.isEmpty())return;require(m.preview.isEmpty(),"Clearing a ghost left an output");mc.gameMode.handleInventoryMouseClick(m.containerId,38,0,ClickType.QUICK_MOVE,mc.player);next();}
            case 5->{if(!(mc.player.containerMenu instanceof AutoCrafterMenu m)||m.preview.isEmpty())return;screenshot(mc,"utilities-auto-crafter.png");int left=(mc.getWindow().getGuiScaledWidth()-176)/2,top=(mc.getWindow().getGuiScaledHeight()-198)/2;mc.screen.mouseClicked(left+139,top+82,0);next();}
            case 6->{if(!(mc.player.containerMenu instanceof AutoCrafterMenu m)||!m.enabled())return;mc.gameMode.handleInventoryMouseClick(m.containerId,9,0,ClickType.PICKUP,mc.player);next();}
            case 7->{var m=(AutoCrafterMenu)mc.player.containerMenu;require(m.getCarried().isEmpty(),"The ghost output was taken as a real item");mc.player.closeContainer();next();}
            case 8->{if(mc.player.getInventory().items.stream().filter(s->s.is(Items.OAK_PLANKS)).mapToInt(ItemStack::getCount).sum()!=8)return;server(mc,()->{var p=player(mc);require(p.getInventory().items.stream().noneMatch(s->s.is(Items.OAK_LOG)),"Automatic crafting did not consume both logs");var b=p.getInventory().items.stream().filter(s->s.getItem() instanceof ManaStorage).findFirst().orElseThrow();require(((ManaStorage)b.getItem()).stored(b)==10000-2*UtilityConfig.CRAFT.get(),"Automatic crafting charged the wrong total");});mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);next();}
            case 9->{if(!(mc.screen instanceof AutoCrafterScreen)||!(mc.player.containerMenu instanceof AutoCrafterMenu m))return;require(m.enabled()&&m.pattern.getItem(0).is(Items.OAK_LOG)&&m.preview.getItem(0).is(Items.OAK_PLANKS),"Saved references and running state did not survive reopening");screenshot(mc,"utilities-auto-crafter-running.png");mc.player.closeContainer();next();}
            case 10->{var runtime=ArtificeJeiPlugin.runtime;if(runtime==null)return;var recipes=runtime.getRecipeManager().createRecipeLookup(mezz.jei.api.constants.RecipeTypes.CRAFTING).get().map(h->h.id().toString()).collect(java.util.stream.Collectors.toSet());for(String id:IDS)require(recipes.contains("modularmagic:"+id),"Missing utility recipe in JEI: "+id);
                var book=book();var contents=book.getClass().getMethod("getContents").invoke(book);require(!(boolean)contents.getClass().getMethod("isErrored").invoke(contents),"The handbook failed to load");var entries=(Map<?,?>)contents.getClass().getField("entries").get(contents);
                String name=(String)book.getClass().getField("name").get(book),landing=(String)book.getClass().getField("landingText").get(book);require(!net.minecraft.network.chat.Component.translatable(name).getString().equals(name)&&!net.minecraft.network.chat.Component.translatable(landing).getString().equals(landing),"The guide's name or landing text is untranslated");require(book.getClass().getMethod("getOwnerName").invoke(book).equals("Modular Magic"),"The mod display name is missing its space");
                for(String id:IDS){require(entries.containsKey(ModularMagic.id("equipment/"+id)),"Missing utility chapter: "+id);var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ModularMagic.id(id));require(mc.getItemRenderer().getModel(new ItemStack(item),mc.level,mc.player,0)!=mc.getModelManager().getMissingModel(),"Missing utility model: "+id);require(!new ItemStack(item).getHoverName().getString().startsWith("item.modularmagic."),"Untranslated utility: "+id);}show("equipment/launch_unit");next();}
            case 11->{screenshot(mc,"utilities-launch-unit-guide.png");show("equipment/auto_crafter");next();}
            case 12->{screenshot(mc,"utilities-auto-crafter-guide.png");var book=book();mc.setScreen((net.minecraft.client.gui.screens.Screen)Class.forName("vazkii.patchouli.client.book.gui.GuiBookLanding").getConstructor(book.getClass()).newInstance(book));next();}
            case 13->{screenshot(mc,"dev-guide-landing.png");show("equipment/unformed");next();}
            case 14->{screenshot(mc,"dev-dark-matter-guide.png");show("equipment/advanced_wand");next();}
            case 15->{screenshot(mc,"dev-advanced-wand-guide.png");ModularMagic.LOGGER.info("MODULARMAGIC_UTILITIES_SMOKE_PASSED");mc.options.pauseOnLostFocus=savedPause;stage=99;mc.stop();}
        }}catch(Throwable ex){ModularMagic.LOGGER.error("UTILITIES_SMOKE_FAILED stage="+stage,ex);stage=99;mc.stop();}
    }
    private static net.minecraft.server.level.ServerPlayer player(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());}
    private static void server(Minecraft mc,Runnable action){mc.getSingleplayerServer().execute(()->{try{action.run();}catch(Throwable ex){ModularMagic.LOGGER.error("UTILITIES_SMOKE_SERVER_FAILED stage="+stage,ex);stage=99;mc.execute(mc::stop);}});}
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    private static void next(){ModularMagic.LOGGER.info("UTILITIES_SMOKE_STAGE {}",stage);stage++;changed=ticks;}
    private static void screenshot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),c->ModularMagic.LOGGER.info("UTILITIES_SCREENSHOT {}",name));}
    private static Object book()throws ReflectiveOperationException{var registry=Class.forName("vazkii.patchouli.common.book.BookRegistry");var books=(Map<?,?>)registry.getField("books").get(registry.getField("INSTANCE").get(null));return books.get(ModularMagic.id("assembly_guide"));}
    private static void show(String id)throws ReflectiveOperationException{var cls=Class.forName("vazkii.patchouli.client.book.ClientBookRegistry");cls.getMethod("displayBookGui",net.minecraft.resources.ResourceLocation.class,net.minecraft.resources.ResourceLocation.class,int.class).invoke(cls.getField("INSTANCE").get(null),ModularMagic.id("assembly_guide"),ModularMagic.id(id),0);}
}
