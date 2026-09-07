package com.testmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.testmod.TestMod;
import com.testmod.client.render.CustomRender;
import com.testmod.client.render.HurtBoxRenderLayer;
import com.testmod.client.render.OBBRenderState;
import com.testmod.client.util.OrientedBoundingBox;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.minecraft.client.ClientClockManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.*;

public class OBBManager {
    private static final Map<UUID, OrientedBoundingBox> OBB_map = new HashMap<>();
    private static final Map<UUID, ArrayList<OrientedBoundingBox>> PlayerToOBBS_map = new HashMap<>();
    private static final Map<UUID, PlayerModelPart> OBBuuidToBodyPart = new HashMap<>();
    private static final Vector<Player> players = new Vector<>();
    private static final Vector<UUID> ids = new Vector<>();
    public static int getOBBcount(){
        return ids.size();
    }
    public static int getPlayercount(){
        return players.size();
    }
    public static void addPlayerOBBs(Player player){
        OrientedBoundingBox body = new OrientedBoundingBox(0.66f, 0.5f, 0.33f,
                new Vec3(player.getX(), player.getY(), player.getZ()),
                Vec3.ZERO, new Matrix3f().identity().rotateY((float)Math.toRadians(player.yBodyRot)));
        ArrayList<OrientedBoundingBox> parts = new ArrayList<>();
        players.add(player);
        parts.add(body);
        PlayerToOBBS_map.put(player.getUUID(), parts);
        OBBuuidToBodyPart.put(body.getId(), PlayerModelPart.JACKET);
    }
    public static void removePlayerOBBs(Player player){
        ArrayList<OrientedBoundingBox> obbsToRemove = PlayerToOBBS_map.get(player.getUUID());
        for (OrientedBoundingBox obb : obbsToRemove){
            OBB_map.remove(obb.getId());
            ids.remove(obb.getId());
            OBBuuidToBodyPart.remove(obb.getId());
        }
        players.remove(player);
        PlayerToOBBS_map.remove(player.getUUID());
    }

    public static UUID registerOBB(OrientedBoundingBox newOBB){
        ids.add(UUID.randomUUID());
        OBB_map.put(ids.lastElement(), newOBB);
        return ids.lastElement();
    }
    public static void checkIntersections(){
        if (ids.isEmpty()){return;}
        for (UUID id : ids) {
            OBB_map.get(id).Intersects.clear();
            OBB_map.get(id).updateVertices();
        }
        for (int i = 0; i < ids.size(); i++){
            for (int j = i + 1; j < ids.size(); j++){
                OrientedBoundingBox a = OBB_map.get(ids.get(i));
                OrientedBoundingBox b = OBB_map.get(ids.get(j));
                if (OrientedBoundingBox.Intersects(a,b)){
                    a.Intersects.add(b.getId());
                    b.Intersects.add(a.getId());
                }
            }
        }
    }
    public static void deleteOBB(UUID id){
        OBB_map.remove(id);
    }
    public static void captureStartValues(Minecraft client){
        if (ids.isEmpty()){return;}
        for (UUID id : ids) {
            OBB_map.get(id).tick();
        }
    }
    //TODO make proper logging
    public static void update(float bodyRot, Vec3 position, boolean enableLogging){ //VERY SLOW WITH LOGGING. MAY CREATE VISUAL BUGS VIA DESYNC
        for(Player player : players){
            for (OrientedBoundingBox obb : PlayerToOBBS_map.get(player.getUUID())){
                obb.setPosition(position);
                Matrix3f rotation = new Matrix3f().identity().rotateY((float)Math.toRadians(-bodyRot));
                obb.setRotation(rotation);

            }

        }
    }
    private void logEverything(float bodyRot){
        long start = System.nanoTime();
        Thread current = Thread.currentThread();
        boolean isValidThread = current.getName().equals("Render thread")      // Singleplayer
                || current.getName().equals("Client thread")       // Multiplayer client
                || current.getName().equals("Server thread");      // Dedicated server
        /*System.out.printf("Y-Rot Matrix det=%.4f | col0=(%.3f,%.3f,%.3f)%n",
                        rotation.determinant(),
                        rotation.m00(), rotation.m10(), rotation.m20()
                );*/
        /*System.out.printf("OBB World Pos: (%.3f, %.3f, %.3f) | Camera Pos: (%.3f, %.3f, %.3f)%n",
                        //obb.pos().x, obb.pos().y, obb.pos().z,
                        //state.x, state.y, state.z
                );*/
        if (!isValidThread){
            TestMod.LOGGER.error("⚠️ OBB UPDATE ON WRONG THREAD: {} | Stack:", current.getName());
            Thread.dumpStack(); // Prints full call stack to log
            return; // Abort to prevent corruption
        }
        TestMod.LOGGER.debug("[GAME] OBB updated at tick={} rot={}",
                Minecraft.getInstance().level.getGameTime(), bodyRot);
        /*long elapsed = System.nanoTime() - start;
            if (elapsed > 100_000) { // Only log if suspiciously slow
                TestMod.LOGGER.warn("OBB update took {}ns", elapsed);
            }*/
    }
    public static Vector<OBBRenderState> extractRenderStates(float partialTicks){
        Vector<OBBRenderState> states = new Vector<>();
        if (OBB_map != null){
            for (UUID id : ids){
                OrientedBoundingBox current = OBB_map.get(id);
                Vector4f color = new Vector4f(0f, 1f, 0f, 1f);
                if (!current.Intersects.isEmpty()){
                    color.set(1f,0f,0f,1f);
                }
                states.add(CustomRender.createInterpolatedState(current, partialTicks));
            }
        }
        return states;
    }
}
