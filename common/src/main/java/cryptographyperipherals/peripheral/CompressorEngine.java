package cryptographyperipherals.peripheral;

import cryptographyperipherals.Limits;
import cryptographyperipherals.block.CryptoBlockEntity;
import cryptographyperipherals.core.Compression;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static cryptographyperipherals.peripheral.LuaUtil.bytes;
import static cryptographyperipherals.peripheral.LuaUtil.string;

/** Dedicated compression block: zlib (deflate) and gzip. Decompression is limited to 8 MiB. */
public class CompressorEngine extends EnginePeripheral {
    public CompressorEngine(CryptoBlockEntity owner) {
        super(owner, "compressor");
    }

    private static int maxSize(IArguments args, int index) throws LuaException {
        return LuaUtil.optBoundedInt(args, index, Limits.INFLATE_MAX_SIZE, 0, Limits.INFLATE_MAX_SIZE, "maxSize");
    }

    /** deflate(data [, level]): zlib data, level 0 (store) to 9 (best). */
    @LuaFunction
    public final String deflate(IArguments args) throws LuaException {
        var level = args.optInt(1, -1);
        if (level < -1 || level > 9) {
            throw new LuaException("bad argument #2 (level must be between 0 and 9)");
        }
        return Compression.deflate(args.getString(0), level);
    }

    /** inflate(data [, maxSize]) */
    @LuaFunction
    public final String inflate(IArguments args) throws LuaException {
        try {
            return Compression.inflate(args.getString(0), maxSize(args, 1));
        } catch (DataFormatException e) {
            var message = e.getMessage();
            throw new LuaException(message != null && message.contains("size limit") ? message : "invalid compressed data format");
        }
    }

    /** gzip(data): a .gz file content. */
    @LuaFunction
    public final String gzip(String data) throws LuaException {
        var out = new ByteArrayOutputStream();
        try (var gzip = new GZIPOutputStream(out)) {
            gzip.write(bytes(data));
        } catch (IOException e) {
            throw new LuaException(String.valueOf(e.getMessage()));
        }
        return string(out.toByteArray());
    }

    /** gunzip(data [, maxSize]) */
    @LuaFunction
    public final String gunzip(IArguments args) throws LuaException {
        var limit = maxSize(args, 1);
        var out = new ByteArrayOutputStream();
        var buffer = new byte[4096];
        try (var in = new GZIPInputStream(new ByteArrayInputStream(bytes(args.getString(0))))) {
            int count;
            while ((count = in.read(buffer)) != -1) {
                out.write(buffer, 0, count);
                if (out.size() > limit) {
                    throw new LuaException("decompressed data exceeds the size limit");
                }
            }
        } catch (IOException e) {
            throw new LuaException("invalid compressed data format");
        }
        return string(out.toByteArray());
    }

    /** ratio(data): compressed size divided by original size, with the best deflate level. */
    @LuaFunction
    public final double ratio(String data) {
        if (data.isEmpty()) {
            return 1;
        }
        return (double) Compression.deflate(data, 9).length() / data.length();
    }
}
