package com.testmod.client.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.PolygonMode;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.testmod.TestMod;
import com.testmod.client.OBBManager;
import com.testmod.client.util.BasicMashes;
import com.testmod.client.util.BufferUtils;
import com.testmod.client.util.MathUtils;
import com.testmod.client.util.OrientedBoundingBox;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.*;

import java.lang.Math;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Vector;

public final class CustomRender {

    // Define format explicitly to match BufferUtils output
    // 3 Floats (Position) + 4 UBytes (Color) = 16 bytes
    private CustomRender(){}
    private static final VertexFormat universalFormat = VertexFormat.builder().add("Position", VertexFormatElement.POSITION).add("Color", VertexFormatElement.COLOR).build();
    //private static final Identifier vertexShaderIdentifier = Identifier.fromNamespaceAndPath(TestMod.MOD_ID, "instance_position_color"); //TODO place it inside client only resources!!!
    public static final RenderPipeline UNIVERSAL_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(TestMod.MOD_ID, "pipeline/debug_test_pipeline")).withDepthStencilState(DepthStencilState.DEFAULT)
            .withVertexFormat(universalFormat, VertexFormat.Mode.TRIANGLES).withVertexShader("core/position_color").withFragmentShader("core/position_color").withCull(false).withPolygonMode(PolygonMode.WIREFRAME)
            .build();
    private static boolean DO_RENDER_PASS = false;
    private static final Vector4f COLOR_MODULATION = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f(0f, 0f, 0f);
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f().identity();
    private static BasicMashes.BufferInfo bufferInfo = new BasicMashes.BufferInfo();
    private static GpuBuffer vertexBuffer;
    private static GpuBuffer indexBuffer;
    //private static GpuBuffer uniformBuffer;
    private static int currentObjectCount;
    private static Vector<OBBRenderState> OBBRenderStates;
    private static final Vector<BasicMashes.BufferInfo> objectBuffers = new Vector<>();

    //private static final String model_path = "C:/Users/admin/Desktop/test/testmod26.1/src/client/resources/models/model.obj"; // Use resource path, not absolute OS path
    public static int renderCalls = 0;
    public static void extractOBBInfo(LevelExtractionContext context){
        OBBRenderStates = OBBManager.extractRenderStates(context.deltaTracker().getGameTimeDeltaPartialTick(true));
        if (!OBBRenderStates.isEmpty() && Minecraft.getInstance().level != null){
            DO_RENDER_PASS = true;
        }
    }
    public static OBBRenderState createInterpolatedState(OrientedBoundingBox obb, float partialTick){
        if (obb.startTickValues == null){
            TestMod.LOGGER.debug("[Extraction] previous obb position, rotation values are not recorded");
        }
        if (!(0f <= partialTick && 1f >= partialTick)){
            TestMod.LOGGER.debug("[Extraction] partial tick value are out of range with value: {}", partialTick);
            partialTick = Math.clamp(partialTick, 0, 1);
        }
        OrientedBoundingBox.DataTracker rawData = obb.startTickValues;

        Vec3 currentPos = rawData.position().lerp(obb.pos(), partialTick);
        Matrix3f currentRot = MathUtils.slerpRotation(rawData.rotation(), obb.getRotation(), partialTick);
        Vector4f color = new Vector4f(0f, 1f, 0f, 1f);
        if (!obb.Intersects.isEmpty()){
            color.set(1f, 0f, 0f, 1f);
        }
        return new OBBRenderState(currentPos, currentRot, obb.getOrigin(), obb.getExtent(), color, obb.getId());
    }
    public static void render(LevelRenderContext context){
        if (!OBBRenderStates.isEmpty())
        {
            objectBuffers.setSize(OBBRenderStates.size());
            renderMeshes(context);
        }
    }
    private static void renderMeshes(LevelRenderContext context){
        int i = 0;
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 camPos = context.levelState().cameraRenderState.pos;
        Matrix4f view =  new Matrix4f()
                .rotateX((float)Math.toRadians(camera.xRot()))
                .rotateY((float) Math.toRadians(camera.yRot() + 180.0f))
                .translate((float) -camPos.x, (float) -camPos.y, (float) -camPos.z);
        for (OBBRenderState state : OBBRenderStates){
            Matrix4f modelView = new Matrix4f(view).translate(state.position().toVector3f());
            Matrix4f model = new Matrix4f().translate(state.position().toVector3f());
            objectBuffers.set(i, BasicMashes.createOBBBufferInfo(state, model, view));
            if (objectBuffers.get(i) == null){
                throw new IllegalStateException("BufferInfo is null");
            }
            i++;
            //TestMod.LOGGER.debug("[RENDER] Vertices built at frame={} pos={}",
                    //age, state.position());
        }
    }
    public static void renderAndDrawBox(LevelRenderContext context){
        Minecraft client = Minecraft.getInstance();
        if (DO_RENDER_PASS){
            if (vertexBuffer == null || vertexBuffer.isClosed() || currentObjectCount != OBBManager.getOBBcount()){
                currentObjectCount = OBBManager.getOBBcount();
                render(context);
                setupBuffers();
            }
            else if (vertexBuffer != null && !vertexBuffer.isClosed()){
                render(context);
                updateBuffers();
                renderCalls += 1;
                draw(client, context, vertexBuffer, indexBuffer);
            }
        }
    }
    private static void updateBuffers(){
        bufferInfo = BufferUtils.mergeBuffers(objectBuffers);
        BufferUtils.mapBuffer(bufferInfo.vertexBuffer, vertexBuffer);
    }
    private static void setupBuffers() {
        try {
            GpuDevice device = RenderSystem.getDevice();
            bufferInfo = BufferUtils.mergeBuffers(objectBuffers);
            if(vertexBuffer != null)vertexBuffer.close();
            if(indexBuffer != null)indexBuffer.close();
            vertexBuffer = device.createBuffer(() -> "test vertex buffer",
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE,
                    bufferInfo.vertexBuffer);

            indexBuffer = device.createBuffer(() -> "test index buffer",
                    GpuBuffer.USAGE_INDEX | GpuBuffer.USAGE_MAP_WRITE,
                    bufferInfo.indexBuffer);
            //bufferInfo.vertexBuffer = null;
            //bufferInfo.indexBuffer = null;
            objectBuffers.clear();
            //System.out.println("BufferV prepared: pos=" + bufferInfo.vertexBuffer.position() + " lim=" + bufferInfo.vertexBuffer.limit() + "\nBufferI prepared: pos=" + bufferInfo.indexBuffer.position() + " lim=" + bufferInfo.indexBuffer.limit());
            //System.out.println("Buffers Created: V=" + vertexBuffer.size() / VertexSize + " I=" + indexBuffer.size() / 4);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static int age;
    private static void draw(Minecraft client, LevelRenderContext context, GpuBuffer vertexBuffer, GpuBuffer indexBuffer) {
        age++;
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(new Matrix4f(), COLOR_MODULATION, MODEL_OFFSET, TEXTURE_MATRIX);
        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(
                        () -> TestMod.MOD_ID + " Test RenderPass",
                        client.getMainRenderTarget().getColorTextureView(),
                        OptionalInt.empty(),
                        client.getMainRenderTarget().getDepthTextureView(),
                        OptionalDouble.empty()
                )) {
                renderPass.setPipeline(UNIVERSAL_PIPELINE);
                renderPass.setUniform("DynamicTransforms", dynamicTransforms);

                renderPass.setVertexBuffer(0, vertexBuffer);
                renderPass.setIndexBuffer(indexBuffer, VertexFormat.IndexType.INT);

                renderPass.drawIndexed(0, 0, bufferInfo.indexCount(), 1);


            //BasicMashes.setVertexBaseColor(new Vector4f((float)Math.abs(Math.sin(age * 0.01)), (float)Math.abs(Math.cos(age * 0.02)), (float)Math.abs(Math.cos(age * 0.015 + Math.PI*3/2)), 1f));
            // DO NOT CLOSE BUFFERS HERE
        }
    }

    public static void cleanup() {
        if (vertexBuffer != null) vertexBuffer.close();
        if (indexBuffer != null) indexBuffer.close();
    }
}