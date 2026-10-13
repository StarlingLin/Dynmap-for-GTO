package org.dynmap.forge_1_20.compat.ae2;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.chunk.LevelChunk;
import org.dynmap.Log;

/** Runs alongside ChunkSerializer on the server thread; never puts a live object into a render snapshot. */
public final class AeCableSnapshot {
    private static final AtomicBoolean warned = new AtomicBoolean();
    private AeCableSnapshot() {}
    public static void capture(LevelChunk chunk, CompoundTag snapshot) {
        ListTag entities = snapshot.getList("block_entities", Tag.TAG_COMPOUND);
        for (int i = 0; i < entities.size(); i++) {
            CompoundTag tag = entities.getCompound(i);
            if (!tag.contains("cable", Tag.TAG_COMPOUND) || !tag.getCompound("cable").getString("id").startsWith("ae2:")) continue;
            Object host = chunk.getBlockEntities().get(new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z")));
            if (host == null) continue;
            try {
                Class<?> hostType = Class.forName("appeng.api.parts.IPartHost");
                Class<?> partType = Class.forName("appeng.api.parts.IPart");
                Class<?> nodeType = Class.forName("appeng.api.networking.IGridNode");
                if (!hostType.isInstance(host)) continue;
                Object cable = hostType.getMethod("getPart", Direction.class).invoke(host, new Object[]{null});
                if (cable == null) continue;
                Object node = partType.getMethod("getGridNode").invoke(cable);
                if (node == null) continue; // Network not initialized yet: preserve saved/fallback state.
                Set<?> directions = (Set<?>)nodeType.getMethod("getConnectedSides").invoke(node);
                int mask = 0;
                for (Object direction : directions) if (direction instanceof Direction d) mask |= 1 << d.ordinal();
                tag.putInt("dynmap_ae_connections", mask);
                Class<?> cableType = Class.forName("appeng.api.util.AECableType");
                int[] internal = new int[6];
                for (Direction direction : Direction.values()) {
                    Object part = hostType.getMethod("getPart", Direction.class).invoke(host, direction);
                    if (part == null) continue;
                    float end = ((Number)partType.getMethod("getCableConnectionLength", cableType).invoke(part, new Object[]{null})).floatValue();
                    if (end > 0 && end <= 8) internal[direction.ordinal()] = Math.round(end * 256);
                }
                tag.putIntArray("dynmap_ae_internal", internal);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
                if (warned.compareAndSet(false, true)) Log.warning("Cannot snapshot AE cable network: " + error);
            }
        }
    }
}
