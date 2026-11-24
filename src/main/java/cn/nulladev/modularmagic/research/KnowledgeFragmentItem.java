package cn.nulladev.modularmagic.research;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public final class KnowledgeFragmentItem extends Item {
    public KnowledgeFragmentItem() {
        super(new Properties());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.success(stack);

        var unknown = ResearchCatalog.all().stream()
                .filter(topic -> !ResearchKnowledge.completed(player, topic))
                .toList();
        if (unknown.isEmpty()) {
            player.displayClientMessage(Component.translatable("research.modularmagic.fragment_complete"), true);
            return InteractionResultHolder.fail(stack);
        }

        var topic = unknown.get(player.getRandom().nextInt(unknown.size()));
        ResearchKnowledge.learn(player, topic);
        ResearchNetworking.sync(serverPlayer);
        player.displayClientMessage(Component.translatable("research.modularmagic.learned",
                Component.translatable(topic.key())), false);
        stack.consume(1, player);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.modularmagic.knowledge_fragment.hint").withStyle(ChatFormatting.GRAY));
    }
}
