package cryptographyperipherals.peripheral;

import cryptographyperipherals.Limits;
import cryptographyperipherals.block.CryptoBlockEntity;
import cryptographyperipherals.core.Crypto;
import cryptographyperipherals.utils.CryptoUtils;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static cryptographyperipherals.peripheral.LuaUtil.bytes;
import static cryptographyperipherals.peripheral.LuaUtil.hex;
import static cryptographyperipherals.peripheral.LuaUtil.optBoundedInt;
import static cryptographyperipherals.peripheral.LuaUtil.string;

/** Dedicated BLAKE3 block: hash, keyed hash, key derivation and extendable output. */
public class Blake3Engine extends EnginePeripheral {
    public Blake3Engine(CryptoBlockEntity owner) {
        super(owner, "blake3_engine");
    }

    private static int size(IArguments args, int index) throws LuaException {
        return optBoundedInt(args, index, 32, 1, Limits.BLAKE3_MAX_OUTPUT, "output size");
    }

    /** hash(data [, outputSize]): hexadecimal, 32 bytes by default. */
    @LuaFunction
    public final String hash(IArguments args) throws LuaException {
        return hex(bytes(Crypto.blake3Hash(args.getString(0), false, size(args, 1))));
    }

    @LuaFunction
    public final String raw(IArguments args) throws LuaException {
        return Crypto.blake3Hash(args.getString(0), false, size(args, 1));
    }

    /** keyed(key, data [, outputSize]): a MAC. The key is 32 bytes. */
    @LuaFunction
    public final String keyed(IArguments args) throws LuaException {
        var key = CryptoUtils.assertKey(args.getString(0));
        return hex(bytes(Crypto.blake3Keyed(args.getString(1), key, false, size(args, 2))));
    }

    /** derive(context, material [, outputSize]): derives a key from some key material. */
    @LuaFunction
    public final String derive(IArguments args) throws LuaException {
        return string(bytes(Crypto.blake3Derive(args.getString(1), args.getString(0), false, size(args, 2))));
    }

    /** many(list [, outputSize]): hashes every string of the list. */
    @LuaFunction
    public final List<String> many(IArguments args) throws LuaException {
        if (!(args.get(0) instanceof Map<?, ?> table)) {
            throw new LuaException("bad argument #1 (expected table)");
        }
        var items = LuaUtil.strings(table);
        var outputSize = size(args, 1);
        var result = new ArrayList<String>(items.size());
        for (var item : items) {
            result.add(hex(bytes(Crypto.blake3Hash(item, false, outputSize))));
        }
        return result;
    }
}
