package org.dynmap.hdmap.renderer.compat.gtceu;

import java.util.BitSet;
import java.util.Map;
import org.dynmap.hdmap.renderer.compat.CableGeometry;
import org.dynmap.renderer.*;

public final class GtCableRenderer extends CustomRenderer {
    private final RenderPatch[][] meshes = new RenderPatch[64][];
    @Override public boolean initializeRenderer(RenderPatchFactory factory, String name, BitSet states, Map<String,String> parameters) {
        double width = Double.parseDouble(parameters.get("width"));
        for (int mask = 0; mask < 64; mask++) meshes[mask] = CableGeometry.build(factory, width, mask, 0, 1);
        return true;
    }
    @Override public String[] getTileEntityFieldsNeeded() { return new String[]{"dynmap_gt_connections"}; }
    @Override protected int getMaximumTextureCount() { return 2; }
    @Override public boolean isOnlyBlockStateSensitive() { return false; }
    @Override public RenderPatch[] getRenderPatchList(MapDataContext context) {
        Object bits = context.getBlockTileEntityField("dynmap_gt_connections");
        // Missing data must not invent connections, especially manually disconnected touching wires.
        int mask = bits instanceof Number ? ((Number)bits).intValue() & 63 : 0;
        return meshes[mask];
    }
}
