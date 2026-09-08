package com.testmod.client.util;

import com.testmod.TestMod;
import com.testmod.client.OBBManager;
import net.minecraft.world.phys.Vec3;
import org.joml.*;

import java.lang.Math;
import java.util.ArrayList;
import java.util.UUID;

public class OrientedBoundingBox{
    private Vec3[] vertices = new Vec3[8];
    private final Vec3 origin;
    private Vec3 position;
    private Matrix3f rotation = new Matrix3f();
    private UUID id;
    private Vec3 extent;
    public ArrayList<UUID> Intersects = new ArrayList<>();
    public DataTracker startTickValues;
    private Vec3 axisX, axisY, axisZ;
    public Vec3[] getVertices(){return this.vertices;}
    public Vec3 getOrigin(){return this.origin;}
    public Vec3 pos(){return this.position;}
    public UUID getId(){return this.id;}
    public OrientedBoundingBox(double width, double height, double depth, Vec3 position, Vec3 origin, Matrix3f rotation){
        this.extent = new Vec3( width/2, height/2, depth/2);
        this.origin = origin;
        this.position = position;
        this.id = OBBManager.registerOBB(this);
        setRotation(rotation);
        this.startTickValues = new DataTracker(this.position, this.rotation);
        create(this.extent, position, origin);
        System.out.println("created new OBB with id " + this.id.toString());
    }
    public Vec3 getExtent(){return this.extent;}

    public Matrix3f getRotation(){return this.rotation;}
    public void updateVertices(){
        for (int i = 0; i < 8; i++){
            Vec3 c = BasicMashes.LOCAL_CORNERS[i];
            Vec3 local = new Vec3(c.x * extent.x - origin.x,
                    c.y * extent.y - origin.y,
                    c.z * extent.z - origin.z);

            vertices[i] = position
                    .add(axisX.multiply(local.x, local.y, local.z))
                    .add(axisY.multiply(local.x, local.y, local.z))
                    .add(axisZ.multiply(local.x, local.y, local.z));
        }
    }
    public void tick(){
        updateVertices();
        this.startTickValues = new DataTracker(this.position, this.rotation);
    }
    public void discard()
    {
        if (this.id == null){
            TestMod.LOGGER.debug("Trying to remove already discarded obb or it's uuid was null");
            return;
        }
        if (OBBManager.hasOBB(this.id)){
            OBBManager.deleteOBB(this.id);
        }
        this.id = null;
    }
    public void setPosition(Vec3 newPos){
        this.position = newPos;
        updateVertices();
    }
    public void setRotation(Matrix3f rotation){
        this.rotation = rotation;
        float det = rotation.determinant();
        if (Math.abs(det - 1.0f) > 0.001f) {
            System.err.printf("WARNING: Non-orthonormal rotation matrix! det=%.4f%n", det);
            // Optional: reorthonormalize here
        }
        this.axisX = new Vec3(rotation.m00, rotation.m10, rotation.m20);
        this.axisY = new Vec3(rotation.m01, rotation.m11, rotation.m21);
        this.axisZ = new Vec3(rotation.m02, rotation.m12, rotation.m22);
        //this.scaledAxisX = axisX.scale(extent.x);
        //this.scaledAxisY = axisY.scale(extent.y);
        //this.scaledAxisZ = axisZ.scale(extent.z);
        updateVertices();
    }

    private void create(Vec3 extent, Vec3 position, Vec3 origin){
        for (int i = 0; i < 8; i++){
            vertices[i] = axisX.multiply(BasicMashes.LOCAL_CORNERS[i]).multiply(extent).subtract(origin).add(position);
        }
    }
    public static boolean Intersects(OrientedBoundingBox a, OrientedBoundingBox b){
        // Test 6 face normals (UNIT axes)
        if (Separated(a.vertices, b.vertices, a.axisX)) return false;
        if (Separated(a.vertices, b.vertices, a.axisY)) return false;
        if (Separated(a.vertices, b.vertices, a.axisZ)) return false;
        if (Separated(a.vertices, b.vertices, b.axisX)) return false;
        if (Separated(a.vertices, b.vertices, b.axisY)) return false;
        if (Separated(a.vertices, b.vertices, b.axisZ)) return false;

        // Test 9 edge-cross axes (must be normalized!)
        Vec3[] crossAxes = new Vec3[9];
        int idx = 0;
        Vec3[] aAxes = {a.axisX, a.axisY, a.axisZ};
        Vec3[] bAxes = {b.axisX, b.axisY, b.axisZ};

        for (Vec3 ax : aAxes) {
            for (Vec3 bx : bAxes) {
                Vec3 cross = ax.cross(bx);
                // Skip near-parallel axes (cross ≈ 0)
                if (cross.lengthSqr() < 1e-12f) continue;
                crossAxes[idx++] = cross.normalize();
            }
        }

        for (int i = 0; i < idx; i++) {
            if (Separated(a.vertices, b.vertices, crossAxes[i])) return false;
        }

        return true;
    }
    private static final double SAT_EPSILON = 1e-6;
    private static boolean Separated(Vec3[] vertsA, Vec3[] vertsB, Vec3 unitAxis){
        if (unitAxis.lengthSqr() < 1e-12f) return false; // Skip degenerate axes
        var aMin = Double.POSITIVE_INFINITY;
        var aMax = Double.NEGATIVE_INFINITY;
        var bMin = Double.POSITIVE_INFINITY;
        var bMax = Double.NEGATIVE_INFINITY;

        // Define two intervals, a and b. Calculate their min and max values
        for (var i = 0; i < 8; i++)
        {
            var aDist = vertsA[i].dot(unitAxis);
            aMin = Math.min(aMin, aDist);
            aMax = Math.max(aMax, aDist);
            var bDist = vertsB[i].dot(unitAxis);
            bMin = Math.min(bMin, bDist);
            bMax = Math.max(bMax, bDist);
        }

        // One-dimensional intersection test between a and b
        return !(aMax + SAT_EPSILON >= bMin && bMax + SAT_EPSILON >= aMin); // > to treat touching as intersection
    }
    public record DataTracker(Vec3 position,
                               Matrix3f rotation){
        //Vec3 extent;
    }
}