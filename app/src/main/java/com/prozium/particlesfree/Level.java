package com.prozium.particlesfree;

import java.util.Random;

/**
 * Created by cristian on 11.03.2016.
 */
public class Level {

    final float[] position;
    final int[] ptype;

    Level(final float width, final float height, final int[] totalObjects) {
        int i;
        final Random r = new Random();
        final int t = totalObjects[0] + totalObjects[1];
        position = new float[2 * t];
        ptype = new int[t];
        for (i = t - 1; i >= 0; i--) {
            position[2 * i] = (r.nextFloat() * 2f - 1f) * width;
            position[2 * i + 1] = (r.nextFloat() * 2f  - 1f) * height;
        }
        for (i = totalObjects[1] - 1; i >= 0; i--) {
            ptype[i] = 1;
        }
    }
}
