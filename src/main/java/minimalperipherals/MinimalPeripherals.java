package minimalperipherals;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

public class MinimalPeripherals implements ModInitializer {
    public static final String MOD_ID = "minimalperipherals";

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModRegistry.init();
    }
}
