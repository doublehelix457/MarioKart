package net.stormdev.mario.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.stormdev.mario.mariokart.Race;
import net.stormdev.mario.mariokart.User;
import net.stormdev.mario.mariokart.MarioKart;

import org.bukkit.entity.Player;

public class RaceMethods {
	private MarioKart plugin = null;

	public RaceMethods() {
		this.plugin = MarioKart.plugin;
	}

	public synchronized Race inAGame(Player player, Boolean update) {
		Map<UUID, Race> races = MarioKart.plugin.raceScheduler.getRaces();
		for (UUID id : new ArrayList<UUID>(races.keySet())) {
			Race r = races.get(id);
			if (update) {
				r.updateUser(player);
			}
			List<User> users = r.getUsersIn(); // Exclude those that have
												// finished the race
			for (User u : users) {
				if (u.getPlayerName().equals(player.getName())) {
					return r;
				}
			}
		}
		return null;
	}

	public synchronized RaceQueue inGameQue(Player player) {
		Map<UUID, RaceQueue> queues = MarioKart.plugin.raceQueues.getAllQueues();
		for (UUID id : queues.keySet()) {
			try {
				RaceQueue queue = queues.get(id);
				if (queue.containsPlayer(player)) {
					return queue;
				}
			} catch (Exception e) {
				return null;
			}
		}
		return null;
	}
}
