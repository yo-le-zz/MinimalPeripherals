package minimalperipherals.nfc;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.AttachedComputerSet;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import minimalperipherals.utils.CryptoUtils;

import java.util.Optional;

public class NfcReaderPeripheral implements IPeripheral {
    private static final int MAX_LABEL_LENGTH = 32;

    private final NfcReaderBlockEntity nfcReader;
    private final AttachedComputerSet computers = new AttachedComputerSet();

    public NfcReaderPeripheral(NfcReaderBlockEntity nfcReader) {
        this.nfcReader = nfcReader;
    }

    @Override
    public String getType() {
        return "nfc_reader";
    }

    @Override
    public boolean equals(IPeripheral other) {
        return other instanceof NfcReaderPeripheral o && nfcReader == o.nfcReader;
    }

    @Override
    public void attach(IComputerAccess computer) {
        computers.add(computer);
    }

    @Override
    public void detach(IComputerAccess computer) {
        computers.remove(computer);
    }

    // ---- events ----

    public void read(String data, String publicKey) {
        computers.forEach(computer -> computer.queueEvent("nfc_data", computer.getAttachmentName(), data, publicKey));
    }

    public void writeFeedback(boolean success, String message) {
        computers.forEach(computer -> computer.queueEvent("nfc_write", computer.getAttachmentName(), success, message));
    }

    public void signFeedback(String signature, String publicKey) {
        computers.forEach(computer -> computer.queueEvent("nfc_sign", computer.getAttachmentName(), signature, publicKey));
    }

    // ---- Lua API ----

    @LuaFunction(mainThread = true)
    public final void write(String data, Optional<String> label, Optional<Boolean> flagReadOnly, Optional<String> privateKey) throws LuaException {
        if (privateKey.isPresent()) {
            CryptoUtils.assertKey(privateKey.get());
        }

        nfcReader.flagWrite(
                data,
                label.map(NfcReaderPeripheral::normaliseLabel).orElse(null),
                flagReadOnly.orElse(false),
                privateKey.orElse(null)
        );
    }

    @LuaFunction(mainThread = true)
    public final boolean cancelWrite() {
        var inWriteMode = nfcReader.isWriteMode();
        nfcReader.cancelWrite();
        return inWriteMode;
    }

    @LuaFunction(mainThread = true)
    public final void sign(String data) {
        nfcReader.flagSign(data);
    }

    @LuaFunction(mainThread = true)
    public final boolean cancelSign() {
        var inSignMode = nfcReader.isSigningMode();
        nfcReader.cancelSign();
        return inSignMode;
    }

    private static String normaliseLabel(String label) {
        var builder = new StringBuilder();
        for (var i = 0; i < label.length() && builder.length() < MAX_LABEL_LENGTH; i++) {
            var c = label.charAt(i);
            builder.append(c >= 32 && c <= 126 ? c : '?');
        }
        return builder.toString();
    }
}
