package cryptographyperipherals.block;

import cryptographyperipherals.ModRegistry;
import cryptographyperipherals.peripheral.EnginePeripheral;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CryptoBlockEntity extends BlockEntity {
    private final EnginePeripheral peripheral;

    public CryptoBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.CRYPTO_BE, pos, state);
        this.peripheral = ((CryptoBlock) state.getBlock()).createPeripheral(this);
    }

    public EnginePeripheral peripheral() {
        return peripheral;
    }
}
