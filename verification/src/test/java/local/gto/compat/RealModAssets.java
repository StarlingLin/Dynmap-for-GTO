package local.gto.compat;

import com.google.gson.*;
import org.dynmap.modsupport.GeneratedTextures;
import org.dynmapblockscan.forge_1_20.compat.ModelBaker;
import org.dynmapblockscan.core.model.BlockModel;
import org.dynmapblockscan.core.blockstate.Variant;
import org.dynmap.utils.PatchDefinitionFactory;
import org.dynmap.modsupport.ModelBlockModel.SideRotation;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/** Optional integration fixtures: the user's jars stay outside Git and the distributed mod. */
public final class RealModAssets implements AutoCloseable {
    private final List<ZipFile> jars = new ArrayList<>();
    public final ModelBaker baker;
    public RealModAssets() throws Exception {
        String directory = System.getProperty("gtoFixtureDir", "");
        assumeFalse(directory.isEmpty(), "Pass -PgtoFixtureDir=<directory containing exact mod jars>");
        try (DirectoryStream<Path> files = Files.newDirectoryStream(Path.of(directory), "*.jar")) {
            for (Path file : files) jars.add(new ZipFile(file.toFile()));
        }
        assertFalse(jars.isEmpty());
        baker = new ModelBaker(this::open);
    }
    public InputStream open(String path) throws IOException {
        for (ZipFile jar : jars) { ZipEntry entry = jar.getEntry(path); if (entry != null) return jar.getInputStream(entry); }
        // Vanilla's block/block parent contains display settings only.
        if (path.equals("assets/minecraft/models/block/block.json")) return new ByteArrayInputStream("{}".getBytes());
        return null;
    }
    public void validate(JsonObject json) throws Exception {
        BlockModel parsed = BlockModel.buildParser().fromJson(json, BlockModel.class);
        Variant v = new Variant("test:block", 0, 0, 0, false, 1);
        assertTrue(v.generateElements(Map.of("test:block", parsed)), "Must yield actual scanner geometry");
        assertFalse(v.elements.isEmpty());
        for (var element : v.elements) {
            assertFalse(element.faces.isEmpty());
            for (var face : element.faces.values()) {
                assertNotNull(face.texture);
                String path = ModelBaker.path(face.texture, "textures", ".png");
                InputStream in = GeneratedTextures.open(path);
                if (in == null) in = open(path);
                assertNotNull(in, "Missing render texture " + path);
                in.close();
            }
            var patches = new PatchDefinitionFactory();
            for (var entry : element.faces.entrySet()) {
                var face = entry.getValue();
                var patch = patches.getModelFace(element.from, element.to, entry.getKey().side,
                        face.uv, SideRotation.values()[Math.floorMod(face.rotation, 360) / 90], element.shade, 0);
                assertNotNull(patch, "Dynmap rejected actual model face " + entry.getKey());
                if (element.rotation != null && element.rotation.angle != 0) {
                    assertEquals("z", element.rotation.axis);
                    assertNotNull(patches.getPatch(patch, 0, 0, -element.rotation.angle, 0));
                }
            }
        }
    }
    @Override public void close() throws IOException { for (ZipFile jar : jars) jar.close(); }
}
