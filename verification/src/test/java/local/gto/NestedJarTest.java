package local.gto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import net.minecraftforge.jarjar.nio.layzip.LayeredZipFileSystemProvider;
import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;

class NestedJarTest {
    @TempDir Path temp;
    private static byte[] jar(String path, byte[] bytes) throws IOException {
        var out = new ByteArrayOutputStream();
        try(var zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry(path)); zip.write(bytes); zip.closeEntry();
        }
        return out.toByteArray();
    }
    @Test void normalPathsAreNotCopied() throws Exception {
        Path p=temp.resolve("regular.jar");Files.write(p,jar("test",new byte[]{1}));
        assertEquals(p.toFile(),org.dynmap.forge_1_20.NestedJarAccess.toFile(p));
        assertEquals(p.toFile(),org.dynmapblockscan.forge_1_20.NestedJarAccess.toFile(p));
    }
    @Test void forgeJijRootAndNestedZipResourcesAreReadable() throws Exception {
        byte[] child=jar("assets/example/blockstates/casing.json", "{\"variants\":{}}".getBytes());
        Path outer=temp.resolve("outer.jar");
        Files.write(outer,jar("META-INF/jarjar/child.jar",child));
        try(var outerFs=FileSystems.newFileSystem(outer)) {
            Path nested=outerFs.getPath("/META-INF/jarjar/child.jar");
            var provider=new LayeredZipFileSystemProvider();
            try(var jij=provider.newFileSystem(URI.create("jij:"+UUID.randomUUID()),Map.of("packagePath",nested))) {
                Path root=jij.getPath("/");
                assertFalse(root.toFile().isFile(), "reproduces the upstream silent skip");
                File map=org.dynmap.forge_1_20.NestedJarAccess.toFile(root);
                File scan=org.dynmapblockscan.forge_1_20.NestedJarAccess.toFile(root);
                assertArrayEquals(child,Files.readAllBytes(map.toPath()));
                assertArrayEquals(child,Files.readAllBytes(scan.toPath()));
                assertEquals(scan,org.dynmapblockscan.forge_1_20.NestedJarAccess.toFile(root));
                // Nested modules must also be able to locate their OWN original resources.
                try(var z=new ZipFile(map)) { assertNotNull(z.getEntry("assets/example/blockstates/casing.json")); }
            }
            assertArrayEquals(child,Files.readAllBytes(org.dynmap.forge_1_20.NestedJarAccess.toFile(nested).toPath()));
            assertThrows(UnsupportedOperationException.class,
                ()->org.dynmap.forge_1_20.NestedJarAccess.toFile(outerFs.getPath("/missing.jar")));
        }
    }
}
