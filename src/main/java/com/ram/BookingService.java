package com.ram;

import java.util.Collection;

/**
 * API for booking service
 * 
 * @author Ram
 * 
 */
public interface BookingService {
	/**
	 * Checks in a guest by their guestId
	 * 
	 * @param guestId
	 * @return
	 */
	Booking checkIn(String guestId);

	/**
	 * Checks out a room
	 * 
	 * @param roomNo
	 */
	void checkOut(String roomNo);

	/**
	 * Client can subscribe to booking updates, like waiting list got confirmed
	 * etc.
	 * 
	 * @param listener
	 */
	void subscribeToBookingEvents(BookingEventListener listener);

	/**
	 * Unsubscribe from booking events.
	 * 
	 * @param listener
	 */
	void unsubscribeFromBookingEvents(BookingEventListener listener);

	/**
	 * Finds the occupancy ratio at any given time
	 * 
	 * @return
	 */
	double getOccupancyRatio();

	/**
	 * Gets the list of waiting bookings.
	 * 
	 * @return
	 */
	Collection<Booking> getWaitingList();

	/**
	 * @author Ram
	 * 
	 */
	public static interface BookingEventListener {
		void updated(Booking booking);
	}
}
