package cn.nulladev.modularmagic.research;

import cn.nulladev.modularmagic.ModularMagic;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.*;

public final class ResearchContent {
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(ModularMagic.MOD_ID);
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(ModularMagic.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,ModularMagic.MOD_ID);
    private static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,ModularMagic.MOD_ID);
    public static final DeferredItem<Item> BLANK=ITEMS.registerSimpleItem("blank_spell_node");
    public static final DeferredItem<Item> INK=ITEMS.registerSimpleItem("research_ink",new Item.Properties().stacksTo(1).durability(32));
    public static final DeferredItem<LensItem> LENS=ITEMS.register("void_lens",LensItem::new);
    public static final DeferredItem<ResearchNoteItem> NOTE=ITEMS.register("research_note",()->new ResearchNoteItem(false));
    public static final DeferredItem<ResearchNoteItem> DIRECT_NOTE=ITEMS.register("deciphered_note",()->new ResearchNoteItem(true));
    public static final DeferredBlock<Block> TABLE=BLOCKS.register("rune_research_table",()->new ResearchBlock(false));
    public static final DeferredBlock<Block> CARVER=BLOCKS.register("rune_carving_table",()->new ResearchBlock(true));
    public static final DeferredItem<BlockItem> TABLE_ITEM=ITEMS.registerSimpleBlockItem(TABLE),CARVER_ITEM=ITEMS.registerSimpleBlockItem(CARVER);
    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>,net.minecraft.world.level.block.entity.BlockEntityType<RuneTableBlockEntity>> TABLE_ENTITY=ENTITIES.register("rune_table",()->net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(RuneTableBlockEntity::new,TABLE.get(),CARVER.get()).build(null));
    public static final DeferredHolder<MenuType<?>,MenuType<ResearchMenu>> MENU=MENUS.register("research",()->IMenuTypeExtension.create((id,inv,buf)->new ResearchMenu(id,inv,buf.readBlockPos(),buf.readBoolean())));
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue KNOWLEDGE_FOCUS,FOCUS_BONUS_CAP,INK_BASE,INK_PER_NODE,UNKNOWN_LINEAR,UNKNOWN_QUADRATIC;
    static {var b=new ModConfigSpec.Builder();KNOWLEDGE_FOCUS=b.defineInRange("focusPerKnowledge",.5,0,1000);FOCUS_BONUS_CAP=b.defineInRange("focusBonusCap",50.,0,100000);
        INK_BASE=b.defineInRange("inkBase",2.,0,10000);INK_PER_NODE=b.defineInRange("inkPerNode",.75,0,10000);UNKNOWN_LINEAR=b.defineInRange("unknownNodeLinear",.2,0,100);UNKNOWN_QUADRATIC=b.defineInRange("unknownNodeQuadratic",.05,0,100);SPEC=b.build();}
    public static void register(IEventBus bus){ITEMS.register(bus);BLOCKS.register(bus);MENUS.register(bus);ENTITIES.register(bus);ResearchKnowledge.register(bus);}
    public static boolean spend(Player p,Item item,int count){if(p.isCreative())return true;int have=0;for(var s:p.getInventory().items)if(s.is(item))have+=s.getCount();if(have<count)return false;for(var s:p.getInventory().items)if(s.is(item)){int n=Math.min(count,s.getCount());s.shrink(n);count-=n;if(count==0)break;}return true;}
    public static boolean ink(Player p,int amount,boolean simulate){if(p.isCreative())return true;int have=0;for(var s:p.getInventory().items)if(s.is(INK.get()))have+=s.getMaxDamage()-s.getDamageValue();if(have<amount)return false;if(simulate)return true;
        for(var s:p.getInventory().items)if(s.is(INK.get())){int use=Math.min(amount,s.getMaxDamage()-s.getDamageValue());s.setDamageValue(s.getDamageValue()+use);amount-=use;if(s.getDamageValue()>=s.getMaxDamage())s.shrink(1);if(amount==0)break;}return true;}
    public static void open(Player p,BlockPos pos,boolean carving){open(p,pos,carving,InteractionHand.MAIN_HAND);}
    public static void open(Player p,BlockPos pos,boolean carving,InteractionHand hand){
        if(!(p instanceof net.minecraft.server.level.ServerPlayer server)||!(p.level().getBlockEntity(pos) instanceof RuneTableBlockEntity table))return;
        if(table.inUse()){p.displayClientMessage(Component.translatable("gui.modularmagic.table_busy"),true);return;}
        if(!carving){
            var held=p.getItemInHand(hand);
            if(!(held.getItem() instanceof ResearchNoteItem item)||item.direct)return;
            try{var topic=ResearchCatalog.get(cn.nulladev.modularmagic.energy.EnergyLinkItem.data(held).getString("Research"));
                if(ResearchKnowledge.completed(p,topic)){p.displayClientMessage(Component.translatable("research.modularmagic.complete"),true);return;}
                if(!ResearchKnowledge.ready(p,topic)){p.displayClientMessage(Component.translatable("research.modularmagic.prerequisite"),true);return;}
            }catch(RuntimeException ex){return;}
            var old=table.inventory.removeItemNoUpdate(0);if(!old.isEmpty()&&!p.getInventory().add(old))p.drop(old,false);
            table.inventory.setItem(0,held.split(1));
        }
        var opened=server.openMenu(new SimpleMenuProvider((id,inv,user)->new ResearchMenu(id,inv,pos,carving),Component.translatable(carving?"block.modularmagic.rune_carving_table":"block.modularmagic.rune_research_table")),buf->{buf.writeBlockPos(pos);buf.writeBoolean(carving);});
        if(opened.isEmpty()&&!carving){var returned=table.inventory.removeItemNoUpdate(0);if(p.getItemInHand(hand).isEmpty())p.setItemInHand(hand,returned);else if(!p.getInventory().add(returned))p.drop(returned,false);}
    }
    private static final class ResearchBlock extends Block implements net.minecraft.world.level.block.EntityBlock{
        private final boolean carving;
        private ResearchBlock(boolean carving){super(BlockBehaviour.Properties.of().strength(2.5f).sound(SoundType.WOOD).noOcclusion());this.carving=carving;}
        @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){
            if(!carving&&stack.is(INK.get())){
                if(!level.isClientSide&&level.getBlockEntity(pos) instanceof RuneTableBlockEntity table&&!table.inUse()&&table.inventory.getItem(1).isEmpty())table.inventory.setItem(1,stack.split(1));
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            return super.useItemOn(stack,state,level,pos,p,hand,hit);
        }
        @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player p,BlockHitResult hit){open(p,pos,carving);return InteractionResult.sidedSuccess(level.isClientSide);}
        @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos p,BlockState s){return new RuneTableBlockEntity(p,s);}
        @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState replacement,boolean moving){if(!s.is(replacement.getBlock())&&l.getBlockEntity(p) instanceof RuneTableBlockEntity table){Containers.dropContents(l,p,table.inventory);table.inventory.clearContent();}super.onRemove(s,l,p,replacement,moving);}
        @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState s,net.minecraft.world.level.BlockGetter l,BlockPos p,net.minecraft.world.phys.shapes.CollisionContext c){return net.minecraft.world.phys.shapes.Shapes.or(box(0,12,0,16,16,16),box(1,0,1,4,12,4),box(12,0,1,15,12,4),box(1,0,12,4,12,15),box(12,0,12,15,12,15));}
    }
    private ResearchContent(){}
}
