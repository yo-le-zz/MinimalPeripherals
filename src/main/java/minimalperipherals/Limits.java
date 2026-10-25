package minimalperipherals;

/** Fixed limits (the original mod exposed these through a config file). */
public final class Limits {
    /** Maximum size of the data stored on an NFC card / RFID badge. */
    public static final int NFC_MAX_DATA_SIZE = 128;
    /** Range of the RFID scanner, in blocks. */
    public static final int RFID_SCAN_RANGE = 8;
    /** Maximum size of the buffer returned by cryptographic_accelerator.randomBytes. */
    public static final int RANDOM_BYTES_MAX_SIZE = 65536;

    private Limits() {
    }
}
