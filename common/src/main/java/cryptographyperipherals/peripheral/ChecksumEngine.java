package cryptographyperipherals.peripheral;

import cryptographyperipherals.block.CryptoBlockEntity;
import cryptographyperipherals.core.CryptoExtras;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32C;

import static cryptographyperipherals.peripheral.LuaUtil.bytes;

/** Dedicated checksum block: fast, non-cryptographic fingerprints. Do not use them for security. */
public class ChecksumEngine extends EnginePeripheral {
    public ChecksumEngine(CryptoBlockEntity owner) {
        super(owner, "checksum_engine");
    }

    private static long crc32c(byte[] data) {
        var crc = new CRC32C();
        crc.update(data, 0, data.length);
        return crc.getValue();
    }

    private static long fnv1a32(byte[] data) {
        var hash = 0x811c9dc5;
        for (var value : data) {
            hash ^= value & 0xff;
            hash *= 0x01000193;
        }
        return hash & 0xffffffffL;
    }

    private static long fnv1a64(byte[] data) {
        var hash = 0xcbf29ce484222325L;
        for (var value : data) {
            hash ^= value & 0xff;
            hash *= 0x100000001b3L;
        }
        return hash;
    }

    @LuaFunction
    public final long crc32(String data) {
        return CryptoExtras.crc32(bytes(data));
    }

    /** CRC-32C (Castagnoli), used by iSCSI, ext4 and Btrfs. */
    @LuaFunction
    public final long crc32c(String data) {
        return crc32c(bytes(data));
    }

    @LuaFunction
    public final long adler32(String data) {
        return CryptoExtras.adler32(bytes(data));
    }

    /** FNV-1a, 32 bits, as a number. */
    @LuaFunction
    public final long fnv1a32(String data) {
        return fnv1a32(bytes(data));
    }

    /** FNV-1a, 64 bits, as 16 hexadecimal digits (a Lua number cannot hold 64 bits). */
    @LuaFunction
    public final String fnv1a64(String data) {
        return String.format("%016x", fnv1a64(bytes(data)));
    }

    /** all(data): every checksum at once, as a table. */
    @LuaFunction
    public final Map<String, Object> all(String data) {
        var input = bytes(data);
        var result = new HashMap<String, Object>();
        result.put("crc32", CryptoExtras.crc32(input));
        result.put("crc32c", crc32c(input));
        result.put("adler32", CryptoExtras.adler32(input));
        result.put("fnv1a32", fnv1a32(input));
        result.put("fnv1a64", String.format("%016x", fnv1a64(input)));
        return result;
    }

    /** luhnDigit(digits): the Luhn check digit to append to a number (credit cards, IMEI...). */
    @LuaFunction
    public final int luhnDigit(String digits) throws LuaException {
        return (10 - luhnSum(digits, true) % 10) % 10;
    }

    /** luhnValid(number): true if the last digit is a correct Luhn check digit. */
    @LuaFunction
    public final boolean luhnValid(String number) throws LuaException {
        return number.length() > 1 && luhnSum(number, false) % 10 == 0;
    }

    private static int luhnSum(String digits, boolean appendingCheckDigit) throws LuaException {
        if (digits.isEmpty()) {
            throw new LuaException("bad argument #1 (expected a string of digits)");
        }

        var sum = 0;
        var double_ = appendingCheckDigit;
        for (var i = digits.length() - 1; i >= 0; i--) {
            var c = digits.charAt(i);
            if (c < '0' || c > '9') {
                throw new LuaException("bad argument #1 (expected a string of digits)");
            }

            var value = c - '0';
            if (double_) {
                value *= 2;
                if (value > 9) {
                    value -= 9;
                }
            }
            sum += value;
            double_ = !double_;
        }
        return sum;
    }
}
