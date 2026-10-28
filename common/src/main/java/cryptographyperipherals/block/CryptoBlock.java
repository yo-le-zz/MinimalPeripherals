package cryptographyperipherals.block;

import com.mojang.serialization.MapCodec;
import cryptographyperipherals.peripheral.EnginePeripheral;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import java.util.function.Function;

/**
 * One class for every crypto block: the block only knows how to build the peripheral it exposes.
 * The same block entity type serves all of them.
 */
public class CryptoBlock extends HorizontalDirectionalBlock implements EntityBlock {
    private static final MapCodec<CryptoBlock> CODEC = simpleCodec(properties -> {
        throw new UnsupportedOperationException("Crypto blocks are created by ModRegistry");
    });

    private final Function<CryptoBlockEntity, EnginePeripheral> factory;

    public CryptoBlock(Properties properties, Function<CryptoBlockEntity, EnginePeripheral> factory) {
        super(properties);
        this.factory = factory;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    public EnginePeripheral createPeripheral(CryptoBlockEntity blockEntity) {
        return factory.apply(blockEntity);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CryptoBlockEntity(pos, state);
    }
}
