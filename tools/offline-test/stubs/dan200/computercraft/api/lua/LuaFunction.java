package dan200.computercraft.api.lua;
import java.lang.annotation.*;
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
public @interface LuaFunction { String[] value() default {}; boolean mainThread() default false; }
