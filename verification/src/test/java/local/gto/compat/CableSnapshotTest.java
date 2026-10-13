package local.gto.compat;

import java.util.*;
import org.dynmap.*;
import org.dynmap.common.chunk.*;
import org.dynmap.hdmap.CustomBlockModel;
import org.dynmap.renderer.DynmapBlockState;
import org.dynmap.utils.MapChunkCache;
import org.junit.jupiter.api.Test;
import static local.gto.compat.CableTestData.*;
import static org.junit.jupiter.api.Assertions.*;

class CableSnapshotTest {
    static final class World extends DynmapWorld {
        World() { super("cable-test",320,63,-64); }
        public boolean isNether(){return false;} public DynmapLocation getSpawnLocation(){return null;}
        public long getTime(){return 0;} public boolean hasStorm(){return false;} public boolean isThundering(){return false;}
        public boolean isLoaded(){return true;} public void setWorldUnloaded(){}
        public int getLightLevel(int x,int y,int z){return 15;} public int getHighestBlockYAt(int x,int z){return 0;}
        public boolean canGetSkyLightLevel(){return true;} public int getSkyLightLevel(int x,int y,int z){return 15;}
        public String getEnvironment(){return "normal";} public MapChunkCache getChunkCache(List<DynmapChunk> chunks){return null;}
    }
    static final class Cache extends GenericMapChunkCache {
        Cache(){super(null);}
        protected GenericChunk getLoadedChunk(DynmapChunk c){return null;}
        protected GenericChunk loadChunk(DynmapChunk c){return null;}
        void install(GenericChunk... snapshots) throws Exception {
            var field=GenericMapChunkCache.class.getDeclaredField("snaparray");field.setAccessible(true);field.set(this,snapshots);
        }
    }
    @Test void parserRetainsOnlyRendererFieldsAndIteratorCrossesChunkBoundary() throws Exception {
        var block=new DynmapBlockState(null,0,"test:ae_cable_bus","","AIR");
        var model=new CustomBlockModel(block,new BitSet(),"org.dynmap.hdmap.renderer.compat.ae2.AeCableRenderer",Map.of(),"test");
        assertNotNull(model.render);
        var cache=new Cache();
        cache.setChunks(new World(),List.of(new DynmapChunk(0,0),new DynmapChunk(1,0),new DynmapChunk(0,1),new DynmapChunk(1,1)));
        var center=tag(Map.of("id","ae2:fluix_glass_cable"));
        var section=tag(Map.of("Y",(byte)-2,"block_states",tag(Map.of("palette",List.of(tag(Map.of("Name",block.blockName)))))));
        var entity=tag(Map.of("x",15,"y",-32,"z",0,"cable",center,"dynmap_ae_connections",32,"inventory","must not retain"));
        var chunk=cache.parseChunkFromNBT(tag(Map.of("xPos",0,"zPos",0,"Status","minecraft:full","sections",List.of(section),"block_entities",List.of(entity))));
        assertEquals(32,chunk.getBlockEntityField(15,-32,0,"dynmap_ae_connections"));
        assertNull(chunk.getBlockEntityField(15,-32,0,"inventory"));
        var other=new GenericChunk.Builder(-64,320).coords(1,0).blockEntity(16,-32,0,Map.of("cable",center,"dynmap_ae_connections",16)).build();
        var wrapTrap=new GenericChunk.Builder(-64,320).coords(0,1).blockEntity(0,-32,0,Map.of("cable","wrong row")).build();
        cache.install(chunk,other,wrapTrap,new GenericChunk.Builder(-64,320).build());
        var iterator=cache.getIterator(15,-32,0);
        assertSame(center,iterator.getBlockTileEntityField("cable"));
        assertEquals(16,iterator.getBlockTileEntityFieldAt("dynmap_ae_connections",1,0,0));
        assertNull(iterator.getBlockTileEntityFieldAt("cable",1,1,0));
        iterator.initialize(31,-32,0);
        assertNull(iterator.getBlockTileEntityFieldAt("cable",1,0,0),"Must not wrap east edge into next Z row");
    }
    @Test void builderReuseCannotEraseSnapshotAndArrayFieldsAreCopied() {
        int[] connections={0,0,0,0,0,1024};
        var fields=SnapshotFields.select(tag(Map.of("dynmap_ae_internal",connections,"inventory","secret")),new String[]{"dynmap_ae_internal"});
        connections[5]=0;
        assertEquals(1024,((int[])fields.get("dynmap_ae_internal"))[5]);
        var builder=new GenericChunk.Builder(-64,320).blockEntity(0,-32,0,fields).blockEntity(0,224,0,Map.of("different",true));
        var chunk=builder.build();builder.reset(-64,320);
        assertNotNull(chunk.getBlockEntityField(0,-32,0,"dynmap_ae_internal"));
        assertNull(chunk.getBlockEntityField(0,224,0,"dynmap_ae_internal"));
        assertNull(builder.build().getBlockEntityField(0,-32,0,"dynmap_ae_internal"));
    }
}
