package org.dynmapblockscan.forge_1_20.compat.gtceu;

import com.google.gson.*;
import java.io.IOException;
import java.util.*;
import org.dynmapblockscan.forge_1_20.compat.ModelBaker;
import org.dynmapblockscan.forge_1_20.compat.gtocore.GtoCoreModels;
import org.dynmapblockscan.forge_1_20.compat.gtmthings.GtmThingsModels;

/** Unpainted, idle machine shells. World-dependent covers and animations are intentionally not baked. */
public final class GtMachineModels {
    private static final String[] SIDES = {"down", "up", "north", "south", "west", "east"};
    private static final String[] TIERS = {"ulv", "lv", "mv", "hv", "ev", "iv", "luv", "zpm", "uv", "uhv", "uev", "uiv", "uxv", "opv", "max"};
    private final ModelBaker baker;
    public GtMachineModels(ModelBaker baker) { this.baker = baker; }
    public record Profile(String casing, String workable, String overlay) {}
    public static Profile profile(String id) {
        Profile specific = GtoCoreModels.profile(id);
        if (specific == null) specific = GtmThingsModels.profile(id);
        if (specific != null) return specific;
        String overlay = null;
        if (id.matches("gtceu:[a-z]+_(input|output)_hatch(?:_[49]x)?")) {
            String suffix = id.endsWith("_4x") ? "_4x" : id.endsWith("_9x") ? "_9x" : "";
            overlay = "gtceu:block/machine/part/fluid_hatch." + (id.contains("_output_") ? "export" : "import") + suffix;
        }
        return new Profile(null, null, overlay);
    }
    public JsonObject tierHull(int tier) throws IOException {
        if (tier < 0 || tier >= TIERS.length) throw new IOException("Unknown GT tier: " + tier);
        Map<String, String> faces = new LinkedHashMap<>();
        for (String side : SIDES) {
            String tex = "gtceu:block/casings/voltage/" + TIERS[tier] + "/" + (side.equals("up") ? "top" : side.equals("down") ? "bottom" : "side");
            if (!baker.exists(tex, "textures", ".png")) throw new IOException("Missing tier casing: " + tex);
            faces.put(side, tex);
        }
        return ModelBaker.cube(faces);
    }
    /** Bake a static, unpainted machine. Overlays are merged into faces to avoid z-fighting. */
    public JsonObject machine(String id, int tier, JsonObject appearance) throws IOException {
        JsonObject shell;
        Profile profile = profile(id);
        if (profile.casing() != null) {
            Map<String, String> faces = new LinkedHashMap<>();
            for (String side : SIDES) faces.put(side, profile.casing());
            shell = ModelBaker.cube(faces);
        } else shell = appearance != null ? appearance.deepCopy() : tierHull(tier);
        // Resolve inherited shell textures and remove unsupported painting/biome tints.
        JsonArray elements = shell.getAsJsonArray("elements");
        if (elements == null || elements.isEmpty()) throw new IOException("Empty machine shell: " + id);
        for (JsonElement e : elements) for (JsonElement f : e.getAsJsonObject().getAsJsonObject("faces").asMap().values()) {
            JsonObject face = f.getAsJsonObject();
            face.addProperty("texture", ModelBaker.texture(face.get("texture").getAsString(), shell.has("textures") ? shell.getAsJsonObject("textures") : new JsonObject()));
            face.remove("tintindex");
        }
        String workable = profile.workable(), overlay = profile.overlay();
        if (elements.size() == 1) {
            JsonObject faces = elements.get(0).getAsJsonObject().getAsJsonObject("faces");
            if (workable != null) for (String side : SIDES) {
                String part = side.equals("north") ? "front" : side.equals("south") ? "back" : side.equals("up") ? "top" : side.equals("down") ? "bottom" : "side";
                String tex = workable + "/overlay_" + part;
                if (baker.exists(tex, "textures", ".png")) mergeFace(faces, side, List.of(tex));
            }
            if (overlay != null) {
                JsonObject overlayModel = baker.model(overlay);
                List<String> layers = new ArrayList<>();
                for (JsonElement e : overlayModel.getAsJsonArray("elements")) {
                    JsonObject face = e.getAsJsonObject().getAsJsonObject("faces").getAsJsonObject("north");
                    // GT suppresses the color ring (tint 9) on unpainted machines.
                    if (face != null && (!face.has("tintindex") || face.get("tintindex").getAsInt() != 9))
                        layers.add(ModelBaker.texture(face.get("texture").getAsString(), overlayModel.getAsJsonObject("textures")));
                }
                mergeFace(faces, "north", layers);
            }
        }
        return shell;
    }
    private void mergeFace(JsonObject faces, String side, List<String> overlays) throws IOException {
        if (!faces.has(side)) return;
        JsonObject face = faces.getAsJsonObject(side);
        List<String> layers = new ArrayList<>(); layers.add(face.get("texture").getAsString()); layers.addAll(overlays);
        face.addProperty("texture", baker.composite(layers, Collections.nCopies(layers.size(), -1)));
    }
}
