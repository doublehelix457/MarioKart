package net.stormdev.mario.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import net.stormdev.mario.mariokart.MarioKart;

public class UnlockableManager {

	private Map<String, String> data = new HashMap<String, String>();
	private Map<String, Unlockable> unlocks = new HashMap<String, Unlockable>(); // ShortId:Unlockable
	private File saveFile = null;
	private boolean sql = false;
	private SQLManager sqlManager = null;
	private boolean enabled = true;

	public UnlockableManager(File saveFile, Boolean sql,
			Map<String, Unlockable> unlocks) {
		this.saveFile = saveFile;
		this.sql = sql;
		this.unlocks = unlocks;
		this.enabled = MarioKart.config.getBoolean("general.upgrades.enable");
		if (sql) {
			try {
				sqlManager = new SQLManager(
						MarioKart.config.getString("general.upgrades.sqlHostName"),
						MarioKart.config.getString("general.upgrades.sqlPort"),
						MarioKart.config
								.getString("general.upgrades.sqlDataBaseName"),
						MarioKart.config.getString("general.upgrades.sqlUsername"),
						MarioKart.config.getString("general.upgrades.sqlPassword"));
			} catch (Exception e) {
				sql = false;
			}
			if (sqlManager.MySQL == null || sqlManager.c == null) {
				sql = false;
			}
			if (sql) { // Check that it loaded okay...
				sqlManager.createTable("MarioKartUnlocks", new String[] {
						"playername", "unlocks" }, new String[] {
						"varchar(255)", "varchar(255)" });
			}
		}
		// SQL setup...
		load(); // Load the data
	}
	
	public synchronized void unloadSQL(){
		if(this.sqlManager != null){
			this.sqlManager.closeConnection();
		}
		return;
	}

	public List<Upgrade> getUpgrades(String playerName) {
		if (!data.containsKey(playerName) || !enabled) {
			return new ArrayList<Upgrade>();
		}
		List<Upgrade> upgrades = new ArrayList<Upgrade>();
		String[] unlocks = this.data.get(playerName).split(Pattern.quote(","));
		for (String unlock : unlocks) {
			String[] upgradeData = unlock.split(Pattern.quote(":"));
			if (upgradeData.length > 1) {
				String shortId = upgradeData[0];
				String amount = upgradeData[1];
				int a = 1;
				try {
					a = Integer.parseInt(amount);
				} catch (NumberFormatException e) {
					a = 0;
				}
				if (a > 0) {
					upgrades.add(new Upgrade(this.unlocks.get(shortId), a));
				}
			}
		}
		return upgrades;
	}

	public Boolean useUpgrade(String player, Upgrade upgrade) {
		if(!enabled){
			return false;
		}
		String[] unlocks = this.data.get(player).split(Pattern.quote(","));
		String[] un = unlocks.clone();
		Boolean used = false;
		Boolean remove = false;
		Boolean update = false;
		for (int i = 0; i < un.length; i++) {
			remove = false;
			String unlock = un[i];
			String[] upgradeData = unlock.split(Pattern.quote(":"));
			if (upgradeData.length > 1) {
				String shortId = upgradeData[0];
				String amount = upgradeData[1];
				int a = 1;
				try {
					a = Integer.parseInt(amount);
				} catch (NumberFormatException e) {
					a = 0;
				}
				if (a > 0) {
					if (shortId.equals(upgrade.getUnlockedAble().shortId)) {
						int q = a - upgrade.getQuantity();
						if (q < 1) {
							remove = true;
						} else {
							// Set quantity to q
							unlocks[i] = shortId + ":" + q;
						}
						used = true;
					}
				} else {
					remove = true;
				}
			}
			if (remove) {
				unlocks[i] = " ";
				update = true;
			}
		}
		if (used || update) {
			// Update database
			String s = "";
			for (String u : unlocks) {
				if (u.length() > 1) {
					if (s.length() < 1) {
						s = u;
					} else {
						s = s + "," + u;
					}
				}
			}
			if (s.length() < 2) {
				this.data.remove(player);
			} else {
				this.data.put(player, s);
			}
			save(player); // Save to file/sql
		}
		return used;
	}

	public Boolean addUpgrade(String player, Upgrade upgrade) {
		if(!enabled){
			return false;
		}
		String[] un = new String[] {};
		String[] unlocks = new String[] {};
		if (this.data.containsKey(player)) {
			unlocks = this.data.get(player).split(Pattern.quote(","));
			un = unlocks.clone();
		}
		Boolean added = false;
		for (int i = 0; i < un.length; i++) {
			String unlock = un[i];
			String[] upgradeData = unlock.split(Pattern.quote(":"));
			if (upgradeData.length > 1) {
				String shortId = upgradeData[0];
				String amount = upgradeData[1];
				int a = 1;
				try {
					a = Integer.parseInt(amount);
				} catch (NumberFormatException e) {
					a = 0;
				}
				if (shortId.equals(upgrade.getUnlockedAble().shortId)) {
					int q = a + upgrade.getQuantity();
					if (q < 1) {
						added = true;
					} else {
						if (q <= 64) {
							// Set quantity to q
							unlocks[i] = shortId + ":" + q;
							added = true;
						} else {
							return false; // Not allowed more than 64 of an
											// upgrade
						}
					}
				}
			}
		}
		// Update database
		String s = "";
		for (String u : unlocks) {
			if (s.length() < 1) {
				s = "" + u;
			} else {
				s += "," + u;
			}
		}
		if (!added) {
			if (s.length() < 1) {
				s = upgrade.getUnlockedAble().shortId + ":"
						+ upgrade.getQuantity();
			} else {
				s += "," + upgrade.getUnlockedAble().shortId + ":"
						+ upgrade.getQuantity();
			}
		}
		if (s.length() < 255) {
			this.data.put(player, s);
			save(player); // Save to file/sql
			return true;
		}
		return false; // They have too many upgrades
	}

	public Boolean hasUpgradeById(String player, String shortId) {
		List<Upgrade> ups = getUpgrades(player);
		for (Upgrade u : ups) {
			if (u.getUnlockedAble().shortId.equals(shortId)) {
				return true;
			}
		}
		return false;
	}

	public Boolean hasUpgradeByName(String player, String upgradeName) {
		List<Upgrade> ups = getUpgrades(player);
		for (Upgrade u : ups) {
			if (u.getUnlockedAble().upgradeName.equals(upgradeName)) {
				return true;
			}
		}
		return false;
	}

	public void resetUpgrades(String player) {
		if(!enabled){
			return;
		}
		this.data.remove(player);
		save(player);
		return;
	}

	public Unlockable getUnlockable(String shortId) {
		if (!unlocks.containsKey(shortId)) {
			return null;
		}
		return unlocks.get(shortId);
	}

	public String getShortId(String unlockName) {
		List<String> keys = new ArrayList<String>(unlocks.keySet());
		for (String s : keys) {
			Unlockable u = unlocks.get(s);
			if (u.upgradeName.equals(unlockName)) {
				return s;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public void load() {
		if (!sql) {
			if (!(this.saveFile.length() < 1 || !this.saveFile.exists())) {
				// Load from file
				try {
					ObjectInputStream ois = new ObjectInputStream(
							new FileInputStream(this.saveFile));
					Object result = ois.readObject();
					ois.close();
					data = (Map<String, String>) result;
				} catch (Exception e) {
					// File just created
				}
			}
		} else {
			// Load from sql
			try {
				data.putAll(sqlManager.getStringsFromTable("MarioKartUnlocks",
						"playername", "unlocks"));
			} catch (SQLException e) {
				// SQL Error
				e.printStackTrace();
			}
		}
	}

	public void save(final String playerName) {
		MarioKart.plugin.getServer().getScheduler()
				.runTaskAsynchronously(MarioKart.plugin, new Runnable() {

					@Override
					public void run() {
						if (!sql) {
							saveFile.getParentFile().mkdirs();
							if (!saveFile.exists() || saveFile.length() < 1) {
								try {
									saveFile.createNewFile();
								} catch (IOException e) {
								}
							}
							try {
								ObjectOutputStream oos = new ObjectOutputStream(
										new FileOutputStream(saveFile));
								oos.writeObject(data);
								oos.flush();
								oos.close();
							} catch (Exception e) {
								e.printStackTrace();
							}
							return;
						}
						// Save to SQL
						if (data.containsKey(playerName)) {
							try {
								sqlManager.setInTable("MarioKartUnlocks",
										"playername", playerName, "unlocks",
										data.get(playerName));
							} catch (SQLException e) {
								// SQL Error
								e.printStackTrace();
							}
						} else {
							try {
								sqlManager.deleteFromTable("MarioKartUnlocks",
										"playername", playerName, "unlocks");
							} catch (SQLException e) {
								// Player wasn't in database
							}
						}
						return;
					}
				});
	}

	public void save() {
		if (!sql) {
			save("");
			return;
		}
		List<String> keys = new ArrayList<String>(data.keySet());
		for (String k : keys) {
			save(k);
		}
		return;
	}

}
