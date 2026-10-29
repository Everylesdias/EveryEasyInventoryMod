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
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Contour doré des conteneurs proches qui ont l'item cherché, sur un liseré noir plus large (comme le contour
 * « contraste élevé » du jeu). Dessiné avec le test de profondeur normal : les blocs devant le conteneur le cachent.
 */
public final class SearchOutlineRenderer {

    private static final int COLOR = 0xFFFFC21A;
    /** Liseré sombre sous le trait doré, pour qu'il reste lisible en pleine lumière ou avec des shaders. */
    private static final int BACKING_COLOR = 0xFF000000;
    private static final double MAX_DISTANCE = 12.0;

    private SearchOutlineRenderer() {
    }

    public static void submit(SubmitNodeCollector collector, Vec3 camera) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || ClientState.searchResults().isEmpty()) {
            return;
        }
        float width = minecraft.gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth;
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
            VoxelShape shape = Shapes.box(0, 0, 0, box.getXsize(), box.getYsize(), box.getZsize());
            collector.submitShapeOutline(poseStack, shape, RenderTypes.secondaryBlockOutline(), BACKING_COLOR, width * 4.5F, false);
            collector.submitShapeOutline(poseStack, shape, RenderTypes.linesDepthBias(), COLOR, width * 2.5F, false);
            poseStack.popPose();
        }
    }
}
