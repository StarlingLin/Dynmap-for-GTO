package org.dynmapblockscan.forge_1_20;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipFile;

/** Compatibility bridge for Forge Jar-in-Jar primary paths. Java 17. */
public final class NestedJarAccess {
    private static final Map<Path, File> CACHE = new HashMap<>();

    private NestedJarAccess() {}

    public static synchronized File toFile(Path path) {
        // Preserve the original behavior for ordinary mods and development paths.
        if (path.getFileSystem() == FileSystems.getDefault()) return path.toFile();
        File cached = CACHE.get(path);
        if (cached != null && cached.isFile()) return cached;
        Path temporary = null;
        try {
            // Forge's jij root exposes the original nested JAR bytes via NIO.
            // Never put the copy in mods/: it is only a resource-reading cache.
            temporary = Files.createTempFile("DynmapBlockScan-nested-", ".jar");
            try (InputStream input = Files.newInputStream(path)) {
                Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
            }
            try (ZipFile check = new ZipFile(temporary.toFile())) {
                check.size();
            }
            File result = temporary.toFile();
            result.deleteOnExit();
            CACHE.put(path, result);
            System.out.println("[DynmapBlockScan NestedJarFix] Resource JAR cached: " + path.toUri());
            return result;
        } catch (IOException | RuntimeException failure) {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); }
                catch (IOException | RuntimeException ignored) { temporary.toFile().deleteOnExit(); }
            }
            // Both callers already handle UnsupportedOperationException.
            throw new UnsupportedOperationException("NestedJarFix could not read " + path
                    + " (" + path.getFileSystem().provider().getScheme() + "): " + failure, failure);
        }
    }
}
