package cryptographyperipherals.peripheral;

import cryptographyperipherals.block.CryptoBlockEntity;
import dan200.computercraft.api.peripheral.IPeripheral;

/** Base class of every crypto peripheral. */
public abstract class EnginePeripheral implements IPeripheral {
    private final CryptoBlockEntity owner;
    private final String type;

    protected EnginePeripheral(CryptoBlockEntity owner, String type) {
        this.owner = owner;
        this.type = type;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public boolean equals(IPeripheral other) {
        return other instanceof EnginePeripheral o && o.owner == owner;
    }
}
