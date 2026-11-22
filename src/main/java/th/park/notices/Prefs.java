package th.park.notices;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;

/** Each player's on/off choice per notice, saved to prefs.yml. Unset means the configured default. */
public final class Prefs {
    public enum Notice {
        DAY("day"), DEATHS("deaths-in-tab"), WELCOME("welcome"), CLOCK("clock");

        final String key;
        Notice(String key) { this.key = key; }
    }

    private final File file;
    private final Logger log;
    private final Map<Notice, Boolean> defaults;
    private final Map<UUID, Map<Notice, Boolean>> choices = new HashMap<>();

    public Prefs(File file, Logger log, Map<Notice, Boolean> defaults) {
        this.file = file;
        this.log = log;
        this.defaults = defaults;
    }

    public void load() {
        choices.clear();
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        for (String id : y.getKeys(false)) {
            UUID u;
            try { u = UUID.fromString(id); } catch (IllegalArgumentException e) { continue; }
            Map<Notice, Boolean> m = new HashMap<>();
            for (Notice n : Notice.values()) if (y.isBoolean(id + "." + n.key)) m.put(n, y.getBoolean(id + "." + n.key));
            choices.put(u, m);
        }
    }

    public boolean on(UUID player, Notice n) {
        Boolean v = choices.getOrDefault(player, Map.of()).get(n);
        return v != null ? v : defaults.getOrDefault(n, true);
    }

    /** Flips the choice and returns the new value. */
    public boolean toggle(UUID player, Notice n) {
        boolean now = !on(player, n);
        choices.computeIfAbsent(player, k -> new HashMap<>()).put(n, now);
        save();
        return now;
    }

    private void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (var e : choices.entrySet())
            for (var c : e.getValue().entrySet()) y.set(e.getKey() + "." + c.getKey().key, c.getValue());
        try {
            y.save(file);
        } catch (IOException ex) {
            log.severe("could not save " + file + ": " + ex.getMessage());
        }
    }
}
