import dan200.computercraft.api.lua.*;
import java.util.Optional;
/** A small stand-in for CC's IArguments, strict like the real one. */
public class Args implements IArguments {
    private final Object[] values;
    public Args(Object... values) { this.values = values; }
    public int count() { return values.length; }
    public Object get(int i) { return i < values.length ? values[i] : null; }
    private LuaException bad(int i, String expected) { return new LuaException("bad argument #" + (i + 1) + " (" + expected + " expected)"); }
    public String getString(int i) throws LuaException {
        var v = get(i);
        if (v instanceof String s) return s;
        if (v instanceof Number n) return n.toString();
        throw bad(i, "string");
    }
    public int getInt(int i) throws LuaException { return (int) getLong(i); }
    public long getLong(int i) throws LuaException {
        if (get(i) instanceof Number n && n.doubleValue() == Math.floor(n.doubleValue())) return n.longValue();
        throw bad(i, "number");
    }
    public double getFiniteDouble(int i) throws LuaException {
        if (get(i) instanceof Number n) return n.doubleValue();
        throw bad(i, "number");
    }
    public int optInt(int i, int def) throws LuaException { return get(i) == null ? def : getInt(i); }
    public long optLong(int i, long def) throws LuaException { return get(i) == null ? def : getLong(i); }
    public String optString(int i, String def) throws LuaException { return get(i) == null ? def : getString(i); }
    public boolean optBoolean(int i, boolean def) throws LuaException {
        if (get(i) == null) return def;
        if (get(i) instanceof Boolean b) return b;
        throw bad(i, "boolean");
    }
    public <T extends Enum<T>> T getEnum(int i, Class<T> k) throws LuaException {
        if (get(i) instanceof String s) {
            for (var c : k.getEnumConstants()) if (c.name().equals(s)) return c;
        }
        throw new LuaException("bad argument #" + (i + 1) + " (unknown value)");
    }
    public <T extends Enum<T>> Optional<T> optEnum(int i, Class<T> k) throws LuaException {
        return get(i) == null ? Optional.empty() : Optional.of(getEnum(i, k));
    }
}
