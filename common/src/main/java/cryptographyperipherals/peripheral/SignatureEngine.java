package cryptographyperipherals.peripheral;

import cryptographyperipherals.block.CryptoBlockEntity;
import cryptographyperipherals.core.Crypto;
import cryptographyperipherals.utils.CryptoUtils;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Dedicated Ed25519 block. Keys are 32 raw bytes, signatures are 64 raw bytes. */
public class SignatureEngine extends EnginePeripheral {
    public SignatureEngine(CryptoBlockEntity owner) {
        super(owner, "signature_engine");
    }

    private static boolean verifySafe(String message, String signature, String publicKey) {
        try {
            return Crypto.verify(message, signature, publicKey);
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** generateKeyPair() returns {privateKey = ..., publicKey = ...}. */
    @LuaFunction
    public final Map<String, String> generateKeyPair() {
        var privateKey = Crypto.generatePrivateKey();
        var pair = new HashMap<String, String>();
        pair.put("privateKey", privateKey);
        pair.put("publicKey", Crypto.derivePublicKey(privateKey));
        return pair;
    }

    @LuaFunction
    public final String publicKey(String privateKey) throws LuaException {
        CryptoUtils.assertKey(privateKey);
        return Crypto.derivePublicKey(privateKey);
    }

    @LuaFunction
    public final String sign(String message, String privateKey) throws LuaException {
        CryptoUtils.assertKey(privateKey);
        return Crypto.sign(message, privateKey);
    }

    @LuaFunction
    public final boolean verify(String message, String signature, String publicKey) throws LuaException {
        CryptoUtils.assertKey(publicKey);
        CryptoUtils.assertSignature(signature);
        return verifySafe(message, signature, publicKey);
    }

    /** signMany(messages, privateKey): signs every message of the list. */
    @LuaFunction
    public final List<String> signMany(IArguments args) throws LuaException {
        if (!(args.get(0) instanceof Map<?, ?> table)) {
            throw new LuaException("bad argument #1 (expected table)");
        }

        var privateKey = CryptoUtils.assertKey(args.getString(1));
        var result = new ArrayList<String>();
        for (var message : LuaUtil.strings(table)) {
            result.add(Crypto.sign(message, privateKey));
        }
        return result;
    }

    /** verifyMany(messages, signatures, publicKey): a list of booleans, one per message. */
    @LuaFunction
    public final List<Boolean> verifyMany(IArguments args) throws LuaException {
        if (!(args.get(0) instanceof Map<?, ?> messageTable) || !(args.get(1) instanceof Map<?, ?> signatureTable)) {
            throw new LuaException("bad argument #1 and #2 (expected tables)");
        }

        var publicKey = CryptoUtils.assertKey(args.getString(2));
        var messages = LuaUtil.strings(messageTable);
        var signatures = LuaUtil.strings(signatureTable);
        if (messages.size() != signatures.size()) {
            throw new LuaException("the lists must have the same size");
        }

        var result = new ArrayList<Boolean>();
        for (var i = 0; i < messages.size(); i++) {
            var signature = signatures.get(i);
            result.add(signature.length() == 64 && verifySafe(messages.get(i), signature, publicKey));
        }
        return result;
    }
}
