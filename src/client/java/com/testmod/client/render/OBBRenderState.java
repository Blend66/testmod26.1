package com.testmod.client.render;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

/*public record OBBRenderState(
        float x0,
        float y0,
        float z0,

        float x,
        float y,
        float z,

        Quaternionf rotation0,
        Quaternionf rotation,

        Vec3 origin,
        Vec3 extent,
        Vector4f color)
{
}*/
public record OBBRenderState(
        Vec3 position,
        Matrix3f rotation,

        Vec3 origin,
        Vec3 extent,
        Vector4f color)
{
}
