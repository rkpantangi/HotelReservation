package com.ram;

public class Room {
	public static enum RoomType {
		STANDARD, GOLD, PLATINUM
	}

	private String roomNo;
	private RoomType type;
	private boolean booked;

	public String getRoomNo() {
		return roomNo;
	}

	public void setRoomNo(String roomNo) {
		this.roomNo = roomNo;
	}

	public RoomType getType() {
		return type;
	}

	public void setType(RoomType type) {
		this.type = type;
	}

	public boolean isBooked() {
		return booked;
	}

	public void setBooked(boolean booked) {
		this.booked = booked;
	}

}
