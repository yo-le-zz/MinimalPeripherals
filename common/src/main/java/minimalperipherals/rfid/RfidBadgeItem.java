package minimalperipherals.rfid;

import minimalperipherals.dataholder.DataHolderItem;
import net.minecraft.world.item.Item;

public class RfidBadgeItem extends Item implements DataHolderItem {
    public RfidBadgeItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean supportsPrivateKey() {
        return false;
    }
}
