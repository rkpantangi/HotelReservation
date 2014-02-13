package com.ram;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ram.Booking.BookingStatus;
import com.ram.BookingService.BookingEventListener;
import com.ram.Guest.MembershipType;
import com.ram.Room.RoomType;

/**
 * Considering this is an interview test, to save some time, I made a few
 * compromises:
 * <p>
 * 1. Used Thread.sleep in couple of places to keep tests simple, in real
 * projects I strive hard to avoid them.
 * <p>
 * 2. Used some tests without stubbing to keep tests easy to understand.
 * 
 * @author Ram
 * 
 */
public class DefaultBookingServiceTest {
	private static final double EPSILON = 0.000001;
	private DefaultRoomService roomService;
	private DefaultGuestService guestService;
	private BookingService bookingService;
	private RoomMembershipTypesConverter converter = new PriorityBasedRoomMembershipTypesConverter();

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		roomService = new DefaultRoomService(getDefaultRoomsCache());
		guestService = new DefaultGuestService(getDefaultGuestCache());
	}

	private Map<String, Room> getDefaultRoomsCache() {
		Map<String, Room> roomCache = new LinkedHashMap<>(10);
		for (int i = 1; i <= 10; i++) {
			Room room = new Room();
			room.setRoomNo("R" + i);
			if (i <= 5)
				room.setType(RoomType.STANDARD);
			else if (i <= 8)
				room.setType(RoomType.GOLD);
			else
				room.setType(RoomType.PLATINUM);
			roomCache.put(room.getRoomNo(), room);
		}
		return roomCache;
	}

	private Map<String, Guest> getDefaultGuestCache() {
		Map<String, Guest> guestCache = new LinkedHashMap<>();
		for (int i = 1; i <= 10; i++) {
			Guest guest = new Guest();
			guest.setGuestId("G" + i);
			if (i <= 5)
				guest.setMemType(MembershipType.STANDARD);
			else if (i <= 8)
				guest.setMemType(MembershipType.GOLD);
			else
				guest.setMemType(MembershipType.PLATINUM);
			guest.setName("GuestName" + i);
			guestCache.put(guest.getGuestId(), guest);
		}
		return guestCache;
	}

	@Test
	public void first_standard_guest_gets_standard_room() {
		bookingService = new DefaultBookingService(20, guestService, roomService, converter);

		Booking booking = bookingService.checkIn("G1");
		assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
		assertEquals("R1", booking.getRoomNo());
		assertEquals(10d, bookingService.getOccupancyRatio(), EPSILON);
	}

	@Test
	public void second_standard_guest_gets_standard_room() {
		bookingService = new DefaultBookingService(20, guestService, roomService, converter);

		bookingService.checkIn("G1");

		String roomNo = bookingService.checkIn("G2").getRoomNo();
		assertEquals("R2", roomNo);
		assertEquals(20d, bookingService.getOccupancyRatio(), EPSILON);
	}

	@Test
	public void first_gold_guest_gets_gold_room() {
		bookingService = new DefaultBookingService(20, guestService, roomService, converter);

		String roomNo = bookingService.checkIn("G6").getRoomNo();
		assertEquals("R6", roomNo);
	}

	@Test
	public void second_gold_guest_gets_gold_room() {
		bookingService = new DefaultBookingService(20, guestService, roomService, converter);

		bookingService.checkIn("G6");
		String roomNo = bookingService.checkIn("G7").getRoomNo();
		assertEquals("R7", roomNo);
	}

	@Test
	public void first_platinm_guest_gets_platinum_room() {
		bookingService = new DefaultBookingService(20, guestService, roomService, converter);

		String roomNo = bookingService.checkIn("G9").getRoomNo();
		assertEquals("R9", roomNo);
	}

	@Test
	public void second_platinum_guest_gets_platinum_room() {
		bookingService = new DefaultBookingService(20, guestService, roomService, converter);

		bookingService.checkIn("G9");
		String roomNo = bookingService.checkIn("G10").getRoomNo();
		assertEquals("R10", roomNo);
	}

	@Test
	public void checkin_platinum_guest_when_only_gold_standard_rooms_available() {
		bookingService = new DefaultBookingService(20, guestService, roomService, converter);

		bookingService.checkIn("G9");
		bookingService.checkIn("G10");
		String roomNo = bookingService.checkIn("G9").getRoomNo();
		assertEquals("R6", roomNo);
	}

	@Test
	public void checkin_gold_guest_when_only_standard_rooms_available() {
		bookingService = new DefaultBookingService(20, guestService, roomService, converter);

		bookingService.checkIn("G8");
		bookingService.checkIn("G7");
		bookingService.checkIn("G6");
		assertEquals(30d, bookingService.getOccupancyRatio(), EPSILON);
		String roomNo = bookingService.checkIn("G7").getRoomNo();
		assertEquals("R1", roomNo);
		assertEquals(40d, bookingService.getOccupancyRatio(), EPSILON);
	}

	@Test
	public void checkout_a_room_and_checkin_another_guest() {
		bookingService = new DefaultBookingService(20, guestService, roomService, converter);

		bookingService.checkIn("G9");
		bookingService.checkIn("G10");
		bookingService.checkOut("R9");
		bookingService.checkOut("R10");
		String roomNo = bookingService.checkIn("G10").getRoomNo();
		assertEquals("R9", roomNo);
		assertEquals(10d, bookingService.getOccupancyRatio(), EPSILON);
	}

	@Test
	public void checkout_triggers_checkin_of_guest_in_waiting_list() throws InterruptedException {
		Map<String, Guest> localGuestCache = getDefaultGuestCache();
		// add one more guest with STANDARD membership
		Guest guest = new Guest();
		guest.setGuestId("G11");
		guest.setMemType(MembershipType.STANDARD);
		guest.setName("GuestName11");
		localGuestCache.put(guest.getGuestId(), guest);

		DefaultGuestService localGuestService = new DefaultGuestService(localGuestCache);
		bookingService = new DefaultBookingService(20, localGuestService, roomService, converter);

		bookingService.checkIn("G1");
		bookingService.checkIn("G2");
		bookingService.checkIn("G3");
		bookingService.checkIn("G4");
		Booking booking1 = bookingService.checkIn("G5");

		// all standard rooms are booked, so keep the next guest waiting
		Booking booking2 = bookingService.checkIn("G11");
		assertEquals(BookingStatus.WAITING, booking2.getStatus());
		// subscribe to booking events
		final CountDownLatch confirmLatch = new CountDownLatch(1);
		bookingService.subscribeToBookingEvents(new BookingEventListener() {

			@Override
			public void updated(Booking booking) {
				confirmLatch.countDown();
			}

		});

		// now check out one guest, which should confirm the waiting guest
		// booking
		bookingService.checkOut(booking1.getRoomNo());

		// NOTE: this is not an approach I use in Unit tests, but considering
		// this is an interview test and in the interest of time, I have taken a
		// quick route.
		confirmLatch.await(2000, TimeUnit.SECONDS);
		assertEquals(BookingStatus.CONFIRMED, booking2.getStatus());
		assertEquals(booking1.getRoomNo(), booking2.getRoomNo());
	}

	@Test
	public void move_the_longest_waiting_guest_into_priority_waiting_list() throws InterruptedException {
		DefaultGuestService localGuestService = mock(DefaultGuestService.class);
		DefaultRoomService localRoomService = mock(DefaultRoomService.class);
		bookingService = new DefaultBookingService(1, localGuestService, localRoomService, converter);

		Guest guest11 = new Guest();
		guest11.setGuestId("G11");
		guest11.setMemType(MembershipType.STANDARD);
		guest11.setName("GuestName11");
		when(localGuestService.getGuest(guest11.getGuestId())).thenReturn(guest11);

		Booking booking1 = bookingService.checkIn(guest11.getGuestId());
		Thread.sleep(2000);
		assertEquals(BookingStatus.PRIORITY_WAITING, booking1.getStatus());
	}

	@Test
	public void choose_priority_waiting_guest_over_regular_waiting_guest_both_have_same_memebership_types()
			throws Exception {
		DefaultGuestService localGuestService = mock(DefaultGuestService.class);
		DefaultRoomService localRoomService = mock(DefaultRoomService.class);
		bookingService = new DefaultBookingService(1, localGuestService, localRoomService, converter);

		// checkout a standard room
		Room room1 = new Room();
		room1.setRoomNo("R1");
		room1.setType(RoomType.STANDARD);
		when(localRoomService.getRoomInfo(room1.getRoomNo())).thenReturn(room1);
		when(localRoomService.reserveRoom(RoomType.STANDARD)).thenReturn(room1);

		Guest guest1 = new Guest();
		guest1.setGuestId("G1");
		guest1.setMemType(MembershipType.STANDARD);
		when(localGuestService.getGuest(guest1.getGuestId())).thenReturn(guest1);

		bookingService.checkIn(guest1.getGuestId());

		// no more rooms left
		when(localRoomService.reserveRoom(RoomType.STANDARD)).thenReturn(null);
		Guest guest2 = new Guest();
		guest2.setGuestId("G1");
		guest2.setMemType(MembershipType.STANDARD);
		when(localGuestService.getGuest(guest2.getGuestId())).thenReturn(guest2);

		Booking booking1 = bookingService.checkIn(guest2.getGuestId());
		Thread.sleep(2000);
		assertEquals(BookingStatus.PRIORITY_WAITING, booking1.getStatus());

		Guest guest3 = new Guest();
		guest3.setGuestId("G2");
		guest3.setMemType(MembershipType.STANDARD);
		when(localGuestService.getGuest(guest3.getGuestId())).thenReturn(guest3);

		Booking booking2 = bookingService.checkIn(guest3.getGuestId());
		assertEquals(BookingStatus.WAITING, booking2.getStatus());

		bookingService.checkOut(room1.getRoomNo());
		verify(localRoomService).getRoomInfo(room1.getRoomNo());
		assertEquals(BookingStatus.CONFIRMED, booking1.getStatus());
		assertEquals(room1.getRoomNo(), booking1.getRoomNo());
		assertEquals(BookingStatus.WAITING, booking2.getStatus());
	}

	@Test
	public void choose_priority_waiting_guest_over_regular_waiting_guest_both_have_different_memebership_types()
			throws Exception {
		DefaultGuestService localGuestService = mock(DefaultGuestService.class);
		DefaultRoomService localRoomService = mock(DefaultRoomService.class);
		bookingService = new DefaultBookingService(1, localGuestService, localRoomService, converter);

		// checkout a standard room
		Room room1 = new Room();
		room1.setRoomNo("R1");
		room1.setType(RoomType.STANDARD);
		when(localRoomService.getRoomInfo(room1.getRoomNo())).thenReturn(room1);
		when(localRoomService.reserveRoom(RoomType.STANDARD)).thenReturn(room1);

		Guest guest1 = new Guest();
		guest1.setGuestId("G1");
		guest1.setMemType(MembershipType.STANDARD);
		when(localGuestService.getGuest(guest1.getGuestId())).thenReturn(guest1);

		bookingService.checkIn(guest1.getGuestId());

		// no more rooms left
		when(localRoomService.reserveRoom(RoomType.STANDARD)).thenReturn(null);
		Guest guest2 = new Guest();
		guest2.setGuestId("G1");
		guest2.setMemType(MembershipType.STANDARD);
		when(localGuestService.getGuest(guest2.getGuestId())).thenReturn(guest2);

		Booking booking1 = bookingService.checkIn(guest2.getGuestId());
		Thread.sleep(2000);
		assertEquals(BookingStatus.PRIORITY_WAITING, booking1.getStatus());

		Guest guest3 = new Guest();
		guest3.setGuestId("G2");
		guest3.setMemType(MembershipType.GOLD);
		when(localGuestService.getGuest(guest3.getGuestId())).thenReturn(guest3);

		Booking booking2 = bookingService.checkIn(guest3.getGuestId());
		assertEquals(BookingStatus.WAITING, booking2.getStatus());

		bookingService.checkOut(room1.getRoomNo());
		verify(localRoomService).getRoomInfo(room1.getRoomNo());
		assertEquals(BookingStatus.CONFIRMED, booking1.getStatus());
		assertEquals(room1.getRoomNo(), booking1.getRoomNo());
		assertEquals(BookingStatus.WAITING, booking2.getStatus());
	}

	@Test
	public void get_waiting_list_details() throws Exception {
		DefaultGuestService localGuestService = mock(DefaultGuestService.class);
		DefaultRoomService localRoomService = mock(DefaultRoomService.class);
		bookingService = new DefaultBookingService(1, localGuestService, localRoomService, converter);

		// no more rooms left
		when(localRoomService.reserveRoom(RoomType.STANDARD)).thenReturn(null);
		Guest guest2 = new Guest();
		guest2.setGuestId("G1");
		guest2.setMemType(MembershipType.STANDARD);
		when(localGuestService.getGuest(guest2.getGuestId())).thenReturn(guest2);

		Booking booking1 = bookingService.checkIn(guest2.getGuestId());
		Thread.sleep(2000);
		assertEquals(BookingStatus.PRIORITY_WAITING, booking1.getStatus());

		Guest guest3 = new Guest();
		guest3.setGuestId("G2");
		guest3.setMemType(MembershipType.GOLD);
		when(localGuestService.getGuest(guest3.getGuestId())).thenReturn(guest3);

		Booking booking2 = bookingService.checkIn(guest3.getGuestId());
		assertEquals(BookingStatus.WAITING, booking2.getStatus());

		Collection<Booking> bookings = bookingService.getWaitingList();
		assertEquals(2, bookings.size());
		Iterator<Booking> iter = bookings.iterator();
		Booking first = iter.next();
		Booking second = iter.next();

		assertEquals(BookingStatus.PRIORITY_WAITING, first.getStatus());
		assertTrue(first.getCreated().before(new Date()));
		assertEquals(BookingStatus.WAITING, second.getStatus());
		assertTrue(second.getCreated().before(new Date()));
	}
}
