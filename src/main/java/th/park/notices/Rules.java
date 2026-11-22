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
}
