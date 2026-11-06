# Modern HUD

A ModernWarfare ish hudding mod

Needs Forge 47+ and [YetAnotherConfigLib](https://modrinth.com/mod/yacl) 3.6+ (client).
The mod is optional on either side of a connection: players without it can join a server that has it, and the other way round.
Optional: TACZ, ModernLoadout, Modern Ping, SuperbWarfare, Ping Wheel.

## What it does

- **Player widget:** head, name, health and absorption bars, HP text, armor.
- **Team list:** one row per scoreboard teammate above your own widget.
- **Weapon widget (TACZ):** weapon icon, magazine and reserve ammo, fire mode, secondary weapon, attachment dots,
  gear slots (melee and throwables from ModernLoadout / lrtactical), low-ammo and reload alerts.
  TACZ's own gun HUD is turned off so the two never overlap. A keybind switches between them.
- **Compass:** a heading tape with a readout and place name. It can also show enemy gunfire (TACZ) and pings (Modern Ping).
- **Killstreak tracker and perk popup (ModernLoadout), compact boss bars, hit direction arcs.**
- **Motion HUD:** the HUD sways with the camera and settles back on a spring. Strength per element, optional vanilla hotbar.
- **Spec readout:** FPS, ping, packet loss, GPU temperature and time in small boxes, plus optional CPU / GPU usage, temperatures, VRAM, RAM, frame time and 1 % low. With Modern HUD on the server the ping is
  measured live; otherwise it is the tab-list ping. GPU temperature: NVIDIA, Linux AMD / Intel, or LibreHardwareMonitor on Windows.
- **Sounds:** hit, kill, headshot-kill, shield hit and shield break. Three sound styles, volume and pitch for each,
  and the option to silence the hit and kill sounds of TACZ and SuperbWarfare.
- **Shield feedback:** hits on absorption play a sound and show an icon for the attacker and the victim only.

## Configuration

Open **Mods > Modern HUD > Config**. The screen has tabs for General, Player, Team, Weapon, Killstreak, Boss bar,
Compass, Motion, Specs, Sounds and Feedback. Changes are applied to the HUD while you edit, saved when you press Apply or Done,
and undone if you press Cancel. If you open the screen in a world, the HUD stays visible behind it.

Values are stored in `config/modernhud-*.toml` (hud, weapon, killstreak, bossbar, hitindication, compass, sounds, motion, specs, common).
Editing the files by hand also works and is picked up while the game runs.

The HUD position of some elements can be set with the drag editor: Controls > Modern HUD > Open HUD Editor.

### Sounds

- Sound style, volume and pitch are in the Sounds tab (`modernhud-sounds.toml`) and are per player.
- When a hit lands on a shield, the shield sound plays instead of the hit sound. When the shield is gone, the hit sound plays.
  A kill always plays the kill sound.
- When and for which damage the sounds fire is set on the server in `modernhud-common.toml`, section `combatSounds`.
- To silence other mods' sounds, edit `override.patterns` in the Sounds tab. Turn on `override.debugLog` to see the ids of
  sounds played by TACZ and SuperbWarfare in `logs/latest.log`.

### Pings

With Modern Ping installed, pings appear on the compass: enemy players, hostile mobs, vehicles that are not on your team
(SuperbWarfare by default) and optionally plain locations. Modern Ping sends pings to your whole team, so everyone with
Modern HUD sees them. If a ping does not show, turn on `pings.debugLog` in the Compass tab and read `logs/latest.log`.

## Commands

All commands run on the client and need no permissions.

| Command | What it does |
| --- | --- |
| `/modernhud config` | Opens the config screen. |
| `/modernhud toggle <element>` | Turns one element on or off: player, team, compass, killstreak, perktoast, bossbar, hitindicator, ammoglow. |
| `/modernhud preset <name>` | `default` resets every option, `performance` turns off the heavier glows and pulses, `minimal` hides team, killstreak, perk popup, boss bars and the weapon glows. |
| `/modernhud sound <cue>` | Plays hit, kill, headshot, shieldhit or shieldbreak with your current style, volume and pitch. |
| `/modernhud reload` | Re-reads all config values. |
| `/modernhud info` | Prints the mod version and which optional mods were found. Include it in bug reports. |

## Building

Requires JDK 17.

```
./gradlew build        # Linux and macOS
.\gradlew.bat build    # Windows
```

The jar is written to `build/libs/modernhud-Stable-<version>.jar`. Install it on the client. Put it on the server too for
shield feedback on other entities and for the hit and kill sounds.

## Changelog and notes

See `CHANGELOG.md`. Older development notes are in `docs/dev-notes.md`.
