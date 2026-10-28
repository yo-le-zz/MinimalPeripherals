package cryptographyperipherals.peripheral;

import cryptographyperipherals.Limits;
import cryptographyperipherals.block.CryptoBlockEntity;
import cryptographyperipherals.core.Crypto;
import cryptographyperipherals.core.CryptoExtras;
import cryptographyperipherals.core.CryptoExtras.HashAlgorithm;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

import java.security.MessageDigest;
import java.util.Base64;

import static cryptographyperipherals.peripheral.LuaUtil.bytes;

/**
 * Password hashing in one call. The result is a self-contained string:
 * $pbkdf2-sha256$i=120000$salt$hash (salt and hash in unpadded url-safe base64).
 */
public class PasswordEngine extends EnginePeripheral {
    private static final String PREFIX = "pbkdf2-sha256";
    private static final int SALT_SIZE = 16;
    private static final int HASH_SIZE = 32;

    public PasswordEngine(CryptoBlockEntity owner) {
        super(owner, "password_hasher");
    }

    private static String b64(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private record Parsed(int iterations, byte[] salt, byte[] hash) {
    }

    private static Parsed parse(String encoded) {
        try {
            var parts = encoded.split("\\$");
            if (parts.length != 5 || !parts[0].isEmpty() || !parts[1].equals(PREFIX) || !parts[2].startsWith("i=")) {
                return null;
            }

            var iterations = Integer.parseInt(parts[2].substring(2));
            if (iterations < 1 || iterations > Limits.PBKDF2_MAX_ITERATIONS) {
                return null;
            }

            return new Parsed(iterations, Base64.getUrlDecoder().decode(parts[3]), Base64.getUrlDecoder().decode(parts[4]));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static byte[] derive(byte[] password, byte[] salt, int iterations, int length) {
        return CryptoExtras.pbkdf2(HashAlgorithm.sha256, password, salt, iterations, length);
    }

    /** hash(password [, iterations]): returns the string to store. Default: 120000 iterations. */
    @LuaFunction
    public final String hash(IArguments args) throws LuaException {
        var iterations = LuaUtil.optBoundedInt(args, 1, Limits.PASSWORD_DEFAULT_ITERATIONS, 1, Limits.PBKDF2_MAX_ITERATIONS, "iterations");
        var salt = Crypto.secureRandomBuffer(SALT_SIZE);
        var hash = derive(bytes(args.getString(0)), salt, iterations, HASH_SIZE);
        return "$" + PREFIX + "$i=" + iterations + "$" + b64(salt) + "$" + b64(hash);
    }

    /** verify(password, stored): true if the password matches. Never throws on a malformed stored value. */
    @LuaFunction
    public final boolean verify(String password, String stored) {
        var parsed = parse(stored);
        if (parsed == null || parsed.hash().length == 0) {
            return false;
        }

        var actual = derive(bytes(password), parsed.salt(), parsed.iterations(), parsed.hash().length);
        return MessageDigest.isEqual(actual, parsed.hash());
    }

    /** needsRehash(stored [, iterations]): true if the stored hash is malformed or weaker than the target. */
    @LuaFunction
    public final boolean needsRehash(IArguments args) throws LuaException {
        var target = LuaUtil.optBoundedInt(args, 1, Limits.PASSWORD_DEFAULT_ITERATIONS, 1, Limits.PBKDF2_MAX_ITERATIONS, "iterations");
        var parsed = parse(args.getString(0));
        return parsed == null || parsed.iterations() < target || parsed.hash().length < HASH_SIZE || parsed.salt().length < SALT_SIZE;
    }
}
