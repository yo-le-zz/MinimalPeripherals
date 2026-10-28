package cryptographyperipherals.core;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.Blake2bDigest;
import org.bouncycastle.crypto.digests.Blake2sDigest;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.crypto.digests.RIPEMD160Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.digests.SHA224Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.digests.SHA384Digest;
import org.bouncycastle.crypto.digests.SHA3Digest;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.generators.SCrypt;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.bouncycastle.crypto.params.KeyParameter;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.zip.Adler32;
import java.util.zip.CRC32;

/**
 * Additional primitives of the cryptographic accelerator. Everything works on raw bytes;
 * Lua strings are converted with ISO-8859-1 (one char = one byte) by the peripheral.
 */
public final class CryptoExtras {
    private static final Charset CHARSET = StandardCharsets.ISO_8859_1;
    private static final SecureRandom RANDOM = new SecureRandom();

    private CryptoExtras() {
    }

    // ---- formats ----

    /** How binary results are returned to Lua. */
    public enum OutputFormat {
        hex,
        base64,
        base64url,
        raw,
    }

    public static String encode(byte[] bytes, OutputFormat format) {
        return switch (format) {
            case hex -> HexFormat.of().formatHex(bytes);
            case base64 -> Base64.getEncoder().encodeToString(bytes);
            case base64url -> Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            case raw -> new String(bytes, CHARSET);
        };
    }

    public static byte[] bytes(String data) {
        return data.getBytes(CHARSET);
    }

    public static String string(byte[] bytes) {
        return new String(bytes, CHARSET);
    }

    // ---- hashes ----

    public enum HashAlgorithm {
        md5,
        sha1,
        sha224,
        sha256,
        sha384,
        sha512,
        sha3_256,
        sha3_384,
        sha3_512,
        ripemd160,
        blake2b_256,
        blake2b_512,
        blake2s_256,
    }

    public static Digest createDigest(HashAlgorithm algorithm) {
        return switch (algorithm) {
            case md5 -> new MD5Digest();
            case sha1 -> new SHA1Digest();
            case sha224 -> new SHA224Digest();
            case sha256 -> new SHA256Digest();
            case sha384 -> new SHA384Digest();
            case sha512 -> new SHA512Digest();
            case sha3_256 -> new SHA3Digest(256);
            case sha3_384 -> new SHA3Digest(384);
            case sha3_512 -> new SHA3Digest(512);
            case ripemd160 -> new RIPEMD160Digest();
            case blake2b_256 -> new Blake2bDigest(256);
            case blake2b_512 -> new Blake2bDigest(512);
            case blake2s_256 -> new Blake2sDigest(256);
        };
    }

    public static byte[] hash(HashAlgorithm algorithm, byte[] data) {
        var digest = createDigest(algorithm);
        digest.update(data, 0, data.length);
        var out = new byte[digest.getDigestSize()];
        digest.doFinal(out, 0);
        return out;
    }

    public static byte[] hmac(HashAlgorithm algorithm, byte[] key, byte[] data) {
        var mac = new HMac(createDigest(algorithm));
        mac.init(new KeyParameter(key));
        mac.update(data, 0, data.length);
        var out = new byte[mac.getMacSize()];
        mac.doFinal(out, 0);
        return out;
    }

    public static long crc32(byte[] data) {
        var crc = new CRC32();
        crc.update(data, 0, data.length);
        return crc.getValue();
    }

    public static long adler32(byte[] data) {
        var adler = new Adler32();
        adler.update(data, 0, data.length);
        return adler.getValue();
    }

    /** Compares two strings in constant time. */
    public static boolean constantTimeEquals(byte[] a, byte[] b) {
        return MessageDigest.isEqual(a, b);
    }

    // ---- key derivation ----

    public static byte[] pbkdf2(HashAlgorithm algorithm, byte[] password, byte[] salt, int iterations, int length) {
        var generator = new PKCS5S2ParametersGenerator(createDigest(algorithm));
        generator.init(password, salt, iterations);
        return ((KeyParameter) generator.generateDerivedParameters(length * 8)).getKey();
    }

    public static int hashSize(HashAlgorithm algorithm) {
        return createDigest(algorithm).getDigestSize();
    }

    public static byte[] hkdf(HashAlgorithm algorithm, byte[] ikm, byte[] salt, byte[] info, int length) {
        var generator = new HKDFBytesGenerator(createDigest(algorithm));
        generator.init(new HKDFParameters(ikm, salt.length == 0 ? null : salt, info));
        var out = new byte[length];
        generator.generateBytes(out, 0, length);
        return out;
    }

    public static byte[] scrypt(byte[] password, byte[] salt, int n, int r, int p, int length) {
        return SCrypt.generate(password, salt, n, r, p, length);
    }

    // ---- symmetric ciphers (JDK providers) ----

    /** AES in CTR mode. Encryption and decryption are the same operation. */
    public static byte[] aesCtr(byte[] data, byte[] key, byte[] iv) throws GeneralSecurityException {
        var cipher = Cipher.getInstance("AES/CTR/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        return cipher.doFinal(data);
    }

    /** AES-GCM with a 128 bit tag. The tag is appended to the ciphertext. */
    public static byte[] aesGcm(boolean encrypt, byte[] data, byte[] key, byte[] nonce, byte[] aad) throws GeneralSecurityException {
        var cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(
                encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE,
                new SecretKeySpec(key, "AES"),
                new GCMParameterSpec(128, nonce));
        if (aad.length > 0) {
            cipher.updateAAD(aad);
        }
        return cipher.doFinal(data);
    }

    /** ChaCha20-Poly1305 (RFC 8439). The tag is appended to the ciphertext. */
    public static byte[] chaCha20Poly1305(boolean encrypt, byte[] data, byte[] key, byte[] nonce, byte[] aad) throws GeneralSecurityException {
        var cipher = Cipher.getInstance("ChaCha20-Poly1305");
        cipher.init(
                encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE,
                new SecretKeySpec(key, "ChaCha20"),
                new IvParameterSpec(nonce));
        if (aad.length > 0) {
            cipher.updateAAD(aad);
        }
        return cipher.doFinal(data);
    }

    // ---- encoding ----

    public static String encodeBase64(byte[] data, boolean urlSafe) {
        return urlSafe
                ? Base64.getUrlEncoder().withoutPadding().encodeToString(data)
                : Base64.getEncoder().encodeToString(data);
    }

    public static byte[] decodeBase64(String data, boolean urlSafe) {
        return urlSafe ? Base64.getUrlDecoder().decode(data) : Base64.getDecoder().decode(data);
    }

    public static byte[] decodeHex(String data) {
        return HexFormat.of().parseHex(data);
    }

    // ---- random ----

    /** Uniform random integer in [min, max]. */
    public static long randomInt(long min, long max) {
        return RANDOM.nextLong(min, max + 1);
    }

    public static String randomUuid() {
        return java.util.UUID.randomUUID().toString();
    }
}
