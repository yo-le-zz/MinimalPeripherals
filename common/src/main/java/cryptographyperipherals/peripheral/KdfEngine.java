package cryptographyperipherals.peripheral;

import cryptographyperipherals.Limits;
import cryptographyperipherals.block.CryptoBlockEntity;
import cryptographyperipherals.core.CryptoExtras;
import cryptographyperipherals.core.CryptoExtras.HashAlgorithm;
import cryptographyperipherals.core.CryptoExtras.OutputFormat;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

import static cryptographyperipherals.peripheral.LuaUtil.boundedInt;
import static cryptographyperipherals.peripheral.LuaUtil.bytes;
import static cryptographyperipherals.peripheral.LuaUtil.encode;
import static cryptographyperipherals.peripheral.LuaUtil.optFormat;

/** Dedicated key derivation block. Every function returns raw bytes unless a format is given. */
public class KdfEngine extends EnginePeripheral {
    public KdfEngine(CryptoBlockEntity owner) {
        super(owner, "kdf_engine");
    }

    private static HashAlgorithm algorithm(IArguments args, int index) throws LuaException {
        return args.optEnum(index, HashAlgorithm.class).orElse(HashAlgorithm.sha256);
    }

    /** pbkdf2(password, salt, iterations, length [, algorithm [, format]]) */
    @LuaFunction
    public final String pbkdf2(IArguments args) throws LuaException {
        var password = bytes(args.getString(0));
        var salt = bytes(args.getString(1));
        var iterations = boundedInt(args, 2, 1, Limits.PBKDF2_MAX_ITERATIONS, "iterations");
        var length = boundedInt(args, 3, 1, Limits.KDF_MAX_OUTPUT, "length");
        var algorithm = algorithm(args, 4);
        return encode(CryptoExtras.pbkdf2(algorithm, password, salt, iterations, length), optFormat(args, 5, OutputFormat.raw));
    }

    /** hkdf(inputKeyMaterial, salt, info, length [, algorithm [, format]]) */
    @LuaFunction
    public final String hkdf(IArguments args) throws LuaException {
        var ikm = bytes(args.getString(0));
        var salt = bytes(args.getString(1));
        var info = bytes(args.getString(2));
        var length = boundedInt(args, 3, 1, Limits.KDF_MAX_OUTPUT, "length");
        var algorithm = algorithm(args, 4);
        if (length > 255 * CryptoExtras.hashSize(algorithm)) {
            throw new LuaException("bad argument #4 (length is too large for this algorithm)");
        }
        return encode(CryptoExtras.hkdf(algorithm, ikm, salt, info, length), optFormat(args, 5, OutputFormat.raw));
    }

    /** scrypt(password, salt, n, r, p, length [, format]) */
    @LuaFunction
    public final String scrypt(IArguments args) throws LuaException {
        var password = bytes(args.getString(0));
        var salt = bytes(args.getString(1));
        var n = boundedInt(args, 2, 2, Limits.SCRYPT_MAX_N, "n");
        var r = boundedInt(args, 3, 1, Limits.SCRYPT_MAX_R, "r");
        var p = boundedInt(args, 4, 1, Limits.SCRYPT_MAX_P, "p");
        var length = boundedInt(args, 5, 1, Limits.KDF_MAX_OUTPUT, "length");
        if (Integer.bitCount(n) != 1) {
            throw new LuaException("bad argument #3 (n must be a power of two)");
        }

        try {
            return encode(CryptoExtras.scrypt(password, salt, n, r, p, length), optFormat(args, 6, OutputFormat.raw));
        } catch (RuntimeException e) {
            throw new LuaException(String.valueOf(e.getMessage()));
        }
    }

    /** expand(secret, info, length [, format]): HKDF-SHA256 without salt, to get several keys from one secret. */
    @LuaFunction
    public final String expand(IArguments args) throws LuaException {
        var length = boundedInt(args, 2, 1, 255 * 32, "length");
        return encode(
                CryptoExtras.hkdf(HashAlgorithm.sha256, bytes(args.getString(0)), new byte[0], bytes(args.getString(1)), length),
                optFormat(args, 3, OutputFormat.raw));
    }
}
