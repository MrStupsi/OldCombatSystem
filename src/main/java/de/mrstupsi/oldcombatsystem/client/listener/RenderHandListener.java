package de.mrstupsi.oldcombatsystem.client.listener;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import de.mrstupsi.oldcombatsystem.mixin.client.accessor.ItemInHandRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class RenderHandListener {
    @SubscribeEvent
    public void onRenderSword(RenderHandEvent e) {
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p.getUsedItemHand() == e.getHand() && p.isBlocking()) {
            ItemInHandRenderer itemRenderer = mc.gameRenderer.itemInHandRenderer;
            PoseStack matrixStack = e.getPoseStack();
            matrixStack.pushPose();
            boolean isMainHand = e.getHand() == InteractionHand.MAIN_HAND;
            HumanoidArm handSide = isMainHand ? p.getMainArm() : p.getMainArm().getOpposite();
            boolean isHandSideRight = handSide == HumanoidArm.RIGHT;
            ((ItemInHandRendererAccessor) itemRenderer).callApplyItemArmTransform(matrixStack, handSide, e.getEquipProgress());
            ((ItemInHandRendererAccessor) itemRenderer).callApplyItemArmAttackTransform(matrixStack, handSide, e.getSwingProgress());
            this.transformBlockFirstPerson(matrixStack, handSide);
            itemRenderer.renderItem(p, e.getItemStack(), isHandSideRight ? ItemTransforms.TransformType.FIRST_PERSON_RIGHT_HAND : ItemTransforms.TransformType.FIRST_PERSON_LEFT_HAND, !isHandSideRight, matrixStack, e.getMultiBufferSource(), e.getPackedLight());
            matrixStack.popPose();
            e.setCanceled(true);
        }
    }

    private void transformBlockFirstPerson(PoseStack matrixStack, HumanoidArm hand) {
        int signum = hand == HumanoidArm.RIGHT ? 1 : -1;
        matrixStack.translate(signum * -0.14142136F, 0.08F, 0.14142136F);
        matrixStack.mulPose(Vector3f.XP.rotationDegrees(-102.25F));
        matrixStack.mulPose(Vector3f.YP.rotationDegrees(signum * 13.365F));
        matrixStack.mulPose(Vector3f.ZP.rotationDegrees(signum * 78.05F));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onRenderHand(RenderHandEvent e) {
        ItemStack stack = e.getItemStack();
        if (stack.isEmpty() || stack.is(Items.FILLED_MAP)) return;
        final Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == e.getHand()) {
            e.setCanceled(true);
            ItemInHandRenderer itemRenderer = minecraft.gameRenderer.itemInHandRenderer;
            PoseStack matrixStack = e.getPoseStack();
            float partialTicks = e.getPartialTick();
            float equippedProgress = e.getEquipProgress();
            float swingProgress = e.getSwingProgress();
            boolean isMainHand = e.getHand() == InteractionHand.MAIN_HAND;
            HumanoidArm handSide = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
            boolean isHandSideRight = (isMainHand ? player.getMainArm() : player.getMainArm().getOpposite()) == HumanoidArm.RIGHT;
            matrixStack.pushPose();
            switch (stack.getUseAnimation()) {
                case NONE, BLOCK -> {
                    ((ItemInHandRendererAccessor) itemRenderer).callApplyItemArmTransform(matrixStack, handSide, equippedProgress);
                    ((ItemInHandRendererAccessor) itemRenderer).callApplyItemArmAttackTransform(matrixStack, handSide, swingProgress);
                }
                case EAT, DRINK -> {
                    ((ItemInHandRendererAccessor) itemRenderer).callApplyEatTransform(matrixStack, partialTicks, handSide, stack);
                    ((ItemInHandRendererAccessor) itemRenderer).callApplyItemArmTransform(matrixStack, handSide, equippedProgress);
                    ((ItemInHandRendererAccessor) itemRenderer).callApplyItemArmAttackTransform(matrixStack, handSide, swingProgress);
                }
                case BOW -> {
                    ((ItemInHandRendererAccessor) itemRenderer).callApplyItemArmTransform(matrixStack, handSide, equippedProgress);
                    ((ItemInHandRendererAccessor) itemRenderer).callApplyItemArmAttackTransform(matrixStack, handSide, swingProgress);
                    this.applyBowTransform(matrixStack, partialTicks, handSide, stack, minecraft);
                }
                case SPEAR -> {
                    ((ItemInHandRendererAccessor) itemRenderer).callApplyItemArmTransform(matrixStack, handSide, equippedProgress);
                    ((ItemInHandRendererAccessor) itemRenderer).callApplyItemArmAttackTransform(matrixStack, handSide, swingProgress);
                    this.applyTridentTransform(matrixStack, partialTicks, handSide, stack, minecraft);
                }
                case CROSSBOW -> {
                    ((ItemInHandRendererAccessor) itemRenderer).callApplyItemArmTransform(matrixStack, handSide, equippedProgress);
                    ((ItemInHandRendererAccessor) itemRenderer).callApplyItemArmAttackTransform(matrixStack, handSide, swingProgress);
                    this.applyCrossbowTransform(matrixStack, partialTicks, handSide, stack, minecraft);
                }
            }
            minecraft.gameRenderer.itemInHandRenderer.renderItem(player, stack, isHandSideRight ? ItemTransforms.TransformType.FIRST_PERSON_RIGHT_HAND : ItemTransforms.TransformType.FIRST_PERSON_LEFT_HAND, !isHandSideRight, matrixStack, e.getMultiBufferSource(), e.getPackedLight());
            matrixStack.popPose();
        }
    }

    private void applyBowTransform(PoseStack matrixStackIn, float partialTicks, HumanoidArm handside, ItemStack stack, Minecraft minecraft) {
        int sideSignum = handside == HumanoidArm.RIGHT ? 1 : -1;
        matrixStackIn.translate(sideSignum * -0.2785682F, 0.18344387F, 0.15731531F);
        matrixStackIn.mulPose(Vector3f.XP.rotationDegrees(-13.935F));
        matrixStackIn.mulPose(Vector3f.YP.rotationDegrees(sideSignum * 35.3F));
        matrixStackIn.mulPose(Vector3f.ZP.rotationDegrees(sideSignum * -9.785F));
        float f8 = stack.getUseDuration() - ((minecraft.player != null ? minecraft.player.getUseItemRemainingTicks() : 0.0F) - partialTicks + 1.0F);
        float f12 = f8 / 20.0F;
        f12 = (f12 * f12 + f12 * 2.0F) / 3.0F;
        if (f12 > 1.0F) {
            f12 = 1.0F;
        }
        if (f12 > 0.1F) {
            float f15 = Mth.sin((f8 - 0.1F) * 1.3F);
            float f18 = f12 - 0.1F;
            float f20 = f15 * f18;
            matrixStackIn.translate(f20 * 0.0F, f20 * 0.004F, f20 * 0.0F);
        }
        matrixStackIn.translate(f12 * 0.0F, f12 * 0.0F, f12 * 0.04F);
        matrixStackIn.scale(1.0F, 1.0F, 1.0F + f12 * 0.2F);
        matrixStackIn.mulPose(Vector3f.YN.rotationDegrees(sideSignum * 45.0F));
    }

    private void applyTridentTransform(PoseStack matrixStackIn, float partialTicks, HumanoidArm handside, ItemStack stack, Minecraft minecraft) {
        int sideSignum = handside == HumanoidArm.RIGHT ? 1 : -1;
        matrixStackIn.translate(sideSignum * -0.5F, 0.7F, 0.1F);
        matrixStackIn.mulPose(Vector3f.XP.rotationDegrees(-55.0F));
        matrixStackIn.mulPose(Vector3f.YP.rotationDegrees(sideSignum * 35.3F));
        matrixStackIn.mulPose(Vector3f.ZP.rotationDegrees(sideSignum * -9.785F));
        float f7 = stack.getUseDuration() - ((minecraft.player != null ? minecraft.player.getUseItemRemainingTicks() : 0.0F) - partialTicks + 1.0F);
        float f11 = f7 / 10.0F;
        if (f11 > 1.0F) {
            f11 = 1.0F;
        }
        if (f11 > 0.1F) {
            float f14 = Mth.sin((f7 - 0.1F) * 1.3F);
            float f17 = f11 - 0.1F;
            float f19 = f14 * f17;
            matrixStackIn.translate(f19 * 0.0F, f19 * 0.004F, f19 * 0.0F);
        }
        matrixStackIn.translate(0.0D, 0.0D, f11 * 0.2F);
        matrixStackIn.scale(1.0F, 1.0F, 1.0F + f11 * 0.2F);
        matrixStackIn.mulPose(Vector3f.YN.rotationDegrees(sideSignum * 45.0F));
    }

    private void applyCrossbowTransform(PoseStack matrixStackIn, float partialTicks, HumanoidArm handside, ItemStack stack, Minecraft minecraft) {
        int sideSignum = handside == HumanoidArm.RIGHT ? 1 : -1;
        matrixStackIn.translate(sideSignum * -0.4785682F, -0.094387F, 0.05731531F);
        matrixStackIn.mulPose(Vector3f.XP.rotationDegrees(-11.935F));
        matrixStackIn.mulPose(Vector3f.YP.rotationDegrees(sideSignum * 65.3F));
        matrixStackIn.mulPose(Vector3f.ZP.rotationDegrees(sideSignum * -9.785F));
        float f9 = stack.getUseDuration() - (minecraft.player.getUseItemRemainingTicks() - partialTicks + 1.0F);
        float f13 = f9 / CrossbowItem.getChargeDuration(stack);
        if (f13 > 1.0F) {
            f13 = 1.0F;
        }
        if (f13 > 0.1F) {
            float f16 = Mth.sin((f9 - 0.1F) * 1.3F);
            float f3 = f13 - 0.1F;
            float f4 = f16 * f3;
            matrixStackIn.translate(f4 * 0.0F, f4 * 0.004F, f4 * 0.0F);
        }
        matrixStackIn.translate(f13 * 0.0F, f13 * 0.0F, f13 * 0.04F);
        matrixStackIn.scale(1.0F, 1.0F, 1.0F + f13 * 0.2F);
        matrixStackIn.mulPose(Vector3f.YN.rotationDegrees(sideSignum * 45.0F));
    }
}