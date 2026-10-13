package org.dynmap.forge_1_20;

import java.util.List;
import java.util.NoSuchElementException;

import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import org.dynmap.DynmapChunk;
import org.dynmap.Log;
import org.dynmap.common.BiomeMap;
import org.dynmap.common.chunk.GenericChunk;
import org.dynmap.common.chunk.GenericChunkCache;
import org.dynmap.common.chunk.GenericMapChunkCache;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;

/**
 * Container for managing chunks - dependent upon using chunk snapshots, since
 * rendering is off server thread
 */
public class ForgeMapChunkCache extends GenericMapChunkCache {
    @Override
    protected java.util.Map<String,Object> readBlockEntityFields(org.dynmap.common.chunk.GenericNBTCompound tag, String[] fields) {
        java.util.Map<String,Object> values = new java.util.HashMap<>(super.readBlockEntityFields(tag, fields));
        for (String field : fields) if (field.equals("dynmap_gt_connections")) {
            Integer connections = org.dynmap.forge_1_20.compat.gtceu.GtCableSnapshot.connections(tag);
            if (connections != null) values.put(field, connections);
        }
        return values;
    }
    private ServerLevel w;
	private ServerChunkCache cps;
	/**
	 * Construct empty cache
	 */
	public ForgeMapChunkCache(GenericChunkCache cc) {
		super(cc);
	}

	// Load generic chunk from existing and already loaded chunk
	protected GenericChunk getLoadedChunk(DynmapChunk chunk) {
		GenericChunk gc = null;
		ChunkAccess ch = cps.getChunk(chunk.x, chunk.z, ChunkStatus.FULL, false);
		if (ch != null) {
			CompoundTag nbt = ChunkSerializer.write(w, ch);
			if (nbt != null) {
				if (ch instanceof net.minecraft.world.level.chunk.LevelChunk loaded)
                    org.dynmap.forge_1_20.compat.ae2.AeCableSnapshot.capture(loaded, nbt);
                gc = parseChunkFromNBT(new NBT.NBTCompound(nbt));
			}
		}
		return gc;
	}
	// Load generic chunk from unloaded chunk
	protected GenericChunk loadChunk(DynmapChunk chunk) {
		GenericChunk gc = null;
		CompoundTag nbt = readChunk(chunk.x, chunk.z);
		// If read was good
		if (nbt != null) {
			gc = parseChunkFromNBT(new NBT.NBTCompound(nbt));
		}
		return gc;
	}

	public void setChunks(ForgeWorld dw, List<DynmapChunk> chunks) {
		this.w = dw.getWorld();
		if (dw.isLoaded()) {
			/* Check if world's provider is ServerChunkProvider */
			cps = this.w.getChunkSource();
		}
		super.setChunks(dw, chunks);
	}

	private CompoundTag readChunk(int x, int z) {
		try {
			CompoundTag rslt = cps.chunkMap.read(new ChunkPos(x, z)).join().get();
			if (rslt != null) {
				CompoundTag lev = rslt;
				if (lev.contains("Level")) {
					lev = lev.getCompound("Level");
				}
				// Don't load uncooked chunks
				String stat = lev.getString("Status");
				ChunkStatus cs = ChunkStatus.byName(stat);
				if ((stat == null) ||
				// Needs to be at least lighted
						(!cs.isOrAfter(ChunkStatus.LIGHT))) {
					rslt = null;
				}
			}
			// Log.info(String.format("loadChunk(%d,%d)=%s", x, z, (rslt != null) ?
			// rslt.toString() : "null"));
			return rslt;
		} catch (NoSuchElementException nsex) {
			return null;
		} catch (Exception exc) {
			Log.severe(String.format("Error reading chunk: %s,%d,%d", dw.getName(), x, z), exc);
			return null;
		}
	}
	@Override
	public int getFoliageColor(BiomeMap bm, int[] colormap, int x, int z) {
		return bm.<Biome>getBiomeObject().map(Biome::getSpecialEffects)
				.flatMap(BiomeSpecialEffects::getFoliageColorOverride)
				.orElse(colormap[bm.biomeLookup()]);
	}

	@Override
	public int getGrassColor(BiomeMap bm, int[] colormap, int x, int z) {
		BiomeSpecialEffects effects = bm.<Biome>getBiomeObject().map(Biome::getSpecialEffects).orElse(null);
		if (effects == null) return colormap[bm.biomeLookup()];
		return effects.getGrassColorModifier().modifyColor(x, z, effects.getGrassColorOverride().orElse(colormap[bm.biomeLookup()]));
	}
}
