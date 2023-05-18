package com.HiWord9.RPRenames.mixin;

import com.HiWord9.RPRenames.configGeneration.CEMList;
import com.HiWord9.RPRenames.RPRenames;
import com.HiWord9.RPRenames.Rename;
import com.HiWord9.RPRenames.configGeneration.ConfigManager;
import com.HiWord9.RPRenames.modConfig.ModConfig;
import com.google.gson.Gson;
import io.github.cottonmc.cotton.gui.widget.WItemSlot;
import io.github.cottonmc.cotton.gui.widget.WLabel;
import io.github.cottonmc.cotton.gui.widget.data.HorizontalAlignment;
import io.github.cottonmc.cotton.gui.widget.icon.ItemIcon;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.*;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin extends Screen {

	private static final ModConfig config = ModConfig.INSTANCE;
	boolean isOnServer = !MinecraftClient.getInstance().isInSingleplayer();

	@Shadow private TextFieldWidget nameField;

	@Shadow public abstract void removed();

	protected AnvilScreenMixin(Text title) {
		super(title);
	}

	boolean open = false;

	private static final Identifier RENAMES_MENU = new Identifier(RPRenames.MOD_ID,"textures/gui/rename_menu.png");
	int menuWidth = 256;
	int menuHeight = 206;
	private static final Identifier RENAMES_BUTTON = new Identifier(RPRenames.MOD_ID,"textures/gui/rename_button.png");
	int page = 0;
	int currentRenameListSize;

	String currentItem = null;
	TexturedButtonWidget background;
	TexturedButtonWidget opener;
	TexturedButtonWidget openerOpened;
	TexturedButtonWidget openerFavoriteOnly;
	TexturedButtonWidget button1;
	TexturedButtonWidget button2;
	TexturedButtonWidget button3;
	TexturedButtonWidget button4;
	TexturedButtonWidget button5;
	TexturedButtonWidget searchTab;
	TexturedButtonWidget favoriteTab;
	TexturedButtonWidget inventoryTab;
	TexturedButtonWidget searchTab2;
	TexturedButtonWidget favoriteTab2;
	TexturedButtonWidget inventoryTab2;
	int tabNum = 1;
	TexturedButtonWidget addToFavorite;
	TexturedButtonWidget removeFromFavorite;
	WLabel button1text = new WLabel(Text.of(""),0xffffff);
	WLabel button2text = new WLabel(Text.of(""),0xffffff);
	WLabel button3text = new WLabel(Text.of(""),0xffffff);
	WLabel button4text = new WLabel(Text.of(""),0xffffff);
	WLabel button5text = new WLabel(Text.of(""),0xffffff);
	WLabel button1textShadow = new WLabel(Text.of(""),0x3f3f3f);
	WLabel button2textShadow = new WLabel(Text.of(""),0x3f3f3f);
	WLabel button3textShadow = new WLabel(Text.of(""),0x3f3f3f);
	WLabel button4textShadow = new WLabel(Text.of(""),0x3f3f3f);
	WLabel button5textShadow = new WLabel(Text.of(""),0x3f3f3f);
	TexturedButtonWidget pageDown;
	TexturedButtonWidget pageUp;
	WLabel pageCount = new WLabel(Text.of(""),0xffffff);
	Rename currentRenameList;

	TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
	TextFieldWidget searchField;

	WItemSlot iconSlot1 = new WItemSlot(new PlayerInventory(MinecraftClient.getInstance().player),51,1,1,false);
	WItemSlot iconSlot2 = new WItemSlot(new PlayerInventory(MinecraftClient.getInstance().player),52,1,1,false);
	WItemSlot iconSlot3 = new WItemSlot(new PlayerInventory(MinecraftClient.getInstance().player),53,1,1,false);
	WItemSlot iconSlot4 = new WItemSlot(new PlayerInventory(MinecraftClient.getInstance().player),54,1,1,false);
	WItemSlot iconSlot5 = new WItemSlot(new PlayerInventory(MinecraftClient.getInstance().player),55,1,1,false);

	ItemStack icon1;
	ItemStack icon2;
	ItemStack icon3;
	ItemStack icon4;
	ItemStack icon5;

	ItemStack iconAfterUpdate1;
	ItemStack iconAfterUpdate2;
	ItemStack iconAfterUpdate3;
	ItemStack iconAfterUpdate4;
	ItemStack iconAfterUpdate5;

	CallbackInfo ci;

	ArrayList<ArrayList<String>> mobName = new ArrayList<>();

	@Inject(at = @At("RETURN"), method = "setup")
	private void init(CallbackInfo ci) {
		if (config.enableAnvilModification) {
			this.ci = ci;

			open = false;

			background = new TexturedButtonWidget(this.width / 2 - 200 - 28, this.height / 2 - 83, 110 + 28, 166, 118, 0, 0, RENAMES_MENU, 256, 206, null);
			background.active = false;

			pageDown = new TexturedButtonWidget(this.width / 2 - 200 + 10 - 28, this.height / 2 - 83 + 140, 30, 16, 58, 40, 16, RENAMES_MENU, 256, 206, (button -> {
				page--;
				hideButtons();
				buttonsDefine();
				addDrawableChild(background);
				showButtons();
				tabsUpdate();
				updatePageWidgets();
				if (page == 0) {
					button.active = false;
				}
				pageUp.active = true;
			}));
			pageUp = new TexturedButtonWidget(this.width / 2 - 200 + 10 + 60, this.height / 2 - 83 + 140, 30, 16, 88, 40, 16, RENAMES_MENU, 256, 206, (button -> {
				page++;
				hideButtons();
				buttonsDefine();
				addDrawableChild(background);
				showButtons();
				tabsUpdate();
				updatePageWidgets();
				pageDown.active = true;
				if (5 + page * 5 > currentRenameListSize - 1) {
					button.active = false;
				}
			}));

			boolean clientConfigReadable = RPRenames.configClientFolder.exists();
			boolean serverConfigReadable = RPRenames.configServerFolder.exists();
			if (!isOnServer) {
				serverConfigReadable = false;
			}

			if (!clientConfigReadable && !serverConfigReadable) {
				Text noConfigText = Text.translatable("rprenames.configrenames.notfound", RPRenames.configPath);
				opener = new TexturedButtonWidget(this.width / 2 - 83, this.height / 2 - 38, 20, 20, 0, 0, 20, RENAMES_BUTTON, 20, 100, (button) -> {
					if (!open) {
						open = true;
						page = 0;
						System.out.println("[RPR] Opened RP Renames Menu");
						screenUpdate();
					} else {
						open = false;
						clearAll();
						searchField.setTextFieldFocused(false);
						searchField.setFocusUnlocked(false);
						searchField.setText("");
						remove(searchField);
						nameField.setTextFieldFocused(true);
						nameField.setFocusUnlocked(false);
						remove(openerOpened);
						remove(searchTab);
						remove(favoriteTab);
						remove(searchTab2);
						remove(favoriteTab2);
						tabNum = 1;
						System.out.println("[RPR] Closed RP Renames Menu");
					}
				}, new ButtonWidget.TooltipSupplier() {
					public void onTooltip(ButtonWidget buttonWidget, MatrixStack matrixStack, int i, int j) {
						renderTooltip(matrixStack, noConfigText, i, j);
					}

					public void supply(Consumer<Text> consumer) {
						consumer.accept(noConfigText);
					}
				}, noConfigText);
			} else {
				opener = new TexturedButtonWidget(this.width / 2 - 83, this.height / 2 - 38, 20, 20, 0, 0, 20, RENAMES_BUTTON, 20, 100, (button) -> {
					if (!open) {
						open = true;
						page = 0;
						System.out.println("[RPR] Opened RP Renames Menu");
						screenUpdate();
					} else {
						open = false;
						clearAll();
						searchField.setTextFieldFocused(false);
						searchField.setFocusUnlocked(false);
						searchField.setText("");
						remove(searchField);
						nameField.setTextFieldFocused(true);
						nameField.setFocusUnlocked(false);
						remove(openerOpened);
						remove(searchTab);
						remove(favoriteTab);
						remove(searchTab2);
						remove(favoriteTab2);
						tabNum = 1;
						System.out.println("[RPR] Closed RP Renames Menu");
					}
				});
			}

			openerOpened = new TexturedButtonWidget(this.width / 2 - 83, this.height / 2 - 38, 20, 20, 0, 60, 20, RENAMES_BUTTON, 20, 100, null);
			openerFavoriteOnly = new TexturedButtonWidget(this.width / 2 - 83, this.height / 2 - 38, 20, 20, 0, 40, 0, RENAMES_BUTTON, 20, 100, null);

			addDrawableChild(opener);

			searchTab = new TexturedButtonWidget(this.width / 2 - 200 - 28 - 30, this.height / 2 - 83 + 3, 33, 26, 48 + 4, 88, 0, RENAMES_MENU, menuWidth, menuHeight, button -> {
				tabNum = 1;
				remove(searchTab2);
				remove(favoriteTab2);
				remove(inventoryTab2);
				addDrawableChild(searchTab2);
				screenUpdate();
			});
			favoriteTab = new TexturedButtonWidget(this.width / 2 - 200 - 28 - 30, this.height / 2 - 83 + 3 + 31, 33, 26, 48 + 4, 88 + 26, 0, RENAMES_MENU, menuWidth, menuHeight, button -> {
				tabNum = 2;
				remove(searchTab2);
				remove(favoriteTab2);
				remove(inventoryTab2);
				addDrawableChild(favoriteTab2);
				screenUpdate();
			});
			inventoryTab = new TexturedButtonWidget(this.width / 2 - 200 - 28 - 30, this.height / 2 - 83 + 3 + 31 + 31, 33, 26, 48 + 4, 88 + 26, 0, RENAMES_MENU, menuWidth, menuHeight, button -> {
				tabNum = 3;
				remove(searchTab2);
				remove(favoriteTab2);
				remove(inventoryTab2);
				addDrawableChild(inventoryTab2);
				screenUpdate();
			});
			searchTab2 = new TexturedButtonWidget(this.width / 2 - 200 - 28 - 30, this.height / 2 - 83 + 3, 33, 26, 48 + 35 + 2, 88, 0, RENAMES_MENU, menuWidth, menuHeight, null);
			favoriteTab2 = new TexturedButtonWidget(this.width / 2 - 200 - 28 - 30, this.height / 2 - 83 + 3 + 31, 33, 26, 48 + 35 + 2, 88 + 26, 0, RENAMES_MENU, menuWidth, menuHeight, null);
			inventoryTab2 = new TexturedButtonWidget(this.width / 2 - 200 - 28 - 30, this.height / 2 - 83 + 3 + 31 + 31, 33, 26, 48 + 35 + 2, 88 + 26, 0, RENAMES_MENU, menuWidth, menuHeight, null);
			searchTab2.active = false;
			favoriteTab2.active = false;
			inventoryTab2.active = false;

			addToFavorite = new TexturedButtonWidget(this.width / 2 + config.favoritePosX, this.height / 2 + config.favoritePosY, 9, 9, 43, 88 + 9, 0, RENAMES_MENU, menuWidth, menuHeight, button -> {
				String favoriteName = nameField.getText();

				String item = currentItem;

				File currentFile = new File(RPRenames.configPathFavorite + item + ".json");
				boolean nameExist = false;
				if (currentFile.exists() && favoriteName != null) {
					Rename alreadyExist = ConfigManager.configRead(currentFile);
					String[] ae = alreadyExist.getName();
					for (String s : ae) {
						if (favoriteName.equals(s)) {
							nameExist = true;
							break;
						}
					}
					if (!nameExist) {
						int AEsize = ae.length;
						String[] newConfig = new String[AEsize + 1];
						int h = 0;
						while (h < AEsize) {
							newConfig[h] = ae[h];
							h++;
						}
						newConfig[h] = favoriteName;

						Rename newRename = new Rename(newConfig);
						ArrayList<Rename> listFiles = new ArrayList<>();
						listFiles.add(newRename);

						try {
							FileWriter fileWriter = new FileWriter(currentFile);
							Gson gson = new Gson();
							gson.toJson(listFiles, fileWriter);
							fileWriter.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				} else {
					if (favoriteName != null) {
						try {
							new File(RPRenames.configPathFavorite).mkdirs();
							System.out.println("[RPR] Created new file for favorites config: " + RPRenames.configPathFavorite + item + ".json");
							ArrayList<Rename> listNames = new ArrayList<>();
							Rename name1 = new Rename(new String[]{favoriteName});
							listNames.add(name1);
							FileWriter fileWriter = new FileWriter(currentFile);
							Gson gson = new Gson();
							gson.toJson(listNames, fileWriter);
							fileWriter.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
				newNameEntered(nameField.getText(), ci);
				if (tabNum == 1 && open) {
					buttonsDefine();
					showButtons();
				}
				if (tabNum == 2) {
					screenUpdate();
				}
			});
			removeFromFavorite = new TexturedButtonWidget(this.width / 2 + config.favoritePosX, this.height / 2 + config.favoritePosY, 9, 9, 43, 88, 0, RENAMES_MENU, menuWidth, menuHeight, button -> {
				String favoriteName = nameField.getText();

				String item = currentItem;

				File currentFile = new File(RPRenames.configPathFavorite + item + ".json");
				Rename alreadyExist = ConfigManager.configRead(currentFile);
				String[] ae = alreadyExist.getName();
				int n = 0;
				for (String s : ae) {
					if (favoriteName.equals(s)) {
						ae[n] = null;
					}
					n++;
				}
				int AEsize = ae.length;
				String[] newConfig = new String[AEsize - 1];
				int h = 0;
				int h2 = 0;
				while (h2 < newConfig.length) {
					if (ae[h] != null) {
						newConfig[h2] = ae[h];
						h++;
						h2++;
					} else {
						h++;
					}
				}

				Rename newRename = new Rename(newConfig);
				ArrayList<Rename> listFiles = new ArrayList<>();
				listFiles.add(newRename);

				try {
					FileWriter fileWriter = new FileWriter(currentFile);
					Gson gson = new Gson();
					gson.toJson(listFiles, fileWriter);
					fileWriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				newNameEntered(nameField.getText(), ci);
				if (tabNum == 1 && open) {
					buttonsDefine();
					showButtons();
				}
				if (tabNum == 2) {
					screenUpdate();
				}
			});

			searchField = new TextFieldWidget(renderer, this.width / 2 - 200 + 10 + 14 - 28, this.height / 2 - 83 + 30 - 22 + 5, 90 - 14 + 28, 10, Text.of(""));
			searchField.setChangedListener(this::onSearch);
			searchField.setDrawsBackground(false);

			screenUpdate();
		}
	}

	private void screenUpdate() {
		clearAll();
		opener.active = true;
		mobName.clear();
		if (currentItem != null) {
			File jsonRenamesClient = new File(RPRenames.configPathClient + RPRenames.configPathNameCIT + "/" + currentItem + ".json");
			File jsonRenamesServer = new File(RPRenames.configPathServer + RPRenames.configPathNameCIT + "/" + currentItem + ".json");
			File jsonRenamesFavorite = new File(RPRenames.configPathFavorite + currentItem + ".json");
			boolean clientConfigReadable = jsonRenamesClient.exists();
			boolean serverConfigReadable = jsonRenamesServer.exists();
			if (!isOnServer) {
				serverConfigReadable = false;
			}
			boolean clientConfigCEMReadable = RPRenames.configClientCEMFolder.exists();
			boolean serverConfigCEMReadable = RPRenames.configServerCEMFolder.exists();
			if (!isOnServer) {
				serverConfigCEMReadable = false;
			}
			if (clientConfigReadable || serverConfigReadable) {
				searchTab.active = true;
				if (tabNum == 1) {
					List<String> list1 = new ArrayList<>();
					List<String> list2 = new ArrayList<>();
					if (clientConfigReadable) {
						list1 = Arrays.stream(search(ConfigManager.configRead(jsonRenamesClient).getName(), searchField.getText())).toList();
					}
					if (serverConfigReadable) {
						list2 = Arrays.stream(search(ConfigManager.configRead(jsonRenamesServer).getName(), searchField.getText())).toList();
					}
					String[] list3 = new String[list1.size() + list2.size()];
					int n = 0;
					for (String s : list1) {
						list3[n] = s;
						n++;
					}
					for (String s : list2) {
						list3[n] = s;
						n++;
					}
					currentRenameList = new Rename(list3);
				} else if (tabNum == 2) {
					if (jsonRenamesFavorite.exists()) {
						currentRenameList = new Rename(search(ConfigManager.configRead(jsonRenamesFavorite).getName(), searchField.getText()));
					} else {
						currentRenameList = new Rename(new String[0]);
					}
				} else if (tabNum == 3) {
					currentRenameList = new Rename(new String[0]);
				}
				if (tabNum == 1 && currentItem.equals("name_tag")) {
					ArrayList<String> modelsArray = new ArrayList<>();
					if (clientConfigCEMReadable) {
						try {
							Files.walk(Path.of(RPRenames.configPathClient + RPRenames.configPathNameCEM), new FileVisitOption[0]).filter(path -> path.toString().endsWith(".json")).forEach(jsonFile -> {
								File file = new File(String.valueOf(jsonFile));
								for (String s : ConfigManager.configRead(file).getName()) {
									if (Arrays.stream(search(ConfigManager.configRead(file).getName(), searchField.getText())).toList().contains(s)) {
										if (!modelsArray.contains(s)) {
											modelsArray.add(s);
											ArrayList<String> nal = new ArrayList<>();
											nal.add(file.getName().substring(0, file.getName().length() - 5));
											mobName.add(nal);
										} else {
											int n = 0;
											for (String s2 : modelsArray) {
												if (s2.equals(s)) {
													break;
												}
												n++;
											}
											ArrayList<String> nal = mobName.get(n);
											nal.add(file.getName().substring(0, file.getName().length() - 5));
											mobName.set(n, nal);
										}
									}
								}
							});
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					if (serverConfigCEMReadable) {
						try {
							Files.walk(Path.of(RPRenames.configPathServer + RPRenames.configPathNameCEM), new FileVisitOption[0]).filter(path -> path.toString().endsWith(".json")).forEach(jsonFile -> {
								File file = new File(String.valueOf(jsonFile));
								for (String s : ConfigManager.configRead(file).getName()) {
									if (Arrays.stream(search(ConfigManager.configRead(file).getName(), searchField.getText())).toList().contains(s)) {
										if (!modelsArray.contains(s)) {
											modelsArray.add(s);
											ArrayList<String> nal = new ArrayList<>();
											nal.add(file.getName().substring(0, file.getName().length() - 5));
											mobName.add(nal);
										} else {
											int n = 0;
											for (String s2 : modelsArray) {
												if (s2.equals(s)) {
													break;
												}
												n++;
											}
											ArrayList<String> nal = mobName.get(n);
											nal.add(file.getName().substring(0, file.getName().length() - 5));
											mobName.set(n, nal);
										}
									}
								}
							});
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					ArrayList<String> citWithoutModels = new ArrayList<>();
					for (String s : currentRenameList.getName()) {
						if (!modelsArray.contains(s)) {
							citWithoutModels.add(s);
						}
					}
					String[] finalList = new String[citWithoutModels.size() + modelsArray.size()];
					int g = 0;
					for (String s : citWithoutModels) {
						finalList[g] = s;
						g++;
					}
					g = 0;
					for (String s : modelsArray) {
						finalList[citWithoutModels.size() + g] = s;
						g++;
					}
					if (g != 0) {
						currentRenameList = new Rename(finalList);
					}
				}

				currentRenameListSize = currentRenameList.getName().length;

				buttonsDefine();
				clearAll();

			} else if ((!clientConfigReadable && !serverConfigReadable) && (clientConfigCEMReadable || serverConfigCEMReadable) && currentItem.equals("name_tag")) {
				if (tabNum == 1) {
					ArrayList<String> modelsArray = new ArrayList<>();
					if (clientConfigCEMReadable) {
						try {
							Files.walk(Path.of(RPRenames.configPathClient + RPRenames.configPathNameCEM), new FileVisitOption[0]).filter(path -> path.toString().endsWith(".json")).forEach(jsonFile -> {
								File file = new File(String.valueOf(jsonFile));
								for (String s : ConfigManager.configRead(file).getName()) {
									if (Arrays.stream(search(ConfigManager.configRead(file).getName(), searchField.getText())).toList().contains(s)) {
										if (!modelsArray.contains(s)) {
											modelsArray.add(s);
											ArrayList<String> nal = new ArrayList<>();
											nal.add(file.getName().substring(0, file.getName().length() - 5));
											mobName.add(nal);
										} else {
											int n = 0;
											for (String s2 : modelsArray) {
												if (s2.equals(s)) {
													break;
												}
												n++;
											}
											ArrayList<String> nal = mobName.get(n);
											nal.add(file.getName().substring(0, file.getName().length() - 5));
											mobName.set(n, nal);
										}
									}
								}
							});
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					if (serverConfigCEMReadable) {
						try {
							Files.walk(Path.of(RPRenames.configPathServer + RPRenames.configPathNameCEM), new FileVisitOption[0]).filter(path -> path.toString().endsWith(".json")).forEach(jsonFile -> {
								File file = new File(String.valueOf(jsonFile));
								for (String s : ConfigManager.configRead(file).getName()) {
									if (Arrays.stream(search(ConfigManager.configRead(file).getName(), searchField.getText())).toList().contains(s)) {
										if (!modelsArray.contains(s)) {
											modelsArray.add(s);
											ArrayList<String> nal = new ArrayList<>();
											nal.add(file.getName().substring(0, file.getName().length() - 5));
											mobName.add(nal);
										} else {
											int n = 0;
											for (String s2 : modelsArray) {
												if (s2.equals(s)) {
													break;
												}
												n++;
											}
											ArrayList<String> nal = mobName.get(n);
											nal.add(file.getName().substring(0, file.getName().length() - 5));
											mobName.set(n, nal);
										}
									}
								}
							});
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					String[] finalList = new String[modelsArray.size()];
					int g = 0;
					for (String s : modelsArray) {
						finalList[g] = s;
						g++;
					}
					currentRenameList = new Rename(finalList);

				} else if (tabNum == 2) {
					if (jsonRenamesFavorite.exists()) {
						currentRenameList = new Rename(search(ConfigManager.configRead(jsonRenamesFavorite).getName(), searchField.getText()));
					} else {
						currentRenameList = new Rename(new String[0]);
					}
				} else if (tabNum == 3) {
					currentRenameList = new Rename(new String[0]);
				}

				currentRenameListSize = currentRenameList.getName().length;

				buttonsDefine();
				clearAll();
			} else {
				searchTab.active = false;
				inventoryTab.active = false;
				tabNum = 2;
				if (jsonRenamesFavorite.exists()) {
					currentRenameList = new Rename(search(ConfigManager.configRead(jsonRenamesFavorite).getName(), searchField.getText()));
				} else {
					currentRenameList = new Rename(new String[0]);
				}
				currentRenameListSize = currentRenameList.getName().length;

				buttonsDefine();
				clearAll();

				addDrawableChild(openerFavoriteOnly);
			}

			if (open) {
				addDrawableChild(background);
				showButtons();
				updatePageWidgets();
				addDrawableChild(searchField);
				searchField.setFocusUnlocked(true);
				searchField.setTextFieldFocused(true);
				nameField.setTextFieldFocused(false);
				nameField.setFocusUnlocked(true);
				remove(openerOpened);
				addDrawableChild(openerOpened);
				tabsUpdate();
			}
		} else {
			opener.active = false;
			remove(openerOpened);
		}
	}

	@Inject(at = @At("RETURN"), method = "onRenamed")
	private void newNameEntered(String name, CallbackInfo ci) {
		if (config.enableAnvilModification) {
			remove(addToFavorite);
			remove(removeFromFavorite);
			if (!name.isEmpty()) {
				File file = new File(RPRenames.configPathFavorite + currentItem + ".json");
				if (file.exists()) {
					String[] favoriteName = ConfigManager.configRead(file).getName();
					boolean nameExist = false;
					for (String s : favoriteName) {
						if (name.equals(s)) {
							nameExist = true;
							break;
						}
					}
					if (nameExist) {
						addDrawableChild(removeFromFavorite);
					} else {
						addDrawableChild(addToFavorite);
					}
				} else {
					addDrawableChild(addToFavorite);
				}
			}
		}
	}

	@Inject(at = @At("RETURN"), method = "onSlotUpdate")
	private void itemUpdate(ScreenHandler handler, int slotId, ItemStack stack, CallbackInfo ci) {
		if (config.enableAnvilModification) {
			if (slotId == 0) {
				if (stack.isEmpty()) {
					currentItem = null;
					clearAll();
					searchField.setText("");
					searchField.setFocusUnlocked(false);
					remove(searchField);
					searchField.setTextFieldFocused(false);
					screenUpdate();
				} else {
					currentItem = stack.getItem().getTranslationKey();
					int i = 0;
					int t = 0;
					while (t != 2) {
						if (String.valueOf(currentItem.charAt(i)).equals(".")) {
							currentItem = currentItem.substring(i + 1);
							t++;
							i = 0;
						} else {
							i++;
						}
					}

					icon1 = new ItemStack(stack.getItem());
					icon2 = new ItemStack(stack.getItem());
					icon3 = new ItemStack(stack.getItem());
					icon4 = new ItemStack(stack.getItem());
					icon5 = new ItemStack(stack.getItem());
					iconAfterUpdate1 = new ItemStack(stack.getItem());
					iconAfterUpdate2 = new ItemStack(stack.getItem());
					iconAfterUpdate3 = new ItemStack(stack.getItem());
					iconAfterUpdate4 = new ItemStack(stack.getItem());
					iconAfterUpdate5 = new ItemStack(stack.getItem());
					clearAll();
					searchField.setText("");
					searchField.setFocusUnlocked(true);
					tabNum = 1;
					newNameEntered(nameField.getText(), ci);
					screenUpdate();
				}
			}
		}
	}

	@Inject(at = @At("RETURN"), method = "drawForeground")
	private void paintWWidgets(MatrixStack matrices, int mouseX, int mouseY, CallbackInfo ci) {
		if (config.enableAnvilModification) {
			iconSlot1.paint(matrices, -130 + 1, 30 + 1, mouseX, mouseY);
			iconSlot2.paint(matrices, -130 + 1, 52 + 1, mouseX, mouseY);
			iconSlot3.paint(matrices, -130 + 1, 74 + 1, mouseX, mouseY);
			iconSlot4.paint(matrices, -130 + 1, 96 + 1, mouseX, mouseY);
			iconSlot5.paint(matrices, -130 + 1, 118 + 1, mouseX, mouseY);
			pageCount.setHorizontalAlignment(HorizontalAlignment.CENTER);
			pageCount.paint(matrices, 83 + 10 - 200 + 10 + 30 + 5 - 14, 140 + 4, mouseX, mouseY);
			pageCount.setSize(12, 30);
			button1textShadow.paint(matrices, -130 + 20 + 49 - 10 + 1, 30 + 7 + 1, mouseX, mouseY);
			button2textShadow.paint(matrices, -130 + 20 + 49 - 10 + 1, 52 + 7 + 1, mouseX, mouseY);
			button3textShadow.paint(matrices, -130 + 20 + 49 - 10 + 1, 74 + 7 + 1, mouseX, mouseY);
			button4textShadow.paint(matrices, -130 + 20 + 49 - 10 + 1, 96 + 7 + 1, mouseX, mouseY);
			button5textShadow.paint(matrices, -130 + 20 + 49 - 10 + 1, 118 + 7 + 1, mouseX, mouseY);
			button1textShadow.setHorizontalAlignment(HorizontalAlignment.CENTER);
			button2textShadow.setHorizontalAlignment(HorizontalAlignment.CENTER);
			button3textShadow.setHorizontalAlignment(HorizontalAlignment.CENTER);
			button4textShadow.setHorizontalAlignment(HorizontalAlignment.CENTER);
			button5textShadow.setHorizontalAlignment(HorizontalAlignment.CENTER);
			button1text.paint(matrices, -130 + 20 + 49 - 10, 30 + 7, mouseX, mouseY);
			button2text.paint(matrices, -130 + 20 + 49 - 10, 52 + 7, mouseX, mouseY);
			button3text.paint(matrices, -130 + 20 + 49 - 10, 74 + 7, mouseX, mouseY);
			button4text.paint(matrices, -130 + 20 + 49 - 10, 96 + 7, mouseX, mouseY);
			button5text.paint(matrices, -130 + 20 + 49 - 10, 118 + 7, mouseX, mouseY);
			button1text.setHorizontalAlignment(HorizontalAlignment.CENTER);
			button2text.setHorizontalAlignment(HorizontalAlignment.CENTER);
			button3text.setHorizontalAlignment(HorizontalAlignment.CENTER);
			button4text.setHorizontalAlignment(HorizontalAlignment.CENTER);
			button5text.setHorizontalAlignment(HorizontalAlignment.CENTER);
		}
	}

	private void showButtons() {
		if (page * 5 <= currentRenameListSize - 1) {
			this.addDrawableChild(button1);
			iconSlot1.setIcon(new ItemIcon(icon1));
			button1text.setText(Text.of(shortText(Text.of(currentRenameList.getName(page * 5)))));
			button1textShadow.setText(Text.of(shortText(Text.of(currentRenameList.getName(page * 5)))));
		}
		if (1 + page * 5 <= currentRenameListSize - 1) {
			this.addDrawableChild(button2);
			iconSlot2.setIcon(new ItemIcon(icon2));
			button2text.setText(Text.of(shortText(Text.of(currentRenameList.getName(1 + page * 5)))));
			button2textShadow.setText(Text.of(shortText(Text.of(currentRenameList.getName(1 + page * 5)))));
		}
		if (2 + page * 5 <= currentRenameListSize - 1) {
			this.addDrawableChild(button3);
			iconSlot3.setIcon(new ItemIcon(icon3));
			button3text.setText(Text.of(shortText(Text.of(currentRenameList.getName(2 + page * 5)))));
			button3textShadow.setText(Text.of(shortText(Text.of(currentRenameList.getName(2 + page * 5)))));
		}
		if (3 + page * 5 <= currentRenameListSize - 1) {
			this.addDrawableChild(button4);
			iconSlot4.setIcon(new ItemIcon(icon4));
			button4text.setText(Text.of(shortText(Text.of(currentRenameList.getName(3 + page * 5)))));
			button4textShadow.setText(Text.of(shortText(Text.of(currentRenameList.getName(3 + page * 5)))));
		}
		if (4 + page * 5 <= currentRenameListSize - 1) {
			this.addDrawableChild(button5);
			iconSlot5.setIcon(new ItemIcon(icon5));
			button5text.setText(Text.of(shortText(Text.of(currentRenameList.getName(4 + page * 5)))));
			button5textShadow.setText(Text.of(shortText(Text.of(currentRenameList.getName(4 + page * 5)))));
		}
	}

	private void hideButtons() {
		this.remove(background);
		this.remove(button1);
		this.remove(button2);
		this.remove(button3);
		this.remove(button4);
		this.remove(button5);
		iconSlot1.setIcon(null);
		iconSlot2.setIcon(null);
		iconSlot3.setIcon(null);
		iconSlot4.setIcon(null);
		iconSlot5.setIcon(null);
		button1text.setText(Text.of(""));
		button2text.setText(Text.of(""));
		button3text.setText(Text.of(""));
		button4text.setText(Text.of(""));
		button5text.setText(Text.of(""));
		button1textShadow.setText(Text.of(""));
		button2textShadow.setText(Text.of(""));
		button3textShadow.setText(Text.of(""));
		button4textShadow.setText(Text.of(""));
		button5textShadow.setText(Text.of(""));
	}

	private void clearAll() {
		this.remove(background);
		this.remove(button1);
		this.remove(button2);
		this.remove(button3);
		this.remove(button4);
		this.remove(button5);
		this.remove(pageDown);
		this.remove(pageUp);
		pageCount.setText(Text.of(""));
		page = 0;
		iconSlot1.setIcon(null);
		iconSlot2.setIcon(null);
		iconSlot3.setIcon(null);
		iconSlot4.setIcon(null);
		iconSlot5.setIcon(null);
		button1text.setText(Text.of(""));
		button2text.setText(Text.of(""));
		button3text.setText(Text.of(""));
		button4text.setText(Text.of(""));
		button5text.setText(Text.of(""));
		button1textShadow.setText(Text.of(""));
		button2textShadow.setText(Text.of(""));
		button3textShadow.setText(Text.of(""));
		button4textShadow.setText(Text.of(""));
		button5textShadow.setText(Text.of(""));
		remove(searchTab);
		remove(favoriteTab);
		remove(inventoryTab);
		remove(searchTab2);
		remove(favoriteTab2);
		remove(inventoryTab2);
		remove(openerFavoriteOnly);
	}

	private ArrayList<Object> createButton(int order, Text text) {
		ArrayList<Object> settings = new ArrayList<>();
		int u = 0;
		int v = 0;
		File file = new File(RPRenames.configPathFavorite + currentItem + ".json");
		if (file.exists()) {
			boolean favorite = false;
			String[] favoriteList = ConfigManager.configRead(file).getName();
			for (String s : favoriteList) {
				if (text.equals(Text.of(s))) {
					favorite = true;
					break;
				}
			}
			if (favorite) {
				u = 138;
				v = 166;
			}
		}
		int citSize = currentRenameList.getName().length - mobName.size();
		ArrayList<Text> toolTip = new ArrayList<>();
		toolTip.add(text);
		boolean isCEM = false;
		if (currentItem.equals("name_tag") && (page * 5) + order > citSize) {
			isCEM = true;
			for (String s : mobName.get((page * 5) + order - 1 - citSize)) {
				toolTip.add(Text.of(s).copy().fillStyle(Style.EMPTY.withColor(Formatting.GRAY)));
			}
		}

		settings.add(new TexturedButtonWidget(this.width / 2 - 200 + 10 - 28, this.height / 2 - 83 + 30 + ((order - 1) * 22), 118, 20, u, v, 20, RENAMES_MENU, menuWidth, menuHeight, (button) -> nameField.setText(text.getString()), new ButtonWidget.TooltipSupplier() {
			public void onTooltip(ButtonWidget buttonWidget, MatrixStack matrixStack, int i, int j) {
				renderTooltip(matrixStack, toolTip, i, j);
			}

			public void supply(Consumer<Text> consumer) {
				consumer.accept(text);
			}
		}, text));

		if (isCEM) {
			int n = 0;
			for (String s : CEMList.mobsNames) {
				if (s.equals(toolTip.get(1).getString())) {
					break;
				}
				n++;
			}
			settings.add(new ItemStack(CEMList.spawnEggItems[n]));
		}

		return settings;
	}

	private void buttonsDefine() {
		remove(button1);
		remove(button2);
		remove(button3);
		remove(button4);
		remove(button5);
		icon1 = iconAfterUpdate1;
		icon2 = iconAfterUpdate2;
		icon3 = iconAfterUpdate3;
		icon4 = iconAfterUpdate4;
		icon5 = iconAfterUpdate5;
		if (page * 5 <= currentRenameListSize - 1) {
			Text text = Text.of(currentRenameList.getName(page * 5));
			String shortText = shortText(text);
			ArrayList<Object> settings = createButton(1, text);
			button1 = (TexturedButtonWidget) settings.get(0);
			if (settings.size() > 1) {
				icon1 = (ItemStack) settings.get(1);
			}
			button1text.setText(Text.of(shortText));
			button1textShadow.setText(Text.of(shortText));
			iconSlot1.setIcon(new ItemIcon(icon1));
			icon1.setCustomName(text);
		}

		if (1 + page * 5 <= currentRenameListSize - 1) {
			Text text = Text.of(currentRenameList.getName(1 + page * 5));
			String shortText = shortText(text);
			ArrayList<Object> settings = createButton(2, text);
			button2 = (TexturedButtonWidget) settings.get(0);
			if (settings.size() > 1) {
				icon2 = (ItemStack) settings.get(1);
			}
			button2text.setText(Text.of(shortText));
			button2textShadow.setText(Text.of(shortText));
			iconSlot2.setIcon(new ItemIcon(icon2));
			icon2.setCustomName(text);
		}

		if (2 + page * 5 <= currentRenameListSize - 1) {
			Text text = Text.of(currentRenameList.getName(2 + page * 5));
			String shortText = shortText(text);
			ArrayList<Object> settings = createButton(3, text);
			button3 = (TexturedButtonWidget) settings.get(0);
			if (settings.size() > 1) {
				icon3 = (ItemStack) settings.get(1);
			}
			button3text.setText(Text.of(shortText));
			button3textShadow.setText(Text.of(shortText));
			iconSlot3.setIcon(new ItemIcon(icon3));
			icon3.setCustomName(text);
		}

		if (3 + page * 5 <= currentRenameListSize - 1) {
			Text text = Text.of(currentRenameList.getName(3 + page * 5));
			String shortText = shortText(text);
			ArrayList<Object> settings = createButton(4, text);
			button4 = (TexturedButtonWidget) settings.get(0);
			if (settings.size() > 1) {
				icon4 = (ItemStack) settings.get(1);
			}
			button4text.setText(Text.of(shortText));
			button4textShadow.setText(Text.of(shortText));
			iconSlot4.setIcon(new ItemIcon(icon4));
			icon4.setCustomName(text);
		}

		if (4 + page * 5 <= currentRenameListSize - 1) {
			Text text = Text.of(currentRenameList.getName(4 + page * 5));
			String shortText = shortText(text);
			ArrayList<Object> settings = createButton(5, text);
			button5 = (TexturedButtonWidget) settings.get(0);
			if (settings.size() > 1) {
				icon5 = (ItemStack) settings.get(1);
			}
			button5text.setText(Text.of(shortText));
			button5textShadow.setText(Text.of(shortText));
			iconSlot5.setIcon(new ItemIcon(icon5));
			icon5.setCustomName(text);
		}
	}

	private String shortText(Text text) {
		String shortText;
		shortText = text.getString();
		if (renderer.getWidth(shortText) > 92 - 5) {
			while (renderer.getWidth(shortText) > 92 - 5) {
				shortText = shortText.substring(0, shortText.length() - 1);
			}
			return shortText + "...";
		}
		return shortText;
	}

	private void updatePageWidgets() {
		remove(pageUp);
		remove(pageDown);
		addDrawableChild(pageUp);
		addDrawableChild(pageDown);
		if (page == 0) {
			pageDown.active = false;
		}
		pageUp.active = 5 + page * 5 <= currentRenameListSize - 1;
		pageCount.setText(Text.of(page + 1 + "/" + (currentRenameList.getName().length + 4) / 5));
	}

	private void tabsUpdate() {
		remove(searchTab);
		remove(favoriteTab);
		remove(inventoryTab);
		addDrawableChild(searchTab);
		addDrawableChild(favoriteTab);
		addDrawableChild(inventoryTab);
		remove(searchTab2);
		remove(favoriteTab2);
		remove(inventoryTab2);
		if (tabNum == 1) {
			addDrawableChild(searchTab2);
			searchTab.active = false;
			favoriteTab.active = true;
			inventoryTab.active = true;
		} else if (tabNum == 2) {
			addDrawableChild(favoriteTab2);
			searchTab.active = true;
			favoriteTab.active = false;
			inventoryTab.active = true;
		} else if (tabNum == 3) {
			addDrawableChild(inventoryTab2);
			searchTab.active = true;
			favoriteTab.active = true;
			inventoryTab.active = false;
		}
	}

	private void onSearch(String search) {
		screenUpdate();
	}

	private String[] search(String[] list, String match) {
		ArrayList<String> arrayList = new ArrayList<>();
		int length = list.length;
		int i = 0;
		int g = 0;
		if (match.contains("*")) {
			while (!String.valueOf(match.charAt(g)).equals("*")) {
				g++;
			}
			int h = g;
			while (true) {
				h++;
				if (String.valueOf(match.charAt(h - 1)).equals("*") && h == match.length()) {
					break;
				}
				if (!String.valueOf(match.charAt(h)).equals("*")) {
					break;
				}
			}
			while (i < length) {
				if (list[i].startsWith(match.substring(0,g)) && list[i].contains(match.substring(h))) {
					arrayList.add(list[i]);
				}
				i++;
			}
		} else {
			while (i < length) {
				if (list[i].startsWith(match)) {
					arrayList.add(list[i]);
				}
				i++;
			}
		}

		String[] cutList = new String[arrayList.size()];
		int k = 0;
		while (k < arrayList.size()) {
			cutList[k] = arrayList.get(k);
			k++;
		}
		return cutList;
	}
}