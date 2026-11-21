package cn.nulladev.modularmagic.research;

import cn.nulladev.modularmagic.energy.EnergyLinkItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import java.util.List;

public final class PlaceholderItem extends Item{
    public PlaceholderItem(){super(new Properties().stacksTo(1));}
    public static ResourceLocation target(ItemStack stack){String value=EnergyLinkItem.data(stack).getString("MaterialTarget");return value.isBlank()?null:ResourceLocation.tryParse(value);}
    public static ItemStack create(ResourceLocation target){var family=MimicFamily.fromTarget(target);if(family!=null)return family.template();var s=PlaceholderContent.ITEM.toStack();var t=EnergyLinkItem.data(s);t.putString("MaterialTarget",target.toString());EnergyLinkItem.data(s,t);return s;}
    private static final java.util.List<String> APPEARANCES=appearances();
    private static java.util.List<String> appearances(){try(var in=PlaceholderItem.class.getResourceAsStream("/data/modularmagic/mimic_appearances.json")){if(in==null)throw new IllegalStateException("Missing mimic appearances");var list=new java.util.ArrayList<String>();com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonArray().forEach(v->list.add(v.getAsString()));return java.util.List.copyOf(list);}catch(java.io.IOException e){throw new ExceptionInInitializerError(e);}}
    public static float appearance(ItemStack stack){var target=target(stack);if(target==null)return 0;int index=APPEARANCES.indexOf(target.toString());return index<0?1:index+2;}
    @Override public Component getName(ItemStack stack){var id=target(stack);return id==null?Component.translatable("item.modularmagic.item_placeholder"):boundName(id);}
    private static Component boundName(ResourceLocation id){var block=BuiltInRegistries.BLOCK.get(id);var family=MimicFamily.of(block);return Component.translatable(family==null?"placeholder.modularmagic.name":"placeholder.modularmagic."+family.id+"_name",block.getName());}
    @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand hand){var stack=p.getItemInHand(hand);if(target(stack)!=null)return InteractionResultHolder.pass(stack);if(p instanceof ServerPlayer server)server.openMenu(new SimpleMenuProvider((id,inv,user)->new PlaceholderMenu(id,inv,stack),Component.translatable("placeholder.modularmagic.title")),buf->{});return InteractionResultHolder.sidedSuccess(stack,l.isClientSide);}
    @Override public InteractionResult useOn(UseOnContext c){var id=target(c.getItemInHand());if(id==null)return use(c.getLevel(),c.getPlayer(),c.getHand()).getResult();if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
        var placement=new net.minecraft.world.item.context.BlockPlaceContext(c);var pos=placement.getClickedPos();
        var block=BuiltInRegistries.BLOCK.get(id);var state=block.getStateForPlacement(placement);if(state==null)return InteractionResult.FAIL;
        for(var property:state.getProperties())if(java.util.Set.of("age","honey_level","charges","moisture").contains(property.getName()))state=freshValue(state,property);
        if(!state.canSurvive(c.getLevel(),pos))return InteractionResult.FAIL;
        double base=cn.nulladev.modularmagic.spell.billing.CostRules.mana("action/set_block",cn.nulladev.modularmagic.spell.billing.CostRules.defaults());
        boolean okay=MaterialPlacement.placeMimic(p,pos,state,base,id);return okay?InteractionResult.SUCCESS:InteractionResult.FAIL;
    }
    private static <T extends Comparable<T>> net.minecraft.world.level.block.state.BlockState freshValue(net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.level.block.state.properties.Property<T> property){return state.setValue(property,state.getBlock().defaultBlockState().getValue(property));}
    @Override public void appendHoverText(ItemStack s,TooltipContext ctx,List<Component> lines,TooltipFlag flag){var id=target(s);if(id!=null){double cost=MaterialPlacement.fee(BuiltInRegistries.BLOCK.get(id).defaultBlockState());if(Double.isFinite(cost))lines.add(Component.translatable("placeholder.modularmagic.fee",cn.nulladev.modularmagic.util.AetherNumbers.format((long)Math.ceil(cost))));}}
}
