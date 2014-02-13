package com.ram;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ram.Booking.BookingStatus;
import com.ram.Guest.MembershipType;
import com.ram.Room.RoomType;

/**
 * A booking service implementation. I used synchronized on all methods
 * intentionally as it is quite important do the check-in and check-out in a
 * mutually exclusive manner because they both update waiting lists/booking
 * information etc.
 * 
 * 
 * @author Ram
 * 
 */
public class DefaultBookingService implements BookingService {
	private static final Logger logger = LoggerFactory.getLogger(DefaultBookingService.class);

	// roomNo --> Booking (one room, one booking)
	private volatile Map<String, Booking> bookings = new HashMap<>();

	// MembershipType --> List of Bookings, in Standard Waiting
	private Map<MembershipType, LinkedList<Booking>> standardWaitingList = new HashMap<>();

	// MembershipType --> List of Bookings, in Priority Waiting (Waited for more
	// than the Max Waiting Time)
	private Map<MembershipType, LinkedList<Booking>> priorityWaitingList = new HashMap<>();

	// this additional map is to reduce the time complexity of reads (for
	// getWaitingList implementation), it is totally optional.
	// BookingId --> Booking
	private Map<Integer, Booking> allWaitingLists = new HashMap<>();

	private CopyOnWriteArraySet<BookingEventListener> listeners = new CopyOnWriteArraySet<>();

	// just chose random number of threads, but in real projects, I generally
	// estimate based on the load characteristics of the application.
	private final ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(3);
	private final RoomService roomService;
	private GuestService guestService;
	private final RoomMembershipTypesConverter roomTypesFinder;

	// To make testing quick, I have used Seconds, though in real-world probably
	// minutes/hours make more sense
	private final int MAX_WAITING_TIME_IN_SECONDS;

	public DefaultBookingService(int maxWaitingTimeInMinutes, GuestService guestService, RoomService roomService,
			RoomMembershipTypesConverter roomTypesFinder) {
		this.MAX_WAITING_TIME_IN_SECONDS = maxWaitingTimeInMinutes;
		this.roomService = roomService;
		this.guestService = guestService;
		this.roomTypesFinder = roomTypesFinder;

		for (MembershipType memType : MembershipType.values()) {
			standardWaitingList.put(memType, new LinkedList<Booking>());
		}

		for (MembershipType memType : MembershipType.values()) {
			priorityWaitingList.put(memType, new LinkedList<Booking>());
		}

	}

	@Override
	public synchronized Booking checkIn(String guestId) {
		if (guestId == null)
			throw new IllegalArgumentException("GuestId must not be null.");
		Guest guest = guestService.getGuest(guestId);
		if (guest == null)
			throw new IllegalArgumentException("Invalid GuestId");

		Booking booking = new Booking(guest.getGuestId());
		String roomNo = reserveRoomForType(guest.getMemType());
		if (roomNo != null) {
			// found a room
			booking.setStatus(BookingStatus.CONFIRMED);
			booking.setRoomNo(roomNo);
			bookings.put(roomNo, booking);
			logger.info("Checked In Guest: {}, into RoomNo: {}", booking.getGuestId(), roomNo);
		} else {
			// did not find a room, so keep the guest on the waiting list
			standardWaitingList.get(guest.getMemType()).add(booking);
			allWaitingLists.put(booking.getBookingId(), booking);
			// start timer to track how long a guest is waiting
			scheduler.schedule(new WaitingTimeTrackingRunnable(booking), MAX_WAITING_TIME_IN_SECONDS, TimeUnit.SECONDS);
		}
		return booking;
	}

	private String reserveRoomForType(MembershipType memType) {
		String roomNo = null;
		List<RoomType> roomTypes = roomTypesFinder.getEligibleRoomTypes(memType);
		for (RoomType roomType : roomTypes) {
			Room room = roomService.reserveRoom(roomType);
			if (room == null)
				continue;
			roomNo = room.getRoomNo();
			break;
		}
		return roomNo;
	}

	class WaitingTimeTrackingRunnable implements Runnable {
		private Booking booking;

		public WaitingTimeTrackingRunnable(Booking booking) {
			this.booking = booking;
		}

		@Override
		public void run() {
			synchronized (DefaultBookingService.this) {
				Guest guest = guestService.getGuest(booking.getGuestId());
				// remove the waiting guest from regular waiting list
				standardWaitingList.get(guest.getMemType()).remove(booking);
				// and then add this guest to the priority waiting list
				priorityWaitingList.get(guest.getMemType()).add(booking);
				booking.setStatus(BookingStatus.PRIORITY_WAITING);
			}
		}

	}

	@Override
	public synchronized void checkOut(String roomNo) {
		if (roomNo == null)
			throw new IllegalArgumentException("Room Number must not be null.");

		Booking currentBooking = bookings.remove(roomNo);
		if (currentBooking == null) {
			logger.info("No booking for the room: {}", roomNo);
			return;
		}
		roomService.freeUpRoom(roomNo);
		logger.info("Checked out Guest: {}, from RoomNo: {}", currentBooking.getGuestId(), roomNo);

		// check if anybody else is eligible for this room and waiting
		Room room = roomService.getRoomInfo(roomNo);
		Booking newBooking = null;
		MembershipType priorityMemType = roomTypesFinder.getPriorityMembershipType(room.getType());
		LinkedList<Booking> pList = priorityWaitingList.get(priorityMemType);
		if (pList.isEmpty()) {
			// no priority waiting list, so pick somebody from the regular
			// waiting list
			newBooking = pickUpBookingFromWaitingList(room.getType());
		} else {
			// pickup a guest from the priorty waiting list
			newBooking = pList.removeFirst();
		}
		if (newBooking != null) {
			newBooking.setStatus(BookingStatus.CONFIRMED);
			newBooking.setRoomNo(roomNo);
			bookings.put(roomNo, newBooking);
			allWaitingLists.remove(newBooking.getBookingId());
			fireBookingUpdate(newBooking);
			logger.info("Checked In Guest: {} from waiting list, into RoomNo: {}", currentBooking.getGuestId(), roomNo);
		}
	}

	/**
	 * Based on the requirement, this method finds all the eligible types of
	 * members for a given room type and chooses them from the waiting list.
	 * 
	 * @param roomType
	 * @return
	 */
	private Booking pickUpBookingFromWaitingList(RoomType roomType) {
		Booking booking = null;
		List<MembershipType> memTypes = roomTypesFinder.getEligibleMembershipTypes(roomType);
		for (MembershipType memType : memTypes) {
			LinkedList<Booking> wList = standardWaitingList.get(memType);
			if (wList.isEmpty()) {
				continue;
			}
			booking = wList.removeFirst();
			break;
		}
		return booking;
	}

	private void fireBookingUpdate(Booking booking) {
		for (BookingEventListener listener : listeners) {
			listener.updated(booking);
		}
	}

	@Override
	public void subscribeToBookingEvents(BookingEventListener listener) {
		if (listener == null)
			throw new IllegalArgumentException("Listener must not be null.");

		listeners.add(listener);
	}

	@Override
	public void unsubscribeFromBookingEvents(BookingEventListener listener) {
		listeners.remove(listener);
	}

	@Override
	public double getOccupancyRatio() {
		return (bookings.size() / (double) roomService.getTotalRooms()) * 100;
	}

	@Override
	public Collection<Booking> getWaitingList() {
		return Collections.unmodifiableCollection(allWaitingLists.values());
	}
}
