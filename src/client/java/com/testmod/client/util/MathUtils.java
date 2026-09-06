package com.testmod.client.util;

import com.testmod.TestMod;
import org.joml.Matrix3f;
import org.joml.Quaternionf;

public class MathUtils {
    private static final ThreadLocal<Quaternionf> Q_TEMP_A = ThreadLocal.withInitial(Quaternionf::new);
    private static final ThreadLocal<Quaternionf> Q_TEMP_B = ThreadLocal.withInitial(Quaternionf::new);
    public static Matrix3f slerpRotation(Matrix3f from, Matrix3f to, float a){
        if (from == null || to == null) {
            TestMod.LOGGER.debug("[MathUtils] WARNING: one of the interpolated Matrices is null");
            return to;
        }
        Quaternionf qA = Q_TEMP_A.get();
        Quaternionf qB = Q_TEMP_B.get();
        Quaternionf result = new Quaternionf();

        new Quaternionf().setFromNormalized(from).slerp(
                new Quaternionf().setFromNormalized(to), a, result
        );
        return new Matrix3f().rotate(result);
    }
}
