package org.dynmap.modsupport.impl;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CustomBlockModelImpl extends BlockModelImpl {
    private final String renderer;
    private final Map<String, String> parameters;
    public CustomBlockModelImpl(String block, ModModelDefinitionImpl definition, String renderer, Map<String, String> parameters) {
        super(block, definition);
        if (!renderer.matches("[a-zA-Z0-9_.$]+")) throw new IllegalArgumentException("Invalid renderer class");
        this.renderer = renderer;
        this.parameters = new LinkedHashMap<String, String>(parameters);
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            if (!entry.getKey().matches("[a-zA-Z0-9_]+") || entry.getValue().matches("(?s).*[=,\\r\\n].*") ||
                    entry.getKey().equals("class") || entry.getKey().equals("id") || entry.getKey().equals("state") || entry.getKey().equals("data"))
                throw new IllegalArgumentException("Invalid custom model parameter");
        }
    }
    @Override public String getLine() {
        String ids = getIDsAndMeta();
        if (ids == null) return null;
        StringBuilder line = new StringBuilder("customblock:").append(ids).append(",class=").append(renderer);
        for (Map.Entry<String, String> entry : parameters.entrySet()) line.append(',').append(entry.getKey()).append('=').append(entry.getValue());
        return line.toString();
    }
}
