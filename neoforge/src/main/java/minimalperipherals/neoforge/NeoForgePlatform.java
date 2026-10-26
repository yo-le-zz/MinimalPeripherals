package minimalperipherals.neoforge;

import minimalperipherals.platform.Platform;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public class NeoForgePlatform implements Platform {
    @Override
    public <T extends BlockEntity> BlockEntityType<T> createBlockEntityType(BlockEntityFactory<T> factory, Block... blocks) {
        return new BlockEntityType<>(factory::create, blocks);
    }

    @Override
    public CreativeModeTab createTab(Component title, Supplier<ItemStack> icon, CreativeModeTab.DisplayItemsGenerator items) {
        return CreativeModeTab.builder()
                .title(title)
                .icon(icon)
                .displayItems(items)
                .build();
    }
}
