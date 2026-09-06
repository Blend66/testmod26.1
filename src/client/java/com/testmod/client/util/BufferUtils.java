package com.testmod.client.util;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.testmod.TestMod;
import com.testmod.client.render.CustomRender;
import com.testmod.client.render.OBBRenderState;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.world.phys.Vec3;
import org.joml.*;
import org.lwjgl.system.MemoryUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.Math;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class BufferUtils {
    public static BufferedReader reader;
    public static ByteBuffer createVertexBufferFromFile(String path, Matrix4fc positionMatrix){
        int VertexSize = 16;
        try (FileReader file = new FileReader(path)){
            reader = new BufferedReader(file);
            String st;
            Vector<Vertex> vertices = new Vector<>();
            while ((st = reader.readLine()) != null){
                String[] sts = st.split(" ");
                if (sts[0].equals("v")){
                    Vertex vertex = new Vertex(Float.parseFloat(sts[1]), Float.parseFloat(sts[2]),Float.parseFloat(sts[3]), 1f,0f, 0f, 0f);
                    System.out.println(vertex.x + " " + vertex.y + " " + vertex.z + " " + vertex.r + " " + vertex.g + " " + vertex.b + " " + vertex.a + " ");
                    Vector3f pos = positionMatrix.transformPosition(vertex.x, vertex.y, vertex.z, new Vector3f());
                    vertex.x = pos.x;
                    vertex.y = pos.y;
                    vertex.z = pos.z;
                    vertices.add(vertex);
                }
            }
            if (vertices.isEmpty()){
                System.err.println("ERROR: No vertices was parsed");
            }
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(vertices.size() * VertexSize).order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < vertices.size(); i++){
                byteBuffer.putFloat(vertices.get(i).x);
                byteBuffer.putFloat(vertices.get(i).y);
                byteBuffer.putFloat(vertices.get(i).z);
                byteBuffer.put((byte)255);
                byteBuffer.put((byte)0);
                byteBuffer.put((byte)0);
                byteBuffer.put((byte)255);
            }
            if (vertices.isEmpty()) {
                throw new IllegalArgumentException("Data cannot be null or empty for vertex buffer");
            }
            if (byteBuffer.position() == 0) {
                throw new RuntimeException("Buffer position is 0! Data was not written.");
            }
            byteBuffer.flip();
            return byteBuffer;
        } catch (IOException e) {
            System.out.println("Cant read file, wrong path or not existing");
            throw new RuntimeException(e);
        }
    }
    /*public static void createUniformInstanceBuffer(Vector<OBBRenderState> states, Matrix4f viewMatrix, BasicMashes.BufferInfo dst){
        int instanceCount = Math.min(CustomRender.maxInstances, states.size());
        if (instanceCount == CustomRender.maxInstances){
            TestMod.LOGGER.debug("WARNING: reached max render instances (maxInstances: " + CustomRender.maxInstances + ")");
        }
        ByteBuffer result = byteBufferForGpu(instanceCount * BasicMashes.InstanceUniformBufferSize);
        for (int i = 0; i < instanceCount; i++){
            OBBRenderState state = states.get(i);
            Quaternionf rotation = new Quaternionf();
            state.rotation().getNormalizedRotation(rotation);
            Matrix4f modelView = new Matrix4f(viewMatrix).translate(state.position().toVector3f()).rotate(rotation);
            Vec3 scale = state.extent().scale(2f);
            Vec3 origin = state.origin();
            Vector4f color = state.color();

            modelView.get(result); // modelView matrix
            result.putFloat((float)scale.x).putFloat((float)scale.y).putFloat((float)scale.z); //scale vec3
            result.putInt(0); //padding
            result.putFloat((float)origin.x).putFloat((float)origin.y).putFloat((float) origin.z); //origin vec3
            result.putInt(0); //padding
            result.putFloat(color.x).putFloat(color.y).putFloat(color.z).putFloat(color.w); //color vec4
        }
        result.flip();
        dst.uniformBuffer = result;
    }*/
    public static ByteBuffer createIndexBufferFromFile(String path){
        ByteBuffer byteBuffer;
        try (FileReader file = new FileReader(path)) {
            reader = new BufferedReader(file);
            String st;
            List<Integer> data = new ArrayList<>();
            while ((st = reader.readLine()) != null) {
                String[] sts = st.split(" ");
                if (sts[0].equals("f")) {
                    for (int i = 1; i < 4; i += 1) {
                        String[] spl = sts[i].split("/");
                        data.add(Integer.parseInt(spl[0])-1);
                    }
                }
            }
            if (data.isEmpty()) {
                throw new IllegalArgumentException("Data cannot be null or empty for index buffer");
            }
            byteBuffer = ByteBuffer.allocateDirect(data.size() * 4).order(ByteOrder.LITTLE_ENDIAN);
            for (Integer i : data){
                byteBuffer.putInt(i);
            }
            byteBuffer.flip();
            return byteBuffer;
        } catch (IOException e) {
            System.out.println("Cant read file, wrong path or not existing");
            throw new RuntimeException(e);
        }
    }
    public static GpuBuffer.MappedView mapBuffer(ByteBuffer data, GpuBuffer buffer){
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(buffer, false, true)){
            MemoryUtil.memCopy(data, mappedView.data());
            return mappedView;
        }
    }
    public static ByteBuffer byteBufferForGpu(int size){
        ByteBuffer byteBuffer;
        byteBuffer = ByteBuffer.allocateDirect(size).order(ByteOrder.LITTLE_ENDIAN);
        return byteBuffer;
    }
    public static BasicMashes.BufferInfo mergeBuffers(Vector<BasicMashes.BufferInfo> buffers){
        if (buffers == null || buffers.isEmpty()){
            System.err.println("cant merge null/empty buffers");
            throw new RuntimeException();
        }
        for (int i = 0; i < buffers.size(); i++) {
            var buf = buffers.get(i);
            if (buf.vertexBuffer == null || buf.indexBuffer == null) {
                throw new IllegalStateException("Buffer " + i + " has null vertex/index buffer");
            }
            if (buf.vertexCount() <= 0 || buf.indexCount() <= 0) {
                throw new IllegalStateException("Buffer " + i + " has zero vertices/indices");
            }
            // Check if buffer was already closed/deallocated
            try {
                long addr = MemoryUtil.memAddress(buf.vertexBuffer);
                if (addr == 0) {
                    throw new IllegalStateException("Buffer " + i + " vertex memory is freed (address=0)");
                }
            } catch (Exception e) {
                throw new IllegalStateException("Buffer " + i + " vertex buffer is invalid", e);
            }
        }
        BasicMashes.BufferInfo result = new BasicMashes.BufferInfo();

        Vector<Long> buffer_offsets_vertex = new Vector<>(buffers.size());
        buffer_offsets_vertex.add((long)0);

        Vector<Integer> offsets_index = new Vector<>(buffers.size());
        offsets_index.add(0);

        Vector<Long> buffer_offsets_index = new Vector<>(buffers.size());
        buffer_offsets_index.add((long)0);

        int total_size_vertex = buffers.get(0).vertexCount() * BasicMashes.VertexSize;
        int total_size_index = buffers.get(0).indexCount() * BasicMashes.IndexSize;

        for (int i = 1; i < buffers.size(); i++){
            buffer_offsets_vertex.add((long) buffers.get(i - 1).vertexCount() * BasicMashes.VertexSize + buffer_offsets_vertex.get(i-1));

            offsets_index.add(buffers.get(i - 1).vertexCount() + offsets_index.get(i-1));

            buffer_offsets_index.add((long) buffers.get(i-1).indexCount() * BasicMashes.IndexSize + buffer_offsets_index.get(i-1));

            total_size_vertex += buffers.get(i).vertexCount() * BasicMashes.VertexSize;
            total_size_index += buffers.get(i).indexCount() * BasicMashes.IndexSize;
        }
        result.vertexBuffer = BufferUtils.byteBufferForGpu(total_size_vertex);
        result.indexBuffer = BufferUtils.byteBufferForGpu(total_size_index);
        long result_address_vertex = MemoryUtil.memAddress(result.vertexBuffer);
        for (int i = 0; i < buffers.size(); i++){
            MemoryUtil.memCopy(
                    MemoryUtil.memAddress(buffers.get(i).vertexBuffer),
                    result_address_vertex + buffer_offsets_vertex.get(i),
                    (long)buffers.get(i).vertexCount() * BasicMashes.VertexSize
                    );
            buffers.get(i).indexBuffer.rewind();
            for (int j = 0; j < buffers.get(i).indexCount(); j++){
                int original_index = buffers.get(i).indexBuffer.getInt();
                long writePos = buffer_offsets_index.get(i) + (long)j * BasicMashes.IndexSize;
                result.indexBuffer.putInt((int)writePos, original_index + offsets_index.get(i));
            }
        }
        result.vertexBuffer.rewind();
        result.indexBuffer.rewind();
        /*System.out.println(
                result.vertexBuffer.getFloat() + " " //x
                + result.vertexBuffer.getFloat() + " " //y
                + result.vertexBuffer.getFloat() + " " //z
                + result.vertexBuffer.get() + " " //r
                + result.vertexBuffer.get() + " " //g
                + result.vertexBuffer.get() + " " //b
                + result.vertexBuffer.get() + " "//a
        );
        System.out.println(
                result.indexBuffer.getInt() + " " //0
                        + result.indexBuffer.getInt() + " " //1
                        + result.indexBuffer.getInt() + " " //5
                        + result.indexBuffer.getInt() + " " //0
                        + result.indexBuffer.getInt() + " " //5
                        + result.indexBuffer.getInt() + " " //4
        );*/
        result.vertexBuffer.position(0);
        result.vertexBuffer.limit(total_size_vertex);
        result.indexBuffer.position(0);
        result.indexBuffer.limit(total_size_index);
        return result;
    }
    //Useless not working garbage

    /*private static BasicMashes.BufferInfo mergeTwoBuffers(BasicMashes.BufferInfo a, BasicMashes.BufferInfo b){
        BasicMashes.BufferInfo result = new BasicMashes.BufferInfo();

        ByteBuffer merged_vertex = BufferUtils.byteBufferForGpu((a.vertexCount() + b.vertexCount()) * BasicMashes.VertexSize);
        long offset_a = (long) a.vertexCount() * BasicMashes.VertexSize;
        a.vertexBuffer.rewind();
        b.vertexBuffer.rewind();
        MemoryUtil.memCopy(
                MemoryUtil.memAddress(a.vertexBuffer),
                MemoryUtil.memAddress(merged_vertex),
                offset_a);
        MemoryUtil.memCopy(
                MemoryUtil.memAddress(b.vertexBuffer),
                MemoryUtil.memAddress(merged_vertex) + offset_a,
                (long) b.vertexCount() * BasicMashes.VertexSize);
        merged_vertex.flip();

        ByteBuffer merged_index = BufferUtils.byteBufferForGpu((a.indexCount() + b.indexCount()) * BasicMashes.IndexSize);
        a.indexBuffer.rewind();
        b.indexBuffer.rewind();
        offset_a = (long) a.indexCount() * BasicMashes.IndexSize;
        MemoryUtil.memCopy(
                MemoryUtil.memAddress(a.indexBuffer),
                MemoryUtil.memAddress(merged_index),
                offset_a);

        int index_offset = a.vertexCount();
        for (int i = 0; i < b.indexCount(); i++){
            int originalIndex = b.indexBuffer.getInt();
            merged_index.putInt((int)(offset_a + (long)i * BasicMashes.IndexSize), originalIndex + index_offset);
        }
        merged_index.position((int)(offset_a + (long)b.indexCount() * BasicMashes.IndexSize));
        merged_index.flip();

        result.vertexBuffer = merged_vertex;
        result.indexBuffer = merged_index;
        return result;
    }*/
}
