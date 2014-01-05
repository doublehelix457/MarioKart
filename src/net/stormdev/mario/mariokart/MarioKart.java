package net.stormdev.mario.mariokart;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.milkbowl.vault.economy.Economy;
import net.stormdev.mario.utils.DynamicLagReducer;
import net.stormdev.mario.utils.HotBarManager;
import net.stormdev.mario.utils.HotBarUpgrade;
import net.stormdev.mario.utils.MarioKartSound;
import net.stormdev.mario.utils.RaceMethods;
import net.stormdev.mario.utils.RaceQueue;
import net.stormdev.mario.utils.RaceQueueManager;
import net.stormdev.mario.utils.RaceTrackManager;
import net.stormdev.mario.utils.Shop;
import net.stormdev.mario.utils.TrackCreator;
import net.stormdev.mario.utils.Unlockable;
import net.stormdev.mario.utils.UnlockableManager;
import net.stormdev.mariokartAddons.MarioKartAddon;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.rosaloves.bitlyj.Bitly;
import com.rosaloves.bitlyj.Url;
import com.useful.ucars.ucars;

public class MarioKart extends JavaPlugin {
	public static YamlConfiguration lang = null;
	public static MarioKart plugin;
	public static FileConfiguration config = new YamlConfiguration();
	public static Colors colors;
	public static ucars ucars = null;
	public static URaceCommandExecutor cmdExecutor = null;
	public static URaceListener listener = null;
	public RaceTrackManager trackManager = null;
	public RaceScheduler raceScheduler = null;
	public static HashMap<String, TrackCreator> trackCreators = new HashMap<String, TrackCreator>();
	public ConcurrentHashMap<String, LinkedHashMap<UUID, RaceQueue>> queues = new ConcurrentHashMap<String, LinkedHashMap<UUID, RaceQueue>>();
	public RaceQueueManager raceQueues = null;
	public static Lang msgs = null;
	public RaceMethods raceMethods = null;
	public Random random = null;
	public static MarioKartAddon marioKart = null;
	public RaceTimes raceTimes = null;
	public String packUrl = "";
	public HotBarManager hotBarManager = null;
	public double checkpointRadiusSquared = 10.0;
	public List<String> resourcedPlayers = new ArrayList<String>();

	Map<String, Unlockable> unlocks = null;

	public UnlockableManager upgradeManager = null;

	BukkitTask lagReducer = null;

	public static Boolean vault = false;
	public static Economy economy = null;
	public static MarioKart getInstance() {
		return plugin;
	}
	@Override
	public void onEnable() {
		System.gc();
		if (listener != null || cmdExecutor != null || msgs != null || marioKart != null || economy != null) {
			getLogger().warning("Previous plugin instance found, performing clearup...");
			listener = null;
			cmdExecutor = null;
			msgs = null;
			marioKart = null;
			vault = null;
			economy = null;
		}
		random = new Random();
		plugin = this;
		File langFile = new File(getDataFolder(), "lang.yml");
		lang = YamlConfiguration.loadConfiguration(langFile);
		msgs = new Lang(this);
		cmdExecutor = new URaceCommandExecutor(this);
		if (!new File(getDataFolder(), "config.yml").exists() || new File(getDataFolder(), "config.yml").length() < 1) {
			getDataFolder().mkdirs();
			File configFile = new File(getDataFolder().getAbsolutePath()
					+ File.separator + "config.yml");
			try {
				configFile.createNewFile();
			} catch (IOException e) {
			}
			copy(getResource("config.yml"), configFile);
		}
		try {
			lang.save(langFile);
		} catch (IOException e1) {
			getLogger().info("Error parsing lang file!");
		}
		// Load the colour scheme
		colors = new Colors(getConfig().getString("colorScheme.success"),
				getConfig().getString("colorScheme.error"),
				getConfig().getString("colorScheme.info"),
				getConfig().getString("colorScheme.title"),
				getConfig().getString("colorScheme.title"));
		getLogger().info("Config loaded!");
		this.checkpointRadiusSquared = Math.pow(config.getDouble("general.checkpointRadius"), 2);
		getLogger().info("Searching for uCars...");
		if (getServer().getPluginManager().getPlugin("uCars") != null) {
			ucars = (com.useful.ucars.ucars) getServer().getPluginManager().getPlugin("uCars");
		} else {
			getLogger().info("Unable to find uCars!");
			getServer().getPluginManager().disablePlugin(this);
		}
		ucars.hookPlugin(this);
		getLogger().info("uCars found and hooked!");
		getLogger().info("Searching for ProtocolLib...");
		for (String k : getDescription().getCommands().keySet()) {
			getCommand(k).setExecutor(cmdExecutor);
		}
		listener = new URaceListener(this);
		getServer().getPluginManager().registerEvents(MarioKart.listener, this);
		this.trackManager = new RaceTrackManager(this, new File(getDataFolder()
				+ File.separator + "Data" + File.separator
				+ "tracks.uracetracks"));
		this.raceQueues = new RaceQueueManager();
		this.raceMethods = new RaceMethods();
		this.raceScheduler = new RaceScheduler(
				config.getInt("general.raceLimit"));
		// Setup marioKart
		marioKart = new MarioKartAddon(this);
		this.raceTimes = new RaceTimes(new File(getDataFolder()
				+ File.separator + "Data" + File.separator
				+ "raceTimes.uracetimes"),
				config.getBoolean("general.race.timed.log"));
		if (config.getBoolean("general.race.rewards.enable")) {
			try {
				vault = this.vaultInstalled();
				if (!setupEconomy()) {
					getLogger()
							.warning(
									"Attempted to enable rewards but Vault/Economy NOT found. Please install vault to use this feature!");
					getLogger().warning("Disabling reward system...");
					config.set("general.race.rewards.enable", false);
				}
			} catch (Exception e) {
				getLogger()
						.warning(
								"Attempted to enable rewards and shop but Vault/Economy NOT found. Please install vault to use these features!");
				getLogger().warning("Disabling reward system...");
				getLogger().warning("Disabling shop system...");
				MarioKart.config.set("general.race.rewards.enable", false);
				MarioKart.config.set("general.upgrades.enable", false);
			}
		}
		String rl = MarioKart.config.getString("mariokart.resourcePack");
		try {
			new URL(rl);
			if (MarioKart.config.getBoolean("bitlyUrlShortner")) {
				// Shorten url
				// Generic access token: 3676e306c866a24e3586a109b9ddf36f3d177556
				Url url = Bitly
						.as("storm345", "R_b0fae26d68750227470cd06b23be70b7").call(
								Bitly.shorten(rl));
				this.packUrl = url.getShortUrl();
			} else {
				packUrl = rl;
			}
		} catch (MalformedURLException e2) {
			packUrl = rl;
		}
		this.upgradeManager = new UnlockableManager(new File(getDataFolder()
				.getAbsolutePath()
				+ File.separator
				+ "Data"
				+ File.separator
				+ "upgradesData.mkdata"),
				config.getBoolean("general.upgrades.useSQL"), getUnlocks());
		this.hotBarManager = new HotBarManager(config.getBoolean("general.upgrades.enable"));
		this.lagReducer = getServer().getScheduler().runTaskTimer(this,
				new DynamicLagReducer(), 100L, 1L);
		System.gc();
		getLogger().info("MarioKart v" + plugin.getDescription().getVersion()
				+ " has been enabled!");
	}

	@Override
	public void onDisable() {
		if (ucars != null) {
			ucars.unHookPlugin(this);
		}
		HashMap<UUID, Race> races = new HashMap<UUID, Race>(this.raceScheduler.getRaces());
		for (UUID id : races.keySet()) {
			races.get(id).end(); // End the race
		}
		raceQueues.clear();
		Player[] players = getServer().getOnlinePlayers().clone();
		for (Player player : players) {
			if (player.hasMetadata("car.stayIn")) {
				player.removeMetadata("car.stayIn", plugin);
			}
		}
		this.lagReducer.cancel();
		getServer().getScheduler().cancelTasks(this);
		System.gc();
		try {
			Shop.getShop().destroy();
		} catch (Exception e) {
			// Shop is invalid anyway
		}
		this.upgradeManager.unloadSQL();
		System.gc();
	}

	private void copy(InputStream in, File file) {
		try {
			OutputStream out = new FileOutputStream(file);
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			out.close();
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String colorise(String prefix) {
		return ChatColor.translateAlternateColorCodes('&', prefix);
	}

	public boolean vaultInstalled(){
		Plugin[] plugins = getServer().getPluginManager().getPlugins();
		for (Plugin p : plugins) {
			if (p.getName().equals("Vault")) {
				return true;
			}
		}
		return false;
	}
	
	public boolean setupEconomy() {
		if(!vault){
			return false;
		}
		RegisteredServiceProvider<Economy> economyProvider = getServer()
				.getServicesManager().getRegistration(
						net.milkbowl.vault.economy.Economy.class);
		if (economyProvider != null) {
			economy = economyProvider.getProvider();
		}
		return (economy != null);
	}

	public Map<String, Unlockable> getUnlocks() {
		if (unlocks != null) {
			return unlocks;
		}
		getLogger().info("Loading upgrades...");
		// Begin load them from a YAML file
		Map<String, Unlockable> unlockables = new HashMap<String, Unlockable>();
		File saveFile = new File(getDataFolder().getAbsolutePath()
				+ File.separator + "upgrades.yml");
		YamlConfiguration upgrades = new YamlConfiguration();
		saveFile.getParentFile().mkdirs();
		Boolean setDefaults = false;
		try {
			upgrades.load(saveFile);
		} catch (Exception e) {
			setDefaults = true;
		}
		if (!saveFile.exists() || saveFile.length() < 1 || setDefaults) {
			try {
				saveFile.createNewFile();
			} catch (IOException e) {
				return unlockables;
			}
			// Set defaults
			upgrades.set("upgrades.speedBurstI.name", "Speed Burst I (5s)");
			upgrades.set("upgrades.speedBurstI.id", "aa");
			upgrades.set("upgrades.speedBurstI.type", HotBarUpgrade.SPEED_BOOST
					.name().toUpperCase());
			upgrades.set("upgrades.speedBurstI.item", Material.APPLE.name()
					.toUpperCase());
			upgrades.set("upgrades.speedBurstI.length", 5000l);
			upgrades.set("upgrades.speedBurstI.power", 10d);
			upgrades.set("upgrades.speedBurstI.useItem", true);
			upgrades.set("upgrades.speedBurstI.useUpgrade", true);
			upgrades.set("upgrades.speedBurstI.price", 3d);
			upgrades.set("upgrades.speedBurstII.name", "Speed Burst II (10s)");
			upgrades.set("upgrades.speedBurstII.id", "ab");
			upgrades.set("upgrades.speedBurstII.type",
					HotBarUpgrade.SPEED_BOOST.name().toUpperCase());
			upgrades.set("upgrades.speedBurstII.item", Material.CARROT_ITEM
					.name().toUpperCase());
			upgrades.set("upgrades.speedBurstII.length", 10000l);
			upgrades.set("upgrades.speedBurstII.power", 13d);
			upgrades.set("upgrades.speedBurstII.useItem", true);
			upgrades.set("upgrades.speedBurstII.useUpgrade", true);
			upgrades.set("upgrades.speedBurstII.price", 6d);
			upgrades.set("upgrades.immunityI.name", "Immunity I (5s)");
			upgrades.set("upgrades.immunityI.id", "ac");
			upgrades.set("upgrades.immunityI.type", HotBarUpgrade.IMMUNITY
					.name().toUpperCase());
			upgrades.set("upgrades.immunityI.item", Material.IRON_HELMET.name()
					.toUpperCase());
			upgrades.set("upgrades.immunityI.length", 5000l);
			upgrades.set("upgrades.immunityI.useItem", true);
			upgrades.set("upgrades.immunityI.useUpgrade", true);
			upgrades.set("upgrades.immunityI.price", 6d);
			upgrades.set("upgrades.immunityII.name", "Immunity II (10s)");
			upgrades.set("upgrades.immunityII.id", "ad");
			upgrades.set("upgrades.immunityII.type", HotBarUpgrade.IMMUNITY
					.name().toUpperCase());
			upgrades.set("upgrades.immunityII.item", Material.GOLD_HELMET
					.name().toUpperCase());
			upgrades.set("upgrades.immunityII.length", 10000l);
			upgrades.set("upgrades.immunityII.useItem", true);
			upgrades.set("upgrades.immunityII.useUpgrade", true);
			upgrades.set("upgrades.immunityII.price", 12d);
			try {
				upgrades.save(saveFile);
			} catch (IOException e) {
				getLogger().info(MarioKart.colors.getError()
						+ "[WARNING] Failed to create upgrades.yml!");
			}
		}
		// Load them
		ConfigurationSection ups = upgrades.getConfigurationSection("upgrades");
		Set<String> upgradeKeys = ups.getKeys(false);
		for (String key : upgradeKeys) {
			ConfigurationSection sect = ups.getConfigurationSection(key);
			if (!sect.contains("name") || !sect.contains("type")
					|| !sect.contains("id") || !sect.contains("useItem")
					|| !sect.contains("useUpgrade") || !sect.contains("price")
					|| !sect.contains("item")) {
				// Invalid upgrade
				getLogger().info(MarioKart.colors.getError()
						+ "[WARNING] Invalid upgrade: " + key);
				continue;
			}
			String name = sect.getString("name");
			HotBarUpgrade type = null;
			Material item = null;
			try {
				type = HotBarUpgrade.valueOf(sect.getString("type"));
				item = Material.valueOf(sect.getString("item"));
			} catch (Exception e) {
				// Invalid upgrade
				getLogger().info(MarioKart.colors.getError()
						+ "[WARNING] Invalid upgrade: " + key);
				continue;
			}
			if (type == null || item == null) {
				// Invalid upgrade
				getLogger().info(MarioKart.colors.getError()
						+ "[WARNING] Invalid upgrade: " + key);
				continue;
			}
			String shortId = sect.getString("id");
			Boolean useItem = sect.getBoolean("useItem");
			Boolean useUpgrade = sect.getBoolean("useUpgrade");
			double price = sect.getDouble("price");
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("upgrade.name", name);
			data.put("upgrade.useItem", useItem);
			data.put("upgrade.useUpgrade", useUpgrade);
			if (sect.contains("power")) {
				data.put("upgrade.power", sect.getDouble("power"));
			}
			if (sect.contains("length")) {
				data.put("upgrade.length", sect.getLong("length"));
			}
			Unlockable unlock = new Unlockable(type, data, price, name,
					shortId, item);
			unlockables.put(shortId, unlock);
		}
		unlocks = unlockables;
		return unlockables;
	}
	
	@SuppressWarnings("deprecation")
	public Boolean playCustomSound(final Player recipient, final Location location, 
			final String soundPath, final float volume, final float pitch){
		MarioKart.plugin.getServer().getScheduler().runTaskAsynchronously(MarioKart.plugin, new BukkitRunnable(){

			@Override
			public void run() {
				//Running async keeps TPS higher
				recipient.playSound(location, soundPath, volume, pitch); //Deprecated but still best way
			}});
		return true;
		/* Not needed
		if(main.prototcolManager == null){
			//No protocolLib
			return false;
		}
		getServer().getScheduler().runTaskAsynchronously(this, new BukkitRunnable(){
			@Override
			public void run() {
				//Play the sound
				try {
					if(pitch > 255){
						pitch = 255;
					}
					PacketContainer customSound = main.prototcolManager.createPacket(PacketType.Play.Server.NAMED_SOUND_EFFECT);
					customSound.getSpecificModifier(String.class).
					    write(0, soundPath);
					customSound.getSpecificModifier(int.class).
					    write(0, location.getBlockX()).
					    write(1, location.getBlockY()).
					    write(2, location.getBlockZ());
					    write(3, (int) pitch);
					customSound.getSpecificModifier(float.class).
					    write(0, volume);
					main.prototcolManager.sendServerPacket(recipient, customSound);
				} catch (Exception e) {
					main.getLogger().info(main.colors.getError()+"Error playing custom sound: "+soundPath+"!");
					e.printStackTrace();
					return;
				}
				return;
			}});
		return true;
		*/
	}
	
	public Boolean playCustomSound(Player recipient, Location location,
			MarioKartSound sound, float volume, float pitch){
		return playCustomSound(recipient, location, sound.getPath(), volume, pitch);
	}
	
	public Boolean playCustomSound(Player recipient, MarioKartSound sound){
		return playCustomSound(recipient, recipient.getLocation(),
				sound, Float.MAX_VALUE, 1f);
	}
}
