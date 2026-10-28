package cryptographyperipherals.peripheral;

import cryptographyperipherals.Limits;
import cryptographyperipherals.core.CryptoExtras;
import cryptographyperipherals.core.CryptoExtras.OutputFormat;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/** Small helpers shared by the peripherals. Lua strings are byte strings (ISO-8859-1). */
public final class LuaUtil {
    private LuaUtil() {
    }

    public static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.ISO_8859_1);
    }

    public static String string(byte[] value) {
        return new String(value, StandardCharsets.ISO_8859_1);
    }

    public static String hex(byte[] value) {
        return HexFormat.of().formatHex(value);
    }

    /** Constant-time comparison of a hexadecimal digest (case-insensitive) with the expected value. */
    public static boolean hexEquals(byte[] actual, String expectedHex) {
        var expected = bytes(hex(actual));
        return MessageDigest.isEqual(expected, bytes(expectedHex.toLowerCase()));
    }

    /** Returns the values of a Lua list (keys 1..n), in order. */
    public static List<Object> values(Map<?, ?> table, int max) throws LuaException {
        if (table.size() > max) {
            throw new LuaException("too many items (the limit is " + max + ")");
        }

        var entries = new ArrayList<Map.Entry<?, ?>>();
        for (var entry : table.entrySet()) {
            if (!(entry.getKey() instanceof Number)) {
                throw new LuaException("expected a list (a table with the keys 1, 2, 3...)");
            }
            entries.add(entry);
        }
        entries.sort((a, b) -> Double.compare(((Number) a.getKey()).doubleValue(), ((Number) b.getKey()).doubleValue()));

        var result = new ArrayList<Object>(entries.size());
        for (var entry : entries) {
            result.add(entry.getValue());
        }
        return result;
    }

    /** Returns the values of a Lua list of strings, in order. */
    public static List<String> strings(Map<?, ?> table, int max) throws LuaException {
        var values = values(table, max);
        var result = new ArrayList<String>(values.size());
        for (var value : values) {
            if (!(value instanceof String text)) {
                throw new LuaException("expected a list of strings");
            }
            result.add(text);
        }
        return result;
    }

    public static List<String> strings(Map<?, ?> table) throws LuaException {
        return strings(table, Limits.BATCH_MAX_ITEMS);
    }

    /** Reads an output format: a boolean (true = hex, false = raw) or a format name. */
    public static OutputFormat optFormat(IArguments args, int index, OutputFormat fallback) throws LuaException {
        var value = args.get(index);
        if (value == null) {
            return fallback;
        }

        if (value instanceof Boolean flag) {
            return flag ? OutputFormat.hex : OutputFormat.raw;
        }

        if (value instanceof String name) {
            try {
                return OutputFormat.valueOf(name);
            } catch (IllegalArgumentException e) {
                throw new LuaException("bad argument #" + (index + 1) + " (expected hex, base64, base64url or raw)");
            }
        }

        throw new LuaException("bad argument #" + (index + 1) + " (expected boolean or string)");
    }

    public static String encode(byte[] data, OutputFormat format) {
        return CryptoExtras.encode(data, format);
    }

    public static int boundedInt(IArguments args, int index, int min, int max, String name) throws LuaException {
        var value = args.getInt(index);
        if (value < min || value > max) {
            throw new LuaException("bad argument #" + (index + 1) + " (" + name + " must be between " + min + " and " + max + ")");
        }
        return value;
    }

    public static int optBoundedInt(IArguments args, int index, int fallback, int min, int max, String name) throws LuaException {
        return args.get(index) == null ? fallback : boundedInt(args, index, min, max, name);
    }
}
