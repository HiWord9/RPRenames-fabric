package com.HiWord9.RPRenames.util.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.TooltipBackgroundRenderer;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

public class Graphics extends Screen {
    static public final int DEFAULT_TEXT_COLOR = 0xffffff;

    static public final int backgroundWidth = 176;
    static public final int backgroundHeight = 166;

    protected Graphics() {
        super(null);
    }

    public static void renderText(DrawContext context, Text text, int x, int y, boolean shadow, boolean centered) {
        renderText(context, text, DEFAULT_TEXT_COLOR, x, y, shadow, centered);
    }

    public static void renderText(DrawContext context, Text text, int color, int x, int y, boolean shadow, boolean centered) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer renderer = client.textRenderer;
        int xOffset = 0;
        if (centered) {
            xOffset = renderer.getWidth(text) / 2;
        }
        context.drawText(renderer, text, x - xOffset, y, color, shadow);
    }

    public static void renderStack(DrawContext context, ItemStack itemStack, int x, int y) {
        renderStack(context, itemStack, x, y, null, 16);
    }

    public static void renderStack(DrawContext context, ItemStack itemStack, int x, int y, @Nullable Integer z, int size) {
        float scale = size != 16 ? ((float) size / 16f) : 1f;
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(x, y, z == null ? 0 : z);
        matrices.scale(scale, scale, 1);
        context.drawItemWithoutEntity(itemStack, 0, 0);
        matrices.pop();
    }

    public static void renderEntity(DrawContext context, int x, int y, int size, Entity entity, boolean spin) {
        DiffuseLighting.disableGuiDepthLighting();
        context.getMatrices().push();
        if (entity instanceof SquidEntity) {
            size /= 1.5;
        } else if (entity instanceof ItemEntity) {
            size *= 2;
        }
        if (entity instanceof LivingEntity living && living.isBaby()) {
            size /= 1.7;
        }
        context.getMatrices().translate(x, y, 1500);
        context.getMatrices().scale(1f, 1f, -1);
        context.getMatrices().translate(0, 0, 1000);
        context.getMatrices().scale(size, size, size);
        var quaternion = (new Quaternionf()).rotateZ(3.1415927F);
        var quaternion2 = (new Quaternionf()).rotateX(-10.f * 0.017453292F);
        quaternion.mul(quaternion2);
        context.getMatrices().multiply(quaternion);
        if (MinecraftClient.getInstance().cameraEntity != null) {
            entity.setPos(MinecraftClient.getInstance().cameraEntity.getX(), MinecraftClient.getInstance().cameraEntity.getY(), MinecraftClient.getInstance().cameraEntity.getZ());
        }

        assert MinecraftClient.getInstance().player != null;
        entity.age = MinecraftClient.getInstance().player.age;
        setupAngles(entity, spin);

        var entityRenderDispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
        quaternion2.conjugate();
        entityRenderDispatcher.setRotation(quaternion2);
        entityRenderDispatcher.setRenderShadows(false);
        var immediate = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();

        entityRenderDispatcher.render(entity, 0, 0, 0, 0.f, 1.f, context.getMatrices(), immediate,
                LightmapTextureManager.MAX_LIGHT_COORDINATE
        );
        immediate.draw();
        entityRenderDispatcher.setRenderShadows(true);
        context.getMatrices().pop();
        DiffuseLighting.enableGuiDepthLighting();
    }

    public static void renderPlayer(DrawContext context, int x, int y, int size, LivingEntity entity, boolean spin) {
        DiffuseLighting.disableGuiDepthLighting();
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 1500);
        context.getMatrices().scale(1f, 1f, -1);
        context.getMatrices().translate(0, 0, 1000);
        context.getMatrices().scale(size, size, size);
        var quaternion = (new Quaternionf()).rotateZ(3.1415927F);
        var quaternion2 = (new Quaternionf()).rotateX(-10.f * 0.017453292F);
        quaternion.mul(quaternion2);
        context.getMatrices().multiply(quaternion);
        if (MinecraftClient.getInstance().cameraEntity != null) {
            entity.setPos(MinecraftClient.getInstance().cameraEntity.getX(), MinecraftClient.getInstance().cameraEntity.getY(), MinecraftClient.getInstance().cameraEntity.getZ());
        }

        float h = entity.bodyYaw;
        float i = entity.getYaw();
        float j = entity.getPitch();
        float k = entity.prevHeadYaw;
        float l = entity.headYaw;

        setupAngles(entity, spin);

        var entityRenderDispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
        quaternion2.conjugate();
        entityRenderDispatcher.setRotation(quaternion2);
        entityRenderDispatcher.setRenderShadows(false);
        var immediate = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();

        entityRenderDispatcher.render(entity, 0, 0, 0, 0.f, 1.f, context.getMatrices(), immediate,
                LightmapTextureManager.MAX_LIGHT_COORDINATE
        );
        immediate.draw();
        entityRenderDispatcher.setRenderShadows(true);

        entity.bodyYaw = h;
        entity.setYaw(i);
        entity.setPitch(j);
        entity.prevHeadYaw = k;
        entity.headYaw = l;

        context.getMatrices().pop();
        DiffuseLighting.enableGuiDepthLighting();
    }

    private static void setupAngles(Entity entity, boolean spin) {
        float yaw = spin ? (float) (((System.currentTimeMillis() / 10)) % 360) : 225.0F;
        entity.setYaw(yaw);
        entity.setHeadYaw(yaw);
        entity.setPitch(0.f);
        if (entity instanceof LivingEntity living) {
            living.bodyYaw = yaw;
        }
    }

    public static void drawTooltipBackground(DrawContext context, int x, int y, int width, int height) {
        drawTooltipBackground(context, x, y, width, height, 400);
    }

    public static void drawTooltipBackground(DrawContext context, int x, int y, int width, int height, int z) {
        TooltipBackgroundRenderer.render(context, x + 4, y + 4, width - 8, height - 8, z);
    }
}
