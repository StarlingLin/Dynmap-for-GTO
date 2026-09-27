package org.dynmapblockscan.forge_1_20.compat;

import com.google.gson.JsonObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.world.level.block.Block;
import org.dynmapblockscan.core.BlockScanLog;
import org.dynmapblockscan.core.blockstate.BSBlockState;
import org.dynmapblockscan.forge_1_20.compat.extendedae.ExtendedAeModels;
import org.dynmapblockscan.forge_1_20.compat.gtceu.GtceuAdapter;

/** Dispatch and scan diagnostics only. Mod-specific rules belong in the respective compat subpackage. */
public final class ModModelAdapters {
    private final ModelBaker baker;
    private final BlockScanLog logger;
    private final Map<String, byte[]> generated = new HashMap<>();
    private final Set<String> transparent = new HashSet<>();
    private final Map<String, Integer> counts = new LinkedHashMap<>();
    private final List<String> coverage = new ArrayList<>();
    private int failures;
    public ModModelAdapters(ModelBaker.Resources resources, BlockScanLog logger) {
        this.baker = new ModelBaker(resources); this.logger = logger;
        ExtendedAeModels.register(baker);
    }
    public boolean supports(Block block) { return GtceuAdapter.supports(block); }
    public boolean transparent(String id) { return transparent.contains(id); }
    private void publish(String id, JsonObject model) {
        generated.put(ModelBaker.path(id, "models", ".json"), model.toString().getBytes(StandardCharsets.UTF_8));
    }
    public BSBlockState replacementBlockState(String id) {
        try {
            JsonObject state = ExtendedAeModels.blockState(baker, id, this::publish);
            if (state == null) return null;
            record(id, "extendedae-shell");
            return BSBlockState.buildParser().fromJson(state, BSBlockState.class);
        } catch (IOException | RuntimeException error) { failed(id, error); return null; }
    }
    public BSBlockState blockState(Block block, String id, Map<String, List<String>> properties) {
        if (!supports(block)) return null;
        try {
            GtceuAdapter.Result result = GtceuAdapter.bake(baker, block, id);
            String model = "dynmapblockscan:block/gto/" + id.replace(':', '/');
            publish(model, result.model());
            if (result.transparent()) transparent.add(id);
            record(id, result.quality());
            return BSBlockState.buildParser().fromJson(baker.variants(model, properties), BSBlockState.class);
        } catch (ReflectiveOperationException | IOException | RuntimeException | LinkageError error) {
            failed(id, error); return null;
        }
    }
    public JsonObject replacementModel(String id) {
        try {
            JsonObject model = ExtendedAeModels.replacement(baker, id);
            if (model != null) record(id, "extendedae-shell");
            return model;
        } catch (IOException | RuntimeException error) { failed(id, error); return null; }
    }
    public InputStream open(String path) {
        byte[] bytes = generated.get(path);
        return bytes == null ? null : new ByteArrayInputStream(bytes);
    }
    private void record(String id, String quality) {
        counts.merge(quality, 1, Integer::sum); coverage.add(quality + "\t" + id);
    }
    private void failed(String id, Throwable error) {
        Throwable cause = error.getCause() == null ? error : error.getCause();
        coverage.add("FAILED\t" + id + "\t" + cause);
        if (++failures <= 10) logger.warning("GTO model adapter: " + id + ": " + cause);
    }
    public List<String> coverage() { return Collections.unmodifiableList(coverage); }
    public void report() { logger.info("GTO model adapters: " + counts + ", failed=" + failures + ". machine-shell/extendedae-shell omit dynamic contents, covers and LEDs."); }
}
