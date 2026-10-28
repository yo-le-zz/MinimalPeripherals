package cryptographyperipherals.peripheral;

import cryptographyperipherals.Limits;
import cryptographyperipherals.block.CryptoBlockEntity;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import static cryptographyperipherals.peripheral.LuaUtil.bytes;
import static cryptographyperipherals.peripheral.LuaUtil.hex;

/**
 * Proof of work: find a nonce so that sha256(prefix .. nonce) starts with a number of zero bits,
 * where nonce is written in decimal. A dedicated native loop, orders of magnitude faster than Lua.
 */
public class PowEngine extends EnginePeripheral {
    public PowEngine(CryptoBlockEntity owner) {
        super(owner, "pow_miner");
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static int leadingZeroBits(byte[] hash) {
        var bits = 0;
        for (var value : hash) {
            if (value == 0) {
                bits += 8;
                continue;
            }
            bits += Integer.numberOfLeadingZeros(value & 0xff) - 24;
            break;
        }
        return bits;
    }

    private static byte[] hashOf(MessageDigest md, byte[] prefix, long nonce) {
        md.update(prefix);
        md.update(Long.toString(nonce).getBytes(StandardCharsets.ISO_8859_1));
        return md.digest();
    }

    /**
     * mine(prefix, difficulty [, maxAttempts [, startNonce]]): returns {nonce, hash, attempts}, or nil
     * if nothing was found within maxAttempts (1,000,000 by default, 5,000,000 at most).
     * Call it again with startNonce = startNonce + maxAttempts to continue.
     */
    @LuaFunction
    public final Map<String, Object> mine(IArguments args) throws LuaException {
        var prefix = bytes(args.getString(0));
        var difficulty = LuaUtil.boundedInt(args, 1, 0, 64, "difficulty");
        var attempts = LuaUtil.optBoundedInt(args, 2, 1_000_000, 1, Limits.POW_MAX_ITERATIONS, "maxAttempts");
        var start = args.optLong(3, 0);
        if (start < 0) {
            throw new LuaException("bad argument #4 (startNonce must not be negative)");
        }

        var md = sha256();
        for (var i = 0; i < attempts; i++) {
            var nonce = start + i;
            var hash = hashOf(md, prefix, nonce);
            if (leadingZeroBits(hash) >= difficulty) {
                var result = new HashMap<String, Object>();
                result.put("nonce", nonce);
                result.put("hash", hex(hash));
                result.put("attempts", i + 1);
                return result;
            }
        }
        return null;
    }

    /** hash(prefix, nonce): sha256(prefix .. nonce) as hexadecimal. */
    @LuaFunction
    public final String hash(String prefix, long nonce) {
        return hex(hashOf(sha256(), bytes(prefix), nonce));
    }

    /** verify(prefix, nonce, difficulty): true if the nonce gives at least difficulty zero bits. */
    @LuaFunction
    public final boolean verify(String prefix, long nonce, int difficulty) {
        return leadingZeroBits(hashOf(sha256(), bytes(prefix), nonce)) >= difficulty;
    }

    /** difficulty(hashHex): the number of leading zero bits of a hexadecimal hash. */
    @LuaFunction
    public final int difficulty(String hashHex) throws LuaException {
        try {
            return leadingZeroBits(java.util.HexFormat.of().parseHex(hashHex));
        } catch (IllegalArgumentException e) {
            throw new LuaException("bad argument #1 (expected a hexadecimal string)");
        }
    }
}
