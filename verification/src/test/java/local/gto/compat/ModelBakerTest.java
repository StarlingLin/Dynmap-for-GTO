package local.gto.compat;

import com.google.gson.*;
import org.dynmap.modsupport.GeneratedTextures;
import org.dynmapblockscan.forge_1_20.compat.ModelBaker;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ModelBakerTest {
    @Test void compositePreservesHolesAndDistinctMaterialColors() throws Exception {
        BufferedImage base = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        base.setRGB(0, 0, 0xffffffff);
        BufferedImage overlay = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        overlay.setRGB(1, 0, 0xff808080);
        Map<String, byte[]> assets = new HashMap<>();
        for (Map.Entry<String, BufferedImage> e : Map.of("base", base, "overlay", overlay).entrySet()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(); ImageIO.write(e.getValue(), "PNG", out);
            assets.put("assets/test/textures/" + e.getKey() + ".png", out.toByteArray());
        }
        ModelBaker baker = new ModelBaker(path -> assets.containsKey(path) ? new ByteArrayInputStream(assets.get(path)) : null);
        String id = baker.composite(List.of("test:base", "test:overlay"), List.of(0xffcc6633, 0xff00ff00));
        try (InputStream in = GeneratedTextures.open(ModelBaker.path(id, "textures", ".png"))) {
            BufferedImage result = ImageIO.read(in);
            assertEquals(0xffcc6633, result.getRGB(0, 0));
            assertEquals(0xff008000, result.getRGB(1, 0));
            assertEquals(0, result.getRGB(0, 1));
        }
        assertEquals(id, baker.composite(List.of("test:base", "test:overlay"), List.of(0xffcc6633, 0xff00ff00)));
        assertNotEquals(id, baker.composite(List.of("test:base", "test:overlay"), List.of(-1, -1)));
        assertThrows(IllegalArgumentException.class, () -> GeneratedTextures.put("assets/minecraft/textures/test.png", new byte[0]));
    }

    @Test void inheritanceUsesChildAliasesAndRejectsCycles() throws Exception {
        Map<String, String> files = Map.of(
                "assets/test/models/base.json", "{\"textures\":{\"all\":\"test:original\",\"side\":\"#all\"},\"elements\":[]}",
                "assets/test/models/child.json", "{\"parent\":\"test:base\",\"textures\":{\"all\":\"test:changed\"}}",
                "assets/test/models/cycle.json", "{\"parent\":\"test:cycle\"}");
        ModelBaker baker = new ModelBaker(path -> files.containsKey(path) ? new ByteArrayInputStream(files.get(path).getBytes(StandardCharsets.UTF_8)) : null);
        assertEquals("test:changed", ModelBaker.texture("#side", baker.model("test:child").getAsJsonObject("textures")));
        assertEquals("test:original", ModelBaker.texture("#side", baker.model("test:base").getAsJsonObject("textures")));
        assertThrows(IOException.class, () -> baker.model("test:cycle"));
        assertThrows(IOException.class, () -> baker.model("test:missing"));
    }
}
