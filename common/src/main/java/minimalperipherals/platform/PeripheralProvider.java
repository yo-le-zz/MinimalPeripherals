package minimalperipherals.platform;

import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

@FunctionalInterface
public interface PeripheralProvider<B extends BlockEntity> {
    IPeripheral getPeripheral(B blockEntity, Direction side);
}
