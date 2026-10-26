package minimalperipherals;

import minimalperipherals.platform.Platform;
import net.minecraft.resources.Identifier;

/** Loader-independent entry point. The loader modules set {@link #PLATFORM} before touching {@link ModRegistry}. */
public final class MinimalPeripherals {
    public static final String MOD_ID = "minimalperipherals";

    /** Set by the Fabric / NeoForge entrypoint, as early as possible. */
    public static Platform PLATFORM;

    private MinimalPeripherals() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
