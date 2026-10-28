package cryptographyperipherals.peripheral;

import cryptographyperipherals.Limits;
import cryptographyperipherals.block.CryptoBlockEntity;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static cryptographyperipherals.peripheral.LuaUtil.bytes;
import static cryptographyperipherals.peripheral.LuaUtil.hex;
import static cryptographyperipherals.peripheral.LuaUtil.string;

/**
 * A block dedicated to a single hash function. Compared to the general accelerator the call path is
 * shorter (no algorithm lookup, no format parsing) and the batch functions hash many values in one call.
 */
public abstract class HashEngine extends EnginePeripheral {
    protected HashEngine(CryptoBlockEntity owner, String type) {
        super(owner, type);
    }

    protected abstract byte[] digest(byte[] data);

    protected abstract byte[] mac(byte[] key, byte[] data);

    protected abstract int digestSize();

    /** Hash as a lowercase hexadecimal string. */
    @LuaFunction
    public final String hash(String data) {
        return hex(digest(bytes(data)));
    }

    /** Hash as raw bytes. */
    @LuaFunction
    public final String raw(String data) {
        return string(digest(bytes(data)));
    }

    /** Hashes every string of the list, returns the list of hexadecimal hashes. */
    @LuaFunction
    public final List<String> many(Map<?, ?> list) throws LuaException {
        var items = LuaUtil.strings(list);
        var result = new ArrayList<String>(items.size());
        for (var item : items) {
            result.add(hex(digest(bytes(item))));
        }
        return result;
    }

    /** hash(hash(data)): protects against length extension, used by Bitcoin with SHA-256. */
    @LuaFunction
    public final String twice(String data) {
        return hex(digest(digest(bytes(data))));
    }

    /** Applies the hash function rounds times in a row (hash chains, simple key stretching). */
    @LuaFunction
    public final String chain(String data, int rounds) throws LuaException {
        if (rounds < 1 || rounds > Limits.HASH_CHAIN_MAX_ROUNDS) {
            throw new LuaException("bad argument #2 (rounds must be between 1 and " + Limits.HASH_CHAIN_MAX_ROUNDS + ")");
        }

        var current = bytes(data);
        for (var i = 0; i < rounds; i++) {
            current = digest(current);
        }
        return hex(current);
    }

    /** HMAC of the data, as hexadecimal. */
    @LuaFunction
    public final String hmac(String key, String data) {
        return hex(mac(bytes(key), bytes(data)));
    }

    /** HMAC of every string of the list with the same key. */
    @LuaFunction
    public final List<String> hmacMany(String key, Map<?, ?> list) throws LuaException {
        var items = LuaUtil.strings(list);
        var keyBytes = bytes(key);
        var result = new ArrayList<String>(items.size());
        for (var item : items) {
            result.add(hex(mac(keyBytes, bytes(item))));
        }
        return result;
    }

    /** Checks the hexadecimal hash of the data, in constant time. */
    @LuaFunction
    public final boolean verify(String data, String expectedHex) {
        return LuaUtil.hexEquals(digest(bytes(data)), expectedHex);
    }

    /** Checks an HMAC, in constant time. */
    @LuaFunction
    public final boolean verifyHmac(String key, String data, String expectedHex) {
        return LuaUtil.hexEquals(mac(bytes(key), bytes(data)), expectedHex);
    }

    /** Size of the hash, in bytes. */
    @LuaFunction
    public final int size() {
        return digestSize();
    }
}
