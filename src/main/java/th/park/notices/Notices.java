package th.park.notices;

import java.io.File;
import java.time.Duration;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;
import th.park.notices.Prefs.Notice;

/**
 * Three notices each player can switch off in /settings: a "Day N" title each in-game morning,
 * everyone's death count in the Tab list, and a welcome title with a sound on joining.
 */
public final class Notices extends JavaPlugin implements Listener {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final int SLOT_DAY = 10, SLOT_DEATHS = 12, SLOT_WELCOME = 14, SLOT_CLOCK = 16;

    private Prefs prefs;
    private FileConfiguration cfg;
    /** Players who opt in see this board: the death counts sit in its Tab list slot. */
    private Scoreboard deathsBoard;
    private Objective deaths;
    /**
     * The clock is the topmost thing a server can place: a boss bar titled "☀ 07:30". White and empty, and
     * the server resource pack makes the white bar transparent, so Java players see only the text.
     */
    private final net.kyori.adventure.bossbar.BossBar clock = net.kyori.adventure.bossbar.BossBar.bossBar(
        Component.empty(), 0f, net.kyori.adventure.bossbar.BossBar.Color.WHITE, net.kyori.adventure.bossbar.BossBar.Overlay.PROGRESS);
    private final Map<UUID, Long> lastDay = new HashMap<>();

    @Override public void onEnable() {
        saveDefaultConfig();
        cfg = getConfig();
        Map<Notice, Boolean> defaults = new EnumMap<>(Notice.class);
        for (Notice n : Notice.values()) defaults.put(n, cfg.getBoolean("defaults." + n.key, true));
        prefs = new Prefs(new File(getDataFolder(), "prefs.yml"), getLogger(), defaults);
        prefs.load();

        deathsBoard = getServer().getScoreboardManager().getNewScoreboard();
        deaths = deathsBoard.registerNewObjective("deaths", Criteria.DUMMY, Component.text("Deaths"), RenderType.INTEGER);
        deaths.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        tickClock();

        getServer().getPluginManager().registerEvents(this, this);
        for (Player p : getServer().getOnlinePlayers()) { refreshDeaths(p); applyBoard(p); applyClock(p); }
        for (World w : getServer().getWorlds()) lastDay.put(w.getUID(), Rules.day(w.getFullTime()));
        getServer().getScheduler().runTaskTimer(this, this::checkMorning, 20, 20);
        getServer().getScheduler().runTaskTimer(this, this::tickClock, 1, 20);
    }

    @Override public void onDisable() {
        var main = getServer().getScoreboardManager().getMainScoreboard();
        for (Player p : getServer().getOnlinePlayers()) {
            if (p.getScoreboard() == deathsBoard) p.setScoreboard(main);
            p.hideBossBar(clock);
        }
    }

    // ---- day ----

    private void checkMorning() {
        for (World w : getServer().getWorlds()) {
            if (w.getEnvironment() != World.Environment.NORMAL) continue; // no sun in the Nether or the End
            long day = Rules.day(w.getFullTime());
            Long before = lastDay.put(w.getUID(), day);
            if (before == null || day <= before) continue;
            for (Player p : w.getPlayers()) {
                if (!prefs.on(p.getUniqueId(), Notice.DAY)) continue;
                show(p, "day.title", "day.subtitle", day, Duration.ofMillis(500), Duration.ofSeconds(3));
                play(p, "day.sound");
            }
        }
    }

    // ---- deaths in Tab ----

    private void refreshDeaths(Player p) {
        deaths.getScore(p.getName()).setScore(p.getStatistic(Statistic.DEATHS));
    }

    private void applyBoard(Player p) {
        var main = getServer().getScoreboardManager().getMainScoreboard();
        Scoreboard want = prefs.on(p.getUniqueId(), Notice.DEATHS) ? deathsBoard : main;
        if (p.getScoreboard() != want) p.setScoreboard(want);
    }

    // ---- clock ----

    /** The Overworld's time: the Nether and the End have no day cycle of their own. */
    private World overworld() {
        for (World w : getServer().getWorlds()) if (w.getEnvironment() == World.Environment.NORMAL) return w;
        return getServer().getWorlds().getFirst();
    }

    private void tickClock() {
        World w = overworld();
        long t = w.getTime();
        Component time = Component.text(Rules.isDay(t) ? "☀ " : "☾ ", Rules.isDay(t) ? NamedTextColor.YELLOW : NamedTextColor.AQUA)
            .append(Component.text(Rules.clock(t), NamedTextColor.WHITE));
        if (!time.equals(clock.name())) clock.name(time);
    }

    private void applyClock(Player p) {
        if (prefs.on(p.getUniqueId(), Notice.CLOCK)) p.showBossBar(clock); else p.hideBossBar(clock);
    }

    @EventHandler public void onDeath(PlayerDeathEvent e) {
        Player p = e.getPlayer();
        // the death statistic goes up after the event, so read it a tick later
        getServer().getScheduler().runTask(this, () -> { if (p.isOnline()) refreshDeaths(p); });
    }

    // ---- welcome ----

    @EventHandler public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        refreshDeaths(p);
        applyBoard(p);
        applyClock(p);
        boolean first = !p.hasPlayedBefore();
        UUID id = p.getUniqueId();
        // wait out the resource pack screen, or the title plays behind it
        getServer().getScheduler().runTaskLater(this, () -> {
            Player now = getServer().getPlayer(id);
            if (now == null || !prefs.on(id, Notice.WELCOME)) return;
            show(now, first ? "welcome.first-title" : "welcome.title", "welcome.subtitle",
                Rules.day(now.getWorld().getFullTime()), Duration.ofMillis(600), Duration.ofSeconds(3));
            play(now, "welcome.sound");
        }, Math.max(0, cfg.getLong("welcome.delay-ticks", 60)));
    }

    @EventHandler public void onQuit(PlayerQuitEvent e) {
        deathsBoard.resetScores(e.getPlayer().getName());
    }

    // ---- shared helpers ----

    private void show(Player p, String titleKey, String subKey, long day, Duration fadeIn, Duration stay) {
        int d = p.getStatistic(Statistic.DEATHS);
        Component title = MM.deserialize(Rules.fill(cfg.getString(titleKey, ""), p.getName(), day, d));
        Component sub = MM.deserialize(Rules.fill(cfg.getString(subKey, ""), p.getName(), day, d));
        p.showTitle(Title.title(title, sub, Title.Times.times(fadeIn, stay, Duration.ofSeconds(1))));
    }

    private void play(Player p, String path) {
        String id = cfg.getString(path + ".id", "");
        if (id == null || id.isBlank() || id.equalsIgnoreCase("none")) return;
        try {
            p.playSound(Sound.sound(Key.key(id), Sound.Source.MASTER,
                (float) cfg.getDouble(path + ".volume", 0.8), (float) cfg.getDouble(path + ".pitch", 1.0)));
        } catch (Exception ex) {
            getLogger().warning("bad sound id '" + id + "' at " + path);
        }
    }

    // ---- /settings menu ----

    private static final class Menu implements InventoryHolder {
        Inventory inv;
        @Override public Inventory getInventory() { return inv; }
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
        Menu m = new Menu();
        m.inv = getServer().createInventory(m, 27, Component.text("Settings"));
        fill(m.inv, p.getUniqueId());
        p.openInventory(m.inv);
        return true;
    }

    private void fill(Inventory inv, UUID id) {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        pane.editMeta(meta -> meta.displayName(Component.text(" ")));
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, pane);
        inv.setItem(SLOT_DAY, button(Material.CLOCK, "Day notice", "\"Day N\" on screen every morning", prefs.on(id, Notice.DAY)));
        inv.setItem(SLOT_DEATHS, button(Material.TOTEM_OF_UNDYING, "Deaths in Tab", "Everyone's death count in the Tab list", prefs.on(id, Notice.DEATHS)));
        inv.setItem(SLOT_WELCOME, button(Material.BELL, "Welcome", "Welcome title and sound when you join", prefs.on(id, Notice.WELCOME)));
        inv.setItem(SLOT_CLOCK, button(Material.COMPASS, "Clock", "In-game time at the top of the screen", prefs.on(id, Notice.CLOCK)));
    }

    private static ItemStack button(Material icon, String name, String what, boolean on) {
        ItemStack it = new ItemStack(icon);
        it.editMeta(meta -> {
            meta.displayName(Component.text(name, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                Component.text(what, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                (on ? Component.text("ON", NamedTextColor.GREEN) : Component.text("OFF", NamedTextColor.RED))
                    .decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false),
                Component.text("Click to turn " + (on ? "off" : "on"), NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)));
            meta.setEnchantmentGlintOverride(on);
        });
        return it;
    }

    @EventHandler public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder(false) instanceof Menu)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getClickedInventory() != e.getView().getTopInventory()) return;
        Notice n = switch (e.getSlot()) {
            case SLOT_DAY -> Notice.DAY;
            case SLOT_DEATHS -> Notice.DEATHS;
            case SLOT_WELCOME -> Notice.WELCOME;
            case SLOT_CLOCK -> Notice.CLOCK;
            default -> null;
        };
        if (n == null) return;
        prefs.toggle(p.getUniqueId(), n);
        if (n == Notice.DEATHS) applyBoard(p);
        if (n == Notice.CLOCK) applyClock(p);
        fill(e.getView().getTopInventory(), p.getUniqueId());
        p.playSound(Sound.sound(Key.key("ui.button.click"), Sound.Source.MASTER, 0.6f, 1.0f));
    }

    @EventHandler public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Menu) e.setCancelled(true);
    }
}
