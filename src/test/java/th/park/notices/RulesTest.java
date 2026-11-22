package th.park.notices;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RulesTest {
    @Test void firstDayIsDayOne() { assertEquals(1, Rules.day(0)); }
    @Test void lastTickOfFirstDay() { assertEquals(1, Rules.day(23_999)); }
    @Test void secondDay() { assertEquals(2, Rules.day(24_000)); }
    @Test void longWorld() { assertEquals(43, Rules.day(42L * 24_000 + 500)); }

    @Test void fillsPlaceholders() {
        assertEquals("Welcome back, Verpong321! Day 7", Rules.fill("Welcome back, %player%! Day %day%", "Verpong321", 7, 3));
    }
    @Test void deathsPlaceholder() {
        assertEquals("Deaths: 3", Rules.fill("Deaths: %deaths%", "x", 1, 3));
    }

    // day time 0 is 06:00, sunrise; 1000 ticks = 1 hour
    @Test void sunrise() { assertEquals("06:00", Rules.clock(0)); }
    @Test void noon() { assertEquals("12:00", Rules.clock(6000)); }
    @Test void halfPast() { assertEquals("07:30", Rules.clock(1500)); }
    @Test void midnight() { assertEquals("00:00", Rules.clock(18000)); }
    @Test void justBeforeSunrise() { assertEquals("05:59", Rules.clock(23999)); }
    @Test void wrapsPastADay() { assertEquals("06:00", Rules.clock(24000)); }
    @Test void dayAndNight() {
        assertTrue(Rules.isDay(0));
        assertTrue(Rules.isDay(11999));
        assertFalse(Rules.isDay(12000));
        assertFalse(Rules.isDay(23999));
    }
}
