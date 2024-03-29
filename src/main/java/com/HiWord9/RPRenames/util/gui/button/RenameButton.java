package com.HiWord9.RPRenames.util.gui.button;

import com.HiWord9.RPRenames.AnvilScreenMixinAccessor;
import com.HiWord9.RPRenames.RPRenames;
import com.HiWord9.RPRenames.util.config.Rename;
import com.HiWord9.RPRenames.util.gui.RenameButtonHolder.ViewMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.Identifier;

public class RenameButton extends ClickableWidget {
    private static final Identifier TEXTURE_LIST = new Identifier(RPRenames.MOD_ID, "textures/gui/button.png");
    private static final Identifier TEXTURE_GRID = new Identifier(RPRenames.MOD_ID, "textures/gui/button_grid.png");

    static final int buttonWidthList = 127;
    public static final int buttonHeightList = 20;
    public static final int buttonWidthGrid = 25;
    public static final int buttonHeightGrid = 25;

    static final int textureWidthList = 127;
    static final int textureHeightList = 80;
    static final int textureWidthGrid = 50;
    static final int textureHeightGrid = 50;

    static final int focusedOffsetVList = 20;
    static final int favoriteOffsetVList = 40;
    static final int focusedOffsetVGrid = 25;
    static final int favoriteOffsetUGrid = 25;

    ViewMode type;
    boolean favorite;

    int indexInInventory;
    boolean isInInventory;
    boolean asCurrentItem;
    PlayerInventory inventory;
    Rename rename;
    boolean enoughStackSize;
    boolean enoughDamage;
    boolean hasEnchant;
    boolean hasEnoughLevels;

    public RenameButton(int x, int y, ViewMode type, boolean favorite,
                        int indexInInventory, boolean isInInventory, boolean asCurrentItem,
                        PlayerInventory inventory, Rename rename,
                        boolean enoughStackSize, boolean enoughDamage,
                        boolean hasEnchant, boolean hasEnoughLevels) {
        super(x, y, type == ViewMode.LIST ? buttonWidthList : buttonWidthGrid, type == ViewMode.LIST ? buttonHeightList : buttonHeightGrid, null);
        this.type = type;
        this.favorite = favorite;

        this.indexInInventory = indexInInventory;
        this.isInInventory = isInInventory;
        this.asCurrentItem = asCurrentItem;
        this.inventory = inventory;
        this.rename = rename;
        this.enoughStackSize = enoughStackSize;
        this.enoughDamage = enoughDamage;
        this.hasEnchant = hasEnchant;
        this.hasEnoughLevels = hasEnoughLevels;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean focused = isMouseOver(mouseX, mouseY);
        Identifier texture;
        int textureWidth;
        int textureHeight;
        int u = 0;
        int v = 0;
        if (type == ViewMode.LIST) {
            texture = TEXTURE_LIST;
            textureWidth = textureWidthList;
            textureHeight = textureHeightList;
            v += favorite ? favoriteOffsetVList : 0;
            v += focused ? focusedOffsetVList : 0;
        } else {
            texture = TEXTURE_GRID;
            textureWidth = textureWidthGrid;
            textureHeight = textureHeightGrid;
            u += favorite ? favoriteOffsetUGrid : 0;
            v += focused ? focusedOffsetVGrid : 0;
        }
        context.drawTexture(texture, getX(), getY(), u, v, getWidth(), getHeight(), textureWidth, textureHeight);
    }

    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.clicked(mouseX, mouseY)) {
            AnvilScreen screen = ((AnvilScreen) MinecraftClient.getInstance().currentScreen);
            if (screen instanceof AnvilScreenMixinAccessor anvilScreenMixinAccessor) {
                anvilScreenMixinAccessor.onRenameButton(indexInInventory, isInInventory, asCurrentItem,
                        inventory, rename,
                        enoughStackSize, enoughDamage,
                        hasEnchant, hasEnoughLevels);
                return true;
            }
            return false;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {

    }
}
