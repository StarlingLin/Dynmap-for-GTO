package org.dynmapblockscan.forge_1_20.compat;

import com.google.gson.*;
import org.dynmap.modsupport.GeneratedTextures;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.IntUnaryOperator;
import javax.imageio.ImageIO;

/** Static, server-safe model baking. Never loads a Minecraft client renderer. */
public final class ModelBaker {
    public interface Resources { InputStream open(String path) throws IOException; }
    private static final String[] SIDES = {"down", "up", "north", "south", "west", "east"};
    private final Map<String, String> loaders = new HashMap<>();
    public void registerLoader(String loader, String shell) { loaders.put(loader, shell); }
    private final Resources resources;
    private final Map<String, JsonObject> models = new HashMap<>();
    private final Map<String, String> baked = new HashMap<>();
    public ModelBaker(Resources resources) { this.resources = resources; }

    public static String path(String id, String kind, String suffix) {
        String[] parts = (id.contains(":") ? id : "minecraft:" + id).split(":", 2);
        return "assets/" + parts[0] + "/" + kind + "/" + parts[1] + suffix;
    }
    public boolean exists(String id, String kind, String suffix) throws IOException {
        try (InputStream in = resources.open(path(id, kind, suffix))) { return in != null; }
    }
    public JsonObject json(String path) throws IOException {
        try (InputStream in = resources.open(path)) {
            if (in == null) return null;
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
    /** Flatten parents, resolving aliases in the child's texture context. */
    public JsonObject model(String id) throws IOException { return model(id, new HashSet<>()); }
    private JsonObject model(String id, Set<String> visiting) throws IOException {
        if (models.containsKey(id)) return models.get(id).deepCopy();
        if (visiting.size() > 32 || !visiting.add(id)) throw new IOException("Cyclic model parent: " + id);
        JsonObject own = json(path(id, "models", ".json"));
        if (own == null) throw new IOException("Missing model: " + id);
        JsonObject result = new JsonObject();
        // These loaders have real static shells, with additional client-only contents/LEDs.
        if (own.has("loader")) {
            String loader = own.get("loader").getAsString();
            if (loaders.containsKey(loader)) return model(loaders.get(loader), visiting);
            if (!own.has("elements") && !own.has("parent"))
                throw new IOException("Unsupported model loader: " + loader);
        }
        if (own.has("parent")) result = model(own.get("parent").getAsString(), visiting);
        JsonObject textures = result.has("textures") ? result.getAsJsonObject("textures") : new JsonObject();
        if (own.has("textures")) for (Map.Entry<String, JsonElement> e : own.getAsJsonObject("textures").entrySet())
            textures.add(e.getKey(), e.getValue().deepCopy());
        result.add("textures", textures);
        if (own.has("elements")) result.add("elements", own.get("elements").deepCopy());
        visiting.remove(id);
        models.put(id, result.deepCopy());
        return result;
    }
    public static String texture(String name, JsonObject aliases) throws IOException {
        Set<String> seen = new HashSet<>();
        while (name.startsWith("#")) {
            if (!seen.add(name) || !aliases.has(name.substring(1))) throw new IOException("Unresolved texture: " + name);
            name = aliases.get(name.substring(1)).getAsString();
        }
        return name.contains(":") ? name : "minecraft:" + name;
    }
    private BufferedImage readImage(String id) throws IOException {
        try (InputStream in = GeneratedTextures.open(path(id, "textures", ".png"))) {
            if (in != null) return ImageIO.read(in);
        }
        try (InputStream in = resources.open(path(id, "textures", ".png"))) {
            if (in == null) throw new IOException("Missing texture: " + id);
            BufferedImage image = ImageIO.read(in);
            if (image == null) throw new IOException("Invalid PNG: " + id);
            return image;
        }
    }
    /** First animation frame, original alpha, multiplied RGB; no biome tint guessing. */
    private static BufferedImage tint(BufferedImage source, int color) {
        int size = source.getWidth();
        BufferedImage image = new BufferedImage(size, Math.min(size, source.getHeight()), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < size; x++) {
            int pixel = source.getRGB(x, y);
            int r = ((pixel >>> 16) & 255) * ((color >>> 16) & 255) / 255;
            int g = ((pixel >>> 8) & 255) * ((color >>> 8) & 255) / 255;
            int b = (pixel & 255) * (color & 255) / 255;
            image.setRGB(x, y, (pixel & 0xff000000) | (r << 16) | (g << 8) | b);
        }
        return image;
    }
    public String composite(List<String> textures, List<Integer> colors) throws IOException {
        String key = textures.toString() + colors.toString();
        if (baked.containsKey(key)) return baked.get(key);
        List<BufferedImage> layers = new ArrayList<>();
        int size = 0;
        for (int i = 0; i < textures.size(); i++) {
            BufferedImage layer = tint(readImage(textures.get(i)), colors.get(i));
            layers.add(layer); size = Math.max(size, layer.getWidth());
        }
        BufferedImage merged = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = merged.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.SrcOver);
            for (BufferedImage layer : layers) graphics.drawImage(layer, 0, 0, size, size, null);
        } finally { graphics.dispose(); }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(merged, "PNG", out);
        String hash;
        try { hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException e) { throw new AssertionError(e); }
        String id = "dynmapblockscan:gto/" + hash;
        GeneratedTextures.put(path(id, "textures", ".png"), out.toByteArray());
        baked.put(key, id);
        return id;
    }
    public JsonObject tintedModel(String modelId, IntUnaryOperator colors) throws IOException {
        JsonObject source = model(modelId);
        JsonArray elements = source.getAsJsonArray("elements");
        if (elements == null || elements.isEmpty()) throw new IOException("Empty material model: " + modelId);
        // GT's coincident full-cube layers must be composited before ray tracing.
        boolean cubes = true;
        for (JsonElement e : elements) {
            JsonObject elem = e.getAsJsonObject();
            cubes &= elem.get("from").toString().equals("[0,0,0]") && elem.get("to").toString().equals("[16,16,16]")
                    && !elem.has("rotation") && elem.getAsJsonObject("faces").size() == 6;
            for (JsonElement f : elem.getAsJsonObject("faces").asMap().values())
                cubes &= !f.getAsJsonObject().has("uv") && !f.getAsJsonObject().has("rotation");
        }
        if (cubes) {
            Map<String, String> faces = new LinkedHashMap<>();
            for (String side : SIDES) {
                List<String> layers = new ArrayList<>(); List<Integer> tints = new ArrayList<>();
                for (JsonElement e : elements) {
                    JsonObject face = e.getAsJsonObject().getAsJsonObject("faces").getAsJsonObject(side);
                    layers.add(texture(face.get("texture").getAsString(), source.getAsJsonObject("textures")));
                    tints.add(face.has("tintindex") ? colors.applyAsInt(face.get("tintindex").getAsInt()) : -1);
                }
                faces.put(side, composite(layers, tints));
            }
            return cube(faces);
        }
        for (JsonElement e : elements) for (JsonElement f : e.getAsJsonObject().getAsJsonObject("faces").asMap().values()) {
            JsonObject face = f.getAsJsonObject();
            String tex = texture(face.get("texture").getAsString(), source.getAsJsonObject("textures"));
            int color = face.has("tintindex") ? colors.applyAsInt(face.get("tintindex").getAsInt()) : -1;
            face.addProperty("texture", composite(List.of(tex), List.of(color)));
            face.remove("tintindex");
        }
        return source;
    }
    public static JsonObject cube(Map<String, String> textures) {
        JsonObject faces = new JsonObject();
        for (String side : SIDES) {
            JsonObject face = new JsonObject(); face.addProperty("texture", textures.get(side));
            faces.add(side, face);
        }
        JsonObject elem = new JsonObject();
        elem.add("from", JsonParser.parseString("[0,0,0]")); elem.add("to", JsonParser.parseString("[16,16,16]"));
        elem.add("faces", faces);
        JsonArray elements = new JsonArray(); elements.add(elem);
        JsonObject result = new JsonObject(); result.add("elements", elements);
        return result;
    }
    public JsonObject variants(String model, Map<String, List<String>> properties) {
        JsonObject variants = new JsonObject();
        String facing = properties.containsKey("facing") ? "facing" : properties.containsKey("horizontal_facing") ? "horizontal_facing" : null;
        if (facing == null) variants.add("", variant(model, "north"));
        else for (String direction : properties.get(facing)) variants.add(facing + "=" + direction, variant(model, direction));
        JsonObject state = new JsonObject(); state.add("variants", variants); return state;
    }
    private JsonObject variant(String model, String facing) {
        JsonObject v = new JsonObject(); v.addProperty("model", model);
        switch (facing) {
            case "east": v.addProperty("y", 90); break;
            case "south": v.addProperty("y", 180); break;
            case "west": v.addProperty("y", 270); break;
            case "up": v.addProperty("x", 270); break;
            case "down": v.addProperty("x", 90); break;
            default: break;
        }
        return v;
    }
}
