package com.HiWord9.RPRenames.mixin;

import com.HiWord9.RPRenames.AnvilScreenMixinAccessor;
import com.HiWord9.RPRenames.RPRenames;
import com.HiWord9.RPRenames.modConfig.ModConfig;
import com.HiWord9.RPRenames.util.Tabs;
import com.HiWord9.RPRenames.util.config.ConfigManager;
import com.HiWord9.RPRenames.util.config.Rename;
import com.HiWord9.RPRenames.util.gui.GhostCraft;
import com.HiWord9.RPRenames.util.gui.Graphics;
import com.HiWord9.RPRenames.util.gui.RenameButtonHolder;
import com.HiWord9.RPRenames.util.gui.button.*;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin extends Screen implements AnvilScreenMixinAccessor {
    private static final ModConfig config = ModConfig.INSTANCE;

    protected AnvilScreenMixin(Text title) {
        super(title);
    }

    @Shadow
    private TextFieldWidget nameField;

    boolean open;

    private static final Identifier MENU_TEXTURE = new Identifier(RPRenames.MOD_ID, "textures/gui/menu.png");

    int menuTextureHeight = 166;
    int menuWidth = 147;
    int menuHeight = menuTextureHeight;
    int menuXOffset = -1;
    int tabOffsetY = 6;
    int startTabOffsetY = 6;
    int buttonOffsetY = 2;
    int buttonXOffset = 10;

    int highlightColor = config.getSlotHighlightRGBA();

    final int backgroundWidth = Graphics.backgroundWidth;
    final int backgroundHeight = Graphics.backgroundHeight;

    int page = 0;
    int maxPageElements = 5;
    int currentRenameListSize;

    String currentItem = "air";
    ItemStack itemAfterUpdate;
    boolean afterInventoryTab = false;
    boolean afterGlobalTab = false;
    int tempPage;

    OpenerButton opener;

    ArrayList<RenameButtonHolder> buttons = new ArrayList<>();

    TabButton searchTab;
    TabButton favoriteTab;
    TabButton inventoryTab;
    TabButton globalTab;
    Tabs currentTab = Tabs.SEARCH;

    FavoriteButton favoriteButton;

    PageButton pageDown;
    PageButton pageUp;
    Text pageCount = Text.empty();

    GhostCraft ghostCraft = new GhostCraft();

    ArrayList<Rename> originalRenameList = new ArrayList<>();
    ArrayList<Rename> currentRenameList = new ArrayList<>();

    TextRenderer renderer = MinecraftClient.getInstance().textRenderer;

    TextFieldWidget searchField;
    int searchFieldXOffset = 23;
    Text SEARCH_HINT_TEXT = Text.translatable("rprenames.gui.searchHintText").formatted(Formatting.ITALIC).formatted(Formatting.GRAY);

    String searchTag = "";

    @Inject(at = @At("HEAD"), method = "setup")
    private void init(CallbackInfo ci) {
        if (!config.enableAnvilModification) return;
        RPRenames.LOGGER.info("Starting RPRenames modification on AnvilScreen");

        int pageButtonsY = this.height / 2 + 57;
        if (config.viewMode == RenameButtonHolder.ViewMode.GRID) {
            pageButtonsY -= 4;
            maxPageElements = 20;
        }

        for (int i = 0; i < maxPageElements; i++) {
            buttons.add(new RenameButtonHolder(config.viewMode, i));
        }

        pageDown = new PageButton(this.width / 2 - backgroundWidth / 2 - menuWidth + menuXOffset + buttonXOffset, pageButtonsY, PageButton.Type.DOWN);
        pageUp = new PageButton(this.width / 2 - backgroundWidth / 2 + menuXOffset - buttonXOffset - PageButton.buttonWidth, pageButtonsY, PageButton.Type.UP);

        opener = new OpenerButton(this.width / 2 - 85, this.height / 2 - 39);

        searchTab = new TabButton(this.width / 2 - backgroundWidth / 2 - menuWidth + menuXOffset - (TabButton.buttonWidth - 3), this.height / 2 - backgroundHeight / 2 + startTabOffsetY, Tabs.SEARCH);
        favoriteTab = new TabButton(this.width / 2 - backgroundWidth / 2 - menuWidth + menuXOffset - (TabButton.buttonWidth - 3), this.height / 2 - backgroundHeight / 2 + startTabOffsetY + (TabButton.buttonHeight + tabOffsetY), Tabs.FAVORITE);
        inventoryTab = new TabButton(this.width / 2 - backgroundWidth / 2 - menuWidth + menuXOffset - (TabButton.buttonWidth - 3), this.height / 2 - backgroundHeight / 2 + startTabOffsetY + (TabButton.buttonHeight + tabOffsetY) * 2, Tabs.INVENTORY);
        globalTab = new TabButton(this.width / 2 - backgroundWidth / 2 - menuWidth + menuXOffset - (TabButton.buttonWidth - 3), this.height / 2 - backgroundHeight / 2 + startTabOffsetY + (TabButton.buttonHeight + tabOffsetY) * 4, Tabs.GLOBAL);

        favoriteButton = new FavoriteButton(this.width / 2 + config.favoritePosX, this.height / 2 + config.favoritePosY);

        searchField = new TextFieldWidget(renderer, this.width / 2 - (backgroundWidth / 2) - menuWidth + menuXOffset + searchFieldXOffset, this.height / 2 - 68, menuWidth - 38, 10, Text.of(""));
        searchField.setChangedListener(this::onSearch);
        searchField.setDrawsBackground(false);
        searchField.setMaxLength(1024);
    }

    @Inject(at = @At("TAIL"), method = "setup")
    private void initTail(CallbackInfo ci) {
        if (!config.enableAnvilModification) return;
        if (config.openByDefault) {
            openMenu();
        } else {
            screenUpdate();
        }
    }

    public void switchOpen() {
        if (!open) {
            openMenu();
        } else {
            closeMenu();
        }
    }

    public void addOrRemoveFavorite(boolean add) {
        String favoriteName = nameField.getText();

        String item = currentItem;
        if (item.equals("air") && !ghostCraft.slot1.isEmpty()) {
            item = ConfigManager.getIdAndPath(ghostCraft.slot1.getItem());
        }
        if (!item.equals("air")) {
            if (add) {
                ConfigManager.addToFavorites(favoriteName, item);
            } else {
                ConfigManager.removeFromFavorites(favoriteName, item);
            }
            favoriteButtonsUpdate(nameField.getText());
            if (open) {
                screenUpdate(page);
            }
        }
    }

    public void onPageDown() {
        if (hasShiftDown()) {
            page = 0;
        } else {
            page--;
        }
        updateWidgets();
        if (page == 0) {
            pageDown.active = false;
        }
        pageUp.active = true;
    }

    public void onPageUp() {
        if (hasShiftDown()) {
            page = ((currentRenameList.size() + maxPageElements - 1) / maxPageElements - 1);
        } else {
            page++;
        }
        updateWidgets();
        pageDown.active = true;
        if ((page + 1) * maxPageElements > currentRenameListSize - 1) {
            pageUp.active = false;
        }
    }

    public void setTab(Tabs tab) {
        if (tab == currentTab) return;
        currentTab = tab;
        screenUpdate();
    }

    public Tabs getCurrentTab() {
        return currentTab;
    }

    public void onRenameButton(int indexInInventory, boolean isInInventory, boolean asCurrentItem, PlayerInventory inventory, Rename rename, boolean enoughStackSize, boolean enoughDamage, boolean hasEnchant, boolean hasEnoughLevels) {
        ghostCraft.reset();
        if (currentTab == Tabs.INVENTORY || currentTab == Tabs.GLOBAL) {
            if (indexInInventory != 36 && isInInventory) {
                if (currentTab == Tabs.INVENTORY) {
                    afterInventoryTab = true;
                } else {
                    afterGlobalTab = true;
                }
                tempPage = page;
                if (!asCurrentItem) {
                    putInAnvil(indexInInventory, MinecraftClient.getInstance());
                }
                afterInventoryTab = false;
                afterGlobalTab = false;
            } else if (indexInInventory != 36) {
                assert (client != null ? client.player : null) != null;
                for (int s = 0; s < 2; s++) {
                    moveToInventory(s, inventory);
                }

                ItemStack ghostSource = new ItemStack(ConfigManager.itemFromName(rename.getItem()));
                ghostSource.setCount(rename.getStackSize());
                ghostSource.setDamage(rename.getDamage());

                ItemStack ghostEnchant = ItemStack.EMPTY;
                if (rename.getEnchantment() != null) {
                    ghostEnchant = new ItemStack(Items.ENCHANTED_BOOK);
                    ghostEnchant.getOrCreateNbt();
                    assert ghostEnchant.getNbt() != null;
                    if (!ghostEnchant.getNbt().contains("Enchantments", 9)) {
                        ghostEnchant.getNbt().put("Enchantments", new NbtList());
                    }
                    NbtList nbtList = ghostEnchant.getNbt().getList("Enchantments", 10);
                    nbtList.add(EnchantmentHelper.createNbt(new Identifier(rename.getEnchantment()), rename.getEnchantmentLevel()));
                }

                ItemStack ghostResult = RenameButtonHolder.createItem(rename);

                ghostCraft.setSlots(ghostSource, ghostEnchant, ghostResult);
                ghostCraft.setRender(true);
            } else {
                moveToInventory(1, inventory);
            }
        }
        if (currentTab == Tabs.SEARCH || isInInventory) {
            if (!enoughStackSize || !enoughDamage) {
                ghostCraft.setForceRenderBG(true, null, true);
                ghostCraft.setRender(true);
            }
            if (!hasEnchant || !hasEnoughLevels) {
                ItemStack ghostEnchant = new ItemStack(Items.ENCHANTED_BOOK);
                ghostEnchant.getOrCreateNbt();
                assert ghostEnchant.getNbt() != null;
                if (!ghostEnchant.getNbt().contains("Enchantments", 9)) {
                    ghostEnchant.getNbt().put("Enchantments", new NbtList());
                }
                NbtList nbtList = ghostEnchant.getNbt().getList("Enchantments", 10);
                nbtList.add(EnchantmentHelper.createNbt(new Identifier(rename.getEnchantment()), rename.getEnchantmentLevel()));
                ghostCraft.setSlots(ItemStack.EMPTY, ghostEnchant, ItemStack.EMPTY);
                ghostCraft.setForceRenderBG(null, null, true);
                ghostCraft.setRender(true);
            }
        }
        nameField.setText(rename.getName());
    }

    private void openMenu() {
        open = true;
        RPRenames.LOGGER.info("Opening RPRenames Menu");
        if (currentItem.equals("air")) {
            currentTab = Tabs.GLOBAL;
        } else {
            currentTab = Tabs.SEARCH;
        }
        screenUpdate();
    }

    private void closeMenu() {
        open = false;
        searchField.setFocused(false);
        searchField.setFocusUnlocked(false);
        searchField.setText("");
        removeWidgets();
        remove(searchField);
        nameField.setFocused(true);
        nameField.setFocusUnlocked(false);
        opener.setOpen(open);
        currentTab = Tabs.SEARCH;
        RPRenames.LOGGER.info("Closing RPRenames Menu");
    }

    private void updateWidgets() {
        defineButtons();
        showButtons();
        updatePageWidgets();
    }

    private void screenUpdate() {
        screenUpdate(0);
    }

    private void screenUpdate(int savedPage) {
        page = savedPage;
        opener.active = true;
        if (afterInventoryTab) {
            currentTab = Tabs.INVENTORY;
            page = tempPage;
        } else if (afterGlobalTab) {
            currentTab = Tabs.GLOBAL;
            page = tempPage;
        }
        calcRenameList();

        if (open) {
            removeWidgets();
            updateSearchRequest(page);
            opener.setOpen(open);
            addDrawableChild(searchField);
            searchField.setFocusUnlocked(true);
            nameField.setFocused(false);
            nameField.setFocusUnlocked(true);
        }
    }

    private void calcRenameList() {
        if (currentTab == Tabs.SEARCH) {
            originalRenameList = ConfigManager.getAllRenames(currentItem);
        } else if (currentTab == Tabs.FAVORITE) {
            originalRenameList = ConfigManager.getFavorites(currentItem);
        } else if (currentTab == Tabs.INVENTORY) {
            ArrayList<String> currentInvList = getInventory();
            ArrayList<String> checked = new ArrayList<>();
            ArrayList<Rename> names = new ArrayList<>();
            for (String item : currentInvList) {
                if (!item.equals("air") && !checked.contains(item)) {
                    checked.add(item);
                    ArrayList<Rename> renames = ConfigManager.getAllRenames(item);
                    names.addAll(renames);
                }
            }
            originalRenameList = names;
        } else if (currentTab == Tabs.GLOBAL) {
            originalRenameList = ConfigManager.getAllRenames();
        }
    }

    private void updateSearchRequest() {
        updateSearchRequest(0);
    }

    private void updateSearchRequest(int page) {
        hideButtons();
        currentRenameList = search(originalRenameList, searchTag);

        this.page = page;
        if (this.page >= (currentRenameList.size() + maxPageElements - 1) / maxPageElements) {
            this.page = ((currentRenameList.size() + maxPageElements - 1) / maxPageElements) - 1;
            if (this.page == -1) {
                this.page = 0;
            }
        }
        currentRenameListSize = currentRenameList.size();

        defineButtons();
        showButtons();
        updatePageWidgets();
    }

    @Inject(at = @At("RETURN"), method = "onRenamed")
    private void newNameEntered(String name, CallbackInfo ci) {
        if (!config.enableAnvilModification) return;
        favoriteButtonsUpdate(name);
    }

    private void favoriteButtonsUpdate(String name) {
        if (!name.isEmpty()) {
            favoriteButton.active = true;
            boolean favorite = Rename.isFavorite(currentItem.equals("air") ? ghostCraft.slot1.isEmpty() ? "air" : ConfigManager.getIdAndPath(ghostCraft.slot1.getItem()) : currentItem, name);
            favoriteButton.setFavorite(favorite);
        } else {
            favoriteButton.active = false;
        }
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/AnvilScreen;init(Lnet/minecraft/client/MinecraftClient;II)V"), method = "resize")
    private void onResize(AnvilScreen instance, MinecraftClient client, int width, int height) {
        if (!config.enableAnvilModification) {
            instance.init(client, width, height);
            return;
        }
        buttons.clear();
        String tempSearchFieldText = searchField.getText();
        instance.init(client, width, height);
        searchField.setText(tempSearchFieldText);
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;isActive()Z"), method = "keyPressed")
    private boolean onKeyPressedNameFieldIsActive(TextFieldWidget instance, int keyCode, int scanCode, int modifiers) {
        if (!config.enableAnvilModification) return instance.isActive();
        searchField.keyPressed(keyCode, scanCode, modifiers);
        return instance.isActive() || searchField.isActive();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!config.enableAnvilModification) return super.mouseClicked(mouseX, mouseY, button);
        int xScreenOffset = (this.width - backgroundWidth) / 2;
        int yScreenOffset = (this.height - backgroundHeight) / 2;
        if (ghostCraft.doRender) {
            if ((mouseX - xScreenOffset >= 26 && mouseX - xScreenOffset <= 151) && (mouseY - yScreenOffset >= 46 && mouseY - yScreenOffset <= 64)) {
                ghostCraft.reset();
                if (currentItem.equals("air")) {
                    nameField.setText("");
                }
            }
        }
        if (open) {
            for (RenameButtonHolder renameButtonHolder : buttons) {
                if (renameButtonHolder.isActive()) {
                    renameButtonHolder.getButton().mouseClicked(mouseX, mouseY, button);
                }
            }
            pageDown.mouseClicked(mouseX, mouseY, button);
            pageUp.mouseClicked(mouseX, mouseY, button);

            searchTab.mouseClicked(mouseX, mouseY, button);
            favoriteTab.mouseClicked(mouseX, mouseY, button);
            inventoryTab.mouseClicked(mouseX, mouseY, button);
            globalTab.mouseClicked(mouseX, mouseY, button);
        }

        opener.mouseClicked(mouseX, mouseY, button);
        favoriteButton.mouseClicked(mouseX, mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    ArrayList<String> invChangeHandler = new ArrayList<>();

    @Inject(at = @At("RETURN"), method = "drawBackground")
    private void onDrawBackground(CallbackInfo ci) {
        if (!config.enableAnvilModification) return;
        if (invChangeHandler.isEmpty()) {
            invChangeHandler = getInventory();
            return;
        }
        ArrayList<String> temp = getInventory();
        int i = 0;
        boolean equal = true;
        while (i < temp.size() || i < invChangeHandler.size()) {
            if (invChangeHandler.size() > i && temp.size() > i) {
                if (!invChangeHandler.get(i).equals(temp.get(i))) {
                    equal = false;
                    break;
                }
            } else {
                equal = false;
                break;
            }
            i++;
        }
        if (!equal) {
            invChangeHandler = temp;
            if (open) {
                screenUpdate(page);
            }
        }
    }

    @Inject(at = @At("RETURN"), method = "onSlotUpdate")
    private void itemUpdate(ScreenHandler handler, int slotId, ItemStack stack, CallbackInfo ci) {
        if (!config.enableAnvilModification) return;
        if (slotId == 0 || slotId == 1) {
            ghostCraft.reset();
        }
        if (slotId != 0) return;
        if (stack.isEmpty()) {
            currentItem = "air";
            searchField.setText("");
            searchField.setFocusUnlocked(false);
            remove(searchField);
            searchField.setFocused(false);
        } else {
            currentItem = ConfigManager.getIdAndPath(stack.getItem());
            itemAfterUpdate = stack.copy();
            searchField.setFocusUnlocked(true);
            currentTab = Tabs.SEARCH;
            favoriteButtonsUpdate(nameField.getText());
        }
        if (open) {
            if (currentTab != Tabs.GLOBAL) {
                screenUpdate();
            } else {
                updateSearchRequest();
            }
        } else {
            screenUpdate();
        }
    }

    private ArrayList<String> getInventory() {
        ArrayList<String> inventoryList = new ArrayList<>();
        assert MinecraftClient.getInstance().player != null;
        PlayerInventory inventory = MinecraftClient.getInstance().player.getInventory();
        for (ItemStack itemStack : inventory.main) {
            inventoryList.add(ConfigManager.getIdAndPath(itemStack.getItem()));
        }
        inventoryList.add(currentItem);
        return inventoryList;
    }

    @Inject(at = @At("HEAD"), method = "drawForeground")
    private void frameUpdate(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        if (!config.enableAnvilModification) return;
        int xScreenOffset = (this.width - backgroundWidth) / 2;
        int yScreenOffset = (this.height - backgroundHeight) / 2;
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(-xScreenOffset, -yScreenOffset, 0);
        opener.render(context, mouseX, mouseY, 0);
        favoriteButton.render(context, mouseX, mouseY, 0);
        matrices.pop();
        if (!open) {
            searchField.setFocused(false);
            return;
        }
        RenderSystem.enableDepthTest();
        context.drawTexture(MENU_TEXTURE, -menuWidth + menuXOffset, 0, 0, 0, 0, menuWidth, menuHeight, menuWidth, menuHeight);
        matrices.push();
        matrices.translate(-xScreenOffset, -yScreenOffset, 0);
        for (RenameButtonHolder renameButtonHolder : buttons) {
            if (renameButtonHolder.isActive()) {
                renameButtonHolder.getButton().render(context, mouseX, mouseY, 0);
                renameButtonHolder.drawElements(context);
            }
        }
        searchTab.render(context, mouseX, mouseY, 0);
        favoriteTab.render(context, mouseX, mouseY, 0);
        inventoryTab.render(context, mouseX, mouseY, 0);
        globalTab.render(context, mouseX, mouseY, 0);

        pageDown.render(context, mouseX, mouseY, 0);
        pageUp.render(context, mouseX, mouseY, 0);
        matrices.pop();

        Graphics.renderText(context, pageCount, menuWidth / -2 + menuXOffset, backgroundHeight - (config.viewMode == RenameButtonHolder.ViewMode.GRID ? 26 : 22), false, true);
        if (searchField != null && !searchField.isFocused() && searchField.getText().isEmpty()) {
            Graphics.renderText(context, SEARCH_HINT_TEXT, -1, -menuWidth + searchFieldXOffset, searchFieldXOffset - 8, true, false);
        }
        ghostCraft.render(context, mouseX - xScreenOffset, mouseY - yScreenOffset);

        if (currentRenameList.isEmpty()) {
            String key;
            if (currentItem.equals("air") && (currentTab == Tabs.FAVORITE || currentTab == Tabs.SEARCH)) {
                key = "putItem";
            } else {
                key = currentTab == Tabs.FAVORITE ? "noFavoriteRenamesFound" : "noRenamesFound";
            }
            Graphics.renderText(context, Text.translatable("rprenames.gui." + key).copy().fillStyle(Style.EMPTY.withItalic(true).withColor(Formatting.GRAY)), -1, (-menuWidth + menuXOffset) / 2, 37, true, true);
        } else {
            for (RenameButtonHolder renameButtonHolder : buttons) {
                if (renameButtonHolder.getButton() != null && renameButtonHolder.getButton().isMouseOver(mouseX, mouseY) && renameButtonHolder.isActive()) {
                    ArrayList<Text> lines = new ArrayList<>(renameButtonHolder.getTooltip());
                    if (!renameButtonHolder.isCEM() && config.enablePreview) {
                        if (!hasShiftDown() && !config.playerPreviewByDefault) {
                            if (!config.disablePlayerPreviewTips) {
                                lines.add(Text.translatable("rprenames.gui.tooltipHint.playerPreviewTip.holdShift").copy().fillStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(true)));
                            }
                        } else if (hasShiftDown() != config.playerPreviewByDefault) {
                            searchField.setFocused(false);
                            if (!config.disablePlayerPreviewTips) {
                                lines.add(Text.translatable("rprenames.gui.tooltipHint.playerPreviewTip.pressF").copy().fillStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(true)));
                            }
                        }
                    }
                    if ((currentTab == Tabs.INVENTORY || currentTab == Tabs.GLOBAL) && (config.slotHighlightColorALPHA > 0 && config.highlightSlot)) {
                        renameButtonHolder.highlightSlot(context, getInventory(), currentItem, highlightColor);
                    }
                    matrices.push();
                    matrices.translate(-xScreenOffset, -yScreenOffset, 0);
                    context.drawTooltip(MinecraftClient.getInstance().textRenderer, lines, mouseX, mouseY);
                    if (config.enablePreview) {
                        renameButtonHolder.drawPreview(context, mouseX, mouseY, 52, 52, config.scaleFactorItem, config.scaleFactorEntity);
                    }
                    matrices.pop();
                }
            }
        }
        RenderSystem.disableDepthTest();
    }

    private void showButtons() {
        for (RenameButtonHolder renameButtonHolder : buttons) {
            int orderOnPage = renameButtonHolder.getOrderOnPage();
            if (orderOnPage + page * maxPageElements <= currentRenameListSize - 1) {
                renameButtonHolder.setActive(true);
            }
        }
    }

    private void hideButtons() {
        for (RenameButtonHolder renameButtonHolder : buttons) {
            renameButtonHolder.setActive(false);
        }
    }

    private void removeWidgets() {
        hideButtons();
        pageCount = Text.empty();
    }

    private static void putInAnvil(int slotInInventory, MinecraftClient client) {
        assert client.player != null;
        int syncId = client.player.currentScreenHandler.syncId;
        assert client.interactionManager != null;
        client.interactionManager.clickSlot(syncId, 0, slotInInventory, SlotActionType.SWAP, client.player);
    }

    public void moveToInventory(int slot, PlayerInventory inventory) {
        assert (client != null ? client.player : null) != null;
        ItemStack stack = client.player.currentScreenHandler.slots.get(slot).getStack();
        if (!stack.isEmpty()) {
            int syncId = client.player.currentScreenHandler.syncId;
            assert client.interactionManager != null;
            if (inventory.getOccupiedSlotWithRoomForStack(stack) != -1 || inventory.getEmptySlot() != -1) {
                client.interactionManager.clickSlot(syncId, slot, 0, SlotActionType.QUICK_MOVE, client.player);
                moveToInventory(slot, inventory);
            } else {
                client.interactionManager.clickSlot(syncId, slot, 99, SlotActionType.THROW, client.player);
            }
        }
    }

    private void createButton(int orderOnPage, Rename rename) {
        assert MinecraftClient.getInstance().player != null;
        PlayerInventory inventory = MinecraftClient.getInstance().player.getInventory();
        String item = rename.getItem();
        boolean isInInventory = getInventory().contains(item);
        int indexInInventory = getInventory().indexOf(item);
        boolean asCurrentItem = item.equals(currentItem);
        boolean favorite = Rename.isFavorite(item, rename.getName());

        ArrayList<Text> tooltip = new ArrayList<>();
        tooltip.add(Text.of(rename.getName()));
        if (currentTab == Tabs.INVENTORY) {
            tooltip.add(Text.of(config.translateItemNames ? Text.translatable(Registries.ITEM.get(new Identifier(item)).getTranslationKey()).getString() : item).copy().fillStyle(Style.EMPTY.withColor(Formatting.DARK_AQUA)));
        } else if (currentTab == Tabs.GLOBAL) {
            tooltip.add(Text.of(config.translateItemNames ? Text.translatable(Registries.ITEM.get(new Identifier(item)).getTranslationKey()).getString() : item).copy().fillStyle(Style.EMPTY.withColor(getInventory().contains(item) ? Formatting.DARK_AQUA : Formatting.RED)));
        }
        if (item.equals(ConfigManager.getIdAndPath(Items.NAME_TAG)) && rename.isCEM()) {
            Identifier mob = new Identifier(rename.getMob().entity());
            var entityType = Registries.ENTITY_TYPE.get(mob);
            tooltip.add(Text.of(config.translateMobNames ? Text.translatable(entityType.getTranslationKey()).getString() : rename.getMob().entity()).copy().fillStyle(Style.EMPTY.withColor(Formatting.YELLOW)));
        }

        boolean enoughStackSize;
        boolean enoughDamage;
        boolean hasEnchant = false;
        boolean hasEnoughLevels = false;

        if (rename.getStackSize() != null && rename.getStackSize() > 1) {
            if (currentTab == Tabs.INVENTORY || currentTab == Tabs.GLOBAL) {
                if (asCurrentItem) {
                    enoughStackSize = Rename.isInBounds(itemAfterUpdate.getCount(), rename.getOriginalStackSize());
                } else if (isInInventory) {
                    enoughStackSize = Rename.isInBounds(inventory.main.get(indexInInventory).getCount(), rename.getOriginalStackSize());
                } else {
                    enoughStackSize = true;
                }
            } else {
                enoughStackSize = Rename.isInBounds(itemAfterUpdate.getCount(), rename.getOriginalStackSize());
            }

            if (config.showExtraProperties) {
                if (config.showOriginalProperties) {
                    tooltip.add(Text.of("stackSize").copy().fillStyle(Style.EMPTY.withColor(Formatting.GOLD))
                            .append(Text.of("=").copy().fillStyle(Style.EMPTY.withColor(Formatting.GRAY)))
                            .append(Text.of(rename.getOriginalStackSize()).copy().fillStyle(Style.EMPTY.withColor(enoughStackSize ? Formatting.GREEN : Formatting.DARK_RED))));
                } else {
                    tooltip.add(Text.of(Text.translatable("rprenames.gui.tooltipHint.stackSize").getString() + " " + rename.getStackSize()).copy().fillStyle(Style.EMPTY.withColor(enoughStackSize ? Formatting.GRAY : Formatting.DARK_RED)));
                }
            }
        } else {
            enoughStackSize = true;
        }

        if (rename.getDamage() != null && rename.getDamage() > 0) {
            if (currentTab == Tabs.INVENTORY || currentTab == Tabs.GLOBAL) {
                if (asCurrentItem) {
                    enoughDamage = Rename.isInBounds(itemAfterUpdate.getDamage(), rename.getOriginalDamage(), item);
                } else if (isInInventory) {
                    enoughDamage = Rename.isInBounds(inventory.main.get(indexInInventory).getDamage(), rename.getOriginalDamage(), item);
                } else {
                    enoughDamage = true;
                }
            } else {
                enoughDamage = Rename.isInBounds(itemAfterUpdate.getDamage(), rename.getOriginalStackSize());
            }
            if (config.showExtraProperties) {
                if (config.showOriginalProperties) {
                    tooltip.add(Text.of("damage").copy().fillStyle(Style.EMPTY.withColor(Formatting.GOLD))
                            .append(Text.of("=").copy().fillStyle(Style.EMPTY.withColor(Formatting.GRAY)))
                            .append(Text.of(rename.getOriginalDamage()).copy().fillStyle(Style.EMPTY.withColor(enoughDamage ? Formatting.GREEN : Formatting.DARK_RED))));
                } else {
                    tooltip.add(Text.of(Text.translatable("rprenames.gui.tooltipHint.damage").getString() + " " + rename.getDamage()).copy().fillStyle(Style.EMPTY.withColor(enoughDamage ? Formatting.GRAY : Formatting.DARK_RED)));
                }
            }
        } else {
            enoughDamage = true;
        }

        if (rename.getEnchantment() != null) {
            Map<Enchantment, Integer> enchantments = Maps.newLinkedHashMap();
            if (currentTab == Tabs.INVENTORY || currentTab == Tabs.GLOBAL) {
                if (asCurrentItem) {
                    enchantments = EnchantmentHelper.fromNbt(itemAfterUpdate.getEnchantments());
                } else if (isInInventory) {
                    enchantments = EnchantmentHelper.fromNbt(inventory.main.get(indexInInventory).getEnchantments());
                }
            } else {
                enchantments = EnchantmentHelper.fromNbt(itemAfterUpdate.getEnchantments());
            }
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                Enchantment enchantment = entry.getKey();
                String enchantName = rename.getEnchantment();
                if (!enchantName.contains(":")) {
                    enchantName = Identifier.DEFAULT_NAMESPACE + Identifier.NAMESPACE_SEPARATOR + enchantName;
                }
                if (Objects.requireNonNull(Registries.ENCHANTMENT.getId(enchantment)).toString().equals(enchantName)) {
                    hasEnchant = true;
                    if (Rename.isInBounds(entry.getValue(), rename.getOriginalEnchantmentLevel())) {
                        hasEnoughLevels = true;
                        break;
                    }
                }
            }
            if (currentTab == Tabs.GLOBAL && !isInInventory) {
                hasEnchant = true;
                hasEnoughLevels = true;
            }
            if (config.showExtraProperties) {
                if (config.showOriginalProperties) {
                    tooltip.add(Text.of("enchantmentIDs").copy().fillStyle(Style.EMPTY.withColor(Formatting.GOLD))
                            .append(Text.of("=").copy().fillStyle(Style.EMPTY.withColor(Formatting.GRAY)))
                            .append(Text.of(rename.getOriginalEnchantment()).copy().fillStyle(Style.EMPTY.withColor(hasEnchant ? Formatting.GREEN : Formatting.DARK_RED))));
                    if (rename.getOriginalEnchantmentLevel() != null) {
                        tooltip.add(Text.of("enchantmentLevels").copy().fillStyle(Style.EMPTY.withColor(Formatting.GOLD))
                                .append(Text.of("=").copy().fillStyle(Style.EMPTY.withColor(Formatting.GRAY)))
                                .append(Text.of(rename.getOriginalEnchantmentLevel()).copy().fillStyle(Style.EMPTY.withColor(hasEnoughLevels ? Formatting.GREEN : Formatting.DARK_RED))));
                    }
                } else {
                    Identifier enchant = Identifier.splitOn(rename.getEnchantment(), ':');
                    String namespace = enchant.getNamespace();
                    String path = enchant.getPath();
                    Text translatedEnchant = Text.translatable("enchantment." + namespace + "." + path);
                    Text translatedEnchantLevel = Text.translatable("enchantment.level." + rename.getEnchantmentLevel());
                    tooltip.add(Text.of(Text.translatable("rprenames.gui.tooltipHint.enchantment").getString() + " " + translatedEnchant.getString() + " " + translatedEnchantLevel.getString()).copy().fillStyle(Style.EMPTY.withColor(hasEnchant && hasEnoughLevels ? Formatting.GRAY : Formatting.DARK_RED)));
                }
            }
        } else {
            hasEnchant = true;
            hasEnoughLevels = true;
        }

        if (config.showPackName) {
            if (rename.getPackName() != null) {
                tooltip.add(Text.of(rename.getPackName()).copy().fillStyle(Style.EMPTY.withColor(Formatting.GOLD)));
            }
        }

        if (config.showNbtDisplayName && currentTab != Tabs.FAVORITE) {
            if (rename.getOriginalNbtDisplayName() != null) {
                tooltip.add(Text.of("nbt.display.Name=" + rename.getOriginalNbtDisplayName()).copy().fillStyle(Style.EMPTY.withColor(Formatting.BLUE)));
            }
        }
        int x;
        int y;
        if (config.viewMode == RenameButtonHolder.ViewMode.LIST) {
            x = this.width / 2 - backgroundWidth / 2 - menuWidth + menuXOffset + buttonXOffset;
            y = this.height / 2 - 53 + (orderOnPage * (RenameButton.buttonHeightList + buttonOffsetY));
        } else {
            x = this.width / 2 - backgroundWidth / 2 - menuWidth + menuXOffset + buttonXOffset + 1 + (orderOnPage % 5 * RenameButton.buttonWidthGrid);
            y = this.height / 2 - backgroundHeight / 2 + 31 + (orderOnPage / 5 * RenameButton.buttonHeightGrid);
        }
        RenameButton renameButton = new RenameButton(x, y, config.viewMode, favorite,
                indexInInventory, isInInventory, asCurrentItem,
                inventory, rename,
                enoughStackSize, enoughDamage,
                hasEnchant, hasEnoughLevels);
        buttons.get(orderOnPage).setParameters(renameButton, rename, page, tooltip);
    }

    private void defineButtons() {
        hideButtons();
        for (int n = 0; n < maxPageElements; n++) {
            if (n + page * maxPageElements <= currentRenameListSize - 1) {
                createButton(n, currentRenameList.get(n + page * maxPageElements));
            }
        }
    }

    private void updatePageWidgets() {
        pageDown.active = page != 0;
        pageUp.active = (page + 1) * maxPageElements <= currentRenameListSize - 1;
        pageCount = Text.of(page + 1 + "/" + (currentRenameList.size() + maxPageElements - 1) / maxPageElements);
    }

    private void onSearch(String search) {
        searchTag = search;
        if (open) {
            updateSearchRequest();
        }
    }

    private ArrayList<Rename> search(ArrayList<Rename> list, String match) {
        ArrayList<Rename> cutList = new ArrayList<>();
        if (match.startsWith("#")) {
            String matchTag = match.substring(1);
            if (matchTag.contains(" ") && !matchTag.toUpperCase(Locale.ROOT).contains("#REGEX:") && !matchTag.toUpperCase(Locale.ROOT).contains("#IREGEX:")) {
                matchTag = matchTag.substring(0, matchTag.indexOf(" "));
            } else if (matchTag.contains(" #")) {
                matchTag = matchTag.substring(0, matchTag.indexOf(" #"));
            }
            if (matchTag.toUpperCase(Locale.ROOT).startsWith("REGEX:") || matchTag.toUpperCase(Locale.ROOT).startsWith("IREGEX:")) {
                String regex = matchTag;
                boolean caseInsensitive = false;
                if (matchTag.toUpperCase(Locale.ROOT).startsWith("I")) {
                    regex = regex.substring(1);
                    caseInsensitive = true;
                }
                regex = regex.substring(6);

                boolean isRegex;
                try {
                    Pattern.compile(regex);
                    isRegex = true;
                } catch (PatternSyntaxException e) {
                    isRegex = false;
                }

                if (isRegex) {
                    for (Rename r : list) {
                        if (caseInsensitive ? r.getName().toUpperCase(Locale.ROOT).matches(regex.toUpperCase(Locale.ROOT)) : r.getName().matches(regex)) {
                            cutList.add(r);
                        }
                    }
                }
            } else if (matchTag.toUpperCase(Locale.ROOT).startsWith("PACK:") || matchTag.toUpperCase(Locale.ROOT).startsWith("PACKNAME:")) {
                String packName = matchTag.substring(4);
                while (packName.charAt(0) != ':') {
                    packName = packName.substring(1);
                }
                packName = packName.substring(1);
                for (Rename r : list) {
                    if (r.getPackName() != null && r.getPackName().replace(" ", "_").toUpperCase(Locale.ROOT).contains(packName.toUpperCase(Locale.ROOT))) {
                        cutList.add(r);
                    }
                }
            } else if (matchTag.toUpperCase(Locale.ROOT).startsWith("ITEM:")) {
                String itemName = matchTag.substring(5);
                for (Rename r : list) {
                    if (r.getItem() != null && r.getItem().toUpperCase(Locale.ROOT).contains(itemName.toUpperCase(Locale.ROOT))) {
                        cutList.add(r);
                    }
                }
            } else if (matchTag.toUpperCase(Locale.ROOT).startsWith("STACKSIZE:") || matchTag.toUpperCase(Locale.ROOT).startsWith("STACK:") || matchTag.toUpperCase(Locale.ROOT).startsWith("SIZE:")) {
                String stackSize = matchTag.toUpperCase(Locale.ROOT).substring(4);
                while (stackSize.charAt(0) != ':') {
                    stackSize = stackSize.substring(1);
                }
                stackSize = stackSize.substring(1);
                if (stackSize.matches("[0-9]+")) {
                    for (Rename r : list) {
                        if (Rename.isInBounds(Integer.parseInt(stackSize), r.getOriginalStackSize())) {
                            cutList.add(r);
                        }
                    }
                }
            } else if (matchTag.toUpperCase(Locale.ROOT).startsWith("DAMAGE:")) {
                String damage = matchTag.substring(7);
                if (damage.matches("[0-9]+")) {
                    for (Rename r : list) {
                        if (Rename.isInBounds(Integer.parseInt(damage), r.getOriginalDamage(), r.getItem())) {
                            cutList.add(r);
                        }
                    }
                }
            } else if (matchTag.toUpperCase(Locale.ROOT).startsWith("ENCH:") || matchTag.toUpperCase(Locale.ROOT).startsWith("ENCHANT:") || matchTag.toUpperCase(Locale.ROOT).startsWith("ENCHANTMENT:")) {
                String enchant = matchTag.toUpperCase(Locale.ROOT).substring(4);
                while (enchant.charAt(0) != ':') {
                    enchant = enchant.substring(1);
                }
                enchant = enchant.substring(1);
                for (Rename r : list) {
                    if (r.getEnchantment() != null) {
                        ArrayList<String> split = Rename.split(r.getOriginalEnchantment());
                        for (String s : split) {
                            if (s.toUpperCase(Locale.ROOT).contains(enchant)) {
                                cutList.add(r);
                                break;
                            }
                        }
                    }
                }
            } else if (matchTag.toUpperCase(Locale.ROOT).startsWith("FAV:") || matchTag.toUpperCase(Locale.ROOT).startsWith("FAVORITE:")) {
                for (Rename r : list) {
                    if (Rename.isFavorite(r.getItem(), r.getName())) {
                        cutList.add(r);
                    }
                }
            }
            if (match.substring(1).contains(" ") && !matchTag.toUpperCase(Locale.ROOT).contains("#REGEX:") && !matchTag.toUpperCase(Locale.ROOT).contains("#IREGEX:")) {
                cutList = search(cutList, match.substring(match.indexOf(" ") + 1));
            } else if (match.substring(1).contains(" #")) {
                cutList = search(cutList, match.substring(match.indexOf(" #") + 1));
            }
        } else {
            if (match.startsWith("\\#")) {
                match = match.substring(1);
            }
            boolean isRegex = false;
            try {
                Pattern.compile(match);
                isRegex = true;
            } catch (Exception ignored) {
            }
            for (Rename r : list) {
                if (r.getName().toUpperCase(Locale.ROOT).contains(match.toUpperCase(Locale.ROOT)) || (isRegex && r.getName().toUpperCase(Locale.ROOT).matches(match.toUpperCase(Locale.ROOT)))) {
                    cutList.add(r);
                }
            }
        }
        return cutList;
    }
}