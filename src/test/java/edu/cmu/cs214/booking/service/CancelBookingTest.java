package edu.cmu.cs214.booking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.cmu.cs214.booking.domain.Booking;
import edu.cmu.cs214.booking.domain.Room;
import edu.cmu.cs214.booking.domain.TimeInterval;
import edu.cmu.cs214.booking.domain.User;
import edu.cmu.cs214.booking.repo.InMemoryBookingStore;
import org.junit.jupiter.api.Test;

class CancelBookingTest {

    private final Room roomA = new Room("A", "Alpha", 10);
    private final Room roomB = new Room("B", "Beta", 4);
    private final User alice = new User("u1", "Alice");
    private final User bob = new User("u2", "Bob");
    private final User carol = new User("u3", "Carol");
    private final User dave = new User("u4", "Dave");

    private BookingService newService() {
        return new BookingService(new InMemoryBookingStore());
    }

    @Test
    void cancelBookingRemovesTheBooking() {
        BookingService svc = newService();
        BookingResult r = svc.book(roomA, alice, new TimeInterval(600, 660));
        Booking a = assertInstanceOf(BookingResult.Confirmed.class, r).booking();
        assertEquals(1, svc.listBookings(roomA).size());

        svc.cancelBooking(a.id());
        assertEquals(0, svc.listBookings(roomA).size());
    }

    @Test
    void cancelUnknownIdDoesNothing() {
        BookingService svc = newService();
        svc.book(roomA, alice, new TimeInterval(600, 660));
        svc.book(roomA, bob, new TimeInterval(700, 760));

        svc.cancelBooking("nonexistent");
        assertEquals(2, svc.listBookings(roomA).size());
    }

    @Test
    void cancellingPromotesTheWaitlistedUser() {
        InMemoryBookingStore store = new InMemoryBookingStore();
        BookingService svc = new BookingService(store);

        BookingResult first = svc.book(roomA, alice, new TimeInterval(600, 660));
        Booking a = assertInstanceOf(BookingResult.Confirmed.class, first).booking();
        assertInstanceOf(BookingResult.Waitlisted.class,
                svc.book(roomA, bob, new TimeInterval(630, 700)));

        svc.cancelBooking(a.id());

        assertEquals(1, svc.listBookings(roomA).size());
        Booking promoted = svc.listBookings(roomA).get(0);
        assertEquals("u2", promoted.user().id());
        assertEquals(630, promoted.interval().start());
        assertEquals(700, promoted.interval().end());
        assertTrue(store.waitlistForRoom(roomA).isEmpty());
    }

    @Test
    void promotionIsFirstComeFirstServedAndAtMostOne() {
        InMemoryBookingStore store = new InMemoryBookingStore();
        BookingService svc = new BookingService(store);

        BookingResult first = svc.book(roomA, alice, new TimeInterval(600, 720));
        Booking a = assertInstanceOf(BookingResult.Confirmed.class, first).booking();

        svc.book(roomA, bob, new TimeInterval(600, 660));
        svc.book(roomA, carol, new TimeInterval(660, 700));

        svc.cancelBooking(a.id());

        assertEquals(1, svc.listBookings(roomA).size());
        Booking promoted = svc.listBookings(roomA).get(0);
        assertEquals("u2", promoted.user().id());
        assertEquals(1, store.waitlistForRoom(roomA).size());
        assertEquals("u3", store.waitlistForRoom(roomA).get(0).user().id());
    }

    @Test
    void skipsWaiterWhoseIntervalStillConflicts() {
        InMemoryBookingStore store = new InMemoryBookingStore();
        BookingService svc = new BookingService(store);

        BookingResult r1 = svc.book(roomA, alice, new TimeInterval(600, 660));
        Booking a = assertInstanceOf(BookingResult.Confirmed.class, r1).booking();
        svc.book(roomA, dave, new TimeInterval(700, 760));

        svc.book(roomA, bob, new TimeInterval(630, 730));
        svc.book(roomA, carol, new TimeInterval(610, 650));

        assertEquals(2, store.waitlistForRoom(roomA).size());

        svc.cancelBooking(a.id());

        assertEquals(2, svc.listBookings(roomA).size());
        assertFalse(svc.listBookings(roomA).stream()
                .anyMatch(b -> b.user().id().equals("u2")));
        assertTrue(svc.listBookings(roomA).stream()
                .anyMatch(b -> b.user().id().equals("u3")));
        assertEquals(1, store.waitlistForRoom(roomA).size());
        assertEquals("u2", store.waitlistForRoom(roomA).get(0).user().id());
    }

    @Test
    void cancellationDoesNotPromoteAcrossRooms() {
        InMemoryBookingStore store = new InMemoryBookingStore();
        BookingService svc = new BookingService(store);

        BookingResult r = svc.book(roomA, alice, new TimeInterval(600, 660));
        Booking a = assertInstanceOf(BookingResult.Confirmed.class, r).booking();
        svc.book(roomB, bob, new TimeInterval(600, 660));

        svc.cancelBooking(a.id());

        assertEquals(0, svc.listBookings(roomA).size());
        assertEquals(1, svc.listBookings(roomB).size());
        assertEquals("u2", svc.listBookings(roomB).get(0).user().id());
        assertTrue(store.waitlistForRoom(roomB).isEmpty());
    }
}