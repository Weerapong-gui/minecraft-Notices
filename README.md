# Notices

Paper plugin (Minecraft 26.3) with four notices; each player switches each one on or off in `/settings`
(a chest menu, so it works for Bedrock players through Geyser too).

- **Day N**: a title each in-game morning in the Overworld, e.g. **Day 42**, with a soft chime.
- **Deaths in Tab**: everyone's death count next to their name in the Tab list, from the game's own
  death statistic (so past deaths count too).
- **Clock**: in-game time (24-hour, sunrise = 06:00) and day number in the sidebar on the right.
- **Welcome**: "Welcome back" title with the player name and day, plus a sound, a few seconds after joining.

Choices are saved in `plugins/Notices/prefs.yml`. Texts, sounds and defaults are in `config.yml`
(MiniMessage tags; placeholders `%player%`, `%day%`, `%deaths%`).

Players with "Deaths in Tab" or "Clock" on see a separate scoreboard (one per combination). If the server later uses scoreboard teams or
sidebars on the main scoreboard, those players will not see them.

## Build

```sh
./build.sh /path/to/paper-server-dir   # -> build/Notices.jar
```
