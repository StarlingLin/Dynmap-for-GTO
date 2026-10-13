package org.dynmap.forge_1_20.compat.gtceu;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.dynmap.Log;
import org.dynmap.common.chunk.GenericNBTCompound;

/** Decode the GTO fork's saved data through its own codec, also accepting upstream plain NBT. */
public final class GtCableSnapshot {
    private static final AtomicBoolean warned = new AtomicBoolean();
    private GtCableSnapshot() {}
    public static Integer connections(GenericNBTCompound tag) {
        if (tag.contains("connections", GenericNBTCompound.TAG_ANY_NUMERIC)) return tag.getInt("connections") & 63;
        if (!tag.contains("field_save", GenericNBTCompound.TAG_BYTE_ARRAY)) return null;
        try {
            Class<?> type = Class.forName("com.gto.datasynclib.datasream.data.Data");
            byte[] bytes = tag.getByteArray("field_save");
            if (bytes.length == 0) return null;
            Object data = type.getMethod("readData", byte[].class).invoke(null, (Object)bytes);
            Map<?,?> fields = (Map<?,?>)type.getMethod("getMap").invoke(data);
            Object connections = fields.get("connections");
            // SaveToDisk(defaultValue="0") omits an isolated wire's field.
            if (connections == null) return 0;
            return ((Number)type.getMethod("getInt").invoke(connections)).intValue() & 63;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            if (warned.compareAndSet(false, true)) Log.warning("Cannot decode GT cable connections: " + error);
            return null;
        }
    }
}
