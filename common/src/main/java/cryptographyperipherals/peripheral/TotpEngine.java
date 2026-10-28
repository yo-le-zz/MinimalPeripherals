package cryptographyperipherals.peripheral;

import cryptographyperipherals.block.CryptoBlockEntity;
import cryptographyperipherals.core.Codecs;
import cryptographyperipherals.core.Crypto;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;

import static cryptographyperipherals.peripheral.LuaUtil.bytes;

/**
 * One time passwords: HOTP (RFC 4226) and TOTP (RFC 6238), compatible with Google Authenticator,
 * Authy, Aegis and the like. Secrets are base32 strings. Time is the real time of the server, in seconds.
 */
public class TotpEngine extends EnginePeripheral {
    public enum Algorithms {
        sha1("HmacSHA1"),
        sha256("HmacSHA256"),
        sha512("HmacSHA512");

        private final String mac;

        Algorithms(String mac) {
            this.mac = mac;
        }
    }

    public TotpEngine(CryptoBlockEntity owner) {
        super(owner, "totp_generator");
    }

    private static byte[] secret(String text) throws LuaException {
        byte[] key;
        try {
            key = Codecs.base32Decode(text);
        } catch (IllegalArgumentException e) {
            throw new LuaException("bad argument #1 (the secret is not valid base32)");
        }

        if (key.length == 0) {
            throw new LuaException("bad argument #1 (the secret is empty)");
        }
        return key;
    }

    private static String code(byte[] key, long counter, int digits, Algorithms algorithm) throws LuaException {
        try {
            var mac = Mac.getInstance(algorithm.mac);
            mac.init(new SecretKeySpec(key, algorithm.mac));

            var message = new byte[8];
            for (var i = 7; i >= 0; i--) {
                message[i] = (byte) (counter & 0xff);
                counter >>>= 8;
            }

            var hash = mac.doFinal(message);
            var offset = hash[hash.length - 1] & 0x0f;
            var binary = ((long) (hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);

            var modulo = 1L;
            for (var i = 0; i < digits; i++) {
                modulo *= 10;
            }
            return String.format("%0" + digits + "d", binary % modulo);
        } catch (GeneralSecurityException e) {
            throw new LuaException(String.valueOf(e.getMessage()));
        }
    }

    private static long now() {
        return System.currentTimeMillis() / 1000;
    }

    /** generateSecret([bytes]): a random base32 secret, 20 bytes (160 bits) by default. */
    @LuaFunction
    public final String generateSecret(IArguments args) throws LuaException {
        var size = LuaUtil.optBoundedInt(args, 0, 20, 10, 64, "bytes");
        return Codecs.base32Encode(Crypto.secureRandomBuffer(size), false);
    }

    /** hotp(secret, counter [, digits [, algorithm]]): counter based code. */
    @LuaFunction
    public final String hotp(IArguments args) throws LuaException {
        var key = secret(args.getString(0));
        var counter = args.getLong(1);
        var digits = LuaUtil.optBoundedInt(args, 2, 6, 6, 10, "digits");
        var algorithm = args.optEnum(3, Algorithms.class).orElse(Algorithms.sha1);
        if (counter < 0) {
            throw new LuaException("bad argument #2 (counter must not be negative)");
        }
        return code(key, counter, digits, algorithm);
    }

    /** totp(secret [, digits [, period [, time [, algorithm]]]]): the current code. period is 30 s by default. */
    @LuaFunction
    public final String totp(IArguments args) throws LuaException {
        var key = secret(args.getString(0));
        var digits = LuaUtil.optBoundedInt(args, 1, 6, 6, 10, "digits");
        var period = LuaUtil.optBoundedInt(args, 2, 30, 1, 86400, "period");
        var time = args.get(3) == null ? now() : args.getLong(3);
        var algorithm = args.optEnum(4, Algorithms.class).orElse(Algorithms.sha1);
        if (time < 0) {
            throw new LuaException("bad argument #4 (time must not be negative)");
        }
        return code(key, time / period, digits, algorithm);
    }

    /**
     * verify(secret, code [, window [, digits [, period [, time [, algorithm]]]]]): true if the code is valid
     * now or within window periods before or after (1 by default, to allow for clock drift).
     */
    @LuaFunction
    public final boolean verify(IArguments args) throws LuaException {
        var key = secret(args.getString(0));
        var given = bytes(args.getString(1));
        var window = LuaUtil.optBoundedInt(args, 2, 1, 0, 10, "window");
        var digits = LuaUtil.optBoundedInt(args, 3, 6, 6, 10, "digits");
        var period = LuaUtil.optBoundedInt(args, 4, 30, 1, 86400, "period");
        var time = args.get(5) == null ? now() : args.getLong(5);
        var algorithm = args.optEnum(6, Algorithms.class).orElse(Algorithms.sha1);

        var step = time / period;
        var valid = false;
        for (var delta = -window; delta <= window; delta++) {
            if (step + delta < 0) {
                continue;
            }
            // no early exit: every candidate is checked, so timing does not leak which one matched
            valid |= MessageDigest.isEqual(bytes(code(key, step + delta, digits, algorithm)), given);
        }
        return valid;
    }

    /** remaining([period [, time]]): seconds until the current code changes. */
    @LuaFunction
    public final long remaining(IArguments args) throws LuaException {
        var period = LuaUtil.optBoundedInt(args, 0, 30, 1, 86400, "period");
        var time = args.get(1) == null ? now() : args.getLong(1);
        return period - (time % period);
    }
}
