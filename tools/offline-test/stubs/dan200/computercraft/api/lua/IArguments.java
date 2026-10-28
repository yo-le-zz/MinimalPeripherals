package dan200.computercraft.api.lua;
import java.util.Optional;
public interface IArguments {
    int count();
    Object get(int index);
    String getString(int index) throws LuaException;
    int getInt(int index) throws LuaException;
    long getLong(int index) throws LuaException;
    double getFiniteDouble(int index) throws LuaException;
    int optInt(int index, int def) throws LuaException;
    long optLong(int index, long def) throws LuaException;
    String optString(int index, String def) throws LuaException;
    boolean optBoolean(int index, boolean def) throws LuaException;
    <T extends Enum<T>> T getEnum(int index, Class<T> klass) throws LuaException;
    <T extends Enum<T>> Optional<T> optEnum(int index, Class<T> klass) throws LuaException;
}
