package com.testmod.client.mixin;

import com.testmod.TestMod;
import com.testmod.client.extensions.AvatarRenderStateExtension;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin<AvatarLikeEntity extends Avatar & ClientAvatarEntity> {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void setUUID(AvatarLikeEntity entity,
                         AvatarRenderState state,
                         float partialTicks,
                         CallbackInfo ci){
    if (state instanceof AvatarRenderStateExtension accessor){
        accessor.test$setUUID(entity.getUUID());
    }
    }
}
