package org.dynmapblockscan.forge_1_20.compat.gtocore;

import org.dynmapblockscan.forge_1_20.compat.gtceu.GtMachineModels.Profile;

/** Profiles verified against GTOCore 0.5.6-beta machine registration. */
public final class GtoCoreModels {
    private GtoCoreModels() {}
    public static Profile profile(String id) {
        if (id.equals("gtocore:large_steam_solar_boiler"))
            return new Profile("gtceu:block/casings/steam/steel/side", "gtceu:block/multiblock/multiblock_tank", null);
        return null;
    }
}
