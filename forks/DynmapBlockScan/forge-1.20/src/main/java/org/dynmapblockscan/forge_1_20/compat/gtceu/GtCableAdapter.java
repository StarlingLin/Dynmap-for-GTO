package org.dynmapblockscan.forge_1_20.compat.gtceu;

import java.io.IOException;
import java.util.*;
import org.dynmapblockscan.forge_1_20.compat.ModelBaker;
import org.dynmapblockscan.forge_1_20.compat.CableDefinition;

/** Handles CableBlock subclasses regardless of owning namespace, including GTOCore's vibrant alloy. */
public final class GtCableAdapter {
    private GtCableAdapter() {}
    public static boolean supports(Object block) { return GtceuAdapter.hasBase(block, "com.gregtechceu.gtceu.common.block.CableBlock"); }
    public static CableDefinition bake(ModelBaker baker, Object block) throws ReflectiveOperationException, IOException {
        Object material = block.getClass().getField("material").get(block);
        Object type = block.getClass().getField("pipeType").get(block);
        double width = ((Number)type.getClass().getMethod("getThickness").invoke(type)).doubleValue();
        boolean insulated = type.getClass().getField("isCable").getBoolean(type);
        int insulation = type.getClass().getField("insulationLevel").getInt(type);
        int color = ((Number)material.getClass().getMethod("getMaterialRGB").invoke(material)).intValue();
        Object icon = material.getClass().getMethod("getMaterialIconSet").invoke(material);
        List<String> icons = new ArrayList<>();
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        while (icon != null && visited.add(icon)) {
            icons.add(icon.getClass().getField("name").get(icon).toString());
            icon = icon.getClass().getField("parentIconset").get(icon);
        }
        return bake(baker, width, color, insulated, insulation, icons);
    }
    public static CableDefinition bake(ModelBaker baker, double width, int color, boolean insulated, int insulation, List<String> icons) throws IOException {
        String base = null;
        for (String icon : icons) {
            String candidate = "gtceu:block/material_sets/" + icon + "/wire";
            if (baker.exists(candidate + "_side", "textures", ".png") && baker.exists(candidate + "_end", "textures", ".png")) { base = candidate; break; }
        }
        if (base == null) throw new IOException("Missing GT wire textures: " + icons);
        List<String> sides = new ArrayList<>(), ends = new ArrayList<>();
        List<Integer> sideColors = new ArrayList<>(), endColors = new ArrayList<>();
        sides.add(insulated ? "gtceu:block/cable/insulation_5" : base + "_side"); sideColors.add(insulated ? 0x404040 : color);
        ends.add(base + "_end"); endColors.add(color);
        for (String icon : icons) {
            String extra = "gtceu:block/material_sets/" + icon + "/wire_side_overlay";
            if (baker.exists(extra, "textures", ".png")) { sides.add(extra); sideColors.add(insulated ? 0x404040 : color); break; }
        }
        for (String icon : icons) {
            String extra = "gtceu:block/material_sets/" + icon + "/wire_end_overlay";
            if (baker.exists(extra, "textures", ".png")) { ends.add(extra); endColors.add(color); break; }
        }
        if (insulated) { ends.add("gtceu:block/cable/insulation_" + insulation); endColors.add(0x404040); }
        return new CableDefinition("org.dynmap.hdmap.renderer.compat.gtceu.GtCableRenderer",
                Map.of("width", Double.toString(width)), List.of(baker.composite(sides, sideColors), baker.composite(ends, endColors)));
    }
}
