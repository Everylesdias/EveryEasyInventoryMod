package fr.every.everyeasyinventory.client;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.every.everyeasyinventory.config.EeiSetting;
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
 * Contour (couleur et épaisseur réglables) des conteneurs proches qui ont l'item cherché, sur un liseré noir plus large (comme le contour
 * « contraste élevé » du jeu). Dessiné avec le test de profondeur normal : les blocs devant le conteneur le cachent.
 */
public final class SearchOutlineRenderer {

    /** Liseré sombre sous le trait de couleur, pour qu'il reste lisible en pleine lumière ou avec des shaders. */
    private static final int BACKING_COLOR = 0xFF000000;
    private static final double MAX_DISTANCE = 12.0;

    private SearchOutlineRenderer() {
    }

    public static void submit(SubmitNodeCollector collector, Vec3 camera) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || ClientState.searchResults().isEmpty() || !ClientState.bool(EeiSetting.OUTLINE_ENABLED)) {
            return;
        }
        int color = 0xFF000000 | (Integer) ClientState.value(EeiSetting.OUTLINE_COLOR);
        float base = minecraft.gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth;
        float width = base * (Integer) ClientState.value(EeiSetting.OUTLINE_THICKNESS) * 5.0F / 6.0F;
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
            collector.submitShapeOutline(poseStack, shape, RenderTypes.secondaryBlockOutline(), BACKING_COLOR, width + base * 2.0F, false);
            collector.submitShapeOutline(poseStack, shape, RenderTypes.linesDepthBias(), color, width, false);
            poseStack.popPose();
        }
    }
}
