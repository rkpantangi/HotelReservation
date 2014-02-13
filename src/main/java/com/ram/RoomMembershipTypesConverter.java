package com.ram;

import java.util.List;

import com.ram.Guest.MembershipType;
import com.ram.Room.RoomType;

/**
 * This is a converter between MembershipType and RoomType, the relationship
 * could be many-many. By having an interface and different implementations, we
 * can change the approach easily.
 * 
 * @author Ram
 * 
 */
public interface RoomMembershipTypesConverter {
	RoomType getPriorityRoomTypes(MembershipType memType);

	MembershipType getPriorityMembershipType(RoomType roomType);

	List<RoomType> getEligibleRoomTypes(MembershipType memType);

	List<MembershipType> getEligibleMembershipTypes(RoomType roomType);
}
