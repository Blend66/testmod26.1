package com.testmod.client.mixin;

import com.testmod.client.extensions.AvatarRenderStateExtension;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.UUID;

@Mixin(AvatarRenderState.class)
public class AvatarRenderStateMixin implements AvatarRenderStateExtension {
    @Unique
    public UUID uuid;
    @Override
    public UUID test$getUUID() {
        return this.uuid;
    }

    @Override
    public void test$setUUID(UUID uuid) {
        this.uuid = uuid;
    }
}
