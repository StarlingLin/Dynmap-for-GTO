package org.dynmap.modsupport.compat.ae2;

/** Shared texture-index contract between BlockScan and the renderer. Fluix is AE's transparent color. */
public final class AeCableCatalog {
    public static final String[] COLORS = {"white","orange","magenta","light_blue","yellow","lime","pink","gray","light_gray","cyan","purple","blue","brown","green","red","black","fluix"};
    public static final String[] TYPES = {"glass","covered","smart","covered_dense","smart_dense"};
    private static final String[] DIRECTORIES = {"glass","covered","smart","dense_covered","dense_smart"};
    private AeCableCatalog() {}
    public static int index(String id) {
        if (id == null || !id.startsWith("ae2:")) return -1;
        for (int type = 0; type < TYPES.length; type++) for (int color = 0; color < COLORS.length; color++)
            if (id.equals("ae2:" + COLORS[color] + "_" + TYPES[type] + "_cable")) return type * COLORS.length + color;
        return -1;
    }
    public static String texture(int index) {
        int color = index % COLORS.length;
        return "ae2:part/cable/" + DIRECTORIES[index / COLORS.length] + "/" + (color == 16 ? "transparent" : COLORS[color]);
    }
    public static double width(int index) { return index / COLORS.length >= 3 ? 0.625 : index / COLORS.length == 0 ? 0.25 : 0.375; }
    public static boolean connects(int a, int b) {
        return a >= 0 && b >= 0 && (a % 17 == b % 17 || a % 17 == 16 || b % 17 == 16);
    }
}
