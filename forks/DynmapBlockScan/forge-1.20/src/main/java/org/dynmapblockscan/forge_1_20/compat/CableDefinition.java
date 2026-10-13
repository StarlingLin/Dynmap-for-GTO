package org.dynmapblockscan.forge_1_20.compat;

import java.util.List;
import java.util.Map;

public record CableDefinition(String renderer, Map<String,String> parameters, List<String> textures) {}
