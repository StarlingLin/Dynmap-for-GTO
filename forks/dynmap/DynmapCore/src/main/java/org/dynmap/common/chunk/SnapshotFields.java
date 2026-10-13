package org.dynmap.common.chunk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Copies only fields requested by a renderer from a detached chunk snapshot. */
public final class SnapshotFields {
    private SnapshotFields() {}
    public static Map<String, Object> select(GenericNBTCompound tag, String[] names) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        for (String name : names) {
            Object value = read(tag, name);
            if (value != null) result.put(name, value);
        }
        return Collections.unmodifiableMap(result);
    }
    private static Object read(GenericNBTCompound tag, String name) {
        if (!tag.contains(name)) return null;
        if (tag.contains(name, GenericNBTCompound.TAG_COMPOUND)) return tag.getCompound(name);
        if (tag.contains(name, GenericNBTCompound.TAG_STRING)) return tag.getString(name);
        if (tag.contains(name, GenericNBTCompound.TAG_BYTE)) return tag.getByte(name);
        if (tag.contains(name, GenericNBTCompound.TAG_SHORT)) return tag.getShort(name);
        if (tag.contains(name, GenericNBTCompound.TAG_INT)) return tag.getInt(name);
        if (tag.contains(name, GenericNBTCompound.TAG_LONG)) return tag.getLong(name);
        if (tag.contains(name, GenericNBTCompound.TAG_FLOAT)) return tag.getFloat(name);
        if (tag.contains(name, GenericNBTCompound.TAG_DOUBLE)) return tag.getDouble(name);
        if (tag.contains(name, GenericNBTCompound.TAG_BYTE_ARRAY)) return tag.getByteArray(name).clone();
        if (tag.contains(name, GenericNBTCompound.TAG_INT_ARRAY)) return tag.getIntArray(name).clone();
        if (tag.contains(name, GenericNBTCompound.TAG_LONG_ARRAY)) return tag.getLongArray(name).clone();
        return null;
    }
}
