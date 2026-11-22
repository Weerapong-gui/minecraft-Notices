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
}
