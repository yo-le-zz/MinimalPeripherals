package cryptographyperipherals.peripheral;

import cryptographyperipherals.block.CryptoBlockEntity;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Historical ciphers, for puzzles and teaching. They offer no security at all. */
public class ClassicCipherEngine extends EnginePeripheral {
    public ClassicCipherEngine(CryptoBlockEntity owner) {
        super(owner, "classic_cipher");
    }

    private static String shift(String text, int amount) {
        var shift = Math.floorMod(amount, 26);
        var out = new StringBuilder(text.length());
        for (var i = 0; i < text.length(); i++) {
            var c = text.charAt(i);
            if (c >= 'a' && c <= 'z') {
                out.append((char) ('a' + (c - 'a' + shift) % 26));
            } else if (c >= 'A' && c <= 'Z') {
                out.append((char) ('A' + (c - 'A' + shift) % 26));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /** caesar(text, shift): shifts every letter. A negative shift decrypts. */
    @LuaFunction
    public final String caesar(String text, int shift) {
        return shift(text, shift);
    }

    @LuaFunction
    public final String rot13(String text) {
        return shift(text, 13);
    }

    /** rot47: rotates the printable ASCII characters (33 to 126). */
    @LuaFunction
    public final String rot47(String text) {
        var out = new StringBuilder(text.length());
        for (var i = 0; i < text.length(); i++) {
            var c = text.charAt(i);
            out.append(c >= 33 && c <= 126 ? (char) (33 + (c - 33 + 47) % 94) : c);
        }
        return out.toString();
    }

    /** atbash: a becomes z, b becomes y... */
    @LuaFunction
    public final String atbash(String text) {
        var out = new StringBuilder(text.length());
        for (var i = 0; i < text.length(); i++) {
            var c = text.charAt(i);
            if (c >= 'a' && c <= 'z') {
                out.append((char) ('z' - (c - 'a')));
            } else if (c >= 'A' && c <= 'Z') {
                out.append((char) ('Z' - (c - 'A')));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /** vigenere(text, key [, decrypt]): the key must only contain letters. */
    @LuaFunction
    public final String vigenere(IArguments args) throws LuaException {
        var text = args.getString(0);
        var key = args.getString(1).toLowerCase();
        var decrypt = args.optBoolean(2, false);
        if (key.isEmpty()) {
            throw new LuaException("bad argument #2 (the key must not be empty)");
        }

        for (var i = 0; i < key.length(); i++) {
            if (key.charAt(i) < 'a' || key.charAt(i) > 'z') {
                throw new LuaException("bad argument #2 (the key must only contain letters)");
            }
        }

        var out = new StringBuilder(text.length());
        var position = 0;
        for (var i = 0; i < text.length(); i++) {
            var c = text.charAt(i);
            var isLetter = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
            if (!isLetter) {
                out.append(c);
                continue;
            }

            var amount = key.charAt(position % key.length()) - 'a';
            out.append(shift(String.valueOf(c), decrypt ? -amount : amount));
            position++;
        }
        return out.toString();
    }

    /** bruteCaesar(text): the 25 other possible shifts, to crack a Caesar cipher by eye. */
    @LuaFunction
    public final List<String> bruteCaesar(String text) {
        var result = new ArrayList<String>();
        for (var i = 1; i < 26; i++) {
            result.add(shift(text, i));
        }
        return result;
    }

    /** frequency(text): how many times each letter appears (case-insensitive), to help break substitutions. */
    @LuaFunction
    public final Map<String, Integer> frequency(String text) {
        var counts = new int[26];
        for (var i = 0; i < text.length(); i++) {
            var c = Character.toLowerCase(text.charAt(i));
            if (c >= 'a' && c <= 'z') {
                counts[c - 'a']++;
            }
        }

        var result = new LinkedHashMap<String, Integer>();
        for (var i = 0; i < 26; i++) {
            if (counts[i] > 0) {
                result.put(String.valueOf((char) ('a' + i)), counts[i]);
            }
        }
        return result;
    }
}
