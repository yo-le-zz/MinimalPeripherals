package minimalperipherals.dataholder;

/** Implemented by items that carry data written by an NFC reader (NFC card, RFID badge). */
public interface DataHolderItem {
    boolean supportsPrivateKey();
}
