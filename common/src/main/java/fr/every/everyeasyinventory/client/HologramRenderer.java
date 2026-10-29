package fr.every.everyeasyinventory.client;

import fr.every.everyeasyinventory.logic.ContainerFinder;
import fr.every.everyeasyinventory.network.Payloads;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

import java.util.List;

/**
 * Hologramme au-dessus du conteneur visé : une case semi-transparente par type d'item, avec son total.
 * Dessiné dans le HUD à l'endroit où le dessus du conteneur se projette à l'écran, il fait donc toujours
 * face au joueur ; sa taille diminue avec la distance.
 */
public final class HologramRenderer {

    private static final int COLUMNS = 9;
    private static final int CELL = 18;
    private static final int PADDING = 3;
    private static final int PANEL_COLOR = 0x80101018;
    private static final int BORDER_COLOR = 0x60FFFFFF;
    private static final int SLOT_COLOR = 0x508B8B8B;
    private static final int SLOT_SHADOW = 0x40373737;
    private static final int SLOT_LIGHT = 0x40FFFFFF;

    private HologramRenderer() {
    }

    public static void render(GuiGraphicsExtractor graphics, Minecraft minecraft) {
        Payloads.PeekContents contents = ClientState.contents;
        BlockPos block = ClientState.targetBlock;
        if (contents == null || block == null || contents.icons().isEmpty() || minecraft.level == null
                || !contents.pos().equals(ClientState.target)) {
            return;
        }
        BlockPos other = ContainerFinder.otherHalf(minecraft.level.getBlockState(block), block);
        Vec3 center = Vec3.atCenterOf(block).add(Vec3.atCenterOf(other)).scale(0.5);
        Vec3 anchor = new Vec3(center.x, block.getY() + 1.2, center.z);

        Camera camera = minecraft.gameRenderer.mainCamera();
        Vec3 toAnchor = anchor.subtract(camera.position());
        Vector3fc forward = camera.forwardVector();
        if (toAnchor.x * forward.x() + toAnchor.y * forward.y() + toAnchor.z * forward.z() <= 0.1) {
            return;
        }
        Vec3 ndc = minecraft.gameRenderer.projectPointToScreen(anchor);
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        float x = (float) ((ndc.x + 1.0) / 2.0 * screenWidth);
        float y = (float) ((1.0 - ndc.y) / 2.0 * screenHeight);
        float scale = Mth.clamp(1.8F / (float) toAnchor.length(), 0.5F, 1.0F);

        List<ItemStack> icons = contents.icons();
        List<Integer> counts = contents.counts();
        int columns = Math.min(COLUMNS, icons.size());
        int rows = (icons.size() + COLUMNS - 1) / COLUMNS;
        int width = columns * CELL + PADDING * 2;
        int height = rows * CELL + PADDING * 2;

        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-width / 2.0F, -height);

        graphics.fill(0, 0, width, height, PANEL_COLOR);
        graphics.outline(0, 0, width, height, BORDER_COLOR);
        for (int i = 0; i < icons.size(); i++) {
            int slotX = PADDING + (i % COLUMNS) * CELL;
            int slotY = PADDING + (i / COLUMNS) * CELL;
            graphics.fill(slotX, slotY, slotX + CELL - 1, slotY + CELL - 1, SLOT_COLOR);
            graphics.fill(slotX, slotY, slotX + CELL - 1, slotY + 1, SLOT_SHADOW);
            graphics.fill(slotX, slotY, slotX + 1, slotY + CELL - 1, SLOT_SHADOW);
            graphics.fill(slotX, slotY + CELL - 2, slotX + CELL - 1, slotY + CELL - 1, SLOT_LIGHT);
            graphics.fill(slotX + CELL - 2, slotY, slotX + CELL - 1, slotY + CELL - 1, SLOT_LIGHT);
            ItemStack icon = icons.get(i);
            graphics.fakeItem(icon, slotX + 1, slotY + 1);
            int count = i < counts.size() ? counts.get(i) : 1;
            graphics.itemDecorations(minecraft.font, icon, slotX + 1, slotY + 1, count > 1 ? formatCount(count) : null);
        }
        graphics.pose().popMatrix();
    }

    /** 7 → « 7 », 1 234 → « 1.2k », 12 345 → « 12k ». */
    static String formatCount(int count) {
        if (count < 1000) {
            return Integer.toString(count);
        }
        if (count < 10_000) {
            return String.format(java.util.Locale.ROOT, "%.1fk", count / 1000.0);
        }
        return count / 1000 + "k";
    }
}
