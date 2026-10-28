package cryptographyperipherals.peripheral;

import cryptographyperipherals.Limits;
import cryptographyperipherals.block.CryptoBlockEntity;
import cryptographyperipherals.core.Crypto;
import cryptographyperipherals.core.CryptoExtras;
import cryptographyperipherals.core.CryptoExtras.OutputFormat;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Dedicated source of secure randomness. */
public class EntropyEngine extends EnginePeripheral {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final Pattern DICE = Pattern.compile("^(\\d{1,4})d(\\d{1,9})([+-]\\d{1,9})?$");
    private static final long MAX_SAFE_INTEGER = 1L << 53;

    public EntropyEngine(CryptoBlockEntity owner) {
        super(owner, "entropy_source");
    }

    /** bytes(length [, format]): up to 65536 random bytes. Default format: raw. */
    @LuaFunction
    public final String bytes(IArguments args) throws LuaException {
        var length = LuaUtil.boundedInt(args, 0, 0, Limits.RANDOM_BYTES_MAX_SIZE, "length");
        var data = new byte[length];
        RANDOM.nextBytes(data);
        return CryptoExtras.encode(data, LuaUtil.optFormat(args, 1, OutputFormat.raw));
    }

    /** float(): a number in [0, 1). */
    @LuaFunction("float")
    public final double nextFloat() {
        return Crypto.secureRandom();
    }

    /** int(min, max): uniform integer, both bounds included. */
    @LuaFunction("int")
    public final long nextInt(long min, long max) throws LuaException {
        if (max < min) {
            throw new LuaException("bad argument #2 (interval is empty)");
        }

        if (Math.abs(min) > MAX_SAFE_INTEGER || Math.abs(max) > MAX_SAFE_INTEGER) {
            throw new LuaException("bounds must be between -2^53 and 2^53");
        }

        return CryptoExtras.randomInt(min, max);
    }

    /** coin(): true or false with the same probability. */
    @LuaFunction
    public final boolean coin() {
        return RANDOM.nextBoolean();
    }

    /** chance(probability): true with the given probability (0 to 1). */
    @LuaFunction
    public final boolean chance(double probability) throws LuaException {
        if (probability < 0 || probability > 1 || Double.isNaN(probability)) {
            throw new LuaException("bad argument #1 (probability must be between 0 and 1)");
        }
        return RANDOM.nextDouble() < probability;
    }

    @LuaFunction
    public final String uuid() {
        return CryptoExtras.randomUuid();
    }

    /** token(length [, alphabet]): random string, base62 by default. Useful for session tokens and codes. */
    @LuaFunction
    public final String token(IArguments args) throws LuaException {
        var length = LuaUtil.boundedInt(args, 0, 1, Limits.TOKEN_MAX_LENGTH, "length");
        var alphabet = args.optString(1, BASE62);
        if (alphabet.isEmpty() || alphabet.length() > 256) {
            throw new LuaException("bad argument #2 (the alphabet must have 1 to 256 characters)");
        }

        var out = new StringBuilder(length);
        for (var i = 0; i < length; i++) {
            out.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        }
        return out.toString();
    }

    /** shuffle(list): a new list with the same items in a random order (Fisher-Yates). */
    @LuaFunction
    public final List<Object> shuffle(Map<?, ?> list) throws LuaException {
        var items = new ArrayList<>(LuaUtil.values(list, Limits.BATCH_MAX_ITEMS));
        Collections.shuffle(items, RANDOM);
        return items;
    }

    /** pick(list): a random item of the list (nil if it is empty). */
    @LuaFunction
    public final Object pick(Map<?, ?> list) throws LuaException {
        var items = LuaUtil.values(list, Limits.BATCH_MAX_ITEMS);
        return items.isEmpty() ? null : items.get(RANDOM.nextInt(items.size()));
    }

    /** dice("3d6+2"): rolls the dice. Returns {total = ..., rolls = {...}, modifier = ...}. */
    @LuaFunction
    public final Map<String, Object> dice(String expression) throws LuaException {
        var matcher = DICE.matcher(expression.trim().toLowerCase());
        if (!matcher.matches()) {
            throw new LuaException("bad argument #1 (expected something like 3d6 or 2d10+4)");
        }

        var count = Integer.parseInt(matcher.group(1));
        var sides = Integer.parseInt(matcher.group(2));
        var modifier = matcher.group(3) == null ? 0L : Long.parseLong(matcher.group(3));
        if (count < 1 || count > Limits.DICE_MAX_COUNT || sides < 1) {
            throw new LuaException("bad argument #1 (1 to " + Limits.DICE_MAX_COUNT + " dice, at least 1 side)");
        }

        var rolls = new ArrayList<Integer>();
        long total = modifier;
        for (var i = 0; i < count; i++) {
            var roll = RANDOM.nextInt(sides) + 1;
            rolls.add(roll);
            total += roll;
        }

        var result = new HashMap<String, Object>();
        result.put("total", total);
        result.put("rolls", rolls);
        result.put("modifier", modifier);
        return result;
    }
}
