package cryptographyperipherals.platform;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Registers a block entity as a CC: Tweaked peripheral (done differently on Fabric and NeoForge). */
public interface PeripheralRegistrar {
    <B extends BlockEntity> void register(BlockEntityType<B> type, PeripheralProvider<B> provider);
}
