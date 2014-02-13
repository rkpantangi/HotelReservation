package com.ram;

import com.ram.Room.RoomType;

public interface RoomService {
	Room getRoomInfo(String roomNo);

	Room reserveRoom(RoomType roomType);

	void freeUpRoom(String roomNo);

	boolean isRoomAvailable(RoomType roomType);

	int getTotalRooms();
}
