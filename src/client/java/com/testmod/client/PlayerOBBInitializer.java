package com.testmod.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class PlayerOBBInitializer{
    public static void onEntityLoad(Entity entity, ClientLevel level){
        if (entity instanceof Player player){
            OBBManager.addPlayerOBBs(player);
        }
    }
    public static void onEntityUnload(Entity entity, ClientLevel client){
        if (entity instanceof Player player){
            OBBManager.removePlayerOBBs(player);
        }
    }
}
