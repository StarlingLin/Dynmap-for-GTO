package org.dynmapblockscan.forge_1_20.compat.ae2;

import java.io.IOException;
import java.util.*;
import org.dynmap.modsupport.compat.ae2.AeCableCatalog;
import org.dynmapblockscan.forge_1_20.compat.CableDefinition;
import org.dynmapblockscan.forge_1_20.compat.ModelBaker;

public final class AeCableAdapter {
    private AeCableAdapter() {}
    public static boolean supports(String id) { return id.equals("ae2:cable_bus"); }
    public static CableDefinition bake(ModelBaker baker) throws IOException {
        List<String> textures = new ArrayList<>();
        for (int i = 0; i < AeCableCatalog.COLORS.length * AeCableCatalog.TYPES.length; i++) {
            String texture = AeCableCatalog.texture(i);
            if (!baker.exists(texture, "textures", ".png")) throw new IOException("Missing AE cable texture: " + texture);
            textures.add(texture);
        }
        return new CableDefinition("org.dynmap.hdmap.renderer.compat.ae2.AeCableRenderer", Map.of(), textures);
    }
}
