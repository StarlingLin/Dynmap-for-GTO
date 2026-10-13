package org.dynmap.hdmap.renderer.compat;

import java.util.ArrayList;
import java.util.List;
import org.dynmap.renderer.CustomRenderer;
import org.dynmap.renderer.RenderPatch;
import org.dynmap.renderer.RenderPatchFactory;

/** Disjoint core/arms, with internal faces removed. Direction bits follow Minecraft's D,U,N,S,W,E. */
public final class CableGeometry {
    public static final int[][] OFFSETS = {{0,-1,0},{0,1,0},{0,0,-1},{0,0,1},{-1,0,0},{1,0,0}};
    private static final int[] FACE = {0,1,4,5,2,3}; // addBox face order
    private CableGeometry() {}
    public static RenderPatch[] build(RenderPatchFactory factory, double width, int mask, int sideTexture, int endTexture) {
        double[] reach = new double[6];
        for (int d = 0; d < 6; d++) if ((mask & (1 << d)) != 0) reach[d] = 0.5;
        return build(factory, width, reach, sideTexture, endTexture);
    }
    /** Reach is measured from the block center, allowing arms to stop at AE bus attachments. */
    public static RenderPatch[] build(RenderPatchFactory factory, double width, double[] reach, int sideTexture, int endTexture) {
        if (!(width > 0 && width <= 1)) throw new IllegalArgumentException("Cable width must be in (0,1]");
        int mask = 0;
        for (int d = 0; d < 6; d++) if (reach[d] > width / 2 && reach[d] <= 0.5) mask |= 1 << d;
        double lo = (1 - width) / 2, hi = 1 - lo;
        List<RenderPatch> patches = new ArrayList<RenderPatch>();
        int[] center = {sideTexture,sideTexture,sideTexture,sideTexture,sideTexture,sideTexture};
        for (int d = 0; d < 6; d++) if ((mask & (1 << d)) != 0) center[FACE[d]] = -1;
        CustomRenderer.addBox(factory, patches, lo, hi, lo, hi, lo, hi, center);
        for (int d = 0; d < 6; d++) {
            if ((mask & (1 << d)) == 0) continue;
            double[] min = {lo,lo,lo}, max = {hi,hi,hi};
            int axis = d < 2 ? 1 : d < 4 ? 2 : 0;
            if ((d & 1) == 0) { min[axis] = 0.5 - reach[d]; max[axis] = lo; }
            else { min[axis] = hi; max[axis] = 0.5 + reach[d]; }
            int[] textures = {sideTexture,sideTexture,sideTexture,sideTexture,sideTexture,sideTexture};
            textures[FACE[d ^ 1]] = -1;
            textures[FACE[d]] = endTexture;
            CustomRenderer.addBox(factory, patches, min[0], max[0], min[1], max[1], min[2], max[2], textures);
        }
        return patches.toArray(new RenderPatch[patches.size()]);
    }
}
