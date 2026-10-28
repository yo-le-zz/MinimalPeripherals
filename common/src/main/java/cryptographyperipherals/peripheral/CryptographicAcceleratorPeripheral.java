package cryptographyperipherals.peripheral;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import cryptographyperipherals.Limits;
import cryptographyperipherals.block.CryptoBlockEntity;
import cryptographyperipherals.core.Compression;
import cryptographyperipherals.core.Crypto;
import cryptographyperipherals.core.CryptoExtras;
import cryptographyperipherals.core.CryptoExtras.HashAlgorithm;
import cryptographyperipherals.core.CryptoExtras.OutputFormat;
import cryptographyperipherals.utils.CryptoUtils;

import javax.crypto.BadPaddingException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.DataFormatException;

/**
 * Lua API of the general cryptographic accelerator: every algorithm in one block.
 * The dedicated engine blocks (sha256_engine, aes_engine...) are faster for what they do.
 * <p>
 * Binary results (hashes, keys, ciphertexts) are returned as Lua strings where one character is one byte.
 * Functions that take a "format" argument accept a boolean (true = hex, false = raw bytes, the historical behaviour)
 * or one of "hex", "base64", "base64url" and "raw".
 */
public class CryptographicAcceleratorPeripheral extends EnginePeripheral {
    private static final long MAX_SAFE_INTEGER = 1L << 53;

    public CryptographicAcceleratorPeripheral(CryptoBlockEntity owner) {
        super(owner, "cryptographic_accelerator");
    }

    public enum Blake3Modes {
        hash,
        keyed,
        derive,
    }

    public enum AesModes {
        cbc,
        ctr,
    }

    public enum KeyTypes {
        ed25519,
        x25519,
    }

    // ---- argument helpers ----

    private static byte[] bytes(String value) {
        return CryptoExtras.bytes(value);
    }

    /** Reads an output format: a boolean (true = hex, false = raw) or a format name. */
    private static OutputFormat optFormat(IArguments args, int index, OutputFormat fallback) throws LuaException {
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

    private static HashAlgorithm optAlgorithm(IArguments args, int index, HashAlgorithm fallback) throws LuaException {
        return args.optEnum(index, HashAlgorithm.class).orElse(fallback);
    }

    private static int boundedInt(IArguments args, int index, int min, int max, String name) throws LuaException {
        var value = args.getInt(index);
        if (value < min || value > max) {
            throw new LuaException("bad argument #" + (index + 1) + " (" + name + " must be between " + min + " and " + max + ")");
        }
        return value;
    }

    // ---- hashes ----

    @LuaFunction
    public final String md5(IArguments args) throws LuaException {
        return CryptoExtras.encode(CryptoExtras.hash(HashAlgorithm.md5, bytes(args.getString(0))), optFormat(args, 1, OutputFormat.hex));
    }

    @LuaFunction
    public final String sha1(IArguments args) throws LuaException {
        return CryptoExtras.encode(CryptoExtras.hash(HashAlgorithm.sha1, bytes(args.getString(0))), optFormat(args, 1, OutputFormat.hex));
    }

    @LuaFunction
    public final String sha256(IArguments args) throws LuaException {
        return CryptoExtras.encode(CryptoExtras.hash(HashAlgorithm.sha256, bytes(args.getString(0))), optFormat(args, 1, OutputFormat.hex));
    }

    @LuaFunction
    public final String sha512(IArguments args) throws LuaException {
        return CryptoExtras.encode(CryptoExtras.hash(HashAlgorithm.sha512, bytes(args.getString(0))), optFormat(args, 1, OutputFormat.hex));
    }

    /** hash(algorithm, data [, format]) */
    @LuaFunction
    public final String hash(IArguments args) throws LuaException {
        var algorithm = args.getEnum(0, HashAlgorithm.class);
        var data = bytes(args.getString(1));
        return CryptoExtras.encode(CryptoExtras.hash(algorithm, data), optFormat(args, 2, OutputFormat.hex));
    }

    /** hmac(algorithm, key, data [, format]) */
    @LuaFunction
    public final String hmac(IArguments args) throws LuaException {
        var algorithm = args.getEnum(0, HashAlgorithm.class);
        var key = bytes(args.getString(1));
        var data = bytes(args.getString(2));
        return CryptoExtras.encode(CryptoExtras.hmac(algorithm, key, data), optFormat(args, 3, OutputFormat.hex));
    }

    @LuaFunction
    public final String hmacMd5(IArguments args) throws LuaException {
        return CryptoExtras.encode(CryptoExtras.hmac(HashAlgorithm.md5, bytes(args.getString(0)), bytes(args.getString(1))), optFormat(args, 2, OutputFormat.hex));
    }

    @LuaFunction
    public final String hmacSha1(IArguments args) throws LuaException {
        return CryptoExtras.encode(CryptoExtras.hmac(HashAlgorithm.sha1, bytes(args.getString(0)), bytes(args.getString(1))), optFormat(args, 2, OutputFormat.hex));
    }

    @LuaFunction
    public final String hmacSha256(IArguments args) throws LuaException {
        return CryptoExtras.encode(CryptoExtras.hmac(HashAlgorithm.sha256, bytes(args.getString(0)), bytes(args.getString(1))), optFormat(args, 2, OutputFormat.hex));
    }

    @LuaFunction
    public final String hmacSha512(IArguments args) throws LuaException {
        return CryptoExtras.encode(CryptoExtras.hmac(HashAlgorithm.sha512, bytes(args.getString(0)), bytes(args.getString(1))), optFormat(args, 2, OutputFormat.hex));
    }

    /** Lists the algorithms accepted by hash, hmac, pbkdf2 and hkdf. */
    @LuaFunction
    public final List<String> listHashes() {
        return Arrays.stream(HashAlgorithm.values()).map(Enum::name).toList();
    }

    /** blake3(data [, format [, mode [, keyOrContext [, outputSize]]]]) */
    @LuaFunction
    public final String blake3(IArguments args) throws LuaException {
        var data = args.getString(0);
        var format = optFormat(args, 1, OutputFormat.hex);
        var mode = args.optEnum(2, Blake3Modes.class).orElse(Blake3Modes.hash);
        var outputSize = args.optInt(4, 32);

        if (outputSize <= 0) {
            throw new LuaException("output size must be greater than 0");
        }

        // for resource exhaustion reasons, the output size is limited
        if (outputSize > Limits.BLAKE3_MAX_OUTPUT) {
            throw new LuaException("output size must be less than or equal to " + Limits.BLAKE3_MAX_OUTPUT);
        }

        var result = switch (mode) {
            case hash -> Crypto.blake3Hash(data, false, outputSize);
            case keyed -> Crypto.blake3Keyed(data, CryptoUtils.assertKey(args.getString(3)), false, outputSize);
            case derive -> Crypto.blake3Derive(data, args.getString(3), false, outputSize);
        };

        return CryptoExtras.encode(bytes(result), format);
    }

    /** crc32(data [, format]): a number by default, or a string when a format is given. */
    @LuaFunction
    public final Object crc32(IArguments args) throws LuaException {
        return checksum(CryptoExtras.crc32(bytes(args.getString(0))), args);
    }

    @LuaFunction
    public final Object adler32(IArguments args) throws LuaException {
        return checksum(CryptoExtras.adler32(bytes(args.getString(0))), args);
    }

    private static Object checksum(long value, IArguments args) throws LuaException {
        if (args.get(1) == null) {
            return value;
        }

        var format = optFormat(args, 1, OutputFormat.hex);
        var big = new byte[]{(byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value};
        return CryptoExtras.encode(big, format);
    }

    /** Compares two strings in constant time (use it to check MACs and signatures). */
    @LuaFunction
    public final boolean constantTimeEquals(String a, String b) {
        return CryptoExtras.constantTimeEquals(bytes(a), bytes(b));
    }

    // ---- key derivation ----

    /** pbkdf2(password, salt, iterations, length [, algorithm [, format]]); defaults: sha256, raw. */
    @LuaFunction
    public final String pbkdf2(IArguments args) throws LuaException {
        var password = bytes(args.getString(0));
        var salt = bytes(args.getString(1));
        var iterations = boundedInt(args, 2, 1, Limits.PBKDF2_MAX_ITERATIONS, "iterations");
        var length = boundedInt(args, 3, 1, Limits.KDF_MAX_OUTPUT, "length");
        var algorithm = optAlgorithm(args, 4, HashAlgorithm.sha256);
        var format = optFormat(args, 5, OutputFormat.raw);

        return CryptoExtras.encode(CryptoExtras.pbkdf2(algorithm, password, salt, iterations, length), format);
    }

    /** hkdf(inputKeyMaterial, salt, info, length [, algorithm [, format]]); defaults: sha256, raw. */
    @LuaFunction
    public final String hkdf(IArguments args) throws LuaException {
        var ikm = bytes(args.getString(0));
        var salt = bytes(args.getString(1));
        var info = bytes(args.getString(2));
        var length = boundedInt(args, 3, 1, Limits.KDF_MAX_OUTPUT, "length");
        var algorithm = optAlgorithm(args, 4, HashAlgorithm.sha256);
        var format = optFormat(args, 5, OutputFormat.raw);

        if (length > 255 * CryptoExtras.hashSize(algorithm)) {
            throw new LuaException("bad argument #4 (length is too large for this algorithm)");
        }

        return CryptoExtras.encode(CryptoExtras.hkdf(algorithm, ikm, salt, info, length), format);
    }

    /** scrypt(password, salt, n, r, p, length [, format]); n must be a power of two. Default format: raw. */
    @LuaFunction
    public final String scrypt(IArguments args) throws LuaException {
        var password = bytes(args.getString(0));
        var salt = bytes(args.getString(1));
        var n = boundedInt(args, 2, 2, Limits.SCRYPT_MAX_N, "n");
        var r = boundedInt(args, 3, 1, Limits.SCRYPT_MAX_R, "r");
        var p = boundedInt(args, 4, 1, Limits.SCRYPT_MAX_P, "p");
        var length = boundedInt(args, 5, 1, Limits.KDF_MAX_OUTPUT, "length");
        var format = optFormat(args, 6, OutputFormat.raw);

        if (Integer.bitCount(n) != 1) {
            throw new LuaException("bad argument #3 (n must be a power of two)");
        }

        try {
            return CryptoExtras.encode(CryptoExtras.scrypt(password, salt, n, r, p, length), format);
        } catch (RuntimeException e) {
            throw new LuaException(e.getMessage());
        }
    }

    // ---- random ----

    @LuaFunction
    public final double random(IArguments args) throws LuaException {
        if (args.count() == 0) { // 0.0 - 1.0
            return Crypto.secureRandom();
        } else if (args.count() == 1) { // 0.0 - max
            var max = args.getFiniteDouble(0);

            if (max < 0) {
                throw new LuaException("bad argument #1 (interval is empty)");
            }

            return Crypto.secureRandom(max);
        } else { // min - max
            var min = args.getFiniteDouble(0);
            var max = args.getFiniteDouble(1);

            if (max - min <= 0) {
                throw new LuaException("bad argument #1 (interval is empty)");
            }

            return Crypto.secureRandom(min, max);
        }
    }

    /** randomInt(min, max): uniform integer, both bounds included. */
    @LuaFunction
    public final long randomInt(long min, long max) throws LuaException {
        if (max < min) {
            throw new LuaException("bad argument #2 (interval is empty)");
        }

        if (Math.abs(min) > MAX_SAFE_INTEGER || Math.abs(max) > MAX_SAFE_INTEGER) {
            throw new LuaException("bounds must be between -2^53 and 2^53");
        }

        return CryptoExtras.randomInt(min, max);
    }

    /** randomBytes(length [, format]); default format: raw. */
    @LuaFunction
    public final String randomBytes(IArguments args) throws LuaException {
        var length = args.getInt(0);
        if (length < 0) {
            throw new LuaException("bad argument #1 (length must not be negative)");
        }

        if (length > Limits.RANDOM_BYTES_MAX_SIZE) {
            throw new LuaException("buffer size limit exceeded");
        }

        return CryptoExtras.encode(Crypto.secureRandomBuffer(length), optFormat(args, 1, OutputFormat.raw));
    }

    @LuaFunction
    public final String randomUuid() {
        return CryptoExtras.randomUuid();
    }

    // ---- encoding ----

    /** encodeBase64(data [, urlSafe]) */
    @LuaFunction
    public final String encodeBase64(String data, Optional<Boolean> urlSafe) {
        return CryptoExtras.encodeBase64(bytes(data), urlSafe.orElse(false));
    }

    /** decodeBase64(data [, urlSafe]): returns nil if the input is not valid. */
    @LuaFunction
    public final String decodeBase64(String data, Optional<Boolean> urlSafe) {
        try {
            return CryptoExtras.string(CryptoExtras.decodeBase64(data, urlSafe.orElse(false)));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @LuaFunction
    public final String encodeHex(String data) {
        return CryptoExtras.encode(bytes(data), OutputFormat.hex);
    }

    /** decodeHex(data): returns nil if the input is not valid hexadecimal. */
    @LuaFunction
    public final String decodeHex(String data) {
        try {
            return CryptoExtras.string(CryptoExtras.decodeHex(data));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ---- symmetric encryption ----

    /** encryptAes(data, key, iv [, mode]) with mode "cbc" (default, PKCS7 padding) or "ctr". */
    @LuaFunction
    public final String encryptAes(IArguments args) throws LuaException {
        return aes(args, true);
    }

    @LuaFunction
    public final String decryptAes(IArguments args) throws LuaException {
        return aes(args, false);
    }

    private static String aes(IArguments args, boolean encrypt) throws LuaException {
        var data = args.getString(0);
        var key = args.getString(1);
        var iv = args.getString(2);
        var mode = args.optEnum(3, AesModes.class).orElse(AesModes.cbc);

        CryptoUtils.assertLength(1, key.length(), 16, 24, 32);
        CryptoUtils.assertLength(2, iv.length(), 16);

        try {
            return switch (mode) {
                case cbc -> encrypt ? Crypto.encryptAes(data, key, iv) : Crypto.decryptAes(data, key, iv);
                case ctr -> CryptoExtras.string(CryptoExtras.aesCtr(bytes(data), bytes(key), bytes(iv)));
            };
        } catch (BadPaddingException e) {
            throw new LuaException(e.getMessage());
        } catch (GeneralSecurityException e) {
            throw new LuaException(e.getMessage());
        }
    }

    /** encryptAesGcm(data, key, nonce [, aad]): the 16 byte authentication tag is appended to the result. */
    @LuaFunction
    public final String encryptAesGcm(IArguments args) throws LuaException {
        return aead(args, true, true);
    }

    /** decryptAesGcm(data, key, nonce [, aad]): fails if the data was tampered with. */
    @LuaFunction
    public final String decryptAesGcm(IArguments args) throws LuaException {
        return aead(args, false, true);
    }

    /** encryptChaCha20Poly1305(data, key, nonce [, aad]): key is 32 bytes, nonce is 12 bytes. */
    @LuaFunction
    public final String encryptChaCha20Poly1305(IArguments args) throws LuaException {
        return aead(args, true, false);
    }

    @LuaFunction
    public final String decryptChaCha20Poly1305(IArguments args) throws LuaException {
        return aead(args, false, false);
    }

    private static String aead(IArguments args, boolean encrypt, boolean gcm) throws LuaException {
        var data = bytes(args.getString(0));
        var key = args.getString(1);
        var nonce = args.getString(2);
        var aad = bytes(args.optString(3, ""));

        if (gcm) {
            CryptoUtils.assertLength(1, key.length(), 16, 24, 32);
        } else {
            CryptoUtils.assertLength(1, key.length(), 32);
        }
        CryptoUtils.assertLength(2, nonce.length(), 12);

        if (!encrypt && data.length < 16) {
            throw new LuaException("encrypted data is too short");
        }

        try {
            var result = gcm
                    ? CryptoExtras.aesGcm(encrypt, data, bytes(key), bytes(nonce), aad)
                    : CryptoExtras.chaCha20Poly1305(encrypt, data, bytes(key), bytes(nonce), aad);
            return CryptoExtras.string(result);
        } catch (GeneralSecurityException e) {
            throw new LuaException(encrypt ? e.getMessage() : "authentication failed");
        }
    }

    // ---- asymmetric: Ed25519 signatures and X25519 key exchange ----

    @LuaFunction
    public final String generatePrivateKey() {
        return Crypto.generatePrivateKey();
    }

    /** generateKeyPair([type]) returns {privateKey = ..., publicKey = ...}. type is "ed25519" (default) or "x25519". */
    @LuaFunction
    public final Map<String, String> generateKeyPair(IArguments args) throws LuaException {
        var type = args.optEnum(0, KeyTypes.class).orElse(KeyTypes.ed25519);
        var privateKey = Crypto.generatePrivateKey();
        var publicKey = type == KeyTypes.ed25519
                ? Crypto.derivePublicKey(privateKey)
                : Crypto.deriveECDHPublicKey(privateKey);

        var pair = new HashMap<String, String>();
        pair.put("privateKey", privateKey);
        pair.put("publicKey", publicKey);
        return pair;
    }

    @LuaFunction
    public final String derivePublicKey(String privateKey) throws LuaException {
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

        try {
            return Crypto.verify(message, signature, publicKey);
        } catch (RuntimeException e) {
            return false;
        }
    }

    @LuaFunction
    public final String deriveEcdhPublicKey(String privateKey) throws LuaException {
        CryptoUtils.assertKey(privateKey);
        return Crypto.deriveECDHPublicKey(privateKey);
    }

    @LuaFunction
    public final String computeSharedSecret(String privateKey, String peerPublicKey) throws LuaException {
        CryptoUtils.assertKey(privateKey);
        CryptoUtils.assertKey(peerPublicKey);

        try {
            return Crypto.computeSharedSecret(privateKey, peerPublicKey);
        } catch (RuntimeException e) {
            throw new LuaException("invalid public key");
        }
    }

    // ---- compression ----

    /** deflate(data [, level]) with level 0 (store) to 9 (best). */
    @LuaFunction
    public final String deflate(IArguments args) throws LuaException {
        var data = args.getString(0);
        var level = args.optInt(1, -1);
        if (level < -1 || level > 9) {
            throw new LuaException("bad argument #2 (level must be between 0 and 9)");
        }

        return Compression.deflate(data, level);
    }

    /** inflate(data [, maxSize]): the result is limited to maxSize bytes (and never more than the global limit). */
    @LuaFunction
    public final String inflate(IArguments args) throws LuaException {
        var data = args.getString(0);
        var maxSize = args.optInt(1, Limits.INFLATE_MAX_SIZE);
        if (maxSize < 0 || maxSize > Limits.INFLATE_MAX_SIZE) {
            throw new LuaException("bad argument #2 (maxSize must be between 0 and " + Limits.INFLATE_MAX_SIZE + ")");
        }

        try {
            return Compression.inflate(data, maxSize);
        } catch (DataFormatException e) {
            var message = e.getMessage();
            throw new LuaException(message != null && message.contains("size limit") ? message : "invalid compressed data format");
        }
    }

    // ---- information ----

    /** The limits enforced by this peripheral. */
    @LuaFunction
    public final Map<String, Object> getLimits() {
        var limits = new HashMap<String, Object>();
        limits.put("randomBytesMaxSize", Limits.RANDOM_BYTES_MAX_SIZE);
        limits.put("blake3MaxOutput", Limits.BLAKE3_MAX_OUTPUT);
        limits.put("kdfMaxOutput", Limits.KDF_MAX_OUTPUT);
        limits.put("pbkdf2MaxIterations", Limits.PBKDF2_MAX_ITERATIONS);
        limits.put("scryptMaxN", Limits.SCRYPT_MAX_N);
        limits.put("scryptMaxR", Limits.SCRYPT_MAX_R);
        limits.put("scryptMaxP", Limits.SCRYPT_MAX_P);
        limits.put("inflateMaxSize", Limits.INFLATE_MAX_SIZE);
        return limits;
    }
}
