package org.dynmapblockscan.forge_1_20.compat.gtceu;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import org.dynmapblockscan.forge_1_20.compat.ModelBaker;

/** Resolve GT material icon inheritance using server assets, without calling client ResourceHelper. */
public final class GtMaterialModels {
    private GtMaterialModels() {}
    public static JsonObject bake(ModelBaker baker, Object block) throws ReflectiveOperationException, IOException {
        Object material = block.getClass().getField("material").get(block);
        Object prefix = block.getClass().getField("tagPrefix").get(block);
        String iconType = prefix.getClass().getMethod("materialIconType").invoke(prefix).toString();
        Object iconSet = material.getClass().getMethod("getMaterialIconSet").invoke(material);
        List<String> parents = new ArrayList<>();
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        while (iconSet != null && visited.add(iconSet)) {
            parents.add(iconSet.getClass().getField("name").get(iconSet).toString());
            iconSet = iconSet.getClass().getField("parentIconset").get(iconSet);
        }
        Method colorMethod = material.getClass().getMethod("getLayerARGB", int.class);
        // Models currently use material layers 0/1. Obtain colors while reflection errors can be reported.
        int[] colors = new int[16];
        for (int i = 0; i < colors.length; i++) colors[i] = ((Number)colorMethod.invoke(material, i)).intValue();
        String model = findModel(baker, iconType, parents);
        return baker.tintedModel(model, layer -> {
            int index = layer < -100 ? Math.abs(layer) % 100 / 10 : layer;
            return index >= 0 && index < colors.length ? colors[index] : -1;
        });
    }
    public static String findModel(ModelBaker baker, String type, List<String> iconSets) throws IOException {
        for (String icon : iconSets) for (String namespace : new String[]{"gtceu", "gtocore"}) {
            String candidate = namespace + ":block/material_sets/" + icon + "/" + type;
            if (baker.exists(candidate, "models", ".json")) return candidate;
        }
        throw new IOException("Missing GT material model: " + type + " " + iconSets);
    }
}
