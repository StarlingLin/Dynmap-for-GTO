package local.gto.compat.gtceu;

import local.gto.compat.RealModAssets;
import org.dynmapblockscan.forge_1_20.compat.gtceu.*;
import org.dynmapblockscan.core.blockstate.BSBlockState;
import org.dynmap.modsupport.GeneratedTextures;
import org.dynmapblockscan.forge_1_20.compat.ModelBaker;
import org.junit.jupiter.api.Test;
import com.google.gson.*;
import javax.imageio.ImageIO;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class GtceuModelsTest {
    @Test void realFramesRetainHolesAndMaterialColors() throws Exception {
        try (RealModAssets assets = new RealModAssets()) {
            String path = GtMaterialModels.findModel(assets.baker, "frame_gt", List.of("metallic", "dull"));
            assertEquals("gtceu:block/material_sets/dull/frame_gt", path);
            for (int tint : new int[]{0xffd0e8ff, 0xffc89650}) {
                JsonObject model = assets.baker.tintedModel(path, layer -> tint);
                assets.validate(model);
                assertEquals(1, model.getAsJsonArray("elements").size(), "Coincident layers must merge");
                String texture = model.getAsJsonArray("elements").get(0).getAsJsonObject().getAsJsonObject("faces")
                        .getAsJsonObject("north").get("texture").getAsString();
                try (var in = GeneratedTextures.open(ModelBaker.path(texture, "textures", ".png"))) {
                    var image = ImageIO.read(in); int holes = 0, pixels = 0;
                    for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++)
                        if ((image.getRGB(x, y) >>> 24) == 0) holes++; else pixels++;
                    assertTrue(holes > 0); assertTrue(pixels > 0);
                }
            }
        }
    }
    @Test void hvOutputHatchHasCorrectCasingAndOverlayInAllDirections() throws Exception {
        try (RealModAssets assets = new RealModAssets()) {
            JsonObject model = new GtMachineModels(assets.baker).machine("gtceu:hv_output_hatch", 3, null);
            assets.validate(model);
            JsonObject faces = model.getAsJsonArray("elements").get(0).getAsJsonObject().getAsJsonObject("faces");
            assertEquals("gtceu:block/casings/voltage/hv/top", faces.getAsJsonObject("up").get("texture").getAsString());
            assertTrue(faces.getAsJsonObject("north").get("texture").getAsString().startsWith("dynmapblockscan:gto/"));
            JsonObject variants = assets.baker.variants("test:hatch", Map.of("facing", List.of("north", "south", "east", "west", "up", "down")));
            BSBlockState state = BSBlockState.buildParser().fromJson(variants, BSBlockState.class);
            assertEquals(Set.of("facing"), state.getRenderProps());
            for (String dir : List.of("north", "south", "east", "west", "up", "down"))
                assertFalse(state.getMatchingVariants(Map.of("facing", dir), new HashMap<>()).isEmpty());
            assertEquals(90, variants.getAsJsonObject("variants").getAsJsonObject("facing=east").get("y").getAsInt());
        }
    }
}
