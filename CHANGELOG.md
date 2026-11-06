# Changelog

## 1.7.0

Output file: `modernhud-Stable-1.7.0.jar`.

### Added: WARZONE absorption plate (new default) and a thinner lost-item X
- New `PlateShape.WARZONE`, measured on the Warzone armor plate: a flat bar with big round top corners (radius about half the
  height) and a shallow cut-out along the bottom that leaves a foot at each end (cut-out depth about 22 % of the height, each
  foot about 1.2 x the height wide, soft inner corners). Flat shading. The plate is slim, about 9.5 : 1 like the reference: its height is the width of one plate in SCREEN pixels divided by 9.5 (4 px at the least), drawn centred in a row that reserves just enough layout units for it. It follows the HUD's own GUI scale, so it stays slim at any scale; `absorptionHeight` does not thicken it. No outline below 10 px (a slate slot keeps an empty plate visible).
  Default custom absorption colour for new configs is now the reference blue 0x78BBD5. Existing configs keep their values.
- The lost-item X is four flat-ended hairline bars (about 1.15 px wide at 16 px, scales with the icon), 8 x 8 coverage samples
  per pixel and an even raster size so all four arms are exact mirror images. The pixel fallback is a 1 px stroke below 24 px.

### Changed: absorption plates and the lost-item X are now drawn with signed distance fields, not hand-placed pixels
- The 1.6.0 shapes were built from rows of rectangles, so every round or slanted edge was a staircase. They are now computed as
  signed distance fields (negative inside, positive outside) and sampled on a 4 x 4 grid inside every pixel, which gives the exact
  coverage of each pixel: smooth anti-aliased edges, soft shading and a thin outline at any size.
- Each shape is computed once, at the exact size in SCREEN pixels it will be drawn at, kept in a small texture and reused (24
  textures at most, the oldest freed first). It is drawn 1:1 with the screen, its origin snapped to a whole screen pixel, and
  the fill is cut at a whole pixel, so it is both smooth and crisp.
- A whole row of plates, with slot, fill and outline of every plate, goes out as ONE draw call (position / colour / texture
  quads), and the texture carries a vertical shading ramp (bright top, darker bottom) so the plate looks bevelled.
- New plate shapes (`absorptionShape`): `PLATE` (default, the armor plate of the reference: small rounded left corners, a big
  round right end), `PILL`, `CHAMFER`, `HEX`, `SLANT`, and `BAR` (the old rectangle). New `absorptionSmooth` (default on).
  With smooth drawing a shape needs only 4 units of height (6 for the pixel fallback).
- Safety: if the smooth drawing ever fails (a driver or another mod objects), it switches itself off for the session, logs one
  line, and the old pixel-row drawing takes over, so the HUD cannot break. `absorptionSmooth = false` selects the pixel drawing.

### Changed: the "item lost" X in the gear column uses the same method
- A hitmarker-style X (four tapered strokes with rounded ends and an empty centre; they never cross), anti-aliased at the exact
  icon size and snapped to the screen pixel grid, tinted with `gear.spentColor`. The pixel version is only the fallback.

### Changed: gear slot icons are centred
- New `gear.centerIcons` (default on): the item icon of every gear slot is in the middle of its box instead of against one
  side. Melee: centred in its strip. Throwable / consumable: centred in the box with the count at the bottom-left. Compact row
  (idle): the icon and its count are centred together as one group. Off restores the old placement.

### Not changed
- Everything else is as in 1.6.0.

## 1.6.0

Output file: `modernhud-Stable-1.6.0.jar`.

### Added: Warzone / Modern Warfare style absorption plates
- The absorption row is no longer a straight strip. Each plate is a pixel-exact shape (`absorptionShape`, Player tab):
  `CHAMFER` (default, an armor plate with cut corners as in Warzone), `HEX` (hexagon), `SLANT` (leaning parallelogram, the
  Modern Warfare 2019 segments) or `BAR` (the old rectangle).
- `absorptionFill`: `HORIZONTAL` (default, left to right, the most precise on small plates) or `VERTICAL` (from the bottom up,
  like a gauge in an icon). `absorptionOutline`: a thin light outline round each plate (on by default).
- A shape needs room: with a shape on, an `absorptionHeight` below 6 is raised to 6 (the old 3 pixel strip cannot have cut
  corners). `BAR` keeps your exact height. Applies to the player widget and the team list, and to the existing options:
  plate count, points per plate, colour mode, glow and the overflow text.

### Added: lost items show an X, and slots refill by themselves (lrtactical gear column)
- When the item a throwable / consumable slot was tracking is used up or lost, the slot now shows an X instead of a faded empty
  icon. The X is hitmarker style: four short diagonal strokes pointing at the centre, with the centre left empty (they do not
  cross). It is pixel exact at both slot sizes. `gear.spentMarker` = `X` (default) or `DIM_ICON` (the old look), and
  `gear.spentColor` sets its colour (default light grey). The count still reads 00.
- Auto-detect: a slot whose item ran out, or that was empty, takes over another lrtactical item in stock by itself. With
  `gear.countWholeInventory = true` every item in the inventory can take over, not only the hotbar (before, only hotbar items
  could). With it off the behaviour is unchanged: hotbar only. A spent slot gives way as soon as a new item is found (checked
  five times a second); if the same item turns up again it simply resumes.

### Fixed: the newer absorption break sound (style 3) was far too quiet
- Measurement of `break_3.ogg` against the other break sound: 6 LU quieter in loudness (-21.2 against -15.0 LUFS), its peak sat
  6.8 dB below full scale (headroom left unused), and 67 % of its energy was below 250 Hz, where laptop speakers and most
  headphones play almost nothing and where the ear is far less sensitive. So it was both too low in level and in the wrong
  frequency range, as suspected.
- The file was re-mastered: a high-pass at 70 Hz removes the inaudible sub-bass, a lift around 1.8 kHz and 3.6 kHz (plus a
  little air at 7 kHz) moves energy to where hearing is sensitive, then gain and a peak limiter. Now -12.6 LUFS (louder than the
  other break sound), peak -0.4 dBFS, no clipping; the share of energy above 2 kHz went from 24 % to 45 %.
- Minecraft caps playback gain at 1.0, so a louder sound can only come from the file itself; that is why the volume option
  (up to 2.0) could not fix it.

### Not changed
- Everything else is as in 1.5.2. The overlay cache and auto-optimize are unchanged.

## 1.5.2

Output file: `modernhud-Stable-1.5.2.jar`.

### Changed: number options are typed in, not dragged
- Every number option in the config screen (compass scale, points per segment, every size, scale, opacity, delay ...) is now a
  box: click it and type the exact value. The option's minimum and maximum still apply, so a value out of range is brought back
  to the nearest allowed one. This replaces the sliders, which could not take an exact value and were hard to hit on long ranges.
- New option `animation.numberInput` (General tab, "Number options"): `FIELD` (default, type a value) or `SLIDER` (the old
  sliders for options with a bounded range; options with a huge range are boxes in both modes). Reopen the config screen after
  changing it.
- Applies to every tab, including the new Motion, Specs and Performance tabs, because they all come from the same option list.
- Nothing about the stored values, the .toml files or the in-game preview changed.

### Not changed
- Everything else is as in 1.5.1.

## 1.5.1

Output file: `modernhud-Stable-1.5.1.jar`.

### Added: auto-optimize (scans and optimises the HUD by itself)
- Turn it on with `autoOptimize.enabled = true` in `modernhud-perf.toml`, or in the new Performance tab of the config screen.
  It is off by default.
- What it does, once each time you join a world: waits `warmupSeconds` (5) so the loading spikes pass, measures what every HUD
  overlay of every mod costs per frame for `scanSeconds` (15) of real play, then caches the expensive ones with the overlay
  cache from 1.5.0 and tells you in chat what it did and roughly how many milliseconds per frame that saves.
- It measures honestly: only ticks in which a HUD is showing and no menu is open count (opening a menu pauses the scan), and the
  cost is averaged over every frame, so an overlay that is rarely shown (a popup) is not picked. Hold what you normally use
  (a gun) during the scan so those overlays are measured too.
- Which overlays are picked: those costing at least `minCostMs` (0.10) per frame. Other mods' overlays are redrawn
  `redrawsPerSecond` (30) times a second, Modern HUD's own `modernHudRedrawsPerSecond` (60). The vanilla HUD is never touched
  (`exclude`, default `minecraft:`; add ids or prefixes ending in a colon to keep other overlays out as well).
- Overlays that are already cached are skipped. Overlays you cached by hand keep your rate.
- Safety net: an overlay that turns out to be incompatible (it binds its own framebuffer while it draws) is switched off, and
  now also written to a new `blocked` list in the config, so neither the scan nor the next session picks it again.
  `/modernhud optimize retry` empties that list and lets everything try again.
- `/modernhud optimize scan` runs one scan right now, whatever the config says.
- New config screen tab "Performance": the overlay cache master switch and all auto-optimize numbers. The lists (`entries`,
  `blocked`, `exclude`) are edited in `modernhud-perf.toml`, or with the `/modernhud optimize` commands.

### Not changed
- Everything else is as in 1.5.0.

## 1.5.0

Output file: `modernhud-Stable-1.5.0.jar`. New config file `modernhud-perf.toml`.

### Why this exists (from a real `/modernhud profile all` report)
On a 5.76 ms frame all HUD overlays together cost 1.63 ms (28 % of the frame). Modern HUD was about 0.7 ms of that: weapon
0.45, killstreak 0.13, spec readout 0.06, compass 0.08. The rest, about 0.9 ms, was other mods: Xaero's minimap 0.41,
SuperbWarfare's ammo counter 0.14, plus smaller ones. Modern HUD cannot make another mod's code faster, but it can draw that
mod's overlay less often.

### Added: overlay cache (works for ANY overlay, from any mod)
- An overlay can be redrawn only N times a second. In between, the picture it made last time is shown from a texture, so the
  overlay's CPU time is saved on all the other frames. 30 redraws a second on a 170 FPS game saves about 80 % of that
  overlay's cost; the minimap above would give back roughly 0.3 ms, the ammo counter about 0.1 ms.
- How: Forge's Pre / Post overlay events only, no mixin. On a redraw frame the overlay draws into a full-screen framebuffer
  (the GUI batch is flushed before and after) and the picture is shown; on the other frames the overlay is cancelled and the
  picture is shown instead.
- Nothing is cached unless you ask. Commands:
  - `/modernhud profile all on` and wait for the report, then `/modernhud optimize auto`: caches every overlay that costs
    0.12 ms or more (Modern HUD's own at 60 per second, other mods' at 30; the vanilla HUD is left alone).
  - `/modernhud optimize set <overlay id> <redraws per second>`; 0 undoes it. `/modernhud optimize list` shows what is
    active and how often each was redrawn / replayed. `/modernhud optimize reset` empties the list. The entries are saved in
    `modernhud-perf.toml` (`overlayCache.entries`), `overlayCache.enabled` is the master switch.
- Safety, because other mods' overlays are not ours:
  - If an overlay binds a framebuffer of its own while it draws (some minimaps render into a texture), its picture cannot be
    captured. This is detected right after the first redraw, caching is switched off for that overlay, a message says so, and
    `/modernhud optimize retry` tries again later.
  - If something stops between Pre and Post, the screen framebuffer is restored at the end of the GUI pass; any exception also
    switches that overlay's caching off.
  - One full-screen picture is kept per cached overlay (about 8 MB at 1080p, 32 MB at 4K), at most 8 overlays.
- Trade-offs to know: a cached overlay updates N times a second (30 is invisible for a minimap or an ammo number; do not
  cache anything that must react instantly, such as the vanilla hotbar, which `auto` skips). Modern HUD overlays that sway with
  the camera (Motion HUD) sway at that rate too. In the profiler a cached overlay shows its cost on redraw frames only.

### Not changed
- Everything else is as in 1.4.3.

## 1.4.3

Output file: `modernhud-Stable-1.4.3.jar`. Profiler fixes found by reading a real log.

### Fixed: the profiler's per-element numbers were diluted by idle frames
- Each element's average used to include the frames in which it drew nothing (for example the weapon widget while no gun was
  held, or just after joining), so a widget that costs 0.4 ms while drawn could be reported as 0.04 ms. Averages and peaks per
  element, per part and per overlay now use only the frames in which that element was actually drawn. The TOTAL line still
  averages over every frame, because that is what the frame really pays.
- Peaks decay slowly, so the first seconds after joining show one-time warm-up spikes (texture and cache set-up); wait for the
  numbers to settle before reading them.

### Changed: `/modernhud profile all` reports by itself
- `/modernhud profile all on` now starts from a clean slate and prints the full report after 15 seconds, so there is no second
  command to forget. Keep the HUD showing and hold a gun during that time.
- `/modernhud profile all off` prints the report once more before it clears the numbers (turning it off used to throw the
  numbers away without showing them).

### Not changed
- Everything else is as in 1.4.2.

## 1.4.2

Output file: `modernhud-Stable-1.4.2.jar`. Pure performance work on the WEAPON widget, chosen from a real profile
(WEAPON 0.39 ms, KILLSTREAK 0.12 ms, COMPASS 0.07 ms of 0.62 ms in total). Nothing looks different.

### Optimised
- Selected-weapon corner glow: the four corner glows were four separate immediate-mode draws. Each one flushed the open GUI
  batch, set the shader and blend state again, and issued its own draw call. They are now one `SoftDraw.RadialBatch`: one
  flush, one state set-up, one draw call (three fewer per frame while a gun is held). New `SoftDraw.radialBatch / add /
  finish` can be used for any set of glows that share a pose and a blend mode.
- Ammo digit halo: the halo draws the number up to 8 or 16 times at small offsets. Each copy used to push, translate, scale
  and pop the pose stack on its own; now the pose is set up once for the whole halo.
- Ammo text: the zero-padded / plain number strings (`007`, `7`) are cached (0 to 999) instead of formatted every frame.

### Notes
- To cut the weapon cost further without code: `/modernhud preset performance` turns off the digit halo and the pulses, and
  `ammoGlow.haloRadius = 0` alone removes the 8 to 16 text copies.
- Measure with `/modernhud profile all on`, wait about 10 seconds while the HUD is showing, then `/modernhud profile`.
  (`profile all` without `on` or `off` is not a complete command; turning it `off` clears the numbers.)

## 1.4.1

Output file: `modernhud-Stable-1.4.1.jar`.

### Changed: CPU temperature no longer falls back to the ACPI thermal zone by default
- A real report showed the ACPI zone reading 28 C on a gaming laptop while the CPU was busy: it is a board sensor that often
  sits near room temperature and does not follow the CPU. Showing it as "CPU TEMP" was misleading, so `cpuTempSource = AUTO`
  now means: Linux kernel sensors on Linux, LibreHardwareMonitor / OpenHardwareMonitor on Windows, and N/A when none is
  available. `ACPI_THERMAL_ZONE` is still there as an explicit choice.
- For an exact CPU temperature on Windows, run LibreHardwareMonitor in the background as administrator (the same helper
  MSI Afterburner-style overlays need). `/modernhud diag` says whether it is running.

### Added: profiler that covers every HUD of every mod
- `/modernhud profile all on` times every HUD overlay of the whole game, vanilla and all mods, by id (for example
  `minecraft:hotbar`, `tacz:...`, `modernhud:weapon_hud`), and `/modernhud profile` lists the 14 most expensive plus their sum.
  This answers "who is eating my FPS when the HUD is on". `off` stops it; while it is off it costs nothing.
- The WEAPON element now reports its parts (`weapon.logic`, `weapon.infoBox`, `weapon.primaryIcon`, `weapon.heatFlash`,
  `weapon.attachDots`, `weapon.secondary`, `weapon.gear`, `weapon.fireTypeFlash`) under the main table, so the next report shows
  which part of the weapon widget is the expensive one.

### Not changed
- Everything else is as in 1.4.0.

## 1.4.0

Output file: `modernhud-Stable-1.4.0.jar`. Config keys that changed: `gpu.pollSeconds` is now `gpu.pollMs`,
`hardware.pollSeconds` is now `hardware.pollMs` (old values are ignored, the new defaults are 1000 and 500 ms).

### Fixed: CPU temperature showed N/A on Windows
- Windows gives a game no CPU temperature without a helper program. The exact source is still LibreHardwareMonitor (or
  OpenHardwareMonitor) running in the background, as administrator. New: when it is not running, the CPU temperature now
  falls back to the motherboard's ACPI thermal zones (`Win32_PerfFormattedData_Counters_ThermalZoneInformation`, then the
  admin-only `MSAcpi_ThermalZoneTemperature`). No program and no admin rights are needed for the first one. It is the
  board's CPU area, not the die: close, not exact, and some desktop boards report a zone that never moves (then it stays N/A).
- `cpuTempSource` has a new value `ACPI_THERMAL_ZONE`; AUTO tries LibreHardwareMonitor first, then the thermal zone.
- New command `/modernhud diag` explains every N/A: it names each source, says whether it is installed / running, and shows
  what it returned.

### Added: more spec boxes (all off by default, `items.*`)
- `GRAPH`: a live frame time graph (like Afterburner's). One bar per slice of time (`graph.bucketMs`, 100 ms), as tall as the
  slowest frame in that slice, 60 bars by default; bars turn yellow / red by the FPS limits. Spikes show as tall bars.
- `TPS`: server ticks per second, measured from the server's own game clock (ticks that passed / real seconds that passed),
  so it works on ANY server and shows server lag that is not a network problem. Averaged over `tps.windowSeconds`. Skips
  paused single player.
- `PACKETS`: packets per second received / sent, from the connection's own counters.
- `HUD`: how many milliseconds Modern HUD takes to draw per frame (see the profiler below).
- `BATTERY`: laptop battery percent and a + while charging (Linux sysfs / Windows `Win32_Battery`); hidden on a desktop.
- `SESSION`: how long you have been in this world / server.
- Presets: `/modernhud specs preset minimal|standard|gaming|full` (gaming = FPS, 1 % low, ping, loss, TPS, CPU, GPU load / temp,
  VRAM, RAM). Two new keys in Controls: "Toggle Spec Readout" and "Cycle Spec Readout Preset" (shows the preset name on the
  action bar). Both unbound by default.

### Changed: closer to real time
- All sensor polling is in milliseconds now: CPU / RAM `hardware.pollMs` (250 to 10000, default 500), GPU `gpu.pollMs`
  (250 to 10000, default 1000). NVIDIA uses `nvidia-smi -lms`, one long-running process, so 250 ms costs almost nothing.
  Slow Windows queries (LibreHardwareMonitor, thermal zone) are still limited to once per 3 seconds.
- FPS refresh default 250 ms (was 500; minimum 50 ms). The live ping probe interval is configurable
  (`ping.probeMs`, 250 to 2000, default 500); the server accepts up to 4 per second.
- The TPS and packet tracker is now installed in single player too (the packet loss and ping stay off there).

### Added: HUD profiler and diagnostics (tools to find what really costs FPS)
- `/modernhud profile` prints, per Modern HUD element, the CPU time per frame (smoothed, with peak), the total, and the total
  as a share of the frame time. Always measured, nothing to switch on.
- `/modernhud profile gpu on` adds the GPU time of each element with an OpenGL timer query (needs OpenGL 3.3; turn it off again
  with `off`, queries have a small cost of their own).
- `/modernhud diag` also lists which optimisation mods are installed (ImmediatelyFast, Embeddium / Rubidium, Oculus, ModernFix,
  FerriteCore, Entity Culling, Dynamic FPS) and how Modern HUD relates to each.
- How to read it: the profiler only covers Modern HUD. Compare its total with the FPS gain you see when you press F1: whatever
  is left over is the vanilla HUD and other mods' HUDs (TACZ, Jade, ...), which Modern HUD cannot speed up.

### Not changed
- Text sizing, killstreak slot drawing, corner / dot scale, Motion HUD, the ping probe and the CPU / GPU / memory boxes of
  1.3.2 are as they were.

## 1.3.2

Output file: `modernhud-Stable-1.3.2.jar`.

### Reverted: text scale is back to exactly how it worked in 1.3.0
- The 1.3.1 change to text sizing (exact fractional sizes, option `snapTextScale`) made letters look badly uneven at any
  scale other than 50 % / 100 %. All of it is removed: `HudText`, the `animation.snapTextScale` option and the spec
  readout's size handling are identical to 1.3.0 again. If an old `modernhud-hud.toml` still has `snapTextScale`, it is
  ignored.
- Reason it is not "fixed" a different way: a pixel font only looks even when every font pixel covers a whole number of
  screen pixels, so sizes in between always look uneven. Text sizes therefore still step (at GUI scale 2 that is 50 %,
  100 %, 150 %). Elements that have their own scale value are unchanged.

### Fixed: killstreak slot icons had a white or black background
- The dark square behind the icons was the dimming layer that is laid over locked / next / used item icons. 1.3.1 filled it
  with the panel colour, which turned the icons white. Now there is NO background of any kind: the slot box alone is the
  background, and a dimmed item is simply drawn a little darker (the item's own colours are multiplied, nothing is drawn
  over or behind it).
- Perk / streak pictures (textures) are drawn exactly as in 1.2.6 and 1.3.0 again (the additive blending of 1.3.1 is gone).
- `killstreak.iconBackdrop` (default off) brings the old dark cover back.

### Added: corner / dot scale for every element that has corner marks
- New option "Corner / dot scale (%)" (`borderScale`, 25 to 800, default 100). It multiplies the length of the corner
  brackets (CORNERS) and the size of the dots (DOTS, BOTTOM_DOTS and the dots of FRAME). 100 is the size set by
  `borderLength`, 50 is half, 200 is double. It sits right next to Border size in each place:
  - Player widget and Team list: General settings, Panel & border (`panel.borderScale` in `modernhud-hud.toml`)
  - Weapon boxes: Weapon tab, "Weapon boxes: panel & border" (`panel.borderScale` in `modernhud-weapon.toml`). It also
    scales the two dots of the secondary weapon box (`secondaryDotSize`).
  - Killstreak slots (also the selection display), Perk toast, Gear slots, Ammo alerts, Info box, Boss bars and the spec
    readout: in each one's style group (`style.borderScale`).
- Before, the only control was a whole-pixel size (and the dot size never went below 2, so Border size 1, 2 and 3 all
  gave the same dot). The scale has no such floor: a dot can be 1 pixel.
- At 100 % nothing changes.

### Not changed
- Everything from 1.3.1 that is not listed above: the optional CPU / GPU / memory / frame-time boxes, the live ping probe,
  the packet loss estimate and the Motion HUD.

## 1.3.1

Output file: `modernhud-Stable-1.3.1.jar`.

### Fixed: text size only changed at 100 % / 50 %  (REVERTED in 1.3.2, see above)
- Cause: every text size was rounded to a whole number of screen pixels per font pixel (`pixelPerfectText`). At GUI scale
  2 the only sizes that survive that rounding are 50 %, 100 %, 150 %, so 60 %, 75 % or 120 % all looked like one of them.
- Now every scale value changes the size. The rounding of the SIZE is a separate, optional switch:
  `animation.snapTextScale` in `modernhud-hud.toml` (General tab, default off). Letters stay sharp wherever the size
  happens to be a whole number of screen pixels, and the text POSITION is still snapped to a screen pixel in every case,
  so there is no shimmer. Turn `snapTextScale` on to get the old coarse-but-perfectly-even letters back.
- Applies to every element that draws text through `HudText` (player, team, weapon, compass, boss bars, killstreak,
  spec readout). The spec readout's own `position.scale` follows the same rule.
- Note: `guiScaleSnap` (General tab) only matters in RESOLUTION mode and rounds the whole HUD scale to a whole number;
  if the HUD itself still steps in that mode, turn it off.

### Changed: killstreak slots have no dark square behind the icon  (replaced in 1.3.2, see above)
- The slot box (panel and border) is unchanged. The dark square came from two places, both handled:
  - perk / streak pictures that carry a black background are now drawn with additive blending, so black adds nothing and
    only the icon shows;
  - the dimming layer on locked / next / used item icons was a near-black square; it now uses the slot's own panel colour,
    so it fades the icon into the box instead of showing as a square.
- Option `killstreak.iconBackdrop` (default off) brings the old dark square back.

### Added: optional monitoring (a mini MSI Afterburner), all in the spec readout
All new boxes are OFF by default (`items.*` in `modernhud-specs.toml`, Specs tab). Each has its own word, can be moved
with `items.order`, and uses the same style, size and warning colours as the other boxes.
- Frame: `FRAME` (average frame time in ms) and `1% LOW` (average FPS of the slowest 1 % of the frames in the last
  `fps.lowWindowSeconds`, 10 s by default, the way Afterburner reports it). Counted per rendered frame, no extra cost.
- CPU: `CPU` usage in percent (the whole machine, like the task manager) and `CPU TEMP`.
- GPU: `GPU LOAD` (usage), `GPU` (temperature, as before), `VRAM` (used / total), `GPU POWER` (watts) and `GPU CLOCK` (MHz).
- Memory: `RAM` (system memory in use, as task managers count it) and `JVM` (the Java heap the game itself uses, used /
  max). `hardware.showTotal` switches between used / total and just used.
- Colours: CPU and GPU temperatures use their own warn / critical limits, memory boxes turn yellow / red by percent of the
  total, frame time and 1 % low follow the FPS limits.
- Where the numbers come from (read on background threads, only while a box that needs them is shown, nothing from the
  config is ever executed):
  - NVIDIA: one `nvidia-smi` process that stays running and prints a line every `gpu.pollSeconds`, which is much cheaper
    than starting the tool for every reading. It is stopped when the boxes go away and when the game exits. Gives
    temperature, usage, VRAM, power and clock.
  - Linux: kernel sensors. CPU temperature (coretemp, k10temp, zenpower), GPU temperature, and for AMD usage, VRAM, power
    and clock. No tool needed.
  - Windows with AMD or Intel graphics, or CPU temperature on Windows: LibreHardwareMonitor (or OpenHardwareMonitor) must
    be running in the background. One shared query feeds both the CPU and the GPU boxes.
  - CPU usage and RAM work everywhere without any tool.
- A box whose sensor does not answer shows `N/A`, or hides (`gpu.whenUnavailable`, applies to every hardware box).
- `items.order` now accepts the new words (fps, low, frame, ping, loss, cpu, cputemp, gpuload, gpu, vram, gpupower,
  gpuclock, ram, jvm, time). An item missing from an older order list keeps its usual place instead of landing after the
  clock.

### Notes
- CPU temperature is the one value Windows does not give to a game without a helper program; that is why it needs
  LibreHardwareMonitor there. This is the same situation as MSI Afterburner, which needs a helper for it too.
- Not changed: the live ping probe, packet loss estimate, Motion HUD, and every other element.

## 1.3.0

Output file: `modernhud-Stable-1.3.0.jar`. Two new features, both with their own config file and config tab.

### Added: Motion HUD (`modernhud-motion.toml`, tab "Motion")
- The HUD sways with the camera, like GD656 Motion HUD, but built differently. Turn right and the HUD slides left, look
  down and it slides up, then it settles back on a damped spring. The spring is frame-rate independent (integrated in
  small steps), so it feels the same at 60 and at 300 FPS.
- The camera angles are read from the real render camera, so view shakes and gun recoil from other mods make the HUD
  kick as well.
- Distances are in "reference pixels" (a 1080p screen) and scaled to the real resolution, so the sway looks the same on
  1080p, 1440p and 4K and at every GUI scale. The offset is rounded to whole SCREEN pixels (`output.pixelSnap`), which
  keeps text and thin lines sharp while the HUD moves.
- Limits are soft (a tanh curve), so the HUD slows down near `physics.maxOffset` instead of hitting a wall. A jump of
  more than 90 degrees in one frame (teleport, respawn) is ignored.
- Options: overall strength, sideways and up/down strength, invert both axes, max offset, return speed, damping (bounce),
  deadzone.
- Per-element strength (`layers.*`, 0 to 300 %): player, team, weapon, ammo alert, killstreak, perk popup, compass, boss
  bars, spec readout, feedback icon, hit arcs. Different values give a depth (parallax) effect. Defaults: the player,
  team, weapon, ammo alert, killstreak and perk popup move; the compass, boss bars, readout, feedback icon and hit arcs
  stay (the hit arcs are tied to the crosshair).
- Optional: the vanilla hotbar, health, food, armor, air, XP bars and item name sway too (`vanilla.enabled`). The
  crosshair, chat, debug text and tab list never move. This uses Forge's overlay events: no mixin.
- `/modernhud toggle motion`. The `minimal` preset turns it off.
- Implemented without a mixin: the offset is applied in `HudScale`, around every overlay.

### Added: spec readout (`modernhud-specs.toml`, tab "Specs")
- FPS, PING, PACKET LOSS, GPU temperature and the real-world time, in small flat dark boxes like the reference.
  `/modernhud toggle specs`.
- Pixel-perfect: every size is counted in font pixels and one font pixel is drawn as a whole number of screen pixels,
  the origin is snapped to a screen pixel. Boxes have crisp edges and equal gaps, and the letters are centred at every
  GUI scale. The text sits on a fixed character grid (`layout.monospace`, `layout.cellWidth`) so digits never make the
  boxes jitter.
- Configurable: anchor (6 positions), offset, size, row or column, one box per item or one bar, gap, box height,
  padding, order of the items (`items.order`), each item on or off, the words before the numbers, capitals, text colour,
  words opacity, shadow, panel / border / fade (same style group as the other elements), warning colours with
  thresholds, hide while F3 is open, show behind menus.
- FPS: counted per rendered frame, refreshed every `fps.updateMs` (default 500 ms).
- Ping, two sources (`ping.source`):
  - Live probe (new): when the server has Modern HUD too, the client sends a tiny probe twice a second on its own
    channel `modernhud:ping` and the server answers on the network thread, without waiting for a game tick. The round
    trip is measured on the client's clock. The ping is the median of the last 5 probes (`ping.samples`).
  - Tab-list ping: the fallback on servers without the mod. This is the vanilla value that the server updates only every
    15 seconds; tools such as spark read the same server-side value.
- Packet loss: Minecraft uses TCP, so a mod can never see a packet that was lost for good. The value is an estimate of
  what a player feels. With the probe: the share of probes in the last `loss.windowSeconds` (default 30) that got no
  answer, or an answer later than `loss.timeoutMs` (default 1000). Without the mod on the server: the server's
  one-per-second time updates that arrive late or not at all (a pass-through handler on the connection, no mixin). A
  stall that is still going on counts right away. Single player shows 0 %.
- GPU temperature: read by a background thread (never costs frames), only while the box is drawn. Sources (`gpu.source`,
  default AUTO): `nvidia-smi` (NVIDIA, Windows and Linux), Linux kernel sensors (AMD / Intel, no tools needed),
  LibreHardwareMonitor / OpenHardwareMonitor through PowerShell (Windows, AMD / Intel; the program must be running).
  Fixed commands only: nothing from the config is ever executed. If nothing answers, the box shows N/A (or hides,
  `gpu.whenUnavailable`). Celsius or Fahrenheit.
- Time: 12 or 24 hour or the system format, optional seconds and leading zero, AM / PM language (`time.language`, empty
  uses the system language, so a Vietnamese system shows SA / CH).

### Network
- New channel `modernhud:ping` (protocol 1, optional on both sides, like the main channel). The server ignores a player
  who sends more than 10 probes a second. The main channel and its protocol version are unchanged, so 1.3.0 still joins
  servers and clients that run 1.2.x (the readout then uses the tab-list ping).

### Changed
- `HudScale.wrap` and `HudScale.render` have an optional `MotionLayer` argument (the old signatures still work and mean
  "never moves").
- Config screen: new tabs "Motion" and "Specs". `/modernhud preset default` also resets both.

### Not changed
- Everything else (player widget, weapon widget, compass, killstreak, boss bars, sounds, feedback) is untouched.

## 1.2.6

Output file: `modernhud-Stable-1.2.6.jar`.

### Fixed
- The gun name could run out of the info box (and out of any panel sized from text). The cause was not the text length:
  with pixel-perfect text the glyphs are drawn at a whole number of screen pixels per font pixel (2.5 is drawn as 3),
  but the right alignment and the box width were worked out from the unrounded width, so the text was drawn wider than
  the layout expected and its right end stuck out. `HudText.width` and `HudText.layoutRatio` now give the width that is
  really drawn. They are used for the gun name and fire mode label, the info box width, the compass labels and every text
  width in the player widget.
- Not changed: perk popup, ammo alert and boss bar text widths. They draw their text in a pose that is already scaled to
  whole pixels, so the drawn width equals the layout width there.

## 1.2.5

Output file: `modernhud-Stable-1.2.5.jar`. Built on top of the patched 1.2.4 (own info box panel and border, row gaps,
raised digits, new defaults).

### Added
- `infoBox.gapToWeapon`: gap between the info box (ammo, name, fire mode) and the weapon boxes. The box gets narrower by
  the same amount, so raise `appearance.frameWidth` to keep its width.
- `infoBox.contentRaise`: moves everything inside the info box up (ammo, name and fire mode together). The box stays put.
- `infoBox.contentScale`: size of the ammo digits, the reserve icon and the fire mode control inside the box, in percent.
  With pixel-perfect text the digits change in whole-pixel steps, so a small change can show no difference.
  All three are in the Weapon tab under "Info box: size & spacing".

### Fixed
- Bullet tips were skewed by one pixel in the AUTO and BURST icons and in the reserve ammo icon. The tip was rounded to a
  height that did not match the parity of the bullet, so it could not be centered. The tip now grows two rows per column,
  which is centered at every thickness. A test over 8 to 39 px long and 2 to 8 px thick bullets found no asymmetric column.
- Rows of bars in the icons now have the same margin above and below. The thickness or gap is adjusted by one pixel when
  needed, and the left and right margins are whole pixels.

## 1.2.4

Output file: `modernhud-Stable-1.2.4.jar`.

### Changed
- Ammo glow is wider and flatter.
- Reserve box: same height as before, but its bottom edge now sits on the foot of the reserve digits instead of being
  centered on them.
- The reserve number is the same size as the magazine number by default, as in the reference. Before, pixel-perfect text
  forced it one whole screen pixel per font pixel smaller, about 75% of the magazine height.
- Fire mode: cells are 1 unit wider and the icons are about 6% larger.
- Fire mode bullets are drawn the other way round: the short piece is the pointed tip, the long piece is the casing with
  a square end.

### Config key renamed
- `colors.reserveScaleOffset` is now `colors.reserveSizeDrop` (default 0). To get a smaller reserve number again, set it
  above 0.

## 1.2.3

Output file: `modernhud-Stable-1.2.3.jar`.

### Changed
- Fire mode icons are much smaller, like the reference. AUTO and BURST are three short bars (about a third of the cell
  width), SEMI is one thin bullet. The cells are smaller too (20 x 10, still 2:1).
- Reserve box: shorter than the digits (`ammoGlow.reservePanelHeight`, 90% by default), centered on them both ways, and
  the white line at its left edge is gone.
- The ammo row and the gun name sit a little higher in the info box.
- Ammo glow and the selected weapon corner glow are stronger and larger. The ammo haze is bigger and starts at a higher
  strength.

### Config keys renamed (the new defaults only apply to a key that is not in your file yet)
- `ammoGlow.boxGlow` is now `ammoGlow.strength` (default 45).
- `gear.selectedGlowOpacity` is now `gear.selectedGlowStrength` (default 80).
- `gear.selectedGlowRadius` is now `gear.selectedGlowReach` (default 30).
- If the glows still look weak, check `animation.glowIntensity` in `modernhud-hud.toml`. It scales every glow.

## 1.2.2

Output file: `modernhud-Stable-1.2.2.jar`.

### Fixed
- Info box rows no longer move. The ammo row, the gun name and the fire mode control now have fixed positions that are
  the same for every gun. A gun without a fire mode control leaves that slot empty instead of shifting the other rows.
- Fire mode icons were drawn as small tapered bullets and looked deformed inside the rectangular cells. They are now
  worked out from the cell size in screen pixels: centered, with the same margin on every side, and never touching the
  cell border. AUTO and BURST are three bars, SEMI is one larger bar.
- The overheat bar sits just above the fire mode control (or in its place if the gun has none) and no longer pushes
  the name down.

### Changed
- Ammo glow is now one soft elliptical haze centered on the ammo digits, like the reference, instead of rectangular
  layers with visible edges. `ammoGlow.spread` now sets how far the haze reaches past its base size.

## 1.2.1

Output file: `modernhud-Stable-1.2.1.jar`.

### Fixed
- Build error in `ModNetworking` (`player.connection.getConnection()` does not exist in 1.20.1).
- `/modernhud info` looked for ModernLoadout under the wrong mod id.

### Changed
- Weapon widget: ammo (mag and reserve), gun name and fire mode now sit in one framed info box. The frame uses the
  border style from the weapon config (corner brackets by default), has no fill, and its bottom edge lines up with the
  bottom of the secondary weapon box. It rises 14 px above the main weapon box (`appearance.infoBoxRise`).
- The rows are spread through the frame instead of stacked at the top: ammo in the upper part, the gun name under it
  and the fire mode near the bottom.
- Fire mode cells are 2:1 rectangles with padding around the bullet icons, and the control keeps clear of the frame border.
  The selected cell has a light fill.
- Ammo glow: a faint tint over the whole frame and a stronger haze around the ammo row, like the reference.
  The reserve box default opacity is 30.

### Checked: ModernLoadout
- Every method ModernHUD calls on `dev.szo9k4.mwloadout.api.StreakApi` exists with the expected type
  (streak tracker, held flags, selected perks, `setExternalHud`).
- ModernLoadout's own tracker is hidden only while ModernHUD is actually drawing it, and comes back when
  ModernHUD's tracker is turned off.
- ModernLoadout's melee and throwable items use `lrtactical:melee` and `lrtactical:throwable`, which the gear slots read.

## 1.2.0

Output file: `modernhud-Stable-1.2.0.jar`.

### Added
- Client commands: `/modernhud config`, `toggle`, `preset`, `sound`, `reload` and `info`. See the README.
- Presets: `default`, `performance` and `minimal`.

### Fixed
- The network channel now accepts a missing mod on the other side. Before, a server with the mod refused players
  without it, and a player with the mod could not join a server without it.
- Packets are only sent to players whose client has the mod.
- Loading a config of the other side (client configs on a dedicated server) can no longer throw during config events.

### Changed
- Removed the placeholder issue tracker URL from `mods.toml`. Set a real one before publishing.
- Sound style 1 is called "classic" in the settings and docs.

## 1.1.1

Output file: `modernhud-Stable-1.1.1.jar`.

### Added
- Player widget: the absorption row collapses while you have no absorption. The name moves down into its place and the
  bars close up, with a slide. It slides back when absorption returns. Turn it off with `appearance.collapseAbsorptionRow`.

### Changed
- The small gold box behind the reserve count is back (`ammoGlow.reservePanel`, `ammoGlow.reservePanelOpacity`).
  Its bright left edge is the divider between magazine and reserve.
- Selected weapon box: the top-left and bottom-right corners glow equally by default, the other two are off.
  Existing config files keep their old values. Set `selectedGlowTopLeft` and `selectedGlowBottomRight` to 100 and
  `selectedGlowTopRight` and `selectedGlowBottomLeft` to 0 in `modernhud-weapon.toml`, or reset them in the config screen.

### Performance
- Runs of plain rectangles and text are drawn as one batch instead of one GPU call each: panel borders, absorption
  plates, fire mode and reserve bullet icons, compass markers, and the ammo digit halo. This removes a few hundred
  draw calls per frame in a typical weapon HUD.
- Pixel shapes switch to screen-pixel coordinates once per shape instead of once per rectangle.
- Soft glows use fewer rings and corner segments when the glow is small. A 5 px fade used to build about 290 quads.
- The digit halo follows the glow quality setting: 4 offsets on LOW, 8 on MEDIUM, 8 on two rings on HIGH.

## 1.1.0 (Stable)

Output file: `modernhud-Stable-1.1.0.jar`.

### Added
- Config screen built with YetAnotherConfigLib (now required on the client). It has tabs, groups, and a live preview:
  values are applied to the HUD while you edit and undone on Cancel.
- Sounds: hit, kill and headshot-kill with three styles, plus shield hit and shield break. Volume and pitch for each cue.
  Shield sounds take priority over the hit sound while the victim has absorption. Optional silencing of TACZ and
  SuperbWarfare hit and kill sounds. Replaces the standalone MWKillSound mod.
- Compass: Modern Ping support with markers for enemy players, hostile mobs and vehicles, each with its own color and switch.
- Weapon widget: gear column that only takes the room of slots in use, glow for the selected weapon box with a strength
  per corner, ammo box glow, bullet-shaped fire mode and reserve icons.

### Changed
- The compass has one style only. The classic compass and its options were removed.
- Ammo box: the grey fill behind the ammo numbers is gone. The border stays, and a light gold glow fills the inside.
  One glow color is shared by the ammo glow, the corner glow and the switch flash (`panel.weaponGlowColor`).
- The low-ammo glow no longer pulses unless `ammoGlow.lowAmmoPulse` is on.
- Default values now match the author's own configuration.
- Config descriptions and the mod description were rewritten and shortened.

### Fixed
- Config changes now apply immediately instead of waiting for the file watcher.
- Fire mode icons, the divider between magazine and reserve, and the compass ticks and arrows no longer lose pixels
  at fractional scales. They are drawn on whole screen pixels.
- Zero-padded ammo digits no longer draw a shadow through the dimmed zeros.

### Config changes to know about
- `modernhud-compass.toml` has new keys and loses the old classic-compass keys; the file is regenerated with defaults.
- `modernhud-hud.toml`: `sounds.hitSoundVolume` and `sounds.breakSoundVolume` moved to `modernhud-sounds.toml`.
- `modernhud-weapon.toml`: `panel.textPanel*` was removed. The `ammoGlow` section was redone.
