package com.ram;

/**
 * Domain object for Guest
 * 
 * @author Ram
 * 
 */
public class Guest {
	public static enum MembershipType {
		STANDARD, GOLD, PLATINUM
	}

	private String guestId;
	private MembershipType memType;
	private String name;

	public String getGuestId() {
		return guestId;
	}

	public void setGuestId(String guestId) {
		this.guestId = guestId;
	}

	public MembershipType getMemType() {
		return memType;
	}

	public void setMemType(MembershipType memType) {
		this.memType = memType;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
