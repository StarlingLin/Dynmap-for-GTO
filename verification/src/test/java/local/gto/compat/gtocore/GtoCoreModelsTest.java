package local.gto.compat.gtocore;

import local.gto.compat.RealModAssets;
import org.dynmapblockscan.forge_1_20.compat.gtceu.GtMachineModels;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GtoCoreModelsTest {
    @Test void solarBoilerUsesRegisteredSteelCasingAndTankFront() throws Exception {
        try (RealModAssets assets = new RealModAssets()) {
            var model = new GtMachineModels(assets.baker).machine("gtocore:large_steam_solar_boiler", -1, null);
            assets.validate(model);
            var faces = model.getAsJsonArray("elements").get(0).getAsJsonObject().getAsJsonObject("faces");
            assertEquals("gtceu:block/casings/steam/steel/side", faces.getAsJsonObject("up").get("texture").getAsString());
            assertTrue(faces.getAsJsonObject("north").get("texture").getAsString().startsWith("dynmapblockscan:gto/"));
        }
    }
}
