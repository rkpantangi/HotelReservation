package com.ram;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ram.Guest.MembershipType;
import com.ram.Room.RoomType;

/**
 * This is a very specific implementation based on simple Priorities as
 * explained in the requirements document.
 * 
 * @author Ram
 * 
 */
public class PriorityBasedRoomMembershipTypesConverter implements RoomMembershipTypesConverter {

	private Map<MembershipType, List<RoomType>> eligibleRoomTypes = new HashMap<>();
	private Map<RoomType, List<MembershipType>> eligibleMembershipTypes = new HashMap<>();

	public PriorityBasedRoomMembershipTypesConverter() {
		eligibleRoomTypes.put(MembershipType.STANDARD, Arrays.asList(RoomType.STANDARD));
		eligibleRoomTypes.put(MembershipType.GOLD, Arrays.asList(RoomType.GOLD, RoomType.STANDARD));
		eligibleRoomTypes.put(MembershipType.PLATINUM,
				Arrays.asList(RoomType.PLATINUM, RoomType.GOLD, RoomType.STANDARD));

		eligibleMembershipTypes.put(RoomType.STANDARD,
				Arrays.asList(MembershipType.PLATINUM, MembershipType.GOLD, MembershipType.STANDARD));
		eligibleMembershipTypes.put(RoomType.GOLD, Arrays.asList(MembershipType.PLATINUM, MembershipType.GOLD));
		eligibleMembershipTypes.put(RoomType.PLATINUM, Arrays.asList(MembershipType.PLATINUM));
	}

	@Override
	public List<RoomType> getEligibleRoomTypes(MembershipType memType) {
		return eligibleRoomTypes.get(memType);
	}

	@Override
	public List<MembershipType> getEligibleMembershipTypes(RoomType roomType) {
		return eligibleMembershipTypes.get(roomType);
	}

	@Override
	public RoomType getPriorityRoomTypes(MembershipType memType) {
		switch (memType) {
		case STANDARD:
			return RoomType.STANDARD;
		case GOLD:
			return RoomType.GOLD;
		case PLATINUM:
			return RoomType.PLATINUM;
		}
		return null;
	}

	@Override
	public MembershipType getPriorityMembershipType(RoomType roomType) {
		switch (roomType) {
		case STANDARD:
			return MembershipType.STANDARD;
		case GOLD:
			return MembershipType.GOLD;
		case PLATINUM:
			return MembershipType.PLATINUM;
		}
		return null;
	}

}
