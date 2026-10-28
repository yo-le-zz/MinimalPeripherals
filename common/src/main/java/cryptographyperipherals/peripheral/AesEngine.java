package cryptographyperipherals.peripheral;

import cryptographyperipherals.block.CryptoBlockEntity;
import cryptographyperipherals.core.Crypto;
import cryptographyperipherals.core.CryptoExtras;
import cryptographyperipherals.utils.CryptoUtils;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

import javax.crypto.BadPaddingException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static cryptographyperipherals.peripheral.LuaUtil.bytes;
import static cryptographyperipherals.peripheral.LuaUtil.string;

/** Dedicated AES block. AES-GCM is the default: it encrypts and authenticates in one pass. */
public class AesEngine extends EnginePeripheral {
    private static final int NONCE_SIZE = 12;
    private static final int TAG_SIZE = 16;

    public AesEngine(CryptoBlockEntity owner) {
        super(owner, "aes_engine");
    }

    private static byte[] key(IArguments args, int index) throws LuaException {
        var key = args.getString(index);
        CryptoUtils.assertLength(index, key.length(), 16, 24, 32);
        return bytes(key);
    }

    private static byte[] aad(IArguments args, int index) throws LuaException {
        return bytes(args.optString(index, ""));
    }

    private static byte[] gcm(boolean encrypt, byte[] data, byte[] key, byte[] nonce, byte[] aad) throws LuaException {
        try {
            return CryptoExtras.aesGcm(encrypt, data, key, nonce, aad);
        } catch (GeneralSecurityException e) {
            throw new LuaException(encrypt ? String.valueOf(e.getMessage()) : "authentication failed");
        }
    }

    /** generateKey([bits]): a random key, 256 bits by default (128, 192 or 256). */
    @LuaFunction
    public final String generateKey(IArguments args) throws LuaException {
        var bits = args.optInt(0, 256);
        if (bits != 128 && bits != 192 && bits != 256) {
            throw new LuaException("bad argument #1 (bits must be 128, 192 or 256)");
        }
        return string(Crypto.secureRandomBuffer(bits / 8));
    }

    /** seal(data, key [, aad]): encrypts with a random nonce. Returns nonce + ciphertext + tag. */
    @LuaFunction
    public final String seal(IArguments args) throws LuaException {
        var nonce = Crypto.secureRandomBuffer(NONCE_SIZE);
        var sealed = gcm(true, bytes(args.getString(0)), key(args, 1), nonce, aad(args, 2));
        var result = new byte[nonce.length + sealed.length];
        System.arraycopy(nonce, 0, result, 0, nonce.length);
        System.arraycopy(sealed, 0, result, nonce.length, sealed.length);
        return string(result);
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
        return string(gcm(false, body, key(args, 1), nonce, aad(args, 2)));
    }

    /** sealMany(list, key [, aad]): seals every string of the list with the same key. */
    @LuaFunction
    public final List<String> sealMany(IArguments args) throws LuaException {
        if (!(args.get(0) instanceof Map<?, ?> table)) {
            throw new LuaException("bad argument #1 (expected table)");
        }

        var key = key(args, 1);
        var aad = aad(args, 2);
        var result = new ArrayList<String>();
        for (var item : LuaUtil.strings(table)) {
            var nonce = Crypto.secureRandomBuffer(NONCE_SIZE);
            var sealed = gcm(true, bytes(item), key, nonce, aad);
            var blob = new byte[nonce.length + sealed.length];
            System.arraycopy(nonce, 0, blob, 0, nonce.length);
            System.arraycopy(sealed, 0, blob, nonce.length, sealed.length);
            result.add(string(blob));
        }
        return result;
    }

    /** encrypt(data, key, nonce [, aad]): AES-GCM with a nonce you choose (12 bytes, never reuse it). */
    @LuaFunction
    public final String encrypt(IArguments args) throws LuaException {
        var nonce = args.getString(2);
        CryptoUtils.assertLength(2, nonce.length(), NONCE_SIZE);
        return string(gcm(true, bytes(args.getString(0)), key(args, 1), bytes(nonce), aad(args, 3)));
    }

    @LuaFunction
    public final String decrypt(IArguments args) throws LuaException {
        var nonce = args.getString(2);
        CryptoUtils.assertLength(2, nonce.length(), NONCE_SIZE);
        var data = bytes(args.getString(0));
        if (data.length < TAG_SIZE) {
            throw new LuaException("encrypted data is too short");
        }
        return string(gcm(false, data, key(args, 1), bytes(nonce), aad(args, 3)));
    }

    /** ctr(data, key, iv): AES-CTR stream cipher (no authentication). Same call to encrypt and decrypt. */
    @LuaFunction
    public final String ctr(IArguments args) throws LuaException {
        var iv = args.getString(2);
        CryptoUtils.assertLength(2, iv.length(), 16);
        try {
            return string(CryptoExtras.aesCtr(bytes(args.getString(0)), key(args, 1), bytes(iv)));
        } catch (GeneralSecurityException e) {
            throw new LuaException(String.valueOf(e.getMessage()));
        }
    }

    /** cbcEncrypt(data, key, iv): AES-CBC with PKCS7 padding (no authentication). */
    @LuaFunction
    public final String cbcEncrypt(IArguments args) throws LuaException {
        return cbc(args, true);
    }

    @LuaFunction
    public final String cbcDecrypt(IArguments args) throws LuaException {
        return cbc(args, false);
    }

    private static String cbc(IArguments args, boolean encrypt) throws LuaException {
        var data = args.getString(0);
        var key = args.getString(1);
        var iv = args.getString(2);
        CryptoUtils.assertLength(1, key.length(), 16, 24, 32);
        CryptoUtils.assertLength(2, iv.length(), 16);

        try {
            return encrypt ? Crypto.encryptAes(data, key, iv) : Crypto.decryptAes(data, key, iv);
        } catch (BadPaddingException e) {
            throw new LuaException(String.valueOf(e.getMessage()));
        }
    }
}
