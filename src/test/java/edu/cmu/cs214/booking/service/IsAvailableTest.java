package edu.cmu.cs214.booking.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.cmu.cs214.booking.domain.Room;
import edu.cmu.cs214.booking.domain.TimeInterval;
import edu.cmu.cs214.booking.domain.User;
import edu.cmu.cs214.booking.repo.InMemoryBookingStore;
import org.junit.jupiter.api.Test;

class IsAvailableTest {

    private final Room roomA = new Room("A", "Alpha", 10);
    private final Room roomB = new Room("B", "Beta", 4);
    private final User alice = new User("u1", "Alice");
    private final User bob = new User("u2", "Bob");

    private BookingService newService() {
        return new BookingService(new InMemoryBookingStore());
    }

    @Test
    void isAvailableWhenRoomIsEmpty() {
        BookingService svc = newService();
        assertTrue(svc.isAvailable(roomA, new TimeInterval(600, 660)));
    }

    @Test
    void notAvailableWhenAnEarlierBookingRunsIntoTheRequest() {
        BookingService svc = newService();
        svc.book(roomA, alice, new TimeInterval(570, 630));
        assertFalse(svc.isAvailable(roomA, new TimeInterval(600, 660)));
    }

    @Test
    void notAvailableWhenAnExistingBookingContainsTheRequest() {
        BookingService svc = newService();
        svc.book(roomA, alice, new TimeInterval(540, 720));
        assertFalse(svc.isAvailable(roomA, new TimeInterval(600, 660)));
    }

    @Test
    void notAvailableWhenRequestContainsExistingBooking() {
        BookingService svc = newService();
        svc.book(roomA, alice, new TimeInterval(610, 640));
        assertFalse(svc.isAvailable(roomA, new TimeInterval(600, 660)));
    }

    @Test
    void notAvailableWhenExistingBookingExtendsPastRequest() {
        BookingService svc = newService();
        svc.book(roomA, alice, new TimeInterval(630, 720));
        assertFalse(svc.isAvailable(roomA, new TimeInterval(600, 660)));
    }

    @Test
    void notAvailableWhenIdenticalInterval() {
        BookingService svc = newService();
        svc.book(roomA, alice, new TimeInterval(600, 660));
        assertFalse(svc.isAvailable(roomA, new TimeInterval(600, 660)));
    }

    @Test
    void isAvailableAgreesWithBook() {
        BookingService svc = newService();
        svc.book(roomA, alice, new TimeInterval(570, 630));
        boolean available = svc.isAvailable(roomA, new TimeInterval(600, 660));
        BookingResult r = svc.book(roomA, bob, new TimeInterval(600, 660));
        if (available) {
            assertTrue(r instanceof BookingResult.Confirmed,
                    "isAvailable=true but book returned Waitlisted");
        } else {
            assertTrue(r instanceof BookingResult.Waitlisted,
                    "isAvailable=false but book returned Confirmed");
        }
    }

    @Test
    void touchingIntervalsDoNotConflictInEitherDirection() {
        BookingService svc = newService();
        svc.book(roomA, alice, new TimeInterval(540, 600));
        assertTrue(svc.isAvailable(roomA, new TimeInterval(600, 660)));

        BookingService svc2 = newService();
        svc2.book(roomA, bob, new TimeInterval(660, 720));
        assertTrue(svc2.isAvailable(roomA, new TimeInterval(600, 660)));
    }

    @Test
    void differentRoomsDoNotConflict() {
        BookingService svc = newService();
        svc.book(roomA, alice, new TimeInterval(600, 660));
        assertTrue(svc.isAvailable(roomB, new TimeInterval(600, 660)));
    }
}