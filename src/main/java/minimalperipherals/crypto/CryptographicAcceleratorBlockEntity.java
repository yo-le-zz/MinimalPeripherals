package minimalperipherals.crypto;

import minimalperipherals.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CryptographicAcceleratorBlockEntity extends BlockEntity {
    private final CryptographicAcceleratorPeripheral peripheral = new CryptographicAcceleratorPeripheral(this);

    public CryptographicAcceleratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.CRYPTOGRAPHIC_ACCELERATOR_BE, pos, state);
    }

    public CryptographicAcceleratorPeripheral peripheral() {
        return peripheral;
    }
}
