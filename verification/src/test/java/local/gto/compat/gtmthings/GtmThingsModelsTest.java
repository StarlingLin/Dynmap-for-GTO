package local.gto.compat.gtmthings;

import local.gto.compat.RealModAssets;
import org.dynmapblockscan.forge_1_20.compat.gtceu.GtMachineModels;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GtmThingsModelsTest {
    @Test void monitorUsesIvCasingAndItsOwnScreenTexture() throws Exception {
        try (RealModAssets assets = new RealModAssets()) {
            var model = new GtMachineModels(assets.baker).machine("gtmthings:wireless_energy_monitor", 5, null);
            assets.validate(model);
            var faces = model.getAsJsonArray("elements").get(0).getAsJsonObject().getAsJsonObject("faces");
            assertEquals("gtceu:block/casings/voltage/iv/side", faces.getAsJsonObject("south").get("texture").getAsString());
            assertTrue(faces.getAsJsonObject("north").get("texture").getAsString().startsWith("dynmapblockscan:gto/"));
        }
    }
}
