package cryptographyperipherals.peripheral;

import cryptographyperipherals.block.CryptoBlockEntity;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;

/** Hash engine backed by the JVM implementations, which use CPU intrinsics when available. */
public class JdkHashEngine extends HashEngine {
    private final String macAlgorithm;
    private final ThreadLocal<MessageDigest> digests;
    private final ThreadLocal<Mac> macs;

    public JdkHashEngine(CryptoBlockEntity owner, String type, String digestAlgorithm, String macAlgorithm) {
        super(owner, type);
        this.macAlgorithm = macAlgorithm;
        this.digests = ThreadLocal.withInitial(() -> {
            try {
                return MessageDigest.getInstance(digestAlgorithm);
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException(e);
            }
        });
        this.macs = ThreadLocal.withInitial(() -> {
            try {
                return Mac.getInstance(macAlgorithm);
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    @Override
    protected byte[] digest(byte[] data) {
        return digests.get().digest(data);
    }

    @Override
    protected byte[] mac(byte[] key, byte[] data) {
        try {
            var mac = macs.get();
            // HMAC pads the key with zeros, so an empty key is the same as a single zero byte
            mac.init(new SecretKeySpec(key.length == 0 ? new byte[1] : key, macAlgorithm));
            return mac.doFinal(data);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected int digestSize() {
        return digests.get().getDigestLength();
    }
}
