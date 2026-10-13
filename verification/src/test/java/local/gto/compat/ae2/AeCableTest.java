package local.gto.compat.ae2;

import java.util.*;
import javax.imageio.ImageIO;
import local.gto.compat.RealModAssets;
import org.dynmap.hdmap.renderer.compat.ae2.AeCableRenderer;
import org.dynmap.modsupport.compat.ae2.AeCableCatalog;
import org.dynmapblockscan.forge_1_20.compat.ae2.AeCableAdapter;
import org.dynmapblockscan.forge_1_20.compat.ModelBaker;
import org.junit.jupiter.api.Test;
import static local.gto.compat.CableTestData.*;
import static org.junit.jupiter.api.Assertions.*;

class AeCableTest {
    private static Map<String,Object> cable(String color) {return Map.of("cable",tag(Map.of("id","ae2:"+color+"_glass_cable")));}
    @Test void actualJarContainsAll85CableTextures() throws Exception {
        try(var assets=new RealModAssets()) {
            var definition=AeCableAdapter.bake(assets.baker);
            assertEquals(85,definition.textures().size());
            for(String color:AeCableCatalog.COLORS) for(String type:AeCableCatalog.TYPES) {
                int index=AeCableCatalog.index("ae2:"+color+"_"+type+"_cable");assertTrue(index>=0);
                try(var in=assets.open(ModelBaker.path(definition.textures().get(index),"textures",".png"))) {
                    assertNotNull(in);assertNotNull(ImageIO.read(in));
                }
            }
        }
    }
    @Test void neighborFallbackRespectsColorsAndExplicitDisconnectedState() {
        var renderer=new AeCableRenderer();
        var east=Map.of("1,0,0",cable("blue"));
        assertEquals(10,renderer.getRenderPatchList(context(cable("fluix"),east)).length);
        assertEquals(10,renderer.getRenderPatchList(context(cable("blue"),east)).length);
        assertEquals(6,renderer.getRenderPatchList(context(cable("red"),east)).length);
        var isolated=new HashMap<>(cable("fluix")); isolated.put("dynmap_ae_connections",0);
        assertEquals(6,renderer.getRenderPatchList(context(isolated,east)).length);
        assertEquals(6,renderer.getRenderPatchList(context(cable("fluix"),Map.of("1,0,0",isolated))).length);
        assertEquals(0,renderer.getRenderPatchList(context(Map.of(),east)).length);
        assertEquals(0,renderer.getRenderPatchList(context(Map.of("cable",tag(Map.of("id","ae2:quartz_fiber"))),east)).length);
        var part=new HashMap<>(cable("fluix"));part.put("east",tag(Map.of("id","ae2:interface")));
        assertEquals(6,renderer.getRenderPatchList(context(part,east)).length);
    }
    @Test void savedVisualConnectionsAndSnapshotAttachmentsHaveDistinctGeometry() {
        var renderer=new AeCableRenderer();
        var saved=Map.<String,Object>of("cable",tag(Map.of("id","ae2:fluix_glass_cable","visual",tag(Map.of("connections",List.of("east","up"))))));
        assertArrayEquals(new double[]{.375,.375,.375,1,1,.625},bounds(renderer.getRenderPatchList(context(saved,Map.of()))),1e-9);
        var attachment=new HashMap<>(cable("fluix"));attachment.put("dynmap_ae_internal",new int[]{0,0,0,0,0,4*256});
        assertArrayEquals(new double[]{.375,.375,.375,.75,.625,.625},bounds(renderer.getRenderPatchList(context(attachment,Map.of()))),1e-9);
    }
    @Test void allCableTypesAndConnectionMasksProduceValidMeshes() {
        var renderer=new AeCableRenderer();
        for(String type:AeCableCatalog.TYPES) for(int mask=0;mask<64;mask++) {
            String id="ae2:fluix_"+type+"_cable";
            var patches=renderer.getRenderPatchList(context(Map.of("cable",tag(Map.of("id",id)),"dynmap_ae_connections",mask),Map.of()));
            assertEquals(6+4*Integer.bitCount(mask),patches.length);
            bounds(patches);
            for(var patch:patches) assertEquals(AeCableCatalog.index(id),patch.getTextureIndex());
        }
        assertFalse(renderer.isOnlyBlockStateSensitive());
    }
}
