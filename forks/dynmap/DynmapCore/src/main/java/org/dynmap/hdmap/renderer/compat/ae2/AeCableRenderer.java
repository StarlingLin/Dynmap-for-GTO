package org.dynmap.hdmap.renderer.compat.ae2;

import java.util.BitSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.dynmap.common.chunk.GenericNBTCompound;
import org.dynmap.common.chunk.GenericNBTList;
import org.dynmap.hdmap.renderer.compat.CableGeometry;
import org.dynmap.modsupport.compat.ae2.AeCableCatalog;
import org.dynmap.renderer.*;

public final class AeCableRenderer extends CustomRenderer {
    private static final String[] SIDES = {"down","up","north","south","west","east"};
    private static final RenderPatch[] EMPTY = new RenderPatch[0];
    private final Map<Integer,RenderPatch[]> meshes = new ConcurrentHashMap<Integer,RenderPatch[]>();
    @Override public boolean initializeRenderer(RenderPatchFactory factory, String name, BitSet states, Map<String,String> parameters) { return true; }
    @Override public String[] getTileEntityFieldsNeeded() { return new String[]{"cable","dynmap_ae_connections","dynmap_ae_internal","down","up","north","south","west","east"}; }
    @Override protected int getMaximumTextureCount() { return AeCableCatalog.COLORS.length * AeCableCatalog.TYPES.length; }
    @Override public boolean isOnlyBlockStateSensitive() { return false; }
    private static int cable(Object data) {
        return data instanceof GenericNBTCompound ? AeCableCatalog.index(((GenericNBTCompound)data).getString("id")) : -1;
    }
    private static int savedConnections(Object data) {
        if (!(data instanceof GenericNBTCompound)) return -1;
        GenericNBTCompound tag = (GenericNBTCompound)data;
        if (!tag.contains("visual", GenericNBTCompound.TAG_COMPOUND)) return -1;
        GenericNBTCompound visual = tag.getCompound("visual");
        if (!visual.contains("connections", GenericNBTCompound.TAG_LIST)) return -1;
        GenericNBTList sides = visual.getList("connections", GenericNBTCompound.TAG_STRING);
        int mask = 0;
        for (int n = 0; n < sides.size(); n++) for (int d = 0; d < 6; d++) if (SIDES[d].equals(sides.getString(n))) mask |= 1 << d;
        return mask;
    }
    @Override public RenderPatch[] getRenderPatchList(MapDataContext context) {
        Object data = context.getBlockTileEntityField("cable");
        int index = cable(data);
        if (index < 0) return EMPTY; // A terminal-only bus must not acquire an invented fluix cable.
        Object live = context.getBlockTileEntityField("dynmap_ae_connections");
        int mask = live instanceof Number ? ((Number)live).intValue() & 63 : savedConnections(data);
        if (mask < 0) {
            mask = 0;
            for (int d = 0; d < 6; d++) {
                // Side parts may isolate the center from an adjacent host. Only infer unobstructed cable-to-cable links.
                if (context.getBlockTileEntityField(SIDES[d]) != null) continue;
                int[] off = CableGeometry.OFFSETS[d];
                Object other = context.getBlockTileEntityFieldAt("cable", off[0], off[1], off[2]);
                if (!AeCableCatalog.connects(index, cable(other))) continue;
                if (context.getBlockTileEntityFieldAt(SIDES[d ^ 1], off[0], off[1], off[2]) != null) continue;
                Object otherLive = context.getBlockTileEntityFieldAt("dynmap_ae_connections", off[0], off[1], off[2]);
                int otherMask = otherLive instanceof Number ? ((Number)otherLive).intValue() : savedConnections(other);
                if (otherMask >= 0 && (otherMask & (1 << (d ^ 1))) == 0) continue;
                mask |= 1 << d;
            }
        }
        Object attachment = context.getBlockTileEntityField("dynmap_ae_internal");
        if (attachment instanceof int[] && ((int[])attachment).length == 6) {
            int[] distances = (int[])attachment;
            double[] reach = new double[6];
            boolean hasAttachment = false;
            for (int d = 0; d < 6; d++) {
                if ((mask & (1 << d)) != 0) reach[d] = 0.5;
                else if (distances[d] > 0 && distances[d] <= 2048) {
                    reach[d] = 0.5 - distances[d] / 4096.0;
                    hasAttachment = true;
                }
            }
            if (hasAttachment) return CableGeometry.build(context.getPatchFactory(), AeCableCatalog.width(index), reach, index, index);
        }
        int key = index * 64 + mask;
        RenderPatch[] result = meshes.get(key);
        if (result == null) {
            result = CableGeometry.build(context.getPatchFactory(), AeCableCatalog.width(index), mask, index, index);
            meshes.put(key, result);
        }
        return result;
    }
}
