package fr.every.everyeasyinventory.client;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.every.everyeasyinventory.logic.ContainerFinder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

/**
 * Contour doré des conteneurs proches qui ont l'item cherché. Dessiné avec le test de profondeur normal :
 * les blocs devant le conteneur le cachent.
 */
public final class SearchOutlineRenderer {

    private static final int COLOR = 0xFFE0C060;
    private static final double MAX_DISTANCE = 12.0;

    private SearchOutlineRenderer() {
    }

    public static void submit(SubmitNodeCollector collector, Vec3 camera) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || ClientState.searchResults().isEmpty()) {
            return;
        }
        float width = minecraft.gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth * 1.5F;
        PoseStack poseStack = new PoseStack();
        for (BlockPos key : ClientState.searchResults()) {
            if (!ContainerFinder.isSupported(minecraft.level.getBlockEntity(key))
                    || Vec3.atCenterOf(key).distanceTo(camera) > MAX_DISTANCE) {
                continue;
            }
            BlockPos other = ContainerFinder.otherHalf(minecraft.level.getBlockState(key), key);
            AABB box = new AABB(Vec3.atLowerCornerOf(key), Vec3.atLowerCornerOf(key).add(1, 1, 1))
                    .minmax(new AABB(Vec3.atLowerCornerOf(other), Vec3.atLowerCornerOf(other).add(1, 1, 1)))
                    .inflate(0.01);
            poseStack.pushPose();
            poseStack.translate(box.minX - camera.x, box.minY - camera.y, box.minZ - camera.z);
            collector.submitShapeOutline(poseStack, Shapes.box(0, 0, 0, box.getXsize(), box.getYsize(), box.getZsize()),
                    RenderTypes.lines(), COLOR, width, false);
            poseStack.popPose();
        }
    }
}
