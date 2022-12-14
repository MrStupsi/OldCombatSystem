package de.mrstupsi.oldcombatsystem.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlayerItemInHandLayer.class, priority = 500)
public abstract class PlayerItemInHandLayerMixin<T extends Player, M extends EntityModel<T> & ArmedModel & HeadedModel> extends ItemInHandLayer<T, M> {
    public PlayerItemInHandLayerMixin(RenderLayerParent<T, M> p_234846_, ItemInHandRenderer p_234847_) {
        super(p_234846_, p_234847_);
    }

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    protected void renderArmWithItem(LivingEntity entity, ItemStack stack, ItemTransforms.TransformType transform, HumanoidArm arm, PoseStack poseStack, MultiBufferSource multiBufferSource, int combinedLight, CallbackInfo callbackInfo) {
        if (stack.isEmpty()) return;
        if (entity.getUseItem() == stack && entity.isBlocking()) {
            this.renderBlockingWithSword(entity, stack, transform, arm, poseStack, multiBufferSource, combinedLight);
            callbackInfo.cancel();
        }
    }

    private void renderBlockingWithSword(LivingEntity entity, ItemStack stack, ItemTransforms.TransformType transform, HumanoidArm arm, PoseStack poseStack, MultiBufferSource multiBufferSource, int combinedLight) {
        poseStack.pushPose();
        this.getParentModel().translateToHand(arm, poseStack);
        boolean leftHand = arm == HumanoidArm.LEFT;
        this.applyItemBlockingTransform(poseStack, leftHand);
        this.applyItemTransformInverse(entity, stack, transform, poseStack, leftHand);
        Minecraft.getInstance().gameRenderer.itemInHandRenderer.renderItem(entity, stack, transform, leftHand, poseStack, multiBufferSource, combinedLight);
        poseStack.popPose();
    }

    private void applyItemBlockingTransform(PoseStack poseStack, boolean leftHand) {
        poseStack.translate((leftHand ? 1.0F : -1.0F) / 16.0F, 0.4375F, 0.0625F);
        poseStack.translate(leftHand ? -0.035F : 0.05F, leftHand ? 0.045F : 0.0F, leftHand ? -0.135F : -0.1F);
        poseStack.mulPose(Vector3f.YP.rotationDegrees((leftHand ? -1.0F : 1.0F) * -50.0F));
        poseStack.mulPose(Vector3f.XP.rotationDegrees(-10.0F));
        poseStack.mulPose(Vector3f.ZP.rotationDegrees((leftHand ? -1.0F : 1.0F) * -60.0F));
        poseStack.translate(0.0F, 0.1875F, 0.0F);
        poseStack.scale(0.625F, 0.625F, 0.625F);
        poseStack.mulPose(Vector3f.XP.rotationDegrees(180.0F));
        poseStack.mulPose(Vector3f.XN.rotationDegrees(-100.0F));
        poseStack.mulPose(Vector3f.YN.rotationDegrees(leftHand ? 35.0F : 45.0F));
        poseStack.translate(0.0F, -0.3F, 0.0F);
        poseStack.scale(1.5F, 1.5F, 1.5F);
        poseStack.mulPose(Vector3f.YN.rotationDegrees(50.0F));
        poseStack.mulPose(Vector3f.ZP.rotationDegrees(335.0F));
        poseStack.translate(-0.9375F, -0.0625F, 0.0F);
        poseStack.translate(0.5F, 0.5F, 0.25F);
        poseStack.mulPose(Vector3f.YN.rotationDegrees(180.0F));
        poseStack.translate(0.0F, 0.0F, 0.28125F);
    }

    private void applyItemTransformInverse(LivingEntity entity, ItemStack stack, ItemTransforms.TransformType transform, PoseStack poseStack, boolean leftHand) {
        // revert 1.8+ model changes, so we can work on a blank slate
        BakedModel model = Minecraft.getInstance().getItemRenderer().getModel(stack, entity.level, entity, 0);
        applyTransformInverse(model.getTransforms().getTransform(transform), leftHand, poseStack);
    }

    private static void applyTransformInverse(ItemTransform vec, boolean leftHand, PoseStack matrixStackIn) {
        // this does the exact inverse of ItemTransform::apply which should be applied right after, so that in the end nothing has changed
        if (vec != ItemTransform.NO_TRANSFORM) {
            float angleX = vec.rotation.x();
            float angleY = leftHand ? -vec.rotation.y() : vec.rotation.y();
            float angleZ = leftHand ? -vec.rotation.z() : vec.rotation.z();
            Quaternion quaternion = new Quaternion(angleX, angleY, angleZ, true);
            quaternion.conj();

            matrixStackIn.scale(1.0F / vec.scale.x(), 1.0F / vec.scale.y(), 1.0F / vec.scale.z());
            matrixStackIn.mulPose(quaternion);
            matrixStackIn.translate((leftHand ? -1.0F : 1.0F) * -vec.translation.x(), -vec.translation.y(), -vec.translation.z());
        }
    }
}