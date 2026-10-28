package cryptographyperipherals.peripheral;

import cryptographyperipherals.block.CryptoBlockEntity;
import cryptographyperipherals.core.Crypto;
import cryptographyperipherals.core.CryptoExtras;
import cryptographyperipherals.utils.CryptoUtils;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static cryptographyperipherals.peripheral.LuaUtil.bytes;
import static cryptographyperipherals.peripheral.LuaUtil.string;

/** Dedicated ChaCha20-Poly1305 block (RFC 8439): fast without hardware AES support. */
public class ChaChaEngine extends EnginePeripheral {
    private static final int NONCE_SIZE = 12;
    private static final int TAG_SIZE = 16;

    public ChaChaEngine(CryptoBlockEntity owner) {
        super(owner, "chacha_engine");
    }

    private static byte[] key(IArguments args, int index) throws LuaException {
        var key = args.getString(index);
        CryptoUtils.assertLength(index, key.length(), 32);
        return bytes(key);
    }

    private static byte[] run(boolean encrypt, byte[] data, byte[] key, byte[] nonce, byte[] aad) throws LuaException {
        try {
            return CryptoExtras.chaCha20Poly1305(encrypt, data, key, nonce, aad);
        } catch (GeneralSecurityException e) {
            throw new LuaException(encrypt ? String.valueOf(e.getMessage()) : "authentication failed");
        }
    }

    private static byte[] concat(byte[] nonce, byte[] sealed) {
        var result = new byte[nonce.length + sealed.length];
        System.arraycopy(nonce, 0, result, 0, nonce.length);
        System.arraycopy(sealed, 0, result, nonce.length, sealed.length);
        return result;
    }

    /** generateKey(): a random 32 byte key. */
    @LuaFunction
    public final String generateKey() {
        return string(Crypto.secureRandomBuffer(32));
    }

    /** seal(data, key [, aad]): encrypts with a random nonce. Returns nonce + ciphertext + tag. */
    @LuaFunction
    public final String seal(IArguments args) throws LuaException {
        var nonce = Crypto.secureRandomBuffer(NONCE_SIZE);
        var sealed = run(true, bytes(args.getString(0)), key(args, 1), nonce, bytes(args.optString(2, "")));
        return string(concat(nonce, sealed));
    }

    /** open(sealed, key [, aad]): the reverse of seal. Fails if the data was modified. */
    @LuaFunction
    public final String open(IArguments args) throws LuaException {
        var sealed = bytes(args.getString(0));
        if (sealed.length < NONCE_SIZE + TAG_SIZE) {
            throw new LuaException("encrypted data is too short");
        }

        var nonce = Arrays.copyOfRange(sealed, 0, NONCE_SIZE);
        var body = Arrays.copyOfRange(sealed, NONCE_SIZE, sealed.length);
        return string(run(false, body, key(args, 1), nonce, bytes(args.optString(2, ""))));
    }

    /** sealMany(list, key [, aad]) */
    @LuaFunction
    public final List<String> sealMany(IArguments args) throws LuaException {
        if (!(args.get(0) instanceof Map<?, ?> table)) {
            throw new LuaException("bad argument #1 (expected table)");
        }

        var key = key(args, 1);
        var aad = bytes(args.optString(2, ""));
        var result = new ArrayList<String>();
        for (var item : LuaUtil.strings(table)) {
            var nonce = Crypto.secureRandomBuffer(NONCE_SIZE);
            result.add(string(concat(nonce, run(true, bytes(item), key, nonce, aad))));
        }
        return result;
    }

    /** encrypt(data, key, nonce [, aad]): with a nonce you choose (12 bytes, never reuse it). */
    @LuaFunction
    public final String encrypt(IArguments args) throws LuaException {
        var nonce = args.getString(2);
        CryptoUtils.assertLength(2, nonce.length(), NONCE_SIZE);
        return string(run(true, bytes(args.getString(0)), key(args, 1), bytes(nonce), bytes(args.optString(3, ""))));
    }

    @LuaFunction
    public final String decrypt(IArguments args) throws LuaException {
        var nonce = args.getString(2);
        CryptoUtils.assertLength(2, nonce.length(), NONCE_SIZE);
        var data = bytes(args.getString(0));
        if (data.length < TAG_SIZE) {
            throw new LuaException("encrypted data is too short");
        }
        return string(run(false, data, key(args, 1), bytes(nonce), bytes(args.optString(3, ""))));
    }
}
