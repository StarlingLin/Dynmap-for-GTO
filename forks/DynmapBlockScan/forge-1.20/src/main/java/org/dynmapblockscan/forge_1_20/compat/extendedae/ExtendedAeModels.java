package org.dynmapblockscan.forge_1_20.compat.extendedae;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import java.io.IOException;
import java.util.Map;
import java.util.function.BiConsumer;
import org.dynmapblockscan.forge_1_20.compat.ModelBaker;

/** ExtendedAE 1.4.17: use the loader's actual base mesh; cell contents and LEDs require world data. */
public final class ExtendedAeModels {
    private ExtendedAeModels() {}
    public static void register(ModelBaker baker) {
        baker.registerLoader("expatternprovider:ex_drive", "expatternprovider:block/extended_drive/extended_me_drive_base");
    }
    public static JsonObject replacement(ModelBaker baker, String modelId) throws IOException {
        if (!modelId.equals("expatternprovider:block/ex_drive")) return null;
        JsonObject original = baker.json(ModelBaker.path(modelId, "models", ".json"));
        if (original == null || !original.has("loader") ||
                !original.get("loader").getAsString().equals("expatternprovider:ex_drive")) return null;
        return baker.model(modelId);
    }
    public static JsonObject blockState(ModelBaker baker, String id, BiConsumer<String, JsonObject> publish) throws IOException {
        if (!id.equals("expatternprovider:ex_drive")) return null;
        JsonObject shell = replacement(baker, "expatternprovider:block/ex_drive");
        if (shell == null) return null;
        JsonObject state = baker.json(ModelBaker.path(id, "blockstates", ".json"));
        if (state == null || !state.has("variants")) return null;
        for (int spin = 0; spin < 360; spin += 90) {
            JsonObject rotated = shell.deepCopy();
            for (JsonElement e : rotated.getAsJsonArray("elements")) {
                JsonObject element = e.getAsJsonObject();
                if (element.has("rotation") && element.getAsJsonObject("rotation").get("angle").getAsDouble() != 0)
                    throw new IOException("ExDrive base now has element rotation; update adapter");
                element.remove("rotation");
                if (spin != 0) {
                    JsonObject rotation = new JsonObject();
                    rotation.add("origin", com.google.gson.JsonParser.parseString("[8,8,8]"));
                    rotation.addProperty("axis", "z");
                    rotation.addProperty("angle", -spin);
                    element.add("rotation", rotation);
                }
            }
            publish.accept("dynmapblockscan:block/extendedae/ex_drive_z" + spin, rotated);
        }
        // AE applies local Z before vanilla X/Y. A global model Z would turn the front face sideways.
        for (Map.Entry<String, JsonElement> entry : state.getAsJsonObject("variants").entrySet()) {
            JsonObject variant = entry.getValue().getAsJsonObject();
            int z = variant.has("ae2:z") ? variant.get("ae2:z").getAsInt() : 0;
            if (z < 0 || z >= 360 || z % 90 != 0) throw new IOException("Unexpected ExDrive spin: " + z);
            variant.remove("ae2:z");
            variant.addProperty("model", "dynmapblockscan:block/extendedae/ex_drive_z" + z);
        }
        return state;
    }
}
