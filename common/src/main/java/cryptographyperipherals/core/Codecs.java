package cryptographyperipherals.core;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;

/** Base32 (RFC 4648) and Base58 (Bitcoin alphabet) codecs. */
public final class Codecs {
    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final String BASE58 = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final BigInteger FIFTY_EIGHT = BigInteger.valueOf(58);

    private Codecs() {
    }

    public static String base32Encode(byte[] data, boolean padding) {
        var out = new StringBuilder((data.length + 4) / 5 * 8);
        var buffer = 0;
        var bits = 0;
        for (var value : data) {
            buffer = (buffer << 8) | (value & 0xff);
            bits += 8;
            while (bits >= 5) {
                out.append(BASE32.charAt((buffer >> (bits - 5)) & 31));
                bits -= 5;
            }
            buffer &= (1 << bits) - 1;
        }

        if (bits > 0) {
            out.append(BASE32.charAt((buffer << (5 - bits)) & 31));
        }

        if (padding) {
            while (out.length() % 8 != 0) {
                out.append('=');
            }
        }

        return out.toString();
    }

    /** Decodes base32. Case, spaces, dashes and padding are ignored. */
    public static byte[] base32Decode(String text) {
        var out = new ByteArrayOutputStream();
        var buffer = 0;
        var bits = 0;
        for (var i = 0; i < text.length(); i++) {
            var c = text.charAt(i);
            if (c == '=' || c == ' ' || c == '-') {
                continue;
            }

            var value = BASE32.indexOf(Character.toUpperCase(c));
            if (value < 0) {
                throw new IllegalArgumentException("invalid base32 character");
            }

            buffer = (buffer << 5) | value;
            bits += 5;
            if (bits >= 8) {
                out.write((buffer >> (bits - 8)) & 0xff);
                bits -= 8;
                buffer &= (1 << bits) - 1;
            }
        }

        return out.toByteArray();
    }

    public static String base58Encode(byte[] data) {
        var zeros = 0;
        while (zeros < data.length && data[zeros] == 0) {
            zeros++;
        }

        var number = new BigInteger(1, data);
        var out = new StringBuilder();
        while (number.signum() > 0) {
            var division = number.divideAndRemainder(FIFTY_EIGHT);
            out.append(BASE58.charAt(division[1].intValue()));
            number = division[0];
        }

        for (var i = 0; i < zeros; i++) {
            out.append('1');
        }

        return out.reverse().toString();
    }

    public static byte[] base58Decode(String text) {
        var zeros = 0;
        while (zeros < text.length() && text.charAt(zeros) == '1') {
            zeros++;
        }

        var number = BigInteger.ZERO;
        for (var i = 0; i < text.length(); i++) {
            var value = BASE58.indexOf(text.charAt(i));
            if (value < 0) {
                throw new IllegalArgumentException("invalid base58 character");
            }

            number = number.multiply(FIFTY_EIGHT).add(BigInteger.valueOf(value));
        }

        var raw = number.signum() == 0 ? new byte[0] : number.toByteArray();
        var start = raw.length > 0 && raw[0] == 0 ? 1 : 0;
        var result = new byte[zeros + raw.length - start];
        System.arraycopy(raw, start, result, zeros, raw.length - start);
        return result;
    }
}
