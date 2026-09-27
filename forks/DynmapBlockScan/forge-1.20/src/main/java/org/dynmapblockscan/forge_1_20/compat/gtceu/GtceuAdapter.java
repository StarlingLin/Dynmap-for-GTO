package org.dynmapblockscan.forge_1_20.compat.gtceu;

import com.google.gson.*;
import java.io.IOException;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.dynmapblockscan.forge_1_20.compat.ModelBaker;

/** Optional integration through public, server-side GT APIs; no hard dependency on a GT distribution. */
public final class GtceuAdapter {
    private GtceuAdapter() {}
    public record Result(JsonObject model, String quality, boolean transparent) {}
    public static boolean hasBase(Object object, String className) {
        for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass())
            if (type.getName().equals(className)) return true;
        return false;
    }
    public static boolean supports(Block block) {
        return hasBase(block, "com.gregtechceu.gtceu.api.block.MaterialBlock") ||
                hasBase(block, "com.gregtechceu.gtceu.api.block.MetaMachineBlock");
    }
    public static Result bake(ModelBaker baker, Block block, String id) throws ReflectiveOperationException, IOException {
        if (hasBase(block, "com.gregtechceu.gtceu.api.block.MaterialBlock"))
            return new Result(GtMaterialModels.bake(baker, block), "material", id.endsWith("_frame"));
        Object definition = block.getClass().getMethod("getDefinition").invoke(block);
        int tier = ((Number)definition.getClass().getMethod("getTier").invoke(definition)).intValue();
        GtMachineModels.Profile profile = GtMachineModels.profile(id);
        if (profile.workable() == null && profile.overlay() == null) {
            Object shape = definition.getClass().getMethod("getShape", Direction.class).invoke(definition, Direction.NORTH);
            if (!(shape instanceof VoxelShape voxel) || Shapes.joinIsNotEmpty(voxel, Shapes.block(), BooleanOp.NOT_SAME))
                throw new IOException("Non-cube GT machine requires its own adapter");
        }
        Object supplier = definition.getClass().getMethod("getAppearance").invoke(definition);
        JsonObject appearance = null;
        if (supplier instanceof Supplier<?> source && source.get() instanceof BlockState state && state.getBlock() != block) {
            // Appearance state is a real casing, not the self-reference used for ordinary tiered machines.
            String casingId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            JsonObject states = baker.json(ModelBaker.path(casingId, "blockstates", ".json"));
            if (states != null && states.has("variants")) {
                for (Map.Entry<String, JsonElement> entry : states.getAsJsonObject("variants").entrySet()) {
                    if (!entry.getKey().isEmpty()) continue; // Don't pick an arbitrary state-dependent casing.
                    JsonElement variant = entry.getValue();
                    if (variant.isJsonArray()) variant = variant.getAsJsonArray().get(0);
                    if (variant.getAsJsonObject().has("model"))
                        appearance = baker.model(variant.getAsJsonObject().get("model").getAsString());
                }
            }
            if (appearance == null && hasBase(state.getBlock(), "com.gregtechceu.gtceu.api.block.MaterialBlock"))
                appearance = GtMaterialModels.bake(baker, state.getBlock());
            // Never replace an unknown multiblock casing with an unrelated voltage hull.
            if (appearance == null && GtMachineModels.profile(id).casing() == null)
                throw new IOException("Unresolved appearance: " + casingId);
        }
        String quality = profile.workable() != null || profile.overlay() != null ? "machine-static" : "machine-shell";
        return new Result(new GtMachineModels(baker).machine(id, tier, appearance), quality, false);
    }
}
