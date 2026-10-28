package cryptographyperipherals.fabric;

import cryptographyperipherals.platform.Platform;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public class FabricPlatform implements Platform {
    @Override
    public <T extends BlockEntity> BlockEntityType<T> createBlockEntityType(BlockEntityFactory<T> factory, Block... blocks) {
        return FabricBlockEntityTypeBuilder.<T>create(factory::create, blocks).build();
    }

    @Override
    public CreativeModeTab createTab(Component title, Supplier<ItemStack> icon, CreativeModeTab.DisplayItemsGenerator items) {
        return FabricItemGroup.builder()
                .title(title)
                .icon(icon)
                .displayItems(items)
                .build();
    }
}
