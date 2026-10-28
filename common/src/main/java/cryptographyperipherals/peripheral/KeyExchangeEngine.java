package cryptographyperipherals.peripheral;

import cryptographyperipherals.Limits;
import cryptographyperipherals.block.CryptoBlockEntity;
import cryptographyperipherals.core.Crypto;
import cryptographyperipherals.core.CryptoExtras;
import cryptographyperipherals.core.CryptoExtras.HashAlgorithm;
import cryptographyperipherals.utils.CryptoUtils;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

import java.util.HashMap;
import java.util.Map;

import static cryptographyperipherals.peripheral.LuaUtil.bytes;
import static cryptographyperipherals.peripheral.LuaUtil.string;

/** Dedicated X25519 block: Diffie-Hellman key exchange with a ready-to-use key derivation. */
public class KeyExchangeEngine extends EnginePeripheral {
    public KeyExchangeEngine(CryptoBlockEntity owner) {
        super(owner, "key_exchange");
    }

    @LuaFunction
    public final Map<String, String> generateKeyPair() {
        var privateKey = Crypto.generatePrivateKey();
        var pair = new HashMap<String, String>();
        pair.put("privateKey", privateKey);
        pair.put("publicKey", Crypto.deriveECDHPublicKey(privateKey));
        return pair;
    }

    @LuaFunction
    public final String publicKey(String privateKey) throws LuaException {
        CryptoUtils.assertKey(privateKey);
        return Crypto.deriveECDHPublicKey(privateKey);
    }

    private static String secret(String privateKey, String peerPublicKey) throws LuaException {
        CryptoUtils.assertKey(privateKey);
        CryptoUtils.assertKey(peerPublicKey);
        try {
            return Crypto.computeSharedSecret(privateKey, peerPublicKey);
        } catch (RuntimeException e) {
            throw new LuaException("invalid public key");
        }
    }

    /** sharedSecret(privateKey, peerPublicKey): the raw 32 byte secret. Prefer sessionKey. */
    @LuaFunction
    public final String sharedSecret(String privateKey, String peerPublicKey) throws LuaException {
        return secret(privateKey, peerPublicKey);
    }

    /**
     * sessionKey(privateKey, peerPublicKey, info [, length]): HKDF-SHA256 of the shared secret.
     * info names the use of the key ("my-protocol v1 client-to-server"). Length is 32 bytes by default.
     */
    @LuaFunction
    public final String sessionKey(IArguments args) throws LuaException {
        var shared = secret(args.getString(0), args.getString(1));
        var info = args.getString(2);
        var length = LuaUtil.optBoundedInt(args, 3, 32, 1, Limits.KDF_MAX_OUTPUT, "length");
        return string(CryptoExtras.hkdf(HashAlgorithm.sha256, bytes(shared), new byte[0], bytes(info), length));
    }
}
