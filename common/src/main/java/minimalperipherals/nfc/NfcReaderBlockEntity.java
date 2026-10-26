package minimalperipherals.nfc;

import minimalperipherals.Limits;
import minimalperipherals.ModRegistry;
import minimalperipherals.core.Crypto;
import minimalperipherals.dataholder.DataHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class NfcReaderBlockEntity extends BlockEntity {
    private enum Mode {
        IDLE,
        WRITING,
        SIGNING,
    }

    private record PendingWrite(String data, String label, boolean readOnly, String privateKey) {
    }

    private final NfcReaderPeripheral peripheral = new NfcReaderPeripheral(this);

    private Mode mode = Mode.IDLE;
    private PendingWrite pendingWrite = null;
    private String pendingSign = null;

    public NfcReaderBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.NFC_READER_BE, pos, state);
    }

    public NfcReaderPeripheral peripheral() {
        return peripheral;
    }

    private void setVisualState(NfcReaderBlock.NfcReaderState visual) {
        var level = getLevel();
        if (level != null) {
            level.setBlockAndUpdate(getBlockPos(), getBlockState().setValue(NfcReaderBlock.STATE, visual));
        }
    }

    // ---- writing ----

    public void flagWrite(String data, String label, boolean readOnly, String privateKey) {
        if (isSigningMode()) {
            cancelSign();
        }

        if (privateKey != null && privateKey.length() != 32) {
            throw new IllegalArgumentException("Invalid private key length");
        }

        mode = Mode.WRITING;
        data = data.substring(0, Math.min(data.length(), Limits.NFC_MAX_DATA_SIZE));
        pendingWrite = new PendingWrite(data, label, readOnly, privateKey);
        setVisualState(NfcReaderBlock.NfcReaderState.WRITING);
    }

    public void cancelWrite() {
        mode = Mode.IDLE;
        pendingWrite = null;
        setVisualState(NfcReaderBlock.NfcReaderState.NONE);
    }

    public boolean isWriteMode() {
        return mode == Mode.WRITING;
    }

    // ---- signing ----

    public void flagSign(String data) {
        if (isWriteMode()) {
            cancelWrite();
        }

        mode = Mode.SIGNING;
        pendingSign = data;
        setVisualState(NfcReaderBlock.NfcReaderState.SIGNING);
    }

    public void cancelSign() {
        mode = Mode.IDLE;
        pendingSign = null;
        setVisualState(NfcReaderBlock.NfcReaderState.NONE);
    }

    public boolean isSigningMode() {
        return mode == Mode.SIGNING;
    }

    private void doSigning(String privateKey) {
        if (privateKey == null || pendingSign == null) {
            return;
        }

        var signature = Crypto.sign(pendingSign, privateKey);
        var publicKey = Crypto.derivePublicKey(privateKey);
        peripheral.signFeedback(signature, publicKey);
        cancelSign();
    }

    // ---- reading ----

    private void pingRead() {
        var level = getLevel();
        if (level == null) {
            return;
        }

        setVisualState(NfcReaderBlock.NfcReaderState.READING);
        level.scheduleTick(getBlockPos(), ModRegistry.NFC_READER, 10);
    }

    /** Called (server side) when a player right-clicks the reader with a card or badge. */
    public void onUse(ItemStack stack) {
        if (isWriteMode() && pendingWrite != null) {
            if (DataHolder.isReadOnly(stack)) {
                peripheral.writeFeedback(false, "read_only");
                cancelWrite();
                return;
            }

            if (pendingWrite.label() != null) {
                DataHolder.setLabel(stack, pendingWrite.label());
            } else {
                DataHolder.clearLabel(stack);
            }

            DataHolder.setData(stack, pendingWrite.data());
            DataHolder.setReadOnly(stack, pendingWrite.readOnly());

            if (pendingWrite.privateKey() != null) {
                DataHolder.setPrivateKey(stack, pendingWrite.privateKey());
            }

            peripheral.writeFeedback(true, "success");
            cancelWrite();
        } else if (isSigningMode()) {
            DataHolder.getPrivateKey(stack).ifPresent(this::doSigning);
        } else {
            var data = DataHolder.getData(stack);
            if (data.isPresent()) {
                var privateKey = DataHolder.getPrivateKey(stack);
                var publicKey = privateKey.map(Crypto::derivePublicKey).orElse(null);
                peripheral.read(data.get(), publicKey);
                pingRead();
            }
        }
    }
}
