package local.gto.compat;

import java.lang.reflect.Proxy;
import java.util.*;
import org.dynmap.common.chunk.*;
import org.dynmap.renderer.*;
import org.dynmap.utils.PatchDefinition;
import org.dynmap.utils.PatchDefinitionFactory;
import static org.junit.jupiter.api.Assertions.*;

public final class CableTestData {
    private CableTestData() {}
    public static GenericNBTCompound tag(Map<String,Object> values) {
        return (GenericNBTCompound)Proxy.newProxyInstance(GenericNBTCompound.class.getClassLoader(), new Class[]{GenericNBTCompound.class}, (p,m,a) -> {
            if (m.getName().equals("getAllKeys")) return values.keySet();
            if (m.getName().equals("makeBitStorage")) return (GenericBitStorage)(i -> 0); // Single-value test palette.
            Object v = a == null ? null : values.get(a[0]);
            switch (m.getName()) {
                case "contains": return a.length == 1 ? v != null : v != null && ((int)a[1] == type(v) || ((int)a[1] == 99 && v instanceof Number));
                case "getString": case "getAsString": return v == null ? "" : v.toString();
                case "getCompound": return v == null ? tag(Map.of()) : v;
                case "getList": return list(v == null ? List.of() : (List<?>)v);
                case "getInt": return v == null ? 0 : ((Number)v).intValue();
                case "getByte": return v == null ? (byte)0 : ((Number)v).byteValue();
                case "getShort": return v == null ? (short)0 : ((Number)v).shortValue();
                case "getLong": return v == null ? 0L : ((Number)v).longValue();
                case "getFloat": return v == null ? 0F : ((Number)v).floatValue();
                case "getDouble": return v == null ? 0D : ((Number)v).doubleValue();
                case "getBoolean": return v != null && (Boolean)v;
                case "getByteArray": return v == null ? new byte[0] : v;
                case "getIntArray": return v == null ? new int[0] : v;
                case "getLongArray": return v == null ? new long[0] : v;
                default: throw new UnsupportedOperationException(m.getName());
            }
        });
    }
    private static int type(Object v) {
        if (v instanceof Byte) return 1; if (v instanceof Short) return 2; if (v instanceof Integer) return 3;
        if (v instanceof Long) return 4; if (v instanceof Float) return 5; if (v instanceof Double) return 6;
        if (v instanceof byte[]) return 7; if (v instanceof String) return 8; if (v instanceof List) return 9;
        if (v instanceof GenericNBTCompound) return 10; if (v instanceof int[]) return 11; if (v instanceof long[]) return 12;
        return 0;
    }
    private static GenericNBTList list(List<?> values) {
        return (GenericNBTList)Proxy.newProxyInstance(GenericNBTList.class.getClassLoader(), new Class[]{GenericNBTList.class}, (p,m,a) -> {
            if (m.getName().equals("size")) return values.size();
            return values.get((int)a[0]);
        });
    }
    public static MapDataContext context(Map<String,Object> current, Map<String,Map<String,Object>> neighbors) {
        return new MapDataContext() {
            final PatchDefinitionFactory factory = new PatchDefinitionFactory();
            public RenderPatchFactory getPatchFactory() { return factory; }
            public DynmapBlockState getBlockType() { return DynmapBlockState.AIR; }
            public DynmapBlockState getBlockTypeAt(int x,int y,int z) { return DynmapBlockState.AIR; }
            public Object getBlockTileEntityField(String f) { return current.get(f); }
            public Object getBlockTileEntityFieldAt(String f,int x,int y,int z) { return neighbors.getOrDefault(x+","+y+","+z, Map.of()).get(f); }
            public int getX() { return 15; } public int getY() { return -32; } public int getZ() { return 0; }
        };
    }
    public static double[] bounds(RenderPatch[] patches) {
        double[] bounds = {1,1,1,0,0,0};
        for (RenderPatch patch : patches) {
            assertNotNull(patch);
            PatchDefinition p = (PatchDefinition)patch;
            for (double u : new double[]{p.umin,p.umax}) for (double v : new double[]{p.vmin,p.vmax}) {
                double[] xyz = {p.x0 + u*p.u.x + v*p.v.x,p.y0 + u*p.u.y + v*p.v.y,p.z0 + u*p.u.z + v*p.v.z};
                for (int axis = 0; axis < 3; axis++) {
                    assertTrue(xyz[axis] >= -1e-9 && xyz[axis] <= 1+1e-9);
                    bounds[axis] = Math.min(bounds[axis],xyz[axis]); bounds[axis+3] = Math.max(bounds[axis+3],xyz[axis]);
                }
            }
        }
        return bounds;
    }
}
