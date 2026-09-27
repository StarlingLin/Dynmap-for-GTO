package org.dynmapblockscan.forge_1_20.compat.gtmthings;

import org.dynmapblockscan.forge_1_20.compat.gtceu.GtMachineModels.Profile;

/** Profiles verified against the GTMThings 26.7.1 bundled in GTO. Tier comes from the live definition. */
public final class GtmThingsModels {
    private GtmThingsModels() {}
    public static Profile profile(String id) {
        if (id.equals("gtmthings:wireless_energy_monitor"))
            return new Profile(null, "gtmthings:block/machines/wireless_energy_monitor", null);
        return null;
    }
}
