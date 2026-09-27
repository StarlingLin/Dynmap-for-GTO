package local.gto.compat.extendedae;

import local.gto.compat.RealModAssets;
import org.dynmapblockscan.forge_1_20.compat.extendedae.ExtendedAeModels;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;
import com.google.gson.JsonObject;
import org.dynmapblockscan.core.model.BlockModel;
import org.dynmapblockscan.core.blockstate.Variant;
import org.dynmapblockscan.core.blockstate.ElementFace;
import org.dynmap.utils.*;
import org.dynmap.modsupport.ModelBlockModel.SideRotation;
import java.util.Map;

class ExtendedAeModelsTest {
    @Test void customLoaderResolvesRealDrivePanelsAndTextures() throws Exception {
        try (RealModAssets assets = new RealModAssets()) {
            ExtendedAeModels.register(assets.baker);
            var model = ExtendedAeModels.replacement(assets.baker, "expatternprovider:block/ex_drive");
            assertNotNull(model); assets.validate(model);
            assertTrue(model.getAsJsonArray("elements").size() > 1);
            assertFalse(model.has("loader"));
            assertNull(ExtendedAeModels.replacement(assets.baker, "other:drive"));
            var state = assets.baker.json("assets/expatternprovider/blockstates/ex_drive.json");
            assertEquals(24, state.getAsJsonObject("variants").size());
            var generated = new HashMap<String, JsonObject>();
            var adapted = ExtendedAeModels.blockState(assets.baker, "expatternprovider:ex_drive", generated::put);
            assertEquals(4, generated.size());
            assertEquals(24, adapted.getAsJsonObject("variants").size());
            for (var entry : state.getAsJsonObject("variants").entrySet()) {
                var original = entry.getValue().getAsJsonObject();
                var replacement = adapted.getAsJsonObject("variants").getAsJsonObject(entry.getKey());
                int spin = original.has("ae2:z") ? original.get("ae2:z").getAsInt() : 0;
                assertEquals(original.get("x"), replacement.get("x"));
                assertEquals(original.get("y"), replacement.get("y"));
                assertFalse(replacement.has("ae2:z"));
                var rotated = generated.get(replacement.get("model").getAsString());
                assets.validate(rotated);
                for (var element : rotated.getAsJsonArray("elements")) {
                    if (spin == 0) assertFalse(element.getAsJsonObject().has("rotation"));
                    else assertEquals(-spin, element.getAsJsonObject().getAsJsonObject("rotation").get("angle").getAsInt());
                }
                // Feed the scanner geometry to the real Dynmap patch factory; local spin must not change facing.
                int x = original.has("x") ? original.get("x").getAsInt() : 0;
                int y = original.has("y") ? original.get("y").getAsInt() : 0;
                var v = new Variant("test:drive", x, y, 0, false, 1);
                assertTrue(v.generateElements(Map.of("test:drive", BlockModel.buildParser().fromJson(rotated, BlockModel.class))));
                var panel = v.elements.get(0);
                var face = panel.faces.get(ElementFace.NORTH);
                var factory = new PatchDefinitionFactory();
                var patch = factory.getModelFace(panel.from, panel.to, ElementFace.NORTH.side, face.uv, SideRotation.DEG0, true, 0);
                assertNotNull(patch);
                if (panel.rotation != null) patch = factory.getPatch(patch, 0, 0, -panel.rotation.angle, 0);
                assertNotNull(patch);
                patch = factory.getPatch(patch, x, y, 0, 0);
                assertNotNull(patch);
                String direction = entry.getKey().split(",")[0].substring("facing=".length());
                BlockStep expected = Map.of("north", BlockStep.Z_MINUS, "south", BlockStep.Z_PLUS,
                        "east", BlockStep.X_PLUS, "west", BlockStep.X_MINUS, "up", BlockStep.Y_PLUS, "down", BlockStep.Y_MINUS).get(direction);
                assertEquals(expected, patch.step, "Facing after spin: " + entry.getKey());
            }
        }
    }
}
