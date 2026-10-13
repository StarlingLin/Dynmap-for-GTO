package local.gto.compat.gtceu;

import java.util.*;
import javax.imageio.ImageIO;
import local.gto.compat.*;
import org.dynmap.hdmap.renderer.compat.gtceu.GtCableRenderer;
import org.dynmap.forge_1_20.compat.gtceu.GtCableSnapshot;
import org.dynmap.modsupport.GeneratedTextures;
import org.dynmap.modsupport.impl.*;
import org.dynmap.utils.PatchDefinitionFactory;
import org.dynmapblockscan.forge_1_20.compat.gtceu.GtCableAdapter;
import org.dynmapblockscan.forge_1_20.compat.ModelBaker;
import org.junit.jupiter.api.Test;
import static local.gto.compat.CableTestData.*;
import static org.junit.jupiter.api.Assertions.*;

class GtCableTest {
    @Test void all64ConnectionsUseActualWireAndInsulatedCableWidths() {
        for (double width : new double[]{3/16.,5/16.,7/16.,9/16.,13/16.,4/16.,6/16.,8/16.,10/16.,14/16.}) {
            var renderer = new GtCableRenderer();
            assertTrue(renderer.initializeRenderer(new PatchDefinitionFactory(), "gtocore:vibrant_alloy_octal_wire", new BitSet(), Map.of("width", ""+width)));
            for (int mask=0;mask<64;mask++) {
                var patches = renderer.getRenderPatchList(context(Map.of("dynmap_gt_connections", mask),Map.of()));
                assertEquals(6+4*Integer.bitCount(mask), patches.length);
                double low=(1-width)/2, high=1-low;
                assertArrayEquals(new double[]{(mask&16)>0?0:low,(mask&1)>0?0:low,(mask&4)>0?0:low,
                    (mask&32)>0?1:high,(mask&2)>0?1:high,(mask&8)>0?1:high},bounds(patches),1e-8);
            }
            assertFalse(renderer.isOnlyBlockStateSensitive());
        }
    }
    @Test void actualJarTexturesPreserveMaterialTintAndInsulation() throws Exception {
        try (var assets=new RealModAssets()) {
            var wire=GtCableAdapter.bake(assets.baker,9/16.,0x80ff20,false,-1,List.of("metallic","dull"));
            var insulated=GtCableAdapter.bake(assets.baker,10/16.,0x80ff20,true,3,List.of("metallic","dull"));
            assertEquals("0.5625",wire.parameters().get("width"));
            assertNotEquals(wire.textures(),insulated.textures());
            for (var definition : List.of(wire,insulated)) for (String texture:definition.textures()) {
                try(var in=GeneratedTextures.open(ModelBaker.path(texture,"textures",".png"))) {
                    assertNotNull(in);assertNotNull(ImageIO.read(in));
                }
            }
            var models=new ModModelDefinitionImpl(new ModTextureDefinitionImpl("gtocore","test"));
            var model=(BlockModelImpl)models.addCustomModel("vibrant_alloy_octal_wire",wire.renderer(),wire.parameters());
            assertEquals("customblock:id=%vibrant_alloy_octal_wire,state=*,class=org.dynmap.hdmap.renderer.compat.gtceu.GtCableRenderer,width=0.5625",model.getLine());
        }
    }
    @Test void upstreamNbtConnectionsDoNotConfuseBlockedTransfersWithGeometry() {
        assertEquals(37,GtCableSnapshot.connections(tag(Map.of("connections",37,"blockedConnections",63))));
        assertEquals(0,GtCableSnapshot.connections(tag(Map.of("connections",0))));
        assertNull(GtCableSnapshot.connections(tag(Map.of())));
    }
}
