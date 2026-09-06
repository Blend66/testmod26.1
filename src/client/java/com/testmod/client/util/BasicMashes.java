package com.testmod.client.util;

import com.testmod.client.render.OBBRenderState;
import net.fabricmc.loader.impl.lib.sat4j.core.Vec;
import net.minecraft.world.phys.Vec3;
import org.joml.*;

import java.nio.ByteBuffer;
import java.util.Vector;

public class BasicMashes {
    public static int VertexSize = 16;
    public static int IndexSize = 4;
    public static Vector4f baseVertexColor = new Vector4f(1f, 0f, 0f, 1f);
    public static class BufferInfo{
        public ByteBuffer vertexBuffer;
        public ByteBuffer indexBuffer;
        public ByteBuffer uniformBuffer;
        public int vertexCount(){
            if (this.vertexBuffer == null || this.vertexBuffer.capacity() == 0){
                return 0;
            }
            else{
                return this.vertexBuffer.capacity() / VertexSize;
            }
        }
        public int indexCount(){
            if (this.indexBuffer == null || this.indexBuffer.capacity() == 0){
                return 0;
            }
            else{
                return this.indexBuffer.capacity() / IndexSize;
            }
        }

    }
    public static BufferInfo createOBBBufferInfo(OrientedBoundingBox obb){ //TODO make obb parameter usable
        BufferInfo info = new BufferInfo();
        info.indexBuffer = BasicMashes.createIndexBufferForBox();
        info.vertexBuffer = BufferUtils.byteBufferForGpu(8*VertexSize);//VertexSize = 16
        return info;
    }
    public static BufferInfo createOBBBufferInfo(OBBRenderState state, Matrix4f modelView){
        BufferInfo info = createOBBMeshByteBuffers(modelView, state.rotation(), state.origin(), state.extent(), state.color());
        return info;
    }
    /*public static Vector<Vector3f> applyPositionMatrix(Vector<Vector3f> vertices, Matrix4fc positionMatrix, Vector3f globalPos){
        Vector<Vector3f> result = new Vector<>(vertices.size());
        Vector3f temp = new Vector3f();
        for (Vector3f v : vertices){
            //System.out.println(v.x + " " + v.y + " " + v.z);
            temp.set(v).sub(globalPos);
            positionMatrix.transformPosition(temp);
            temp.add(globalPos);
            result.add(new Vector3f(temp));
            //System.out.println(temp.x + " " + temp.y + " " + temp.z);
        }
        return result;
    }*/
    /*public static void uploadVertexData(BufferInfo info, Vector<Vector3f> vertices){
        for (Vector3f vertex : vertices) {
            info.vertexBuffer.putFloat(vertex.x).putFloat(vertex.y).putFloat(vertex.z).put((byte)(baseVertexColor.x * 255)).put((byte)(baseVertexColor.y * 255)).put((byte)(baseVertexColor.z * 255)).put((byte)(baseVertexColor.w * 255));
        }
        info.vertexBuffer.flip();
    }*/
    public static void setVertexBaseColor(float r, float g, float b, float a){
        baseVertexColor = new Vector4f(r, g, b, a);
    }
    public static void setVertexBaseColor(Vector4f newColor){
        baseVertexColor = newColor;
    }
    public static final Vec3[] LOCAL_CORNERS = {
            new Vec3(-0.5d, -0.5d, -0.5d), //0
            new Vec3( 0.5d, -0.5d, -0.5d), //1
            new Vec3( 0.5d, -0.5d,  0.5d), //2
            new Vec3(-0.5d, -0.5d,  0.5d), //3
            new Vec3(-0.5d,  0.5d, -0.5d), //4
            new Vec3( 0.5d,  0.5d, -0.5d), //5
            new Vec3( 0.5d,  0.5d,  0.5d), //6
            new Vec3(-0.5d,  0.5d,  0.5d)  //7
    };
    public static void createInstanceBoxMesh(BufferInfo dst){
        dst.vertexBuffer = localCornersVertices();
        dst.indexBuffer = createIndexBufferForBox();
    }
    public static ByteBuffer localCornersVertices(){
        ByteBuffer result = BufferUtils.byteBufferForGpu(LOCAL_CORNERS.length * BasicMashes.VertexSize);
        for (Vec3 corner : LOCAL_CORNERS){
            result.putFloat((float)corner.x).putFloat((float)corner.y).putFloat((float)corner.z);
        }
        result.flip();
        return  result;
    }
    //origin is chosen relative to the rectangle center
    public static BufferInfo createOBBMeshByteBuffers(Matrix4fc positionMatrix, Matrix3f rotation, Vec3 origin, Vec3 extent, Vector4f color){
        BufferInfo info = new BufferInfo();
        info.vertexBuffer = BufferUtils.byteBufferForGpu(8 * VertexSize);
        Vector3f transformed = new Vector3f();
        Vec3 vertex;
        for (int i = 0; i < 8; i++){
            vertex = LOCAL_CORNERS[i];
            vertex = vertex.multiply(extent);
            vertex = vertex.subtract(origin);
            Vec3 rotated = new Vec3(
                    rotation.m00 * vertex.x + rotation.m10 * vertex.y + rotation.m20 * vertex.z,
                    rotation.m01 * vertex.x + rotation.m11 * vertex.y + rotation.m21 * vertex.z,
                    rotation.m02 * vertex.x + rotation.m12 * vertex.y + rotation.m22 * vertex.z
            );
            positionMatrix.transformPosition(rotated.toVector3f(), transformed);
            //System.out.println(rotated.x + " " + rotated.y + " " + rotated.z);
            info.vertexBuffer
                    .putFloat(transformed.x).putFloat(transformed.y).putFloat(transformed.z)
                    .put((byte)(color.x * 255)).put((byte)(color.y * 255)).put((byte)(color.z * 255)).put((byte)(color.w * 255));
        }
        info.indexBuffer = BasicMashes.createIndexBufferForBox();
        info.vertexBuffer.flip();
        return info;
    }
    public static ByteBuffer createIndexBufferForBox(){
        ByteBuffer result = BufferUtils.byteBufferForGpu(12*3*4);
        result
        // Front (-Z): CCW when viewed from front
        .putInt(0).putInt(1).putInt(5)
        .putInt(0).putInt(5).putInt(4)

        // Back (+Z): CCW when viewed from back
       .putInt(3).putInt(7).putInt(6)
        .putInt(3).putInt(6).putInt(2)

        // Top (+Y): CCW when viewed from above
        .putInt(4).putInt(5).putInt(6)
        .putInt(4).putInt(6).putInt(7)

        // Bottom (-Y): CCW when viewed from below
        .putInt(0).putInt(3).putInt(2)
        .putInt(0).putInt(2).putInt(1)

        // Right (+X): CCW when viewed from right
        .putInt(1).putInt(2).putInt(6)
        .putInt(1).putInt(6).putInt(5)

        // Left (-X): CCW when viewed from left
        .putInt(0).putInt(4).putInt(7)
        .putInt(0).putInt(7).putInt(3);
        result.flip();
        return result;
    }
}
