package org.dynmap.modsupport;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Server-generated textures shared with scanners; ordinary resource paths are never overridden. */
public final class GeneratedTextures {
    public static final String PREFIX = "assets/dynmapblockscan/textures/gto/";
    private static final Map<String, byte[]> IMAGES = new ConcurrentHashMap<String, byte[]>();
    private GeneratedTextures() { }
    public static void put(String path, byte[] png) {
        if (!path.startsWith(PREFIX) || path.contains("..") || !path.endsWith(".png"))
            throw new IllegalArgumentException("Invalid generated texture path");
        IMAGES.put(path, png.clone());
    }
    public static InputStream open(String path) {
        byte[] bytes = IMAGES.get(path);
        return bytes == null ? null : new ByteArrayInputStream(bytes);
    }
    public static void clear() { IMAGES.clear(); }
}
