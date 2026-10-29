package fr.every.everyeasyinventory.network;

import fr.every.everyeasyinventory.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * Tous les paquets du mod.
 */
public final class Payloads {

    private Payloads() {
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> createType(String path) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, path));
    }

    /** Client → serveur : trier la partie conteneur ({@code playerSide = false}) ou l'inventaire du joueur du menu ouvert. */
    public record Sort(int containerId, boolean playerSide) implements CustomPacketPayload {
        public static final Type<Sort> TYPE = createType("sort");
        public static final StreamCodec<RegistryFriendlyByteBuf, Sort> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Sort::containerId,
                ByteBufCodecs.BOOL, Sort::playerSide,
                Sort::new);

        @Override
        public Type<Sort> type() {
            return TYPE;
        }
    }

    /** Client → serveur : rangement express vers les conteneurs proches. */
    public record AreaDeposit() implements CustomPacketPayload {
        public static final Type<AreaDeposit> TYPE = createType("area_deposit");
        public static final StreamCodec<RegistryFriendlyByteBuf, AreaDeposit> CODEC = StreamCodec.unit(new AreaDeposit());

        @Override
        public Type<AreaDeposit> type() {
            return TYPE;
        }
    }

    /** Client → serveur : cases verrouillées de l'inventaire (bit i = case i). */
    public record LockedSlots(long mask) implements CustomPacketPayload {
        public static final Type<LockedSlots> TYPE = createType("locked_slots");
        public static final StreamCodec<RegistryFriendlyByteBuf, LockedSlots> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, LockedSlots::mask,
                LockedSlots::new);

        @Override
        public Type<LockedSlots> type() {
            return TYPE;
        }
    }

    /** Client → serveur : le joueur vise ce conteneur (ou plus aucun). */
    public record Peek(Optional<BlockPos> pos) implements CustomPacketPayload {
        public static final Type<Peek> TYPE = createType("peek");
        public static final StreamCodec<RegistryFriendlyByteBuf, Peek> CODEC = StreamCodec.composite(
                ByteBufCodecs.optional(BlockPos.STREAM_CODEC), Peek::pos,
                Peek::new);

        @Override
        public Type<Peek> type() {
            return TYPE;
        }
    }

    /** Serveur → client : résumé du conteneur visé, un item par type avec son total. */
    public record PeekContents(BlockPos pos, List<ItemStack> icons, List<Integer> counts) implements CustomPacketPayload {
        public static final Type<PeekContents> TYPE = createType("peek_contents");
        public static final StreamCodec<RegistryFriendlyByteBuf, PeekContents> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, PeekContents::pos,
                ItemStack.OPTIONAL_LIST_STREAM_CODEC, PeekContents::icons,
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), PeekContents::counts,
                PeekContents::new);

        @Override
        public Type<PeekContents> type() {
            return TYPE;
        }
    }

    /** Client → serveur : item cherché dans la barre de recherche, ou recherche vidée. */
    public record Search(Optional<Holder<Item>> item) implements CustomPacketPayload {
        public static final Type<Search> TYPE = createType("search");
        public static final StreamCodec<RegistryFriendlyByteBuf, Search> CODEC = StreamCodec.composite(
                ByteBufCodecs.optional(Item.STREAM_CODEC), Search::item,
                Search::new);

        @Override
        public Type<Search> type() {
            return TYPE;
        }
    }

    /** Serveur → client : conteneurs proches qui ont l'item cherché (position clé de chacun, du plus proche au plus loin). */
    public record SearchResults(List<BlockPos> containers) implements CustomPacketPayload {
        public static final Type<SearchResults> TYPE = createType("search_results");
        public static final StreamCodec<RegistryFriendlyByteBuf, SearchResults> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), SearchResults::containers,
                SearchResults::new);

        @Override
        public Type<SearchResults> type() {
            return TYPE;
        }
    }
}
