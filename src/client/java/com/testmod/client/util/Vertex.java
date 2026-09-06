package com.testmod.client.util;

public class Vertex {
    public float x, y, z;
    public float r, g, b, a;
    public Vertex(float x, float y, float z, float rF, float gF, float bF, float aF) {
        this.x = x;
        this.y = y;
        this.z = z;
        // Конвертируем float [0.0-1.0] в byte [0-255]
        this.r = rF;
        this.g = gF;
        this.b = bF;
        this.a = aF;
    }
}
