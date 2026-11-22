package th.park.notices;

/** Pure rules, kept free of Bukkit so they are unit-testable. */
public final class Rules {
    private Rules() {}

    public static final long TICKS_PER_DAY = 24_000;

    /** In-game day number shown to players, counting from Day 1 when the world was made. */
    public static long day(long fullTime) {
        return fullTime / TICKS_PER_DAY + 1;
    }

    /** Replaces %player%, %day% and %deaths% in a configured message. */
    public static String fill(String template, String player, long day, int deaths) {
        return template.replace("%player%", player).replace("%day%", Long.toString(day)).replace("%deaths%", Integer.toString(deaths));
    }

    /** 24-hour clock for a day time (0-23999 ticks, 0 = 06:00 sunrise, 1000 ticks per hour). */
    public static String clock(long dayTime) {
        long t = Math.floorMod(dayTime, TICKS_PER_DAY);
        long hour = (t / 1000 + 6) % 24;
        long minute = (t % 1000) * 60 / 1000;
        return String.format(java.util.Locale.ROOT, "%02d:%02d", hour, minute);
    }

    /** True from 06:00 to 18:00. */
    public static boolean isDay(long dayTime) {
        return Math.floorMod(dayTime, TICKS_PER_DAY) < 12_000;
    }
}
