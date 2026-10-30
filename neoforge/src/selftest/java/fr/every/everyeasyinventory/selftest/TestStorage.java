package fr.every.everyeasyinventory.selftest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;

/**
 * Bloc de stockage comme en ajoute un autre mod : il n'hérite d'aucune classe de coffre du jeu,
 * il expose seulement l'interface {@link Container} avec 18 cases.
 */
public class TestStorage extends BlockEntity implements Container {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("eei_selftest");
    private static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "eei_selftest");
    private static final DeferredBlock<Block> BLOCK = BLOCKS.registerBlock("test_storage", TestStorageBlock::new);
    private static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TestStorage>> TYPE = TYPES.register("test_storage",
            () -> new BlockEntityType<>(TestStorage::new, Set.of(BLOCK.get())));

    private final NonNullList<ItemStack> items = NonNullList.withSize(18, ItemStack.EMPTY);

    static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        TYPES.register(modBus);
    }

    public TestStorage(BlockPos pos, BlockState state) {
        super(TYPE.get(), pos, state);
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        return ContainerHelper.removeItem(items, slot, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    private static final class TestStorageBlock extends Block implements EntityBlock {

        TestStorageBlock(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new TestStorage(pos, state);
        }
    }
}
