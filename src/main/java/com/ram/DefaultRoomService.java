package com.ram;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;

import com.ram.Room.RoomType;

/**
 * This class is not designed to be thread-safe. It is intended to be only
 * accessed from the thread-safe BookingService
 * 
 * @author Ram
 * 
 */
public class DefaultRoomService implements RoomService {
	private Map<String, Room> roomsCache = new HashMap<>();
	private Map<RoomType, HashSet<String>> availableRoomsByType = new HashMap<>();

	/**
	 * In a real production application, this rooms cache is generally backed by
	 * a distribted cache or a database. For this test, I am assuming this data
	 * is injected.
	 * 
	 * @param roomsCache
	 */
	public DefaultRoomService(Map<String, Room> roomsCache) {
		this.roomsCache.putAll(roomsCache);
		for (RoomType roomType : RoomType.values()) {
			availableRoomsByType.put(roomType, new LinkedHashSet<String>());
		}
		for (Room room : roomsCache.values()) {
			availableRoomsByType.get(room.getType()).add(room.getRoomNo());
		}
	}

	@Override
	public Room getRoomInfo(String roomNo) {
		return roomsCache.get(roomNo);
	}

	@Override
	public Room reserveRoom(RoomType roomType) {
		if (isRoomAvailable(roomType)) {
			HashSet<String> availRooms = availableRoomsByType.get(roomType);
			Iterator<String> roomIter = availRooms.iterator();
			if (roomIter.hasNext()) {
				String roomNo = roomIter.next();
				roomIter.remove();
				return roomsCache.get(roomNo);
			}
		}
		return null;
	}

	@Override
	public boolean isRoomAvailable(RoomType roomType) {
		return (availableRoomsByType.get(roomType).size() > 0);
	}

	@Override
	public int getTotalRooms() {
		return roomsCache.size();
	}

	@Override
	public void freeUpRoom(String roomNo) {
		Room room = getRoomInfo(roomNo);
		availableRoomsByType.get(room.getType()).add(roomNo);
	}
}
