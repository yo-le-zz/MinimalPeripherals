package cryptographyperipherals.peripheral;

import cryptographyperipherals.block.CryptoBlockEntity;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;

import java.util.function.Supplier;

/** Hash engine for the algorithms the JVM does not have (RIPEMD-160, BLAKE2). */
public class BcHashEngine extends HashEngine {
    private final Supplier<Digest> factory;

    public BcHashEngine(CryptoBlockEntity owner, String type, Supplier<Digest> factory) {
        super(owner, type);
        this.factory = factory;
    }

    @Override
    protected byte[] digest(byte[] data) {
        var digest = factory.get();
        digest.update(data, 0, data.length);
        var out = new byte[digest.getDigestSize()];
        digest.doFinal(out, 0);
        return out;
    }

    @Override
    protected byte[] mac(byte[] key, byte[] data) {
        var mac = new HMac(factory.get());
        mac.init(new KeyParameter(key));
        mac.update(data, 0, data.length);
        var out = new byte[mac.getMacSize()];
        mac.doFinal(out, 0);
        return out;
    }

    @Override
    protected int digestSize() {
        return factory.get().getDigestSize();
    }
}
