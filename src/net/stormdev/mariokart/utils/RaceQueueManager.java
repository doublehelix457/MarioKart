package net.stormdev.mario.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.stormdev.mario.mariokart.MarioKart;

public class RaceQueueManager {

	public RaceQueueManager() {
	}

	public Boolean queueExists(String trackName, RaceType raceMode) {
		Map<UUID, RaceQueue> trackQueues = getQueues(trackName);
		if (trackQueues.size() < 1) {
			return false;
		}
		Set<UUID> keys = trackQueues.keySet();
		for (UUID key : keys) {
			RaceQueue r = trackQueues.get(key);
			if (r.getRaceMode() == raceMode) {
				return true;
			}
		}
		return false;
	}

	public RaceQueue getQueue(String trackName, RaceType raceMode) {
		LinkedHashMap<UUID, RaceQueue> trackQueues = getQueues(trackName);
		if (trackQueues.size() < 1) {
			return null;
		}
		Set<UUID> keys = trackQueues.keySet();
		for (UUID key : keys) {
			RaceQueue r = trackQueues.get(key);
			if (r.getRaceMode() == raceMode) {
				return r;
			}
		}
		return null;
	}

	public RaceQueue getQueue(String trackName, UUID queueId) {
		Map<UUID, RaceQueue> trackQueues = getQueues(trackName);
		if (trackQueues.size() < 1) {
			return null;
		}
		Set<UUID> keys = trackQueues.keySet();
		for (UUID key : keys) {
			RaceQueue r = trackQueues.get(key);
			if (r.getQueueId() == queueId) {
				return r;
			}
		}
		return null;
	}

	public synchronized LinkedHashMap<UUID, RaceQueue> getQueues(RaceType type) {
		LinkedHashMap<UUID, RaceQueue> trackQueues = getAllQueues();
		for (UUID id : new ArrayList<UUID>(trackQueues.keySet())) {
			RaceQueue queue = trackQueues.get(id);
			if (queue.getRaceMode() != type) {
				trackQueues.remove(queue);
			}
		}
		return trackQueues;
	}

	public LinkedHashMap<UUID, RaceQueue> getOpenQueues(RaceType type) {
		LinkedHashMap<UUID, RaceQueue> trackQueues = getAllQueues();
		for (UUID id : new ArrayList<UUID>(trackQueues.keySet())) {
			RaceQueue queue = trackQueues.get(id);
			if (queue.getRaceMode() != type
					|| queue.playerCount() >= queue.playerLimit()) {
				trackQueues.remove(queue.getQueueId());
			}
		}
		return trackQueues;
	}

	public synchronized LinkedHashMap<UUID, RaceQueue> getQueues(String trackName) {
		LinkedHashMap<UUID, RaceQueue> trackQueues = new LinkedHashMap<UUID, RaceQueue>();
		if (MarioKart.plugin.queues.containsKey(trackName)) {
			trackQueues = MarioKart.plugin.queues.get(trackName);
		}
		return trackQueues;
	}
	
	public synchronized Map<UUID, RaceQueue> getQueues(String trackName, RaceType type) {
		Map<UUID, RaceQueue> trackQueues = new HashMap<UUID, RaceQueue>();
		if (MarioKart.plugin.queues.containsKey(trackName)) {
			trackQueues.putAll(MarioKart.plugin.queues.get(trackName));
		}
		for(UUID id:new HashMap<UUID, RaceQueue>(trackQueues).keySet()){
			RaceQueue q = trackQueues.get(id);
			if(q.getRaceMode() != type){
				trackQueues.remove(id);
			}
		}
		return trackQueues;
	}

	public synchronized void removeQueue(String trackName, UUID queueId) {
		Map<UUID, RaceQueue> trackQueues = new HashMap<UUID, RaceQueue>();
		if (MarioKart.plugin.queues.containsKey(trackName)) {
			trackQueues = MarioKart.plugin.queues.get(trackName);
		}
		if (trackQueues.size() < 1) {
			return;
		}
		List<UUID> keys = new ArrayList<UUID>(trackQueues.keySet());
		for (UUID key : keys) {
			RaceQueue r = trackQueues.get(key);
			if (r.getQueueId() == queueId) {
				removeQueue(r);
				return;
			}
		}
	}

	public synchronized void removeQueue(RaceQueue queue) {
		queue.clear();
		LinkedHashMap<UUID, RaceQueue> trackQueues = new LinkedHashMap<UUID, RaceQueue>();
		if (MarioKart.plugin.queues.containsKey(queue.getTrackName())) {
			trackQueues = MarioKart.plugin.queues.get(queue.getTrackName());
		}
		trackQueues.remove(queue.getQueueId());
		MarioKart.plugin.queues.put(queue.getTrackName(), trackQueues);
	}

	public synchronized LinkedHashMap<UUID, RaceQueue> getAllQueues() {
		List<String> tracks = new ArrayList<String>(MarioKart.plugin.queues.keySet());
		LinkedHashMap<UUID, RaceQueue> queues = new LinkedHashMap<UUID, RaceQueue>();
		for (String tName : tracks) {
			queues.putAll(getQueues(tName));
		}
		return queues;
	}

	public synchronized void updateQueue(RaceQueue queue) {
		LinkedHashMap<UUID, RaceQueue> trackQueues = new LinkedHashMap<UUID, RaceQueue>();
		if (MarioKart.plugin.queues.containsKey(queue.getTrackName())) {
			trackQueues = MarioKart.plugin.queues.get(queue.getTrackName());
		}
		if (trackQueues.containsKey(queue.getQueueId())) {
			trackQueues.put(queue.getQueueId(), queue);
			MarioKart.plugin.queues.put(queue.getTrackName(), trackQueues);
		}
		return;
	}

	public synchronized void clear() {
		LinkedHashMap<UUID, RaceQueue> queues = getAllQueues();
		for (UUID id : queues.keySet()) {
			RaceQueue q = (queues.get(id));
			q.clear();
		}
		for (String trackId:MarioKart.plugin.queues.keySet()){ //ConcurrentHashMap allows this
			MarioKart.plugin.queues.remove(trackId);
		}
	}

	public Boolean queuesFor(RaceTrack track, RaceType type) {
		LinkedHashMap<UUID, RaceQueue> queues = getAllQueues();
		for (UUID id : queues.keySet()) {
			RaceQueue q = queues.get(id);
			if (!(q.getRaceMode() == RaceType.TIME_TRIAL && type == RaceType.TIME_TRIAL)) {
				return true;
			}
		}
		return false;
	}

}
