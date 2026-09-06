package com.testmod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.testmod.client.OBBManager;
import com.testmod.client.TestModClient;
import com.testmod.client.extensions.AvatarRenderStateExtension;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.phys.Vec3;

public class HurtBoxRenderLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    //private static final OrientedBoundingBox test = new OrientedBoundingBox(2, 2, 2, Vec3.ZERO, Vec3.ZERO, 0, 0);
    public HurtBoxRenderLayer(final RenderLayerParent<AvatarRenderState, PlayerModel> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, AvatarRenderState state, float yRot, float xRot) {
        //test.setPosition(new Vec3(state.x, state.y, state.z));
        TestModClient.bodyRot = state.bodyRot;
        TestModClient.Pos = new Vec3(state.x, state.y, state.z);
        if (state instanceof AvatarRenderStateExtension accessor){
            //System.out.println(accessor.test$getUUID().hashCode());
        }
    }
}
