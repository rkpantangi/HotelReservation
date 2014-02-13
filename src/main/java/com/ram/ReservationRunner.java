package com.ram;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ram.Guest.MembershipType;
import com.ram.Room.RoomType;

/**
 * This simulates a concurrent scenario of check-in and check-out. It doesn't
 * certainly cover all the scenarios, but certainly gives an idea of how the
 * program is accurate from concurrency point of view. Due to lack of time, I am
 * not implementing more scenarios at this time.
 * 
 * @author Ram
 * 
 */
public class ReservationRunner {
	private static final Logger logger = LoggerFactory.getLogger(ReservationRunner.class);
	private RoomMembershipTypesConverter converter = new PriorityBasedRoomMembershipTypesConverter();
	private DefaultBookingService bookingService;
	private CountDownLatch latch;
	private DefaultRoomService roomService;
	private DefaultGuestService guestService;

	public ReservationRunner() {
		roomService = new DefaultRoomService(getDefaultRoomsCache());
		guestService = new DefaultGuestService(getDefaultGuestCache());
		bookingService = new DefaultBookingService(20, guestService, roomService, converter);
	}

	public static void main(String[] args) throws Exception {
		ReservationRunner runner = new ReservationRunner();
		runner.runPossibleConcurrentScenario();
	}

	private void runPossibleConcurrentScenario() throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(10);
		latch = new CountDownLatch(50);
		for (int i = 1; i <= 50; i++) {
			// check in 50 standard membership users, there are only 30 rooms,
			// so finally there should be 20 in waiting list
			executor.submit(new CheckInRunnable("G" + i));
		}
		// keeping it simple, with no timed waiting
		latch.await();
		Collection<Booking> waitingList = bookingService.getWaitingList();
		logger.info("WaitingList size: {}", waitingList.size());
		logger.info("Occupancy ratio: {}", bookingService.getOccupancyRatio());

		// now checkout simultaneously many guests
		latch = new CountDownLatch(20);
		for (int i = 1; i <= 20; i++) {
			executor.submit(new CheckOutRunnable("R" + i));
		}
		// keeping it simple, with no timed waiting
		latch.await();
		logger.info("WaitingList size: {}", waitingList.size());
		logger.info("Occupancy ratio: {}", bookingService.getOccupancyRatio());
		System.exit(0);
	}

	private class CheckInRunnable implements Runnable {
		private String guestId;

		public CheckInRunnable(String guestId) {
			this.guestId = guestId;
		}

		@Override
		public void run() {
			bookingService.checkIn(guestId);
			latch.countDown();
		}

	}

	private class CheckOutRunnable implements Runnable {
		private String roomNo;

		private CheckOutRunnable(String roomNo) {
			this.roomNo = roomNo;
		}

		@Override
		public void run() {
			bookingService.checkOut(roomNo);
			latch.countDown();
		}

	}

	private Map<String, Room> getDefaultRoomsCache() {
		Map<String, Room> roomCache = new LinkedHashMap<>(100);
		for (int i = 1; i <= 50; i++) {
			Room room = new Room();
			room.setRoomNo("R" + i);
			if (i <= 30)
				room.setType(RoomType.STANDARD);
			else if (i <= 40)
				room.setType(RoomType.GOLD);
			else
				room.setType(RoomType.PLATINUM);
			roomCache.put(room.getRoomNo(), room);
		}
		return roomCache;
	}

	private Map<String, Guest> getDefaultGuestCache() {
		Map<String, Guest> guestCache = new LinkedHashMap<>();
		for (int i = 1; i <= 100; i++) {
			Guest guest = new Guest();
			guest.setGuestId("G" + i);
			if (i <= 50)
				guest.setMemType(MembershipType.STANDARD);
			else if (i <= 80)
				guest.setMemType(MembershipType.GOLD);
			else
				guest.setMemType(MembershipType.PLATINUM);
			guest.setName("GuestName" + i);
			guestCache.put(guest.getGuestId(), guest);
		}
		return guestCache;
	}

}
