package cryptographyperipherals.peripheral;

import cryptographyperipherals.Limits;
import cryptographyperipherals.block.CryptoBlockEntity;
import cryptographyperipherals.core.Codecs;
import cryptographyperipherals.core.CryptoExtras;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

import java.util.Base64;
import java.util.HexFormat;

import static cryptographyperipherals.peripheral.LuaUtil.bytes;
import static cryptographyperipherals.peripheral.LuaUtil.string;

/** Dedicated encoding block. Every decode function returns nil when the input is not valid. */
public class EncoderEngine extends EnginePeripheral {
    public EncoderEngine(CryptoBlockEntity owner) {
        super(owner, "encoder");
    }

    @LuaFunction
    public final String base64(String data) {
        return Base64.getEncoder().encodeToString(bytes(data));
    }

    @LuaFunction
    public final String unbase64(String text) {
        try {
            return string(Base64.getDecoder().decode(text));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** URL-safe base64 without padding. */
    @LuaFunction
    public final String base64url(String data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes(data));
    }

    @LuaFunction
    public final String unbase64url(String text) {
        try {
            return string(Base64.getUrlDecoder().decode(text));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @LuaFunction
    public final String hex(String data) {
        return HexFormat.of().formatHex(bytes(data));
    }

    @LuaFunction
    public final String unhex(String text) {
        try {
            return string(HexFormat.of().parseHex(text));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** base32(data [, padding]): RFC 4648, with padding by default. */
    @LuaFunction
    public final String base32(IArguments args) throws LuaException {
        return Codecs.base32Encode(bytes(args.getString(0)), args.optBoolean(1, true));
    }

    @LuaFunction
    public final String unbase32(String text) {
        try {
            return string(Codecs.base32Decode(text));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Base58 with the Bitcoin alphabet (no 0, O, I and l). Input is limited to 4096 bytes. */
    @LuaFunction
    public final String base58(String data) throws LuaException {
        if (data.length() > Limits.BASE58_MAX_INPUT) {
            throw new LuaException("bad argument #1 (base58 input is limited to " + Limits.BASE58_MAX_INPUT + " bytes)");
        }
        return Codecs.base58Encode(bytes(data));
    }

    @LuaFunction
    public final String unbase58(String text) throws LuaException {
        if (text.length() > Limits.BASE58_MAX_INPUT * 2) {
            throw new LuaException("bad argument #1 (base58 input is too long)");
        }

        try {
            return string(Codecs.base58Decode(text));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** xor(data, key): XORs the data with the key, repeated as needed. */
    @LuaFunction
    public final String xor(String data, String key) throws LuaException {
        if (key.isEmpty()) {
            throw new LuaException("bad argument #2 (the key must not be empty)");
        }

        var input = bytes(data);
        var keyBytes = bytes(key);
        var out = new byte[input.length];
        for (var i = 0; i < input.length; i++) {
            out[i] = (byte) (input[i] ^ keyBytes[i % keyBytes.length]);
        }
        return string(out);
    }

    /** crc32 of the data as a number, handy to tag encoded payloads. */
    @LuaFunction
    public final long crc32(String data) {
        return CryptoExtras.crc32(bytes(data));
    }
}
