package cryptographyperipherals;

import cryptographyperipherals.platform.Platform;
import net.minecraft.resources.Identifier;

/** Loader-independent entry point. The loader modules set {@link #PLATFORM} before touching {@link ModRegistry}. */
public final class CryptographyPeripherals {
    public static final String MOD_ID = "cryptographyperipherals";

    /** Set by the Fabric / NeoForge entrypoint, as early as possible. */
    public static Platform PLATFORM;

    private CryptographyPeripherals() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
