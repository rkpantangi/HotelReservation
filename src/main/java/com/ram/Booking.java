package com.ram;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Domain Object for Booking
 * 
 * @author Ram
 * 
 */
public class Booking {
	public static enum BookingStatus {
		CONFIRMED, WAITING, PRIORITY_WAITING, CHECKEDOUT
	}

	private int bookingId;
	private String roomNo;
	private String guestId;
	private Date created;
	private BookingStatus status = BookingStatus.WAITING;

	// quick and dirty unique key
	private static final AtomicInteger bookingCounter = new AtomicInteger();

	public Booking(String guestId) {
		bookingId = bookingCounter.incrementAndGet();
		created = new Date();
		this.guestId = guestId;
	}

	public int getBookingId() {
		return bookingId;
	}

	public void setRoomNo(String roomNo) {
		this.roomNo = roomNo;
	}

	public String getRoomNo() {
		return roomNo;
	}

	public String getGuestId() {
		return guestId;
	}

	public BookingStatus getStatus() {
		return status;
	}

	public void setStatus(BookingStatus status) {
		this.status = status;
	}

	public Date getCreated() {
		return new Date(created.getTime());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + bookingId;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Booking other = (Booking) obj;
		if (bookingId != other.bookingId)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Booking [bookingId=" + bookingId + ", roomNo=" + roomNo + ", guestId=" + guestId + ", created="
				+ created + ", status=" + status + "]";
	}

}
