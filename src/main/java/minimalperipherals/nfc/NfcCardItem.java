package minimalperipherals.nfc;

import minimalperipherals.dataholder.DataHolderItem;
import net.minecraft.world.item.Item;

public class NfcCardItem extends Item implements DataHolderItem {
    public NfcCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean supportsPrivateKey() {
        return true;
    }
}
