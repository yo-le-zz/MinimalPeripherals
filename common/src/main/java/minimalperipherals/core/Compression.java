package minimalperipherals.core;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class Compression {
    private static final Charset CHARSET = StandardCharsets.ISO_8859_1;

    public static String inflate(String data) throws DataFormatException {
        return inflate(data, Integer.MAX_VALUE);
    }

    /** Inflates the data, failing if the result would be larger than maxSize bytes. */
    public static String inflate(String data, int maxSize) throws DataFormatException {
        var inflater = new Inflater();
        inflater.setInput(data.getBytes(CHARSET));

        try {
            var baos = new ByteArrayOutputStream();
            var buffer = new byte[1024];
            while (!inflater.finished()) {
                if (inflater.needsInput() || inflater.needsDictionary()) {
                    throw new DataFormatException("Incomplete or invalid deflate data");
                }
                var count = inflater.inflate(buffer);
                baos.write(buffer, 0, count);
                if (baos.size() > maxSize) {
                    throw new DataFormatException("decompressed data exceeds the size limit");
                }
            }

            return baos.toString(CHARSET);
        } finally {
            inflater.end();
        }
    }

    public static String deflate(String data) {
        return deflate(data, Deflater.DEFAULT_COMPRESSION);
    }

    /** Deflates the data. Level is 0 (no compression) to 9 (best), or -1 for the default level. */
    public static String deflate(String data, int level) {
        var bytes = data.getBytes(CHARSET);
        var deflater = new Deflater(level);
        deflater.setInput(bytes);
        deflater.finish();

        var baos = new ByteArrayOutputStream();
        var buffer = new byte[1024];
        while (!deflater.finished()) {
            var count = deflater.deflate(buffer);
            baos.write(buffer, 0, count);
        }

        deflater.end();

        return baos.toString(CHARSET);
    }
}
