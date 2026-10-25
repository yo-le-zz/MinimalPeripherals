package minimalperipherals.rfid;

import minimalperipherals.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class RfidScannerBlockEntity extends BlockEntity {
    private final RfidScannerPeripheral peripheral = new RfidScannerPeripheral(this);

    public RfidScannerBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.RFID_SCANNER_BE, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RfidScannerBlockEntity scanner) {
        scanner.peripheral.tick(level);
    }

    /** The peripheral is exposed on every side of the scanner. */
    public RfidScannerPeripheral peripheral(Direction side) {
        return peripheral;
    }

    public Direction getDirection() {
        return getBlockState().getValue(DirectionalBlock.FACING);
    }
}
