package net.stormdev.mario.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.milkbowl.vault.economy.EconomyResponse;
import net.stormdev.mario.mariokart.MarioKart;
import net.stormdev.mario.utils.IconMenu.OptionClickEvent;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Shop {

	final public static IconMenu getShop() {
		final IconMenu menu = new IconMenu(MarioKart.colors.getTitle()
				+ "MarioKart Shop", 9, new IconMenu.OptionClickEventHandler() {

			@Override
			public void onOptionClick(OptionClickEvent event) {
				event = onClick(SelectMenuType.MENU, event.getPosition(), event, 1);
				return;
			}
		}, MarioKart.plugin);
		menu.setOption(0, new ItemStack(Material.EMERALD),
				MarioKart.colors.getTitle() + "Buy Upgrades",
				new String[] { MarioKart.colors.getInfo() + "Upgrade your Kart!" });
		menu.setOption(1, new ItemStack(Material.EMERALD),
				MarioKart.colors.getTitle() + "My Upgrades",
				new String[] { MarioKart.colors.getInfo()
						+ "View and Remove Kart upgrades!" });
		menu.setOption(8, new ItemStack(Material.BOOK), MarioKart.colors.getTitle()
				+ "Exit Menu", new String[] { MarioKart.colors.getInfo()
				+ "Exit this menu!" });
		return menu;
	}

	public static void openShop(Player player) {
		if(!MarioKart.config.getBoolean("general.upgrades.enable")){
			player.sendMessage(MarioKart.colors.getError()+MarioKart.msgs.get("general.disabled"));
			return;
		}
		getShop().open(player);
		return;
	}

	public static void openUpgradeShop(Player player, int page) {
		getUpgradesForSaleMenu(page).open(player);
	}

	public static void openMyUpgrades(Player player, int page) {
		getUpgradesIOwn(player.getName(), page).open(player);
	}

	public static IconMenu getUpgradesForSaleMenu(final int page) {
		String title = MarioKart.colors.getTitle() + "Buy Upgrades Page: " + page;
		if (title.length() > 32) {
			title = MarioKart.colors.getError() + "Buy Upgrades (ERROR:Too Long)";
		}
		final Map<String, Unlockable> unlocks = new HashMap<String, Unlockable>(
				MarioKart.plugin.getUnlocks());
		final IconMenu menu = new IconMenu(title, 54,
				new IconMenu.OptionClickEventHandler() {
					@Override
					public void onOptionClick(IconMenu.OptionClickEvent event) {
						event.setWillClose(true);
						event.setWillDestroy(true);
						event = onClick(SelectMenuType.BUY_UPGRADES, event.getPosition(), event, page);
					    return;
					}
				}, MarioKart.plugin);
		menu.setOption(0, new ItemStack(Material.BOOK), MarioKart.colors.getTitle()
				+ "Back to menu", MarioKart.colors.getInfo()
				+ "Return back to the selection menu");
		menu.setOption(52, new ItemStack(Material.PAPER),
				MarioKart.colors.getTitle() + "Previous Page", MarioKart.colors.getInfo()
						+ "Go to previous page");
		menu.setOption(53, new ItemStack(Material.PAPER),
				MarioKart.colors.getTitle() + "Next Page", MarioKart.colors.getInfo()
						+ "Go to next page");
		// Set option slots for all upgrades for sale
		// 1-51 slots available on the page
		int pos = 1;
		int arrayStartPos = (page - 1) * 51;
		final Object[] keys = unlocks.keySet().toArray();
		for (int i = arrayStartPos; i < (arrayStartPos + 52)
				&& i < unlocks.size(); i++) {
			if (pos < 52) {
				Unlockable unlock = unlocks.get(keys[i]);
				ItemStack display = new ItemStack(unlock.displayItem);
				ArrayList<String> lore = new ArrayList<String>();
				lore.add(MarioKart.colors.getInfo() + "Effect: "
						+ unlock.type.name().toLowerCase());
				lore.add(MarioKart.colors.getInfo()
						+ "Price: "
						+ unlock.price
						+ " "
						+ MarioKart.config
								.getString("general.race.rewards.currency"));
				menu.setOption(pos, display, MarioKart.colors.getTitle()
						+ unlock.upgradeName, lore);
				pos++;
			}
		}
		return menu;
	}

	public static IconMenu getUpgradesIOwn(String player, final int page) {
		String title = MarioKart.colors.getTitle() + "My Upgrades Page: " + page;
		if (title.length() > 32) {
			title = MarioKart.colors.getError() + "My Upgrades (ERROR:Too Long)";
		}
		List<Upgrade> unlocks = MarioKart.plugin.upgradeManager.getUpgrades(player);
		final IconMenu menu = new IconMenu(title, 54,
				new IconMenu.OptionClickEventHandler() {
					@Override
					public void onOptionClick(IconMenu.OptionClickEvent event) {
						event.setWillClose(true);
						event.setWillDestroy(true);
						event = onClick(SelectMenuType.SELL_UPGRADES, event.getPosition(), event, page);
						return;
					}
				}, MarioKart.plugin);
		menu.setOption(0, new ItemStack(Material.BOOK), MarioKart.colors.getTitle()
				+ "Back to menu", MarioKart.colors.getInfo()
				+ "Return back to the selection menu");
		menu.setOption(52, new ItemStack(Material.PAPER),
				MarioKart.colors.getTitle() + "Previous Page", MarioKart.colors.getInfo()
						+ "Go to previous page");
		menu.setOption(53, new ItemStack(Material.PAPER),
				MarioKart.colors.getTitle() + "Next Page", MarioKart.colors.getInfo()
						+ "Go to next page");
		// Set option slots for all upgrades for sale
		// 1-51 slots available on the page
		int pos = 1;
		int arrayStartPos = (page - 1) * 51;
		for (int i = arrayStartPos; i < (arrayStartPos + 52)
				&& i < unlocks.size(); i++) {
			if (pos < 52) {
				Upgrade upgrade = unlocks.get(i);
				Unlockable unlock = upgrade.getUnlockedAble();
				ItemStack display = new ItemStack(unlock.displayItem);
				display.setAmount(upgrade.getQuantity());
				ArrayList<String> lore = new ArrayList<String>();
				lore.add(MarioKart.colors.getInfo() + "Effect: "
						+ unlock.type.name().toLowerCase());
				lore.add(ChatColor.RED + "Click to delete");
				menu.setOption(pos, display, MarioKart.colors.getTitle()
						+ unlock.upgradeName, lore);
				pos++;
			}
		}
		return menu;
	}
	
	public static OptionClickEvent onClick(SelectMenuType type, int slot, 
			IconMenu.OptionClickEvent event, int page){
		final Player player = event.getPlayer();
		if (type == SelectMenuType.MENU) {
			if (slot == 0) {
				// They clicked on 'Buy Upgrades'
				MarioKart.plugin.getServer().getScheduler()
						.runTaskLater(MarioKart.plugin, new Runnable() {
							@Override
							public void run() {
								Shop.openUpgradeShop(player, 1);
								return;
							}
						}, 2l);
				event.setWillClose(true);
				return event;
			} else if (slot == 1) {
				// They clicked on 'Sell Upgrades'
				MarioKart.plugin.getServer().getScheduler()
						.runTaskLater(MarioKart.plugin, new Runnable() {
							@Override
							public void run() {
								Shop.openMyUpgrades(player, 1);
								return;
							}
						}, 2l);
				event.setWillClose(true);
				return event;
			} else if (slot == 8) {
				// They clicked on 'Exit Menu'
				return event; // Menu closes on-click by default
			}
		} else if (type == SelectMenuType.BUY_UPGRADES) {
			if (slot == 0) {
				MarioKart.plugin.getServer().getScheduler()
						.runTaskLater(MarioKart.plugin, new Runnable() {
							@Override
							public void run() {
								Shop.openShop(player);
								return;
							}
						}, 2l);
				event.setWillClose(true);
				event.setWillDestroy(true);
				return event;
			} else if (slot == 52) {
				if (page <= 1) {
					event.setWillClose(false);
					event.setWillDestroy(false);
					return event;
				}
				final int p = page - 1;
				MarioKart.plugin.getServer().getScheduler()
						.runTaskLater(MarioKart.plugin, new Runnable() {
							@Override
							public void run() {
								Shop.openUpgradeShop(player, p);
								return;
							}
						}, 2l);
				event.setWillClose(true);
				event.setWillDestroy(true);
				return event;
			} else if (slot == 53) {
				final int p = page + 1;
				MarioKart.plugin.getServer().getScheduler()
						.runTaskLater(MarioKart.plugin, new Runnable() {
							@Override
							public void run() {
								Shop.openUpgradeShop(player, p);
								return;
							}
						}, 2l);
				event.setWillClose(true);
				event.setWillDestroy(true);
				return event;
			} else {
				// Get and buy unlockable
				int i = ((page - 1) * 51) + slot - 1;
				String shortId = "";
				Unlockable unlock = null;
				String currency = MarioKart.config
						.getString("general.race.rewards.currency");
				try {
					shortId = (String) MarioKart.plugin.getUnlocks().keySet()
							.toArray()[i];
					unlock = MarioKart.plugin.getUnlocks().get(shortId);
				} catch (Exception e) {
					// Clicked in an invalid place
					return event;
				}
				if (unlock == null) {
					// Invalid unlock
					return event;
				}
				double price = unlock.price;
				if (MarioKart.economy == null) {
					if (!MarioKart.plugin.setupEconomy() || MarioKart.economy == null) {
						player.sendMessage(MarioKart.colors.getError()
								+ MarioKart.msgs.get("general.shop.error"));
						return event;
					}

				}
				double balance = MarioKart.economy.getBalance(player.getName());
				if (balance < price) {
					String msg = MarioKart.msgs.get("general.shop.notEnoughMoney");
					msg = msg.replaceAll(Pattern.quote("%currency%"),
							Matcher.quoteReplacement(currency));
					msg = msg.replaceAll(Pattern.quote("%balance%"),
							Matcher.quoteReplacement(balance + ""));
					player.sendMessage(MarioKart.colors.getError() + msg);
					return event;
				}
				// Confident in success of transaction
				Boolean success = MarioKart.plugin.upgradeManager.addUpgrade(
						player.getName(), new Upgrade(unlock, 1)); // Give them
																	// the
																	// upgrade
				if (!success) {
					player.sendMessage(MarioKart.colors.getError()
							+ MarioKart.msgs.get("general.shop.maxUpgrades"));
					return event;
				}
				EconomyResponse response = MarioKart.economy.withdrawPlayer(
						player.getName(), price);
				balance = response.balance;
				String msg = MarioKart.msgs.get("general.shop.success");
				msg = msg.replaceAll(Pattern.quote("%currency%"),
						Matcher.quoteReplacement(currency));
				msg = msg.replaceAll(Pattern.quote("%balance%"),
						Matcher.quoteReplacement(balance + ""));
				msg = msg.replaceAll(Pattern.quote("%name%"),
						Matcher.quoteReplacement(unlock.upgradeName));
				msg = msg.replaceAll(Pattern.quote("%price%"),
						Matcher.quoteReplacement("" + price));
				player.sendMessage(MarioKart.colors.getInfo() + msg);
				event.setWillDestroy(true);
				return event;
			}
		} else if (type == SelectMenuType.SELL_UPGRADES) {
			if (slot == 0) {
				MarioKart.plugin.getServer().getScheduler()
						.runTaskLater(MarioKart.plugin, new Runnable() {
							@Override
							public void run() {
								Shop.openShop(player);
								return;
							}
						}, 2l);
				event.setWillClose(true);
				event.setWillDestroy(true);
				return event;
			} else if (slot == 52) {
				if (page <= 1) {
					event.setWillClose(false);
					event.setWillDestroy(false);
					return event;
				}
				final int p = page - 1;
				MarioKart.plugin.getServer().getScheduler()
						.runTaskLater(MarioKart.plugin, new Runnable() {
							@Override
							public void run() {
								Shop.openMyUpgrades(player, p);
								return;
							}
						}, 2l);
				event.setWillClose(true);
				event.setWillDestroy(true);
				return event;
			} else if (slot == 53) {
				final int p = page + 1;
				MarioKart.plugin.getServer().getScheduler()
						.runTaskLater(MarioKart.plugin, new Runnable() {
							@Override
							public void run() {
								Shop.openMyUpgrades(player, p);
								return;
							}
						}, 2l);
				event.setWillClose(true);
				event.setWillDestroy(true);
				return event;
			} else {
				// Get and buy unlockable
				int i = ((page - 1) * 51) + slot - 1;
				Upgrade upgrade = null;
				try {
					List<Upgrade> ups = MarioKart.plugin.upgradeManager
							.getUpgrades(player.getName());
					upgrade = ups.get(i);
				} catch (Exception e) {
					// Clicked on invalid slot
					return event;
				}
				if (upgrade == null) {
					return event; // Clicked on invalid slot
				}
				MarioKart.plugin.upgradeManager
						.useUpgrade(player.getName(), upgrade);
				String msg = MarioKart.msgs.get("general.shop.sellSuccess");
				msg = msg.replaceAll(Pattern.quote("%amount%"),
						"" + upgrade.getQuantity());
				msg = msg
						.replaceAll(Pattern.quote("%name%"),
								Matcher.quoteReplacement(upgrade
										.getUnlockedAble().upgradeName));
				player.sendMessage(MarioKart.colors.getInfo() + msg);
				event.setWillClose(true);
				event.setWillDestroy(true);
				final int p = page;
				MarioKart.plugin.getServer().getScheduler()
						.runTaskLater(MarioKart.plugin, new Runnable() {
							@Override
							public void run() {
								Shop.openMyUpgrades(player, p);
								return;
							}
						}, 2l);
				return event;
			}
		}
		return event;
	}

}
