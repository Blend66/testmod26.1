package com.testmod.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.testmod.TestMod;
import com.testmod.client.render.CustomRender;
import com.testmod.client.render.HurtBoxRenderLayer;
import com.testmod.client.util.OrientedBoundingBox;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;

public class TestModClient implements ClientModInitializer {

	public static final double DRAG_DISTANCE = 3d;
	private static final double DRAG_HEIGHT_OFFSET = 1.5d;
	public static boolean debug_mode = false;
	public static final KeyMapping test_key = new KeyMapping("key.test_mod.test", InputConstants.Type.KEYSYM, InputConstants.KEY_F, KeyMapping.Category.DEBUG);
	OrientedBoundingBox test = new OrientedBoundingBox(3f, 4f, 3f, new Vec3(0f, -40f, 0f), new Vec3(0f, 0f, 0f), new Matrix3f().identity());
	OrientedBoundingBox test_2 = new OrientedBoundingBox(2f, 2f, 2f, new Vec3(5f, -40f, 0f), new Vec3(0f, 0f, 0f), new Matrix3f().identity());
	@Override
	public void onInitializeClient() {
		ClientEntityEvents.ENTITY_LOAD.register(PlayerOBBInitializer::onEntityLoad);
		ClientEntityEvents.ENTITY_UNLOAD.register(PlayerOBBInitializer::onEntityUnload);
		KeyMappingHelper.registerKeyMapping(test_key);
		LivingEntityRenderLayerRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
			if (entityRenderer instanceof AvatarRenderer<?> avatarEntityRenderer) {
				registrationHelper.register(new HurtBoxRenderLayer(avatarEntityRenderer));
			}
		});
		LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(CustomRender::renderAndDrawBox);
		LevelRenderEvents.END_EXTRACTION.register(CustomRender::extractOBBInfo);
		ClientTickEvents.START_CLIENT_TICK.register(OBBManager::tickOBBs);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
		if (Minecraft.getInstance().level != null){
			CustomRender.age++;
			if (client.player != null){
				OBBManager.checkIntersections();
				OBBManager.tick();
				//OBBManager.update(false);
				//TestMod.LOGGER.debug("OBB count: {}\nPlayer count: {}", OBBManager.getOBBcount(), OBBManager.getPlayercount());
				debug_mode = test_key.consumeClick();
					/*
					Vec3 playerPos = client.player.position();
					Vec3 lookAt = client.player.getLookAngle();
					Vec3 newPos = new Vec3(playerPos.x + lookAt.x * DRAG_DISTANCE, playerPos.y + lookAt.y * DRAG_DISTANCE + DRAG_HEIGHT_OFFSET, playerPos.z + lookAt.z * DRAG_DISTANCE);
					test.setPosition(newPos);
					Vec3 worldUp = Vec3.Y_AXIS; // (0, 1, 0)
					Vec3 right = lookAt.cross(worldUp);

					if (right.lengthSqr() < 1e-6f) {
						right = new Vec3(1, 0, 0);
					} else {
						right = right.normalize();
					}

					Vec3 up = right.cross(lookAt).normalize();

					//Matrix3f rotation = new Matrix3f()
					//.setColumn(0, new Vector3f((float)right.x, (float)right.y, (float)right.z))
					//.setColumn(1, new Vector3f((float)up.x, (float)up.y, (float)up.z))
					//.setColumn(2, new Vector3f(-(float)lookAt.x, -(float)lookAt.y, -(float)lookAt.z));
					Matrix3f rotation = new Matrix3f().identity().rotateY(CustomRender.age * 0.01f);
					test.setRotation(rotation);
				}*/
			}
		}
		});
	}
}