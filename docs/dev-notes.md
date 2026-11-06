# ModernHUD — rebuild sạch từ v2.0.0

## v1.0.1 (patch 25) — Config áp dụng ngay, hết "thở", ammo glow theo MW4, ping lớn hơn + xe SBW

### Config áp dụng ngay (gốc của "đổi màu / tắt không ăn")
- Trước đây màn YACL chỉ GHI file rồi chờ bộ theo dõi file của Forge nạp lại, nên thay đổi trễ hoặc không áp dụng, màu không đổi, tắt pulse cũng vẫn thấy.
  Mỗi class config giờ có `refresh()` (đọc lại spec vào các biến static); bấm **Apply / Done** là `ConfigRefresh.refreshAll()` chạy ngay.
  Sửa tay file .toml vẫn tự nạp lại như cũ.

### "Thở" (breathing)
- Nguồn còn sót: viền đỏ low-ammo quanh hàng đạn luôn nhấp nháy (sin 260 ms) và không có công tắc nào tắt được. Giờ **đứng yên theo mặc định**; muốn nhấp nháy bật `ammoGlow.lowAmmoPulse`.
  `ammoGlow.pulse` (haze/halo) và `selectedGlowPulse` đều mặc định tắt. Nhịp tim tăng dần của overheat chỉ có khi súng nóng.

### Ammo glow làm lại theo ảnh MW4 2026 (đo trên ảnh)
- **Reserve nằm trên 1 panel riêng** màu glow chung (opacity mặc định 38), **mép trái sáng của panel chính là vạch chia** (thay cho `|`): `ammoGlow.reservePanel`, `reservePanelOpacity`.
- **Halo chỉ ôm các chữ số của mag** (đỏ khi sắp hết đạn); reserve không còn halo như trước (MW4 không có).
- **Haze xanh mạnh hơn nhiều** (đo: độ xanh ≈ 17-19 vùng mag, ≈ 25 vùng reserve): phủ cả hàng, đọng thấp hơn một chút bên dưới chữ, và đậm nhất quanh panel reserve (haze 55, spread 24 mặc định). Haze luôn xanh; chỉ halo đổi đỏ.

### Ping trên compass
- Marker **to hơn** (bán kính 2.4 -> 3.4 đơn vị; mũi tên đầu thanh to hơn, `markerScale` tới 3.0).
- **Xe SBW**: nhận diện thêm bằng **namespace của entity type** (`vehicleNamespaces`, mặc định `superbwarfare`, loại projectile / item / player) ngoài tiền tố tên class —
  hai cách độc lập nên đổi package trong SBW không làm mất marker.
- `pings.debugLog`: mỗi ping mới ghi 1 dòng `ModernHud ping: class=… type=… -> shown/HIDDEN (lý do)` vào `logs/latest.log` — nếu vẫn không thấy marker xe, bật cái này, ping xe, gửi dòng log.

## v1.0.1 (patch 23) — Compass: compat Modern Ping (enemy / hostile / vehicle), marker pixel-exact

Modern Ping (dev.szo9k4.modernping, modId `modernping`) đọc bằng reflection (không cần compile theo jar; thiếu / đổi API thì marker tự tắt, game không crash). Ping Wheel gốc vẫn được hỗ trợ nếu cài thay thế.
- **Ping trúng người chơi khác (không cùng team)**: Modern Ping gắn cờ "enemy" ở server và gửi ping cho cả team / channel → mọi teammate có ModernHUD đều thấy marker trên compass của mình (marker kim cương đặc).
- **Ping trúng hostile mob** (mọi `Enemy`, trừ mob cùng team) → vuông đặc cam. **Ping trúng vehicle** (SuperbWarfare mặc định; thêm prefix class của mod xe khác trong `vehicleClassPrefixes`) → tam giác đặc vàng,
  chỉ hiện khi xe KHÔNG phải của bạn: xe cùng scoreboard team hoặc có teammate đang ngồi = của bạn; xe khác team hoặc có người lạ ngồi = hiện; xe không team không người = hiện nếu bật `emptyVehicles`.
- Ping vị trí (không trúng entity) → kim cương rỗng, tô màu team của người ping (tắt mặc định, bật `locations`). Ping entity khác (động vật, item…) → vuông rỗng (tắt mặc định, `otherEntities`).
- Mục `pings` trong `modernhud-compass.toml` (thay cho `pingWheel`): công tắc từng loại, `maxDistance`, `markerScale`, co nhỏ marker theo khoảng cách (`distanceScaling` / `farDistance` / `farScalePercent`),
  mũi tên ở đầu thanh cho ping ngoài tầm nhìn 90° (`edgeHints`, xếp chồng nếu nhiều ping cùng phía), màu riêng từng loại.
- Hiệu ứng: marker "pop" khi mới xuất hiện, mờ dần theo fade của Modern Ping (ping enemy) và mờ 1 giây trước khi hết hạn (ping thường, theo `pingDuration` của Modern Ping).
- Marker TACZ gunfire cũng chuyển sang kim cương / mũi tên vẽ bằng pixel màn hình nguyên (trước đây quad xoay 45° mất pixel ở scale lẻ).
- Điều kiện để teammate thấy: Modern Ping phải đang gửi ping cho họ (cùng team vanilla / FTB Teams / voice-chat group, hoặc cùng channel) và họ cài ModernHUD.

## v1.0.1 (patch 22) — Combat sounds (3 style), shield-first, glow chung 1 màu, áp config mặc định của bạn

### Combat sounds (tích hợp từ MWKillSound — không cần cài mod đó nữa, gỡ nó đi để khỏi phát đôi)
- Hit / Kill / Headshot-kill với **3 style**: 1 = MW19, 2 = style 2 (hai bộ cũ của MWKillSound), **3 = hit3 / kill3 / headshotkill3** bạn gửi. Shield break có `break_3` (= break2.ogg);
  style 1 & 2 dùng tiếng break gốc. Shield hit dùng chung 1 tiếng.
- **Thứ tự ưu tiên mỗi phát bắn**: kill / headshot-kill > còn shield (Absorption): tiếng SHIELD (hit hoặc break) **thay cho** tiếng hit thường > hết shield mới tính tiếng hit chung.
  Phát chí mạng thì tiếng shield của chính phát đó bị tắt (icon vẫn hiện), chỉ kêu tiếng kill.
- Server quyết định KHI NÀO phát (mục `combatSounds` trong `modernhud-common.toml`: bật/tắt từng loại, `shieldPriority`, `restrictToTacz`, cooldown).
  TACZ đi qua event gốc (có cờ headshot, bằng reflection), mọi sát thương khác (cận chiến, cung, SuperbWarfare, mod khác) đi qua LivingDamage/Death.
- Mỗi người chơi tự chọn trong **`modernhud-sounds.toml`**: style chung + style riêng từng cue, và **bật / volume (0-2) / pitch (0.5-2)** riêng cho hit, kill, headshot, shield hit, shield break.
- **Luôn đè TACZ / SBW**: sự kiện `PlaySoundEvent` ở client huỷ các âm thanh của tacz / superbwarfare khớp danh sách `override.patterns` (glob, sửa được).
  Lưu ý: mình KHÔNG có jar TACZ/SBW để xác nhận tên sound thật nên danh sách mặc định là đoán theo từ khoá (kill / headshot / hitmarker / indication).
  Nếu còn nghe tiếng cũ: bật `override.debugLog`, bắn thử, đọc `logs/latest.log` (dòng `ModernHud sound: ...`) rồi dán id vào `patterns`.
- Hai option âm lượng cũ `hitSoundVolume` / `breakSoundVolume` trong hud.toml được thay bằng volume của `shieldHit` / `shieldBreak` (giữ giá trị mặc định 0.6 / 0.7). Protocol mạng 2 -> 3.

### Glow
- **Một màu chung**: `panel.weaponGlowColor` (mặc định mint `9DFFB0`, đo từ ảnh ref) dùng cho flash đổi súng, glow góc box súng và glow ammo. Đã bỏ `ammoGlow.color` và `gear.selectedGlowColor`.
- **Glow ammo làm lại cho đúng ref**: (1) quầng haze rất nhạt phủ cả hàng ammo (`haze`, `spread`) + (2) **halo ôm sát hình từng con số** (vẽ chữ lệch 8 hướng 2 vòng, trong suốt)
  chỉ trên các chữ số sáng (số 0 đệm mờ không phát sáng) — thay cho khối tròn mờ trước đây. Tuỳ chọn: `opacity` (độ mạnh halo), `haloRadius` (px font), `haze`, `spread`, `followLowAmmo`, `pulse`.

### Config mặc định = file của bạn
- 66 giá trị trong code được đổi để khớp với `modernhud-weapon / hud / compass / bossbar / killstreak / hitindication.toml` bạn gửi (vd weapon scale 0.9, iconScale 1.3, offset 16/16, heat FLASH,
  compass length 400 / scale 0.6, killstreak attachToWeaponHud + autoHide, hitIndication radius 90 + hitMarkers + glow…). Chạy lại bộ so sánh: 299 key khớp, 0 lệch.
- Cố ý KHÔNG áp: `panel.weaponGlowColor` (E8ECF0 -> màu glow chung mới) và cả mục `ammoGlow` (ý nghĩa đã đổi).
- Key thừa trong file cũ (xoá được): `ammoGlow.color`, `ammoGlow.digitBloom`, `gear.selectedGlowColor`, `sounds.hitSoundVolume`, `sounds.breakSoundVolume`. Key mới chưa có trong file của bạn sẽ tự được thêm với mặc định.
- Màn YACL có thêm tab **Sounds** (style, từng cue, override, luật server).

## v1.0.1 (patch 21) — Dọn compass, config YACL, glow ammo / box súng, BURST

### Compass: chỉ còn kiểu MODERN
- Xoá hẳn `ClassicCompassRenderer` (~770 dòng) và `CompassStyle`; ~95 option của kiểu cũ bị bỏ. `CompassConfig` còn 23 option, gom theo mục:
  `general`, `tape` (length/scale/offsetY/fadeLength/color/dimOpacity/textShadow), `readout` (show/border/location/sideInfo), `markers`, `pingWheel`, `gunfire`.
- Key trong `modernhud-compass.toml` đổi tên (bỏ tiền tố `modern`): file cũ sẽ được tạo lại với giá trị mặc định.

### Config: màn hình YACL (bắt buộc cài YetAnotherConfigLib 3.6+ cho Forge 1.20.1)
- Mods > Modern HUD > Config mở màn YACL với các tab: General, Player, Team, Weapon, Killstreak, Boss bar, Compass, Feedback; mỗi tab chia nhóm có thể thu gọn,
  tooltip lấy từ comment của spec; kiểu điều khiển tự chọn theo dữ liệu (tick box / slider / ô số / enum / bảng màu hex / ô chữ). Nút Reset từng option do YACL cung cấp.
- Dữ liệu vẫn nằm trong các file `config/modernhud-*.toml` (ForgeConfigSpec) nên mọi renderer giữ nguyên; bố cục nhóm nằm trong `ModernHudConfigScreen`,
  còn option nào chưa được xếp sẽ tự rơi vào tab "Other" (không thể bị sót).
- `build.gradle`: thêm Xander Maven + `fg.deobf('dev.isxander:yet-another-config-lib:3.6.6+1.20.1-forge')`; `mods.toml`: dependency `yet_another_config_lib_v3` (client).
- Đã xoá các key lang của option không còn tồn tại.

### Weapon HUD
- **BURST** không còn giống AUTO: 3 viên ngắn hơn xếp bậc thang từ trái sang phải; AUTO vẫn 3 viên thẳng hàng.
- **Glow góc box súng đang chọn**: thay enum chọn góc bằng độ mạnh riêng cho từng góc (`selectedGlowTopLeft/TopRight/BottomRight/BottomLeft`, 0-100).
  Mặc định giống ref: góc dưới-phải mạnh nhất (100), trên-phải 55, hai góc trái 20.
- **Glow khung ammo** (mới, mục `ammoGlow`): quầng xanh bạc hà mờ phủ cả hàng mag | reserve + halo tròn sau từng cụm chữ số, đo từ ảnh ref
  (đỉnh ≈ 6% sáng cộng, rộng, nhạt dần ra ngoài). Tuỳ chọn: bật/tắt, màu, độ mạnh, tầm với, halo chữ số, đổi sang màu low-ammo khi sắp hết đạn, "thở".

## v1.0.1 (patch 19) — Icon đầu đạn (fire mode / reserve) + glow vàng góc box súng đang chọn

1. **Icon fire mode vẽ bằng đầu đạn pixel** (theo ảnh ref): mỗi viên = nắp ngắn, khe, thân kết thúc bằng mũi nhọn.
   FULL AUTO = **3 viên xếp chồng cùng hàng**, BURST = 3 viên, SEMI = 1 viên. Chế độ đang chọn trắng đặc, chế độ khác mờ.
2. **Icon cạnh số reserve = 4 viên đạn** xếp chồng, mỗi viên nhọn 2 đầu và tách 3 mảnh bằng khe mảnh (như ref). Vẫn tô theo màu + độ mờ của số reserve.
   Cả hai loại đều vẽ bằng pixel màn hình nguyên (`PixelDraw`): độ dày / khe là số nguyên, không còn texture bị thu nhỏ.
3. **Glow vàng trong góc box súng đang chọn** (vẽ SAU nền box, TRƯỚC icon súng; cộng sáng, bị cắt trong box vì mỗi góc chỉ sáng phần 1/4 bên trong). Cấu hình đầy đủ trong weapon config:
   - `selectedGlow` bật/tắt, `selectedGlowColor` (hex, mặc định vàng ấm FFC83A), `selectedGlowOpacity` (0-100), `selectedGlowRadius` (px, tầm với của glow)
   - `selectedGlowCorners`: DIAGONAL_TL_BR (mặc định) / DIAGONAL_TR_BL / TOP / BOTTOM / LEFT (đầu nòng) / RIGHT (đầu báng) / ALL
   - `selectedGlowPulse` + `selectedGlowPulseMs` + `selectedGlowPulseDepth`: glow "thở" chậm (tắt mặc định)
   - `selectedGlowSwitchBoost`: chớp sáng thêm khi đổi súng rồi tắt dần
   Vẫn nhân với `glowIntensity` toàn cục (0 = tắt mọi glow).

## v1.0.1 (patch 18) — Weapon HUD: icon fire mode / ammo type / dấu `|` / số mờ hết vỡ

Nguyên nhân gốc: các icon (fire_mode_*.png) là texture 128×128 nhưng bị thu xuống ~9px bằng nearest-neighbour → lấy mẫu 1/14 texel, rụng pixel, các vạch lệch nhau (icon "≣" cạnh số reserve bị răng cưa, đầu vạch so le).
1. **Icon fire mode (SEMI / BURST / AUTO) và icon ammo-type cạnh reserve** không còn dùng texture: vẽ bằng pixel màn hình nguyên (`PixelDraw.fireIcon`) — AUTO = 4 vạch, SEMI = 1 vạch,
   BURST = 3 vạch so le; độ dày / khe là số nguyên pixel nên luôn đều ở mọi GUI scale / widget scale. Chế độ đang chọn trắng đặc, chế độ khác mờ.
2. **Dấu `|` giữa mag và reserve**: glyph `|` của font pixel bị đứt đoạn → thay bằng vạch đứng 1 font-pixel vẽ thẳng, cao đúng chiều cao chữ số.
3. **Số 0 đệm mờ (000)**: phần mờ không còn đổ bóng (bóng dưới glyph bán trong suốt lòi ra thành khối tối lệch trông như chữ vỡ); phần sáng vẫn có bóng.
Texture fire_mode_*.png không còn được dùng (có thể xoá khỏi resources khi muốn).

## v1.0.1 (patch 17) — Weapon HUD: cột gear (melee / throwable / consumable) tự bố trí lại

Trước đây khung widget luôn chừa sẵn chỗ cho cột gear (GAP + COLUMN_W) dù không có item nào → cầm 2 súng mà không có melee/throwable thì hộp súng bị đẩy
lệch khỏi góc, thừa một khoảng trống. Giờ (`gear.autoLayout = true`, mặc định):
1. **Không có item gear nào** → cột gear biến mất hoàn toàn, hộp súng trượt vào sát góc màn hình (đúng offset đã cấu hình).
2. **Chỉ có throwable/consumable, không có melee** → cột co lại đúng bề rộng các ô vuông đang dùng; **chỉ 1 ô thì 1 ô vuông**, dồn sang trái, không để lỗ giữa hai ô.
3. **Có melee** → cột đủ rộng như cũ; ô throwable chỉ có 1 thì dồn trái (dấu chấm góc trên cũng tính theo vị trí đã dồn).
4. Bề rộng chuyển động mượt (cùng thời gian fade-in của gear), nên khi nhặt / hết item hộp súng trượt nhẹ thay vì giật. Lần đầu widget hiện ra thì vào thẳng bề rộng đúng, không trượt.
5. Ở chế độ idle (không cầm súng) ô throwable đã hết (00) bị ẩn nên cũng không chiếm chỗ; khi đang cầm súng ô hết vẫn giữ chỗ dạng mờ `00` như cũ.
6. Killstreak HUD tự theo vì dùng chung `gearWidth()`. Tắt tính năng: config `autoLayout = false` (cột luôn giữ đủ rộng như bản cũ).
Kỹ thuật: `LoadoutGear.update(player, idle)` giờ được gọi TRƯỚC `resolvePosition` (chiều rộng khung phụ thuộc slot nào tồn tại).

## v1.0.1 (patch 16) — Compass MODERN: vẽ theo pixel màn hình, vạch dọc thay chấm, tam giác rỗng đúng ref

Đo lại ref (ảnh 1024px: chữ số cao 12px) và sửa:
1. **Cột tick 5°/10° là 3 VẠCH ĐỨNG ngắn xếp chồng** (trên 2px – giữa 2px – dưới 4px, rộng 1px), không còn là 3 chấm 1×1. Ở vị trí 15°: vạch ngắn trên, nhãn, vạch dài dưới.
2. **Tam giác ▽ △ ở tâm và ◁ ▷ hai đầu**: trước là bitmap 5×3 / 4×5 đơn vị nên cạnh chéo bị đứt, dính vào đường tâm thành hình chữ T/đốm. Giờ vẽ từng hàng pixel màn hình
   (kích thước theo ref 8×7px, nét 1px, cạnh chéo luôn liền, đỉnh nằm đúng trên đường tâm).
3. **Ô readout + panel địa điểm**: mọi viền / bracket chữ L / vạch sáng vẽ bằng pixel màn hình nguyên (nét mảnh ~1px như ref, cánh bracket đủ dài), không còn bị méo khi scale lẻ.
4. Mọi hình học (tick, tam giác, mũi tên, ô) đều snap vào lưới pixel màn hình qua `fillPx` → không mất pixel ở `modernScale` lẻ hay GUI scale bất kỳ. Chữ giữ nguyên `HudText`.
5. Vị trí dọc đo lại từ ref: vạch trên y≈-1.2, giữa y≈6.4, dưới y≈13.9..16.3, ▽ y≈-5.9, △ y≈16.8, hàng ô y=25.

## v1.0.1 (patch 15) — Compass MODERN dựng lại theo số đo từ ref (so với ảnh in-game)

Đối chiếu ảnh in-game với ref phóng to, các lệch đã sửa:
1. **Thiếu chấm giữa**: ở các vị trí 5°/10° ref có **cột 3 chấm (trên – GIỮA – dưới)**; trước chỉ có trên + dưới. Ở vị trí 15° ref có chấm trên ngắn, nhãn ở giữa,
   vạch đứng ngắn bên dưới. Đã làm đúng như vậy.
2. **Đường kẻ mờ ở tâm** giữa hai tam giác (ref có, bản trước không).
3. **Ô readout**: ref là nền tối trong suốt + viền mờ + **bracket chữ L** sáng ở 4 góc; bản trước ra viền đặc + chấm vuông. Mặc định `modernReadoutStyle = CORNERS` giờ chính là kiểu ref
   (giá trị khác = viền thường). Key đổi tên từ `modernReadoutBorder` để không dính giá trị cũ.
4. **Readout và ô địa điểm: cùng chiều cao, cùng cỡ chữ (1.33× nhãn, đúng ref) và được căn giữa như MỘT CẶP** (tâm nằm giữa hai ô, không phải tâm của ô readout). Thẻ nhỏ `LOC.*` căn giữa cặp.
5. **Tỉ lệ**: đo lại — chữ nhãn cao 7.9px trong khi hai hàng chấm cách 15.8px và 5° = 23.3px, nên tape = **420** (không phải 336). Key `modernTapeWidth` → `modernTapeLength`.
   Mọi khoảng cách dọc theo đơn vị "chiều cao nhãn": chấm trên y=0, nhãn y=4, chấm giữa y=8, chấm dưới y=16, ▽ y=-6, △ y=18, hàng ô y=25.
6. **Chữ nhỏ hai bên bị dính nhau / dính mũi tên**: mũi tên đưa lên y=1 và khoảng cách dòng tính theo cỡ glyph thực sau khi snap pixel (không còn chồng dòng khi GUI scale lẻ).
7. **Bóng chữ**: ref không có; đã tắt (`modernTextShadow = false`). Chữ hướng (N/NE/E...) và readout in đậm.
8. Độ sáng như ref: số sáng nhất ở tâm và mờ dần, chữ hướng giữ sáng gần hết tape, hai đầu mờ; `modernDimOpacity` 0.6 = độ sáng đo từ ref.

## v1.0.1 (patch 14) — Compass làm lại đúng ref, chữ không còn méo khi scale

1. **Compass MODERN làm lại theo ref (2 ảnh)**: bố cục đo từ ảnh tham chiếu —
   - **hai hàng chấm** (trên 1px mờ, dưới 2px đậm hơn) mỗi 5°, nhãn ở giữa (số mỗi 15°, chữ N/NE/E... mỗi 45°);
   - độ sáng như ref: nhãn gần tâm sáng nhất, số mờ dần ra hai đầu, chữ hướng vẫn sáng gần hết tape, hai đầu mờ;
   - **tam giác rỗng** trên và dưới tâm; **mũi tên rỗng ◁ ▷** ở hai đầu kèm hai dòng chữ nhỏ (toạ độ X/Z, độ cao Y, tên dimension);
   - **ô readout cao 14** có bracket góc (chữ đậm, `E 74`), cạnh đó là **panel tên địa điểm** nền tối + vạch sáng bên trái; thẻ nhỏ `LOC.<dimension>` ngay dưới;
   - marker kẻ địch nằm ngay trên hàng chấm trên (chấm đỏ nhỏ).
   - Tỉ lệ: bề rộng tape mặc định **336** (đo từ ref: chiều cao chữ ≈ 2.4% bề rộng tape). Key `modernWidth` đổi tên thành **`modernTapeWidth`**
     (key cũ lưu giá trị 360 nên mặc định mới không áp dụng); `modernOffsetY` mặc định 8.
   - Mọi chuỗi (toạ độ, biome, readout) được cache, không format lại mỗi frame; cả compass vẫn vẽ trong một batch.
2. **Chữ bị méo / răng cưa khi scale xuống — sửa ở gốc**:
   - Nguyên nhân: patch 13 tắt snap scale của cả widget (để scale chỉnh mượt), nên chữ nằm trong widget scale lẻ lại bị vẽ ở tỉ lệ lẻ.
   - Mới: `HudText.text` — vị trí chữ vẫn đi theo scale mượt của widget (và được căn vào lưới pixel), nhưng **glyph luôn được vẽ ở số nguyên pixel màn hình
     trên mỗi pixel font (tối thiểu 1)**. Toàn bộ chữ của mod (player, team, weapon, ammo, boss, killstreak, perk, gear, compass MODERN) đã đi qua hàm này.
     Có thể tắt bằng `pixelPerfectText`.
   - Giới hạn thật: font bitmap không thể nhỏ hơn 1 pixel màn hình / pixel font nên ở scale rất nhỏ chữ sẽ dừng ở cỡ gốc (hộp co lại còn chữ thì không).
     Muốn chữ nhỏ hơn mà vẫn nét cần font vector (TTF) — chưa làm vì cần chọn font + kiểm tra trong game.
   - Style `CLASSIC` giữ renderer gốc 1:1 (có hiệu ứng co chữ theo cạnh) nên chữ ở style này chưa đi qua hàm trên.
3. Thêm guard chống đệ quy cho việc tự migrate màu panel cũ (`ColorConfig.migrateDarkPanel`).

## v1.0.1 (patch 13) — Modern Compass tích hợp, gear đúng tỉ lệ, tối ưu hiệu năng

1. **Modern Compass đã gộp vào ModernHUD** (không cần mod `moderncompass` riêng — hãy xoá jar đó đi, nếu để cả hai thì la bàn bị vẽ 2 lần):
   - Giữ nguyên toàn bộ chức năng: tape la bàn, ký hiệu hướng, ping/gunfire markers, location readout, mọi key config **giữ nguyên tên và giá trị mặc định**
     (`modernhud-compass.toml`, copy từng section từ `moderncompass-common.toml` là dùng được), phím bật/tắt (`Toggle Compass`, mặc định chưa gán),
     TACZ gunfire markers (mixin `TaczServerMessageSoundMixin`, tự bỏ qua nếu không có TACZ), Ping Wheel markers.
   - **Style mới `MODERN`** (mặc định) theo ảnh tham chiếu: tape mỏng 90°, tick 5° nhỏ / 15° dài + số, chữ N/NE/E... ở mỗi 45°, mờ dần hai đầu,
     tam giác nhỏ phía trên và dưới tâm, **ô readout có ngoặc góc** (`E 74`) và tên địa điểm bên cạnh (biome/toạ độ), toạ độ X/Z/Y mờ ở hai đầu,
     marker kẻ địch nằm ngay trên tape. Config `[Style]`: `modernWidth`, `modernScale`, `modernOffsetY`, `modernFadeLength`, `modernColor`,
     `modernDimOpacity`, `modernShowReadout`, `modernReadoutBorder`, `modernShowSideInfo`, `modernShowMarkers`, `modernLocation`.
   - **Về style cũ**: `[Style] style = "CLASSIC"` — bản port 1:1 của renderer gốc (texture, cull/edge fade...), chỉ khác là đi qua GUI scale riêng của HUD.
   - Giới hạn: Ping Wheel không có API biên dịch sẵn nên được nối bằng reflection; marker ping vẽ dạng hình thoi đúng màu ping thay vì icon animation của Ping Wheel.
2. **Gear dựng lại theo ảnh ref**: khung vũ khí chính 64×32 (tỉ lệ 2:1), ô secondary cao 16; cột gear rộng 66: ô melee dẹt 16 cao,
   hai ô throwable/consumable **vuông đúng 32×32**, thẳng đáy với ô secondary; khe 2. Idle: ô melee 66×16 + hai ô 32×16 thẳng hàng với 2 ô súng idle.
3. **Animation ẩn/hiện từng slot**: mỗi slot (melee, 2 slot tracked) fade in khi có item, fade out khi mất item. Ở idle, slot hết item (00) cũng fade biến mất;
   cầm súng thì giữ dạng mờ `00`.
4. **Scale chỉnh không ăn**: snapping pixel của scale CẢ widget (patch 12) làm các bước scale thô (1/GUI-scale) nên chỉnh nhỏ không thấy đổi.
   Giờ `pixelPerfectElements` mặc định **tắt** (scale đổi mượt); chữ vẫn được snap pixel (`pixelPerfectText`) nên không bị méo. Bật lại nếu thích pixel-art sắc nét.
5. **Hiệu năng (HUD gây lag)**:
   - Glow: bảng sin/cos và đường falloff được cache (trước đây mỗi glow tính hàng nghìn sin/cos/pow mỗi frame); số vòng/đoạn giảm
     (`glowQuality`: LOW 3/3, MEDIUM 5/5, HIGH 8/8 thay cho 4/4, 7/7, 12/10) mà vẫn mượt.
   - Compass MODERN gộp toàn bộ fill/text vào **một lần vẽ** (`drawManaged`) thay vì mỗi tick một draw call; chuỗi số được cache.
   - Bỏ `String.format` trong đường vẽ mỗi frame (gear).
   - **Sửa lỗi rò rỉ shader color**: `RenderSystem.getShaderColor()` trả mảng nội bộ nên việc "lưu rồi khôi phục" không khôi phục gì; alpha mờ của một phần tử có thể
     rò sang các phần tử vẽ sau (HudPanel, WeaponHud, LoadoutGear). Giờ sao chép giá trị trước khi đặt.
   - Nếu vẫn lag: hạ `glowQuality` xuống MEDIUM/LOW, hoặc `glowIntensity = 0` để tắt toàn bộ glow.

## v1.0.1 (patch 12) — Config thật sự ăn, text pixel-perfect, hit indication kiểu MW, gear fix

1. **Config "không áp dụng" — nguyên nhân & fix**
   - `appearance.frameWidth` / `frameHeight` trước đây CHỈ dùng để neo vị trí nên với anchor phải không thấy đổi gì. Giờ:
     `frameWidth` = độ rộng khung widget và **panel ammo giãn ra lấp vùng bên trái ô icon** (số vẫn căn phải); `frameHeight` = nội dung
     được **căn giữa theo chiều dọc** trong khung. (Mặc định `frameWidth` 160 → 150 cho panel khít như cũ.)
   - File config cũ giữ nguyên màu panel tối `0A0D11` sinh từ bản trước nên mặc định mới (trắng xám) không bao giờ áp dụng.
     Giờ tự **migrate**: panel nào còn đúng màu cũ `0A0D11` (chưa chỉnh) sẽ đổi sang `D9DEE3` + opacity mới (weapon, hud, boss, killstreak, perk, ammo alert).
     Giá trị bạn đã tự chỉnh được giữ nguyên.
   - `gear.topDotInner` đổi tên thành **`gear.topDotsOuter`** (mặc định false = dot trên nằm ở góc TRONG) vì giá trị `false` cũ lưu trong file
     làm dot luôn nằm ở góc ngoài. Key mới nên mặc định mới áp dụng ngay.
2. **Text scale nhỏ bị méo — pixel-perfect** (`[animation] pixelPerfectText`, mặc định bật; `guiScaleSnap`):
   - Nguyên nhân: font bitmap + nearest-neighbour; ở scale phủ 1.8 px màn hình/pixel font thì nét dày mỏng không đều, dưới 1 px thì rớt cột/hàng.
   - Cách xử lý: mọi scale của element (weapon, player, team, boss, killstreak, perk, ammo alert) và scale chữ (ammo/reserve, boss number/name, fire type)
     được snap để 1 pixel font = **số nguyên** pixel màn hình (tối thiểu 1); layout tính bằng scale đã snap nên hộp và chữ luôn khớp;
     cặp số lớn/nhỏ (mag/reserve) luôn giữ chênh lệch 1 bậc. `HudText.drawSnapped` căn gốc chữ vào lưới pixel.
   - `guiScaleMode = RESOLUTION` + `guiScaleSnap` làm tròn GUI scale về số nguyên.
   - Giới hạn: font bitmap không thể sạch dưới 1 px màn hình/pixel font. Muốn hết giới hạn hẳn cần font vector (TTF oversample) — chưa làm.
3. **Item `lrtactical:flash_shield`** hiện ở slot melee (ưu tiên melee thật nếu có cả hai); cầm nó cũng giữ HUD súng.
4. **Hit indication làm lại kiểu MW** (`modernhud-hitindication.toml`):
   - Texture mũi tên → **cung glow quanh tâm ngắm**, xoay mượt theo camera (nội suy partialTick), fade theo ms thật.
   - Cấu hình mới: `radius`, `arcSpanDegrees`, `thickness`, `opacity`, `fadeInMs/holdMs/fadeOutMs`, `maxIndicatorCount`, `mergeSameDirection`
     (nhiều hit cùng hướng = 1 cung), `glow`, màu hit / absorption.
   - **Đã xoá** (vô dụng với PvP): edge-of-screen mode, block (shield) indicator, non-directional (rơi/lửa/đói), hit từ potion,
     size theo damage, distance scaling, indicatorDefaultScale, fadeRate (tick).
   - Hit marker crit/kill giữ lại, mặc định bật.

## v1.0.1 (patch 11c) — Gear: ô vuông, idle có 3 ô, HUD súng không ẩn khi cầm item gear

- **Hai ô dưới vuông** (cạnh = nửa cột trừ khe, 29x29), icon nhỏ lại (14px), melee dẹt chiếm phần còn lại phía trên, đáy cột thẳng hàng với ô secondary.
- **Dots nhỏ hơn** (mặc định `borderLength = 2`); hai dot trên của 2 ô dưới nay nằm ở **góc trong** (`topDotInner = true` mặc định).
- **HUD súng không ẩn** khi cầm melee / throwable / consumable lrtactical: giữ nguyên HUD của khẩu súng đang dùng (kể cả khi mod quick-throw đổi item vào tay).
- **Idle**: cột gear luôn hiển thị, bố cục gọn thẳng hàng với 2 ô súng idle (ô melee dẹt; 2 ô nhỏ có icon bên trái + số lượng bên phải, cùng cỡ chữ với bộ đếm reserve).
  Idle không còn "đẩy ra một khúc trống" vì chỗ đó nay có 3 ô.
- Lưu ý: file `modernhud-weapon.toml` đã có sẵn giữ `borderLength = 3` / `topDotInner = false` cũ; xoá hai dòng đó (hoặc cả file) để nhận mặc định mới.

## v1.0.1 (patch 11b) — Dựng lại cột gear theo ảnh tham chiếu

- **Hình học theo ref MW**: cột gear rộng đúng bằng ô vũ khí chính; ô **melee dẹt** (~53% chiều cao ô chính, dài), hai ô **throwable/consumable cao**
  nằm ngay dưới và kết thúc đúng đáy ô secondary; khe 3 giữa ô chính và cột, 2 giữa melee và hai ô dưới.
- **Bỏ dấu X**: hết item thì icon chỉ mờ đi, số hiện rõ `00`.
- **Số lượng** cùng size / màu (`reserveColor`) / lề trái như bộ đếm reserve của ô secondary, nằm góc dưới-trái ô.
- **Dots** vẽ y như ô secondary (ô vuông đặc nằm trong góc, size = `borderLength` mặc định 3):
  melee chỉ 2 dot bên phải (trên-phải, dưới-phải); mỗi ô dưới có 2 dot đáy + 1 dot trên, hai ô đối xứng nhau
  (`topDotInner` đổi sang góc trong).
- **Idle**: cả 3 ô ẩn (fade) khi không cầm súng, hiện lại khi rút súng. Chiều rộng cột vẫn được giữ chỗ để widget không nhảy vị trí.

## v1.0.1 (patch 11) — GUI scale độc lập, gear slots (lrtactical), specialist gọn, glow chất lượng cao

1. **GUI scale độc lập** — `modernhud-hud.toml` `[animation]`:
   - `guiScaleMode`: `MINECRAFT` (mặc định, như cũ) | `FIXED` (luôn dùng `hudGuiScale`) | `RESOLUTION`
     (`hudGuiScale * chiềuCaoCửaSổ / guiScaleReferenceHeight` → HUD giữ đúng tỉ lệ màn hình ở mọi độ phân giải/cửa sổ).
   - Mọi overlay (player, team, weapon, ammo alert, killstreak, perk toast, boss bar, hit indicator, feedback) đều đi qua `HudScale`,
     kể cả khi F1 (IgnoreHideGui). Decimal được (ví dụ 2.5).
2. **Specialist** (killstreak): không vẽ ô thứ 4 ở mốc 8 kill nữa (`specialistHideFinalStage`); khi đủ perk, tracker chạy hiệu ứng hoàn tất
   một lần rồi ẩn và **không update thêm** khi kill tiếp (`specialistStopWhenFull`).
3. **Loadout: cột gear mới** bên phải ô vũ khí chính (`modernhud-weapon.toml` `[gear]`), cần lrtactical:
   - Ô trên (rộng): item **melee** lrtactical tìm thấy trong hotbar.
   - 2 ô dưới: tự dò **throwable** và **consumable** trong hotbar, tối đa 2 item, hiện số lượng (dạng `01`).
     Hết item → ô giữ chỗ, icon mờ + **X đỏ** + số **00** (nhường chỗ cho item mới khi tìm thấy).
   - Viền = **chấm vuông** ở 4 góc (`[gear.style]`, đổi được `borderStyle`, opacity, panel...). `countWholeInventory`, `glow`, `showGear`.
   - Tỉ lệ ô theo ảnh tham chiếu: ô melee = 2 ô nhỏ + khe; hàng dưới kết thúc đúng đáy khung widget. Khung widget tự mở rộng
     để cột gear không bị tràn ra ngoài màn hình; killstreak bám đúng mép phải mới.
   - Detect theo registry (`lrtactical:throwable/consumable/melee...`, fallback theo tên class) + id trong NBT, không phụ thuộc compile.
4. **Glow chất lượng cao áp rộng hơn**:
   - `glowQuality` = LOW | MEDIUM | **HIGH (mặc định)** — số vòng/đoạn cung của mọi halo.
   - `SoftDraw.radial` + `GlowEffect.drawBloom`: bloom tròn mượt cho icon.
   - Áp dụng mới: icon + hộp từng perk (`[killstreak] glow`...), gear slots, chữ ammo alert (`ammoAlertGlow`);
     glow của boss bar giờ cũng nhân với `glowIntensity` toàn cục.

**Cần test trong game:** tên registry của item melee lrtactical (mình dò theo từ khoá `melee`, chưa xác minh được tên chính xác);
icon item dùng `renderItem` nên phụ thuộc cách lrtactical render item trong GUI. Nếu ô melee/throwable không hiện, gửi `/give`-id của item
(hoặc ảnh F3+H tooltip) để mình chỉnh bộ dò. Chưa compile (cùng lý do như trước).

## v1.0.1 (patch 10b) — Perk toast rework, shadow trắng xám nhạt, killstreak dạng chấm, style config ăn thật

1. **Perk toast rework.** Mỗi perk là một box mỏng RIÊNG (shadow trắng xám mềm + nền phẳng nhạt), không còn một box lớn chung.
   Khung ngoài vẫn đóng bằng 4 chấm (2 chấm trên cùng, 2 chấm dưới cùng của cả khối, mặc định `DOTS`; đổi `FRAME` để có thêm viền mảnh).
   Animation là **fade thuần** (các dòng fade lần lượt, `rowStaggerMs`), không còn push/slide. `animations` và `animationSpeed` toàn cục
   giờ cũng áp cho toast này. Config: `[perkToast.style]` (`panelEnabled/Color/Opacity` = box từng perk, `borderStyle`..., `fadeIn/OutMs`).
2. **Shadow/box toàn HUD đổi sang trắng xám nhạt, mỏng** thay cho xám đen: mặc định `panelColor = D9DEE3`, opacity 14-16
   (boss bar, killstreak, perk toast, ammo alert, weapon widget, player widget). Shadow của boss bar cũng lấy màu/độ đậm từ style.
3. **Khung killstreak = chấm dots** ở 4 góc mỗi slot (mặc định `DOTS`, outset 0) thay cho viền khung.
4. **Config style không đổi gì khi chỉnh — nguyên nhân và fix:**
   - Killstreak: `inheritWeaponStyle` mặc định bật nên toàn bộ `[killstreak.style]` bị bỏ qua → **đã xoá option này**, slot luôn dùng style riêng.
   - Boss bar: panel/border của style chỉ chạy khi `backdrop = BOX` → giờ màu/độ đậm style điều khiển cả `SHADOW`,
     và `borderStyle` (dots/line/...) áp ở mọi mode trừ `NONE`. `backdropOpacity` mặc định 100.
   - Perk toast: panel của style trước đây chỉ vẽ một box chung, giờ vẽ box từng perk; fade/stagger/speed đều có tác dụng.
   - Lưu ý: file `.toml` đã có sẵn giữ giá trị cũ (màu `0A0D11`). Muốn thấy mặc định mới, xoá các file `modernhud-*.toml` trong `config/`
     (hoặc tự đổi `panelColor` thành `D9DEE3`).

## v1.0.1 (patch 10) — Bossbar cố định (hết flicker), glow mượt, animation đồng bộ, đủ option cho mọi element

### A. Yêu cầu của bạn
1. **Bossbar flicker / hành xử như auto-hide — FIX.**
   - Nguyên nhân: bossbar chỉ "tồn tại" nếu vanilla báo nó ở *đúng frame đó* (danh sách `FRAME` bị xoá sau mỗi lần render).
     Chỉ cần một frame vanilla không báo (mod khác huỷ overlay boss, hitch, F1...), bar bị coi là "đã mất" → ease-out,
     frame sau quay lại → ease-in. Lặp lại liên tục = nhấp nháy / như auto-hide.
   - Sửa: boss event được lưu bền (`SEEN`) kèm thời điểm thấy lần cuối; bar **chỉ ease-out khi vanilla thôi báo hơn 700 ms**.
     Bar giờ đứng yên cố định cho tới khi boss thật sự biến mất.
   - Thanh fill không còn bị "khởi động lại" mỗi frame (trước đây `setTarget` mỗi frame với ease in-out làm fill lết chậm);
     chỉ retarget khi giá trị đổi thật (> 0.05%).
   - Cảnh báo máu thấp (≤ 20%) giờ là nhịp thở chậm, nhẹ (trước đây nhấp đỏ nhanh, dễ nhầm với flicker).
2. **Config đầy đủ cho các element mới** — mỗi element giờ có nhóm `style` thống nhất (class mới `ElementStyle`):
   `opacity` (độ mờ tổng), `fadeInMs`, `fadeOutMs`, `panelEnabled/Color/Opacity`, và **kiểu border**
   `borderStyle` = `NONE | CORNERS | FULL (line) | DOTS | BOTTOM_DOTS | FRAME (line + dots)` + `borderColor/Opacity/Length/Outset`.
   - Boss bar → `modernhud-bossbar.toml` `[bossBar.style]` (BOX backdrop giờ dùng style RIÊNG, không còn mượn của weapon).
   - Killstreak slots → `modernhud-killstreak.toml` `[killstreak.style]`; `inheritWeaponStyle` (mặc định true = giống hệt trước đây,
     false = dùng style riêng); `animationMs`; `fadeInMs/fadeOutMs` = thời gian slide vào/ra của auto-hide.
   - Perk toast → `[perkToast.style]`: `fadeInMs/fadeOutMs` = thời gian slide vào/ra từng dòng, `rowStaggerMs`, border type
     (mặc định `FRAME` = y như khung cũ), panel tuỳ chọn, opacity.
   - Ammo alert (RELOAD / LOW AMMO / NO AMMO) → `modernhud-weapon.toml` `[ammoAlerts.style]`: alert giờ **fade vào/ra**
     (trước đây bật/tắt cái rụp), panel + border tuỳ chọn, `ammoAlertPulse` (bật/tắt nhịp thở).
   - Icon feedback (hit/break) → `feedbackOpacity`, `feedbackFadeMs` (giữ rõ rồi mới fade ở cuối).
   - Player / Team HUD → `healthAnimMs`, `absorptionAnimMs` (thời gian bar trượt tới giá trị mới).
   - Đã thêm bản dịch en_us cho mọi key mới (và 8 key cũ đang thiếu tên).
3. **Animation mượt và đồng bộ** — `modernhud-hud.toml` `[animation]`:
   - `animations` (công tắc tổng), `animationSpeed` (hệ số tốc độ cho TẤT CẢ element cùng lúc), `glowIntensity` (độ mạnh glow toàn cục).
   - `EasedAnimator`: đổi hướng khi đang chạy sẽ ease-OUT thay vì ease in-out khởi động lại từ đứng yên (hết cảm giác "dính" khi giá trị
     đổi liên tục); dùng đồng hồ monotonic `nanoTime` (không nhảy theo giờ hệ thống); thêm `setDurationMs`.
4. **Glow low-poly → mượt.** `SoftDraw.soft` viết lại: halo bo tròn (cung 7 đoạn mỗi góc, 7 vòng, falloff smoothstep) thay cho 3 vòng
   chữ nhật góc vuông. Dùng chung cho mọi glow + shadow trong HUD. `GlowEffect` rộng/êm hơn một chút và nhân với `glowIntensity`.

### B. Thêm bởi mình
- **Damage trail cho bossbar** (`damageTrail`, mặc định bật): phần máu vừa mất nằm lại sáng hơn phía sau fill rồi tan dần — dễ đọc từng hit.
- Border types mới dùng chung được cho mọi element: `DOTS`, `BOTTOM_DOTS`, `FRAME` (enum `PanelBorderStyle` + `HudPanel.drawCornerDots/drawStyled`).
- `HudPanel.drawStyled(...)` — vẽ panel + border từ một `ElementStyle` chỉ bằng một dòng, để element mới sau này đủ option ngay.

### C. Chưa làm / cần bạn test trong game
- **Chưa compile được** (sandbox không tải được Forge). Đã kiểm tra cú pháp toàn bộ source bằng javac; lỗi duy nhất còn lại là thiếu thư viện Forge/MC.
  Nếu `gradlew build` báo lỗi, gửi log mình sửa ngay.
- Player HUD / Weapon HUD chính chưa có `opacity` tổng (chỉ có opacity panel/border như cũ); Team HUD chưa có fade vào/ra từng hàng.
- Hit indicator giữ nguyên `indicatorOpacity` / `fadeRate` sẵn có.
- Config cũ vẫn đọc bình thường; key mới tự sinh với giá trị mặc định. Look mặc định của mọi thứ cũ không đổi, trừ: glow rộng/êm hơn,
  alert ammo fade, icon feedback giữ rồi mới fade.

## v1.0.1 (patch 9) — Smooth glow overhaul, boss bar v2, selected-perks toast, blue Absorption hits
1. **Smooth rendering (`SoftDraw`).** New float-coordinate, per-vertex-colour draw helper: gradients and glows are
   interpolated by the GPU instead of being built from stacked whole-pixel rectangles (the source of the blocky /
   pixelated look at 2-4x GUI scale). `GlowEffect.drawGlowRect` - used by every glow in the HUD (weapon switch, low
   ammo, ready killstreak slots, team "down" pulse, heat flash, ...) - now draws one feathered mesh: a gentle tint
   over the box, brightest at its rim, fading smoothly outward (instead of 17 offset copies). Same call, so all of
   them improve at once. New `Ease` curves (in-out cubic, out-cubic, out-back) + `CUBIC_IN_OUT` in `EasedAnimator`.
2. **Boss bar v2** (after the MW reference): the track line + shaded fill (deeper tail -> true colour, top sheen)
   + bright leading edge with its own bloom are all `SoftDraw` now. **Each bar has its own backdrop** (`backdrop`:
   NONE / SHADOW = soft feathered drop-off / BOX = weapon-widget panel; `backdropOpacity`) - never one shared
   box. **`numberLayout`**: SIDE, ABOVE_BELOW (default: first bar's number above, last bar's below - the MW
   layout for 2 bars; one bar = above, 3+ = side), ABOVE, BELOW; the name shares a caption row with its number,
   right-aligned over the track end. **Ease in/out** (`animations`, `animationMs`): bars slide in from the screen
   edge and fade in, slide out and fade when the boss is gone (kept alive just long enough), the fill eases to its
   value, and the stack glides when a bar joins/leaves. `glowIntensity`; text stays at native size (1.0).
3. **Selected-perks toast** (needs ModernLoadout; `[perkToast]` in `modernhud-killstreak.toml`): when the loadout's
   perks are selected - on joining or whenever the loadout changes them, Specialist's chosen perks included - ONE
   framed box (thin white outline, a dot on each of the four corners, faint soft shadow) slides in with each perk as
   "NAME [icon]" on a very light thin highlight strip, rows entering one after another with ease-in-out, holds
   `holdSeconds`, and slides back out in the same order. `position`, `offsetX/Y`, `scale`, `iconSize`, `frame`,
   `nameHighlight`. In Specialist mode it replaces the small blink of the selected perk icons above the tracker.
   Reads ModernLoadout's new `StreakApi.selectedPerks*` (version counter + ids/labels/textures of the active class's
   perk picks, or the chosen Specialist perks).
4. **Killstreak animation overhaul:** slots leave the queue / join the ready row / drop down with ease-in-out
   (420 ms) instead of out-only, and the auto-hide slide uses the same curve (460 ms).
5. **Hit indicators are ocean blue while you still have Absorption** and only turn red once it is gone
   (`absorptionBlueIndicator`, `absorptionIndicatorColor`, default `#2F8CFF`). The server decides (after vanilla has
   taken the hit out of Absorption, "still > 0" means it did not break through) and sends the flag with the
   indicator; the colour is fixed at hit time so it never flips while fading. **The network protocol version is
   bumped to 2** (the indicator packet gained a field) - client and server must both be on this build.

## v1.0.1 (patch 8) — Boss bar rebuilt to match the MW reference
1. **Bar redrawn after the Modern Warfare reference:** a thin full-length TRACK line with an end cap (the empty
   part) under a thicker FILL that has a soft glow outline (`barGlow`) and a bright, glowing leading edge
   (`headGlow`) - so where the progress ends is unmistakable and the filled/empty parts are clearly separated.
   The fill ramps from darker to brighter toward the head. Notched boss bars keep their notch gaps.
2. **Number: exact health or percent** (`numberMode`): PERCENT (default, every boss event) or HEALTH - the boss's
   exact health points when its entity is loaded on the client and can be matched to the bar by name (wither,
   ender dragon, most mod bosses; re-matched at most once a second, nearest first); otherwise it falls back to
   the percent. `percentSign` adds a %. The number column auto-widens to the widest number.
3. **Text at native size:** `numberScale` / `nameScale` default 1.0 (and the pose scale is skipped entirely at 1.0)
   so the pixel font is no longer distorted - previously the number was 1.25x and the name 0.65x.
4. **Spacing / layout:** tighter, configurable (`barSpacing`, default 4); the name label (`nameMode`: ABOVE,
   right-aligned over the track like a "WINNING" label, BELOW, or HIDDEN) is uppercase, small and grey.
5. **Drop shadow** behind the block (`dropShadow`, `shadowOpacity`): stacked feathered translucent rects with plain
   alpha blending (an additive black glow would draw nothing). Text keeps its own shadow too.
6. New config: `barSpacing`, `numberMode`, `percentSign`, `numberScale`, `nameMode`, `nameScale`, `showTrack`,
   `barGlow`, `headGlow`, `dropShadow`, `shadowOpacity`; `showName` replaced by `nameMode`; `barWidth` default 80,
   `barHeight` default 4.

## v1.0.1 (patch 7) — Compact corner boss bar
1. **Vanilla's wide top-centre boss bars are replaced by small short bars tucked into a screen corner**
   (`modernhud-bossbar.toml`, `enabled` on by default; off = vanilla look), Modern Warfare match-progress style:
   a big number (the boss's health / objective progress as a percent, 0-100) on the left, a thin bar next to
   it, the boss name small underneath. Notched boss bars keep their notches as thin gaps; the bar takes the boss
   event's colour (`useBossColor`) and pulses red at 20% or less (`lowWarning`).
   Config: `position` (corner), `offsetX/Y`, `scale`, `barWidth` (72), `barHeight` (3), `maxBars` (3),
   `showNumber`, `showName`.
2. How: `CustomizeGuiOverlayEvent.BossEventProgress` is cancelled for every boss event (vanilla then draws
   neither the bar nor its name, and the vertical spacing is zeroed) and the event is remembered; the new
   `BossBarOverlay`, registered above everything, draws the remembered ones in the corner each frame. Vanilla
   still handles screen darkening, boss music and fog itself. Anything using vanilla boss events is covered
   (wither, ender dragon, raids, mod bosses, `/bossbar`).

## v1.0.1 (patch 6) — Killstreak HUD refinement + auto-hide
1. **Kill bar is bare and counts per slot.** No box, no border, no shadow backing - just white segments. It shows
   only the kills still needed for the NEXT reward (the gap from the previous reward, e.g. 3/5/7 -> 3, then 2, then
   2 segments), starts empty for each slot and refreshes empty for the next one once a reward is earned, instead of
   one thick stack of the running total. A partial hostile-mob point fills part of one segment.
2. **More space between slots** (`slotGap` default 8, up to 24; bar gap 6) so the corner brackets no longer touch
   the neighbouring box. Each streak / Specialist slot is its own separate box.
3. **Ready glow is light green** (and the ready tint with it). Config keys were renamed (`readyGlowColor`,
   `activeGlowColor`, `killBarColor`) so an old `modernhud-killstreak.toml` can't keep the previous gold/yellow -
   old keys are simply ignored.
4. **Auto-hide** (`autoHide`, default on; `autoHideSeconds`, default 5): after that long with no change the whole
   tracker slides out toward the screen edge and fades, and pushes back in on the next change (a kill, a reward,
   the loadout changing). Item rewards that are ready to use keep it on screen.
5. **Specialist** always works this way regardless of the toggle: once equipped it stays hidden, pushes in while
   progress is being made (the next perk's slot + bar), glows when the perk comes online, then pushes out again.

## v1.0.1 (patch 5) — ModernLoadout killstreak / Specialist HUD compat (Modern Warfare style)
1. **Killstreak / Specialist tracker, no text, drawn in the weapon widget's style** (only when ModernLoadout /
   `mwloadout` is installed; `modernhud-killstreak.toml`):
   - **Square slots** (`slotSize`, default 26) with the weapon widget's panel + corner brackets, stacked above the
     weapon widget on the same right edge and scale, with a clear gap (`groupGap`, default 14) - or free
     `position`/offsets (`attachToWeaponHud = false`).
   - **Kill bar** beside the column: one segment per kill needed for the NEXT reward, filled yellow from the bottom
     (newest segment flares on a kill, a partial hostile-mob point half-lights one). Empty segments = kills to go.
   - **Queue + ready row**: when a reward is earned its slot glows green and slides sideways into a horizontal row;
     the slots above drop down into the freed positions. When every reward is earned the whole group sits in the
     row glowing green together (with one synchronized flare). A used-up reward fades out of the row and the row
     closes up; Specialist perks stay lit while active.
   - **Selection display**: when the selected streaks / Specialist perks change (joining, or the loadout changing
     them) the chosen set blinks once above the tracker and fades away (`showSelection`, `selectionSeconds`).
   - Hidden in Spectator / SBW vehicles like the weapon widget. Removed the old text captions, `slotHeight`,
     `nearestFirst`, `showKillCount`. New defaults: `readyColor` green, `progressColor` yellow (existing config
     files keep their old values - delete `modernhud-killstreak.toml` to pick up the new colours).
2. **State comes straight from ModernLoadout, not re-derived.** `ModernLoadoutCompat` reads ModernLoadout's client
   API (`dev.szo9k4.mwloadout.api.StreakApi`) by reflection - no compile dependency; a ModernLoadout without that
   class just keeps its own tiles. It polls `version()` each frame and re-reads only when a progress packet
   arrived (thresholds with Hardline applied, the class's equipped selection, earned flags, Specialist stages,
   hostile-mob fractional progress, death/Killchain resets), plus a 250 ms re-read of "reward still carried" for
   READY vs used. A freshly earned reward is treated as ready for 1.5 s so the inventory lagging the packet is not
   read as "already used".
3. **No double HUD.** While ModernHud draws the tracker it calls `StreakApi.setExternalHud(true)`, which stops
   ModernLoadout's own tiles/meter from drawing (tracking keeps running). Turning `enabled` off here gives them back.
4. `mods.toml`: optional `mwloadout` dependency (ordering AFTER, client side).

## v1.0.1 (patch 4) — TaczPresence HUD hidden in spectator, Team HUD keeps down teammates
1. **TACZ / TaczPresence HUD now stays off while you are a Spectator or in an SBW vehicle, whatever
   the HUD source.** The two share one render gate (`GUN_HUD_ENABLE`); previously only the
   ModernHud-source path held it off, and the early check bailed out before this when the preferred
   source was TACZ_HUD/TACZ_P_HUD. `WeaponHudOverlay` now forces the gate off for as long as either
   state lasts (re-asserted ~1/s) and hands it back to the normal preference sync as soon as it ends.
2. **Team HUD: dead or Spectator teammates no longer disappear.** The roster was built from the
   loaded-entity list, so a dead teammate (entity removed after death) or a spectator not currently
   tracked simply vanished. It now comes from the tab list (`PlayerInfo`: team via the scoreboard,
   gamemode, skin), so such teammates always keep their row — empty health/Absorption bars plus the
   red pulse. A death seen while tracked is remembered until the entity is seen alive again.
   Alive teammates out of tracking range are still hidden (no live data to show).

## v1.0.1 (patch 3) — Weapon HUD & hit indicator can now ignore F1
1. **New config: `ignoreHideGui`** — `modernhud-weapon.toml` (governs the weapon widget + the
   low-ammo alert) and `modernhud-hitindication.toml` (governs the hit indicator), both default
   `false`. F1 ("hide GUI") isn't something an overlay can veto from inside its own `render()` —
   Forge's normal overlay pipeline only runs registered overlays at all when
   `Minecraft.options.hideGui` is false, full stop, regardless of what the overlay itself checks
   (confirmed by the hit indicator, which had zero internal hideGui logic yet still hid under F1).
   Implemented via a second, always-runs render pass (`IgnoreHideGuiRenderer`, hooking
   `RenderGuiEvent.Post` — fires unconditionally, wrapping the entire GUI render pass including the
   hideGui gate itself) that re-invokes the SAME overlay instances (not fresh ones, so switch-glow
   timers/active indicators/etc keep animating normally rather than resetting every frame) only for
   the frames the normal pipeline just skipped. Screenshots taken with F1 still hide everything
   else (personal widget, team HUD, feedback icon) as usual — this is scoped to just these two.

## v1.0.1 (patch 2) — TACZ HUD fully hidden in vehicle/spectator, performance pass
1. **TACZ's own gun HUD wasn't fully hidden while riding an SBW vehicle or in Spectator mode.**
   Root cause: the earlier `restoreVanillaHudIfIdle()` fix (see patch 1, item 3) was called from
   BOTH the Spectator and SBW-vehicle early-returns — which actively turned TACZ's HUD back ON in
   exactly those two situations, overlapping SBW's own vehicle HUD and showing stale gun HUD state
   while spectating. Removed those two calls; `restoreVanillaHudIfIdle()` now only fires from the
   genuinely-idle "not carrying any gun at all" case it was originally meant for.
2. **Performance pass** — reduced per-frame allocation/CPU work in the hottest render paths, aimed
   at cutting GC-pause-driven frame stutters:
   - `TeamHudOverlay`: the teammate list was reallocated with `new ArrayList<>()` on every single
     `render()` call (up to hundreds of times a second) — now a reused scratch field, cleared
     instead of reallocated.
   - `TeamHudOverlay`: the per-teammate health/Absorption animator maps grew unboundedly over a
     long session with no eviction — now pruned (players no longer on the roster removed) at most
     once every 5 seconds, cheap enough to not matter but bounds long-session memory growth.
   - `WeaponHudOverlay`: the "carrying but not holding a gun" hotbar scan (`renderNeutral`) rescanned
     all 9 hotbar slots and allocated a List+HashSet+array EVERY frame, for as long as the player
     simply wasn't holding a gun (a common, often long-lived state) — now throttled to once every
     250ms with the result cached in between, and the scan itself rewritten to skip the
     List/HashSet/toArray churn in favour of a small fixed-size array.
   - Confirmed `TaczCompat`'s gun/index lookups already have a 20ms cache, so the heat-display
     methods (`hasHeat`/`heatFraction`/`isOverheatLocked`), each called back-to-back within the same
     frame, don't redundantly re-resolve TACZ's API — no change needed there.
   - `GlowEffect`'s glow-halo rendering was already reasonably batched (one blend-state change per
     glow call, not per rect) — left as-is.

## v1.0.1 — Spectator mode, TACZ overheat display, Team HUD "down" state, ammo-icon scale, name team color
1. **Ammo-type icon next to reserve was rendering oversized** — it was sized off the MAG digits'
   height (`ammoScale`, larger) instead of the RESERVE digits' own height (`reserveScale`, smaller),
   and even matching reserve height 1:1 still read too big next to the actual glyph ink. Added a
   dedicated `ammoTypeIconScale` config (`modernhud-weapon.toml`, default `0.65`) on top of tracking
   reserve height, so it can be tuned independently instead of just inheriting text scale.
2. **Weapon widget and personal widget now hide while in Spectator mode.** Scoped strictly to these
   two overlays' own `render()` — a plain early return before they draw anything — never touches
   vanilla's own HUD/hotbar rendering, which is a separate code path entirely. Team HUD and the
   feedback icon overlay are unaffected.
3. **Fixed TACZ's (and TaczPresence's) own gun HUD staying permanently suppressed even when this
   mod's widget wasn't drawing anything to replace it** — riding an SBW vehicle, Spectator mode, or
   simply not carrying a single gun in the hotbar. Root cause: the suppression toggle
   (`RenderConfig.GUN_HUD_ENABLE`) was only ever reasserted OFF while actively rendering, never
   restored when rendering stopped. Added `restoreVanillaHudIfIdle()`, called from every early-return
   path where the widget genuinely has nothing to draw, so TACZ's own HUD comes back the instant this
   mod isn't covering that space — only while ModernHud is the actively-preferred HUD source, so it
   never fights the TACZ_HUD/TACZ_P_HUD selector if the player picked one of those instead.
4. **Overheat display, configurable (`heatDisplayMode`, `modernhud-weapon.toml`, default `NONE`)** —
   for guns using TACZ's own opt-in heat mechanic (e.g. the Minigun; most guns don't have this at
   all, and no mode shows anything for them). TACZ gates its icon, ammo readout, AND heat bar behind
   the SAME single `GUN_HUD_ENABLE` toggle (confirmed against TACZ's own source), so disabling TACZ's
   built-in HUD to avoid overlap with this widget took the heat display away with it — there's no way
   to keep TACZ's own native heat element specifically while still using this widget for everything
   else; switching `hudSource` to `TACZ_HUD`/`TACZ_P_HUD` hands back the WHOLE HUD instead.
   - `NONE` — off, the default.
   - `BAR` — a thin bar under the fire-mode row, filled from the RIGHT edge growing LEFT (new
     `BarRender#barRTL`) and shorter than a full-width bar (55%), rather than mimicking the
     mag/reserve ammo readout's own left-to-right full-width look.
   - `FLASH` — no bar at all: the primary weapon icon box itself glows, fading from `heatFlashColor`
     (cold, yellow) to `heatBarLockedColor` (hot/locked, red) as heat rises, pulsing faster the
     hotter the gun gets and settling into a fast steady pulse once fully overheat-locked.
   Reads TACZ's public `IGun#getHeatAmount`/`#isOverheatLocked` and `GunData#hasHeatData` API
   directly (no reflection — stable public API), normalised against each gun's own configured
   `heatMax` rather than assuming the default of 100.
5. **Team HUD: a dead or Spectator-mode teammate now stays visible with an obvious "down" state**
   instead of either a confusing empty row or vanishing from the list — their health/Absorption bars
   glide to empty and the whole row pulses with a red glow (new `teamHudDownGlowColor` config,
   default matches the mod's existing alert-red), the same "something's wrong here" visual language
   as the low-ammo/low-health pulse elsewhere in the mod.
6. **Team HUD rows now render smaller than the personal widget by default**, for easier at-a-glance
   distinction between your own vitals and a teammate's. New `teamHudScaleFactor` config
   (`modernhud-hud.toml`, default `0.9` = 10% smaller), multiplied on top of the existing `barScale`.
7. **Your OWN name in the personal widget now follows your vanilla scoreboard team colour too**,
   matching what the Team HUD already did for teammates (`teamHudUseTeamColor`). Previously hardcoded
   plain white. New `useTeamColor` config under the personal-widget section (`modernhud-hud.toml`,
   default on), falling back to white when not on a team or the team has no colour set.
8. `README.md` rewritten to match the mod's actual current feature set — dropped stale content from
   an old Compass-bar feature that no longer exists in the codebase. Mod description in `mods.toml`
   shortened.

## v3.2.2 — fix bug thật: attachment dots hiện sai (getAttachmentId không bao giờ null), dots canh
## phải thay vì canh giữa, giảm border outset, đổi icon ammo-type sang texture full-auto có sẵn
1. **Fix bug attachment dots hiện cả khi súng KHÔNG có phụ kiện nào — tìm ra root cause thật qua
   TACZ source, không phải đoán**: `IGun#getAttachmentId()` được đánh dấu `@Nonnull` trong chính API
   của TACZ — nó KHÔNG BAO GIỜ trả về `null`, kể cả khi slot trống! Khi trống, nó trả về sentinel
   `DefaultAssets.EMPTY_ATTACHMENT_ID` (xác nhận trực tiếp trong source TACZ, nơi mọi chỗ TACZ tự
   check "slot có trống không" đều dùng đúng so sánh này, không phải `!= null`). Code cũ check
   `!= null` nên luôn đúng bất kể có lắp gì hay không → mọi slot súng CHO PHÉP đều hiện vàng dù súng
   trần trụi hoàn toàn. Đã sửa dùng đúng `DefaultAssets.isEmptyAttachmentId(...)` (cũng là API public
   ổn định của TACZ, `com.tacz.guns.api.DefaultAssets`).
2. **Attachment dots canh phải (top-right) thay vì canh giữa** trong box icon chính, đúng yêu cầu.
3. **Giảm border outset mặc định 2→1px** (cả 2 file config) — 2px kết hợp với padding sẵn có khiến
   bracket nhìn "hơi xa" khỏi rìa box như bạn báo, dù kỹ thuật fill/border đã khớp nhau từ bản trước.
4. **Icon "ammo type" đổi từ 3 vạch vẽ tay sang dùng luôn texture full-auto có sẵn**
   (`fire_mode_auto.png`), tint theo màu reserve — nhìn đẹp/nhất quán hơn hẳn 3 gạch thô sơ trước đó.

## v3.2.1 — fix 4 lỗi thật từ ảnh in-game bạn gửi: glow mismatch border, fire-mode box sai kiểu viền,
## attachment dots lạc ra ngoài box, tắt glow default vì kỹ thuật hiện tại quá thô
1. **Glow không match viền/box — đã bỏ hẳn glow ở dòng ammo, quay lại flat fill dùng CHUNG kích thước
   với border**: nguyên nhân gốc là glow (`GlowEffect`) lan ra rộng hơn box thật (dùng để tính vị trí
   bracket), nên viền và "vùng sáng" không bao giờ khớp nhau — nhìn xấu/lệch như ảnh bạn gửi. Sửa:
   dòng ammo giờ dùng `HudPanel.draw()` (fill phẳng) với CHÍNH XÁC `boxX/boxY/boxW/boxH` mà border
   cũng dùng — 2 thứ giờ luôn khớp nhau tuyệt đối.
2. **Tắt TOÀN BỘ tính năng glow theo default** (`showWeaponGlow`, `showVitalsGlow`) — nhận ra vấn đề
   không chỉ ở dòng ammo mà là ở chính kỹ thuật `GlowEffect` (chỉ là vài rect chồng lệch vị trí +
   additive blend, không có blur thật) — nhìn "blocky/thô" ở BẤT KỲ đâu dùng nó, không riêng gì ammo.
   Giữ nguyên tính năng (glow khi đổi súng, pulse khi ít đạn/máu thấp) nhưng tắt default cho tới khi
   cải thiện kỹ thuật render tốt hơn — màu chữ vẫn đổi khi ít đạn/máu thấp như cũ nên không mất cảnh
   báo, chỉ là không còn glow phụ trợ.
3. **Fire-mode box bỏ hẳn 4-góc-bracket**: trước đây dùng chung `drawChipBox` nên bị áp luôn style
   viền chung (CORNERS mặc định = 4 góc bracket) — SAI, box này không nên có kiểu viền đó theo đúng
   yêu cầu. Giờ chỉ còn fill phẳng, không border ngoài nào cả — chỉ còn vạch chia giữa các ô + khung
   riêng cho ô đang active (đúng tinh thần "chế độ nào active thì đóng khung chế độ đó").
4. **Attachment dots chuyển hẳn vào TRONG shadowbox súng chính**: trước đây là 1 hàng riêng phía TRÊN
   box (cần chừa thêm chiều cao, trông "lạc" khỏi box khi glow/border không rõ) — giờ vẽ ĐÈ LÊN icon
   box, sát mép trên, bên trong ranh giới box thật. Chỉ hiện dot cho slot ĐÃ LẮP — slot trống hoặc
   súng không có loại đó thì không hiện gì cả (bỏ hẳn `attachmentDotEmptyColor`, không cần nữa). Revert
   `frameHeight` mặc định 68→60 vì không cần chừa hàng riêng nữa.

## v3.2.0 — tái hiện chính xác hơn theo ảnh MW4 tham chiếu: tách ammo/name/firemode thành 3 phần
## độc lập, corner bracket ra ngoài box, secondary box chấm góc dưới, dot phụ kiện trên icon chính
1. **Corner bracket ra NGOÀI shadowbox** (không còn nằm sát/trong cạnh box nữa): thêm tham số
   `outset` cho `HudPanel.drawCornerBrackets`/`drawBorder` — bracket giờ nổi cách mép box 1 khoảng
   (mặc định 2px), đúng như ảnh ref. Config: `borderOutset` (cả `modernhud-hud.toml` lẫn
   `modernhud-weapon.toml`).
2. **Tách ammo/name/fire-mode thành 3 phần ĐỘC LẬP** (sửa lại nhận định từ bản trước — nhìn kỹ ảnh ref
   lại thì chỉ CÓ RIÊNG dòng ammo mới có nền, không phải cả khối đóng khung):
   - **Dòng ammo** (`drawAmmoRow`): nền giờ là GLOW mềm (dùng `GlowEffect`) thay vì shadowbox phẳng,
     border outset riêng quanh đúng dòng này. Số 0 thừa (leading zero khi bật `zeroPadAmmo`) giờ mờ
     đi rõ rệt so với chữ số có nghĩa (`drawZeroPaddedNumber`) — áp dụng cho cả mag lẫn reserve.
     Thêm icon "3 vạch" (hamburger, vẽ bằng code không cần texture) cạnh reserve
     (`showAmmoTypeIcon`).
   - **Tên súng** (`drawNameRow`): chữ nổi trần, KHÔNG còn nền/box riêng, tách hẳn khỏi dòng ammo.
   - **Fire-mode** (`drawFireModeSegmented`): viết lại hoàn toàn thành segmented control — 1 box chia
     đều theo số chế độ, MỖI CHẾ ĐỘ 1 ô, chế độ đang active được đóng khung THÊM riêng bên trong (viền
     phụ quanh đúng ô đó), có vạch chia mảnh giữa các ô.
3. **Secondary weapon box**: đổi hẳn kiểu viền — bỏ 4 góc bracket, chỉ còn 2 chấm vuông nhỏ ở 2 GÓC
   DƯỚI, không có gì ở 2 góc trên (`HudPanel.drawBottomDots`, config `secondaryDotSize`).
4. **Dot phụ kiện trên icon súng chính** (tính năng mới hoàn toàn): thêm `TaczCompat.attachmentSlotCounts()`
   dùng đúng API public của TACZ (`IGun#allowAttachmentType` + `IGun#getAttachmentId`) — đếm chính
   xác gun này CÓ BAO NHIÊU slot phụ kiện (không phải lúc nào cũng đủ 6 loại: scope/muzzle/stock/grip/
   laser/extended-mag, tùy súng) và bao nhiêu đang lắp. Hiện thành hàng ô vuông nhỏ ngay trên icon
   chính, ô vàng = đã lắp, ô xám = trống (`showAttachmentDots`/`attachmentDotColor`/
   `attachmentDotEmptyColor`). Bump `frameHeight` mặc định 60→68px để chừa chỗ cho hàng dot này.
5. Đã tự rà soát và bắt được 1 lỗi cú pháp javadoc tự gây ra (thiếu dấu `{` mở trong comment) trước
   khi đóng gói — không phải chỉ trông cậy vào bạn build để bắt lỗi lần này.

## v3.1.0 — fix shadowbox "biến mất" (root cause thật), shadowbox riêng cho ammo/name/firemode, gold
## border flash + toggle bật/tắt slide animation
1. **Fix "shadowbox không compat với 1 số mod tối ưu, biến mất luôn" — tìm ra root cause thật qua
   code audit, không phải đoán**: vòng lặp vẽ icon fire-mode set `RenderSystem.setShaderColor()` làm
   mờ icon KHÔNG active, rồi gọi `g.blit()` — nếu texture đó lỗi/thiếu (conflict resource pack, hay
   bất kỳ lý do gì khiến blit throw exception) thì dòng RESTORE alpha về bình thường ngay sau đó bị
   BỎ QUA hoàn toàn vì không có try/finally. Vanilla shader gui NHÂN mọi màu fill với giá trị này —
   nên 1 lần lỗi ở icon fire-mode có thể làm MỌI shadowbox khác (kể cả không liên quan) render mờ đi
   hoặc biến mất hẳn cho tới hết frame đó. Tìm thêm 1 chỗ tương tự ở `TaczCompat.renderHudTexture`
   (còn nặng hơn — `pose.popPose()` cũng bị bỏ qua nếu blit throw, làm lệch pose stack toàn bộ phần
   còn lại của frame). Đã bọc try/finally đảm bảo restore 100% ở cả 2 chỗ trên + `drawCarriedIcon` +
   `FeedbackIconOverlay` — dù lỗi ở bất kỳ đâu, state global không bao giờ bị kẹt nữa.
2. **Lớp phòng thủ thứ 2 trong `HudPanel.java`**: MỌI lệnh fill/border giờ tự ép `shaderColor` về
   trắng đục ngay trước khi vẽ rồi khôi phục lại sau (try/finally) — không còn tin tưởng mù quáng vào
   GL state có sẵn từ bất kỳ đâu (mod khác, hay chính ModernHUD). Đây là fix triệt để nhất có thể làm
   mà không cần biết chính xác tên mod tối ưu bạn dùng — nếu vẫn còn thấy vấn đề, cho mình biết tên mod
   cụ thể để kiểm tra sâu hơn.
3. **Shadowbox riêng cho khối ammo+name+fire-mode**: tách hẳn khỏi màu/opacity của icon box (vẫn tối
   như cũ). Mặc định mới: trắng-xám nhạt (`0xE8ECF0`) ở opacity thấp (14%) — giống phong cách Modern
   Warfare 2026, thay vì nền tối như icon box. Toggle riêng `showTextPanel` — tắt hẳn cũng được nếu
   muốn hoàn toàn không có shadowbox ở khối này, chỉ còn viền (nếu bật). Config:
   `showTextPanel`/`textPanelColor`/`textPanelOpacity` (`modernhud-weapon.toml`).
4. **Animation khi đổi súng — làm lại theo hướng bạn đề xuất**: slide animation giờ có toggle bật/tắt
   riêng (`showSwitchSlide`, mặc định vẫn bật) + giảm khoảng cách trượt mặc định 16→8px (gọn hơn, bớt
   "lỏ"). Thêm **hiệu ứng nháy viền vàng gold** kiểu vanilla Minecraft (giống flash màn hình totem of
   undying) quanh icon chính khi đổi súng — nháy vàng rồi mờ dần về màu viền bình thường, ĐỘC LẬP với
   slide (chạy cả 2, chỉ 1, hoặc tắt hết tùy ý). Config: `showSwitchBorderFlash`/
   `switchBorderFlashColor`/`switchBorderFlashMs`/`switchSlideDistance`.

## v3.0.0 — bắt đầu "animation/rendering overhaul": eased animator thời gian thực + glow effect,
## áp dụng cho weapon hud và health bar trước (theo đúng yêu cầu bắt đầu từ đâu)
1. **Đã học kỹ thuật từ source `gd656killicon` bạn gửi** (mod kill-feed/combat-feedback rất chi
   tiết) — không copy code (kiến trúc 2 mod hoàn toàn khác nhau: bên đó config-driven qua JSON cho
   từng element, ModernHUD thì hard-code từng widget), mà học 2 kỹ thuật cốt lõi rồi viết lại hoàn
   toàn theo phong cách riêng của ModernHUD:
   - **`EasedAnimator.java`** (mới) — animator dựa trên THỜI GIAN THỰC (duration cố định tính bằng
     ms + easing curve: LINEAR/CUBIC_OUT/QUINTIC_OUT), thay cho kiểu "exponential decay theo từng
     frame" cũ (`BarRender.smooth()`) vốn phụ thuộc framerate và không bao giờ "đến đích" thật sự
     rõ ràng. Animator mới: retarget giữa chừng thì tiếp tục mượt từ giá trị hiện tại (không giật/
     restart), y hệt tinh thần "update target không phá animation" của `DigitalScrollEffect` bên
     mod tham chiếu.
   - **`GlowEffect.java`** (mới) — hiệu ứng glow/bloom giá rẻ: vẽ nhiều bản sao lệch vị trí của cùng
     1 rect với additive blend + alpha giảm dần ra ngoài, học từ `IconGlowRenderEffect` bên mod tham
     chiếu nhưng viết lại cho rect phẳng (health bar, ammo box) thay vì sprite icon.
2. **Health bar**: đổi từ smoothing cũ sang `EasedAnimator` (280ms, quintic ease-out) — cảm giác
   "premium" hơn hẳn, mượt đều bất kể framerate. Thêm **glow rung khi bị dính đòn** (flash đỏ ngắn,
   falloff bình phương cho cảm giác "hit" rõ) và **glow thở chậm khi máu thấp** (sine pulse, cùng
   công thức pulse đã dùng cho ammo alert). Toggle + màu chỉnh được qua `showVitalsGlow`/
   `vitalsGlowColor` (`modernhud-hud.toml`).
3. **Weapon hud**: animation "push up" đổi từ ease-out-quad sang **quintic ease-out** (khớp với
   `EasedAnimator`, cảm giác "chốt vào vị trí" gọn hơn). Thêm **glow flash khi đổi súng** (bùng sáng
   ngắn phía sau icon chính, kéo dài hơn animation trượt 1 chút để icon "lắng" lại rõ ràng) và
   **glow thở khi ít đạn** (pulse quanh cả khối ammo+name+firemode, dùng màu `ammoLowColor`). Toggle
   + màu chỉnh được qua `showWeaponGlow`/`weaponGlowColor` (`modernhud-weapon.toml`).
4. **Team HUD**: đồng bộ luôn — thanh máu đồng đội giờ cũng dùng `EasedAnimator` (mỗi người 1
   instance riêng qua `Map<UUID, EasedAnimator>`) thay vì smoothing cũ, nhất quán với health bar
   của bản thân.
5. **Còn tiếp tục ở các bản sau**: đây mới là bước đầu của "v3.0 overhaul" — animation/rendering
   cho player hud (head/name/absorption) và các phần còn lại của weapon hud (secondary box, fire
   type flash...) sẽ tiếp tục cải thiện nếu bạn xác nhận muốn đi tiếp theo hướng này.

## v2.10.1 — fix lỗi build TeamHudOverlay (Team vs PlayerTeam) + compat mới với Superb Warfare (SBW)
1. **Fix lỗi build thật từ log bạn gửi**: `Player#getTeam()` trả về `net.minecraft.world.scores.Team`
   (lớp cha), KHÔNG PHẢI `PlayerTeam` — `TeamHudOverlay.java` khai báo sai kiểu ở 2 chỗ gây lỗi
   compile y hệt log. Đã sửa cả 2 chỗ + import sang `Team`, giữ nguyên toàn bộ logic (Team vẫn có
   `getColor()`/`equals()` như đã dùng). Xin lỗi vì để lọt lỗi này — sandbox của mình không có mạng
   tới Forge/CurseForge nên không tự compile thử được, cảm ơn bạn đã build và gửi log lỗi thật.
2. **Compat mới: "[SBW] Superb Warfare"** (github.com/Mercurows/SuperbWarfare, modid `superbwarfare`)
   — đã tải source thật của mod để xác nhận: mọi vehicle (M1A2, Ztz99a, Truck, trực thăng, tàu, pháo...)
   đều kế thừa (trực tiếp hoặc qua base class trung gian như `GeoVehicleEntity`/`ArtilleryEntity`) từ
   1 class gốc duy nhất `com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity` — nên chỉ cần
   1 check `instanceof` là bắt được TẤT CẢ loại xe. Thêm `SuperbWarfareCompat.java` (soft-dependency
   qua reflection, không cần thêm Gradle dependency, giống cách đã làm với TaczPresence).
3. **Toàn bộ HUD của ModernHUD tự ẩn khi đang ngồi trên vehicle SBW** (mọi ghế — lái, xạ thủ, hành
   khách — không chỉ ghế lái, vì HUD của SBW che kín cùng 1 vùng màn hình dù ngồi ghế nào): player hud
   (máu/tên/armor), weapon hud (kể cả chế độ "neutral" khi mang súng không cầm), team hud, và ammo
   alert (RELOAD/NO AMMO/LOW AMMO) — cả 4 overlay đều check `SuperbWarfareCompat.isInVehicle()` ngay
   đầu `render()`. Không cần patch gì thêm nếu không cài SBW — check này trả về `false` ngay lập tức.

## v2.10.0 — Team HUD mới: thanh máu đồng đội theo vanilla scoreboard team, style Warzone
1. **Tính năng mới hoàn toàn**: `TeamHudOverlay.java` — mỗi đồng đội (cùng team scoreboard vanilla,
   `/team join`) hiện 1 dòng gọn: head icon + tên (màu theo màu team vanilla, có fallback riêng) +
   thanh máu mượt (dùng chung cơ chế smoothing `BarRender.smooth()` như HUD của bản thân, nhưng mỗi
   người 1 trạng thái riêng qua `Map<UUID, Float>` — không bị trộn lẫn animation giữa các đồng đội).
   Xếp CHỒNG LÊN TRÊN player hud của mình, chung 1 box nền + viền RIÊNG (tách biệt hoàn toàn khỏi
   panel của player hud chính) — đúng tinh thần "1 box riêng" bạn yêu cầu.
2. **Giới hạn 6 người, sắp xếp ổn định**: `teamHudMaxPlayers` (mặc định 6, chỉnh 1-16) — nhiều hơn thì
   phần dư bị ẩn, sắp xếp theo tên A-Z để việc "ai bị ẩn" luôn nhất quán giữa các frame thay vì đảo
   lộn ngẫu nhiên. Kick 1 người khỏi team hoặc có người rời/hạ số lượng xuống dưới giới hạn thì người
   bị ẩn trước đó sẽ tự hiện lại (không cần restart/reload gì).
3. **Giới hạn kỹ thuật cần biết rõ**: đây dùng đúng hệ thống team vanilla của Minecraft — client chỉ
   đọc được máu/tên của đồng đội khi entity của họ THỰC SỰ được load/track ở phía client (đúng phạm vi
   mà vanilla vốn đã hiện tên/máu người chơi khác). Đồng đội ở xa/khác dimension sẽ đơn giản là KHÔNG
   hiện cho tới khi họ vào lại phạm vi — đây là giới hạn cố hữu của việc không có hệ thống đồng bộ dữ
   liệu phía server riêng, không phải bug.
4. **Config đầy đủ** (`modernhud-hud.toml`, mục `teamHud`): `enabled`, `maxPlayers`, `barWidth`
   (mặc định 64px), `rowGap`, `groupGap` (khoảng cách với player hud chính), `showHead`,
   `useTeamColor` (bật = màu tên theo team vanilla, tắt = dùng `nameColor` cố định), `nameColor`
   (hex, fallback khi team không có màu).
5. **Lưu ý về vị trí căn chỉnh**: team hud căn trái theo player hud chính — khớp hoàn hảo với default
   BOTTOM_LEFT, nhưng nếu bạn đổi player hud sang neo bên phải (TOP_RIGHT/BOTTOM_RIGHT) mà 2 box có độ
   rộng khác nhau thì cạnh phải sẽ không thẳng hàng — chấp nhận đánh đổi này để giữ bản đầu tiên đơn
   giản, có thể cải thiện sau nếu cần.

## v2.9.1 — fix Fire Type flash (X/Y offset, kiểu hiển thị), reserve màu trắng-xám, bỏ drag player hud
1. **Fire Type flash — hoàn thiện phần config đã thêm dở ở bản trước**: bản 2.9.0 đã thêm config
   `fireTypeXOffset`/`fireTypeShowLabel` nhưng code vẽ (`drawFireTypeFlash`) quên chưa dùng tới — giờ
   đã nối đúng: có X offset thật sự, Y offset đổi hướng dấu giống hệt `ammoAlertYOffset` (dương =
   xuống dưới, ÂM ĐƯỢC PHÉP — trước đây range chỉ 0-400 nên không kéo xuống được, giờ -2000 đến 2000),
   default Y đổi từ +46 (đẩy lên) thành -40 theo quy ước dấu mới, scale mặc định giảm 1.3→1.0 (bớt "to
   đùng"). Thêm toggle `fireTypeShowLabel`: bật = "FIRE TYPE: FULL AUTO" như hiện tại, tắt = chỉ
   "FULL AUTO" không có tiền tố.
2. **Reserve ammo color**: đổi default từ vàng gold sang trắng-xám (`0xB8BFC6`) theo đúng yêu cầu.
   Đã rà soát kỹ lại TOÀN BỘ hệ màu (cả `HudConfig.java` lẫn `WeaponConfig.java`) — không tìm thấy bug
   nào trong code, `reserveColor` đã luôn được wire đúng và config được từ trước. Nguyên nhân thật của
   cảm giác "không config được": màn hình kéo-thả trong game (`HudEditorScreen`, phím tắt Controls >
   Modern HUD) CHỈ chỉnh vị trí, không hề có màu — màu chỉ nằm trong file `modernhud-weapon.toml` /
   `modernhud-hud.toml` (mục `colors`), cần sửa trực tiếp trong đó hoặc qua mod xem config dạng GUI
   nếu có cài (vd Configured). Đã làm rõ hơn trong tooltip màn hình editor.
3. **Player HUD: bỏ hẳn kéo-thả, cố định theo config**: xóa handle "Player HUD" khỏi
   `HudEditorScreen` — giờ CHỈ còn Hit/Break Icon kéo-thả được. Player HUD giờ đối xử y hệt weapon
   hud: neo theo góc + offset trong file config (`modernhud-hud.toml`, mục `position`), không còn kéo
   bằng chuột được nữa. Default sẵn đã là `BOTTOM_LEFT` từ trước (không đổi). Xóa luôn hàm
   `HudConfig.setMainPosition()` (dead code sau khi bỏ drag handle).

**Còn nợ lại (chưa làm trong bản này):** tính năng Team HUD (hiển thị thanh máu đồng đội theo vanilla
scoreboard team, xếp chồng lên trên player hud, tối đa 6 người, style Warzone) — đây là tính năng MỚI
khá lớn (cần đọc scoreboard team, track nhiều entity cùng lúc, smoothing riêng từng người...), chưa bắt
đầu code do ưu tiên fix các lỗi cấp bách trước. Sẽ làm ở bản tiếp theo nếu bạn xác nhận vẫn cần.

## v2.9.0 — gộp ammo+name+fire-mode thành 1 khối duy nhất đúng ảnh ref, màu HEX, fire type flash mới
1. **Hệ màu → HEX string thay vì số thập phân**: toàn bộ 16+ config màu (panel, border, ammo, reserve,
   name, fire mode, alert...) giờ nhập bằng mã hex (vd `"E8B84C"` hoặc `"#E8B84C"`) thay vì số thập
   phân khó đọc (`15263308`). File mới `ColorConfig.java` xử lý parse/format (chấp nhận có/không dấu
   `#` hoặc `0x`, sai định dạng thì fallback về màu mặc định, không crash).
2. **Reserve ammo**: thêm lại dấu `|` phân cách (đã bỏ nhầm ở bản trước), reserve giờ nhỏ hơn mag
   theo tỷ lệ chỉnh được (`reserveScaleOffset`, mặc định 0.1) và mờ hơn theo opacity riêng
   (`reserveOpacity`, mặc định 80%) — đồng thời fix luôn lỗi số reserve bị lệch tâm so với mag khi 2
   chữ ở 2 cỡ khác nhau (căn giữa theo chiều cao dòng của mag, không còn "không đều" nữa).
3. **Bố cục lại đúng ảnh MW4 tham chiếu — thay đổi lớn nhất bản này**: gộp mag|reserve + tên súng +
   fire-mode thành MỘT khối duy nhất, chung MỘT nền + MỘT viền bao quanh cả 3 dòng (trước đây là 3 box
   riêng biệt, không khớp ảnh gốc). Tên súng giờ luôn hiển thị (không còn fade/toast tạm thời như
   trước), nằm ngay dưới dòng ammo, fire-mode nằm dưới cùng — đúng thứ tự trong ảnh. Xóa hẳn hệ thống
   "name toast" cũ (`showNameToast`→`showName`, xóa `nameHoldMs`/`nameFadeMs` vì không còn ý nghĩa).
4. **Secondary weapon box đúng ảnh ref**: box giờ RỘNG BẰNG box súng chính (trước đây nhỏ hơn, lệch),
   icon súng lệch về bên phải/trong của box, số đạn reserve chiếm khoảng trống bên trái — 1 box chứa
   cả 2 thứ như trong ảnh (pistol icon + "40"), không phải icon riêng + số ở chỗ khác. Tighten spacing:
   giảm khoảng cách giữa icon chính/phụ (4→2px) và giữa các dòng trong khối ammo (3→2px).
5. **Thêm "FIRE TYPE: FULL AUTO" flash announcement** khi rút súng (yêu cầu #7): chữ hiện giữa màn
   hình, phía trên tâm ngắm, nháy màu vàng → trắng → mờ dần hết hẳn, gạch chân riêng phần tên chế độ
   bắn (không gạch "FIRE TYPE: "). Timing/màu sắc đều chỉnh được qua config (`fireTypeFlashMs`,
   `fireTypeHoldMs`, `fireTypeFadeMs`, `fireTypeFlashColor`, `fireTypeHoldColor`, `fireTypeScale`,
   `fireTypeYOffset`). Dùng chung timer với animation trượt icon nên chỉ chạy đúng 1 lần mỗi lần đổi
   súng, không lặp lại liên tục.
6. Bump `frameWidth` mặc định 148→160px để chừa đủ chỗ cho khối ammo+name+firemode rộng hơn (do gộp
   3 dòng vào 1 box thay vì chia nhỏ như trước).
7. **playerhud bottom-left**: đã xác nhận default sẵn là `BOTTOM_LEFT` từ trước, không cần sửa gì.

## v2.8.4 — corner bracket nhỏ lại + tỷ lệ theo box, thêm 3 kiểu viền chọn được, gộp ammo+reserve
## thành 1 ô đúng ảnh ref, thêm reserve ammo cho súng phụ/neutral
1. **Corner bracket "to quá, vướng mắt" → đã fix tận gốc**: trước đây `cornerBracketLength` là số px
   CỐ ĐỊNH áp dụng như nhau cho MỌI box, kể cả các chip nhỏ (mag/reserve/fire-mode) — nên cùng 1 độ
   dài trông ổn trên icon box to (62x36) nhưng lại "chunky"/vướng mắt trên chip nhỏ. Đã sửa: thêm giới
   hạn tỷ lệ `min(w,h) * 0.3` trong `HudPanel.drawCornerBrackets` — cùng 1 config length giờ tự động
   nhỏ lại trên box nhỏ, đạt full size trên box to. Ngoài ra giảm luôn default: weapon 3→2px, hud
   máu 4→3px.
2. **Thêm 3 kiểu hiển thị viền chọn được** qua config mới `borderStyle` (enum `NONE`/`CORNERS`/`FULL`),
   thay cho toggle bật/tắt cũ:
   - `NONE` — không viền gì cả, chỉ còn nền phẳng (nếu panel cũng đang bật)
   - `CORNERS` — brackets góc như hiện tại (mặc định, giữ nguyên hành vi cũ)
   - `FULL` — viền liền mỏng 1px bao trọn 4 cạnh (hàm mới `HudPanel.drawFullBorder`)
   Áp dụng đồng bộ cho cả HUD máu (`HudConfig.borderStyle`) lẫn HUD súng (`WeaponConfig.borderStyle`)
   qua 1 hàm dispatch chung `HudPanel.drawBorder()`. Đổi tên toàn bộ config liên quan từ
   `cornerBracket*`/`showCornerBrackets` sang `border*` cho đúng ý nghĩa mới (không còn chỉ là corner
   bracket nữa).
3. **Gộp mag + reserve thành 1 ô duy nhất, đúng bố cục ảnh ref** ("030 060"): trước đây là 2 chip xếp
   chồng (mag trên, reserve dưới), giờ nằm CHUNG 1 box, cạnh nhau theo chiều ngang, cùng cỡ chữ, chỉ
   khác màu (mag trắng/đỏ khi ít đạn, reserve vàng gold) để phân biệt — bỏ luôn ký tự "|" phân cách,
   chỉ còn khoảng trắng giữa 2 số như trong ảnh. Fire-mode row vẫn nằm ngay dưới ô này như cũ.
4. **Thêm reserve ammo count cho súng phụ (secondary) VÀ chế độ neutral (mang súng không cầm)**: gộp
   2 hàm vẽ icon trùng lặp (`drawSecondaryWeapon`/`drawNeutralIcon`) thành 1 helper dùng chung
   `drawCarriedIcon()` — vừa vẽ icon vừa vẽ số đạn reserve (không zero-pad) ngay trong CÙNG box với
   icon, đúng như ảnh ref (pistol icon + "40"). Cả secondary lẫn 2 khẩu ở trạng thái neutral giờ đều
   hiện số đạn dự trữ như nhau. Bump kích thước box phụ 40x16 → 44x18 để đủ chỗ cho cả icon lẫn số.

## v2.8.3 — làm lại giao diện weapon hud theo hướng "MW4-style": box riêng từng element, corner
## brackets, reserve gold, chế độ "đang mang súng nhưng không cầm"
1. **Reserve ammo → màu gold**: đổi màu mặc định của số đạn dự trữ (dòng dưới, nhỏ hơn số đạn trong
   băng) sang vàng gold (`0xE8B84C`), tách biệt rõ với số đạn chính màu trắng phía trên.
2. **Mỗi element trên weapon hud giờ có box riêng, KHÔNG còn dồn chung 1 cục**: tách hẳn số đạn trong
   băng, số đạn dự trữ, và hàng fire-mode thành 3 "chip" riêng biệt — mỗi cái tự đo kích thước ôm sát
   nội dung của nó (`CHIP_PAD_X`/`CHIP_PAD_Y`), có khoảng hở (`CHIP_GAP_Y`) giữa các chip để trông
   tách bạch, không dính liền thành 1 khối. Icon súng chính + súng phụ vẫn có box riêng như cũ. Chỉ
   riêng tên súng (name toast) là KHÔNG có box — vẫn là chữ nổi trần như trước, đúng yêu cầu.
3. **Thêm 4 góc bracket nhỏ kiểu MW4 (2026)** — dựa đúng ảnh HUD MW4 bạn gửi: 2 đoạn ngắn mỗi góc tạo
   hình chữ "L" mở, KHÔNG phải viền liền tục bao quanh cả box. Bật/tắt riêng qua config
   `showCornerBrackets` (mặc định BẬT), chỉ vẽ khi panel/fade box cũng đang bật (brackets là lớp phủ
   THÊM lên trên fill, không phải thay thế). Áp dụng cho MỌI chip (icon chính/phụ, số đạn, reserve,
   fire-mode) ở cả HUD máu lẫn HUD súng — dùng chung 1 hàm `HudPanel.drawCornerBrackets()` nên đồng bộ
   khắp nơi. Có thêm config riêng cho màu (`cornerBracketColor`, mặc định trắng ngà `0xE8ECF0`), độ
   đậm (`cornerBracketOpacity`, mặc định 90% — đậm hơn hẳn fill để luôn rõ nét) và độ dài cạnh
   (`cornerBracketLength`, mặc định 3-4px, dày cố định 1px).
4. **Chế độ "đang mang súng nhưng không cầm trên tay"**: trước đây cầm bất kỳ item nào khác ngoài
   súng TACZ là widget biến mất hoàn toàn. Giờ nếu vẫn còn súng trong hotbar (dù không cầm), widget
   vẫn hiện — nhưng KHÔNG có tên súng/số đạn (không có gì để đọc vì không đang cầm khẩu nào cả). Hiện
   tối đa 2 khẩu súng tìm thấy trong hotbar, xếp theo thứ tự slot (khẩu ở slot nhỏ hơn hiện trước), CẢ
   HAI đều dùng chung kiểu thu nhỏ/mờ như ô "secondary" hiện tại — không khẩu nào được highlight làm
   "chính". Lịch sử chọn súng (`currentGunId`/`secondaryGunId`) dùng khi đang thực sự cầm súng được
   giữ nguyên không đụng tới, nên cầm súng thật trở lại thì primary/secondary tiếp tục đúng như trước.
5. **Về yêu cầu bố cục "modern/sleek như MW4"**: mình không tìm được ảnh/tư liệu đáng tin cậy nào về
   giao diện HUD thật của game "Modern Warfare 4" qua tìm kiếm (có thể game chưa phát hành công khai
   hoặc tên khác) — nên phần layout tổng thể (box riêng từng phần tử, khoảng cách gọn, phối màu
   vàng-trắng) mình tự thiết kế theo hướng "tactical HUD hiện đại" nói chung. Bracket góc ở mục 3 thì
   dựa ĐÚNG theo ảnh bạn gửi (không phải suy đoán).

## v2.8.2 — panel width bị dư khúc thừa, armor bar → text-only, tự nhận diện súng thứ 2
1. **Fix "healthbar background box đang bị dư ra một khúc"**: nguyên nhân là `totalWidth` cũ tính
   bằng công thức cố định `barWidth + 44` — con số `44` là chỗ dành sẵn "phòng khi" có chữ HP/Armor
   nằm cuối thanh (END_OF_BAR), NHƯNG được cộng vào bất kể có thật sự vẽ chữ ở đó hay không. Nếu bạn
   để HP/Armor text ở vị trí khác (hoặc tắt hẳn), 44px đó vẫn bị chừa ra trong panel → thành 1 khúc
   nền tô màu (giờ là màu phẳng, không còn fade để "giấu" nó như trước) mà không có gì bên trong —
   đúng là "một khúc nhìn rất xấu". Đã sửa tận gốc: `layoutPass()` giờ tự đo width thật cần dùng ở
   TỪNG dòng (bar, chữ cuối thanh, dòng thông tin chung, chữ "+N" khiên dư) và lấy max — panel giờ
   luôn ôm sát đúng nội dung thực tế đang vẽ, không còn khoảng dư nào cả.
2. **Bỏ hẳn armor bar, chuyển thành text-only** (vd "20/20"): không còn thanh armor (rectangle) nữa,
   chỉ còn 1 dòng chữ — dùng đúng định dạng `armorTextFormat` sẵn có (CLASSIC "12/20" / PERCENT
   "60%"). Gộp `showArmorBar` + `showArmorText` cũ thành 1 toggle duy nhất `showArmor` (bật là có
   chữ, tắt là ẩn hẳn — không còn khái niệm "bar bật, chữ tắt" nữa vì chữ giờ LÀ toàn bộ armor). Vị
   trí `END_OF_BAR`/`BELOW_BAR` (không còn ý nghĩa "cuối thanh" vì hết thanh) giờ đều nghĩa là "dòng
   riêng ngay dưới thanh máu"; `BELOW_NAME_HEAD` vẫn như cũ, nằm chung dòng với HP text phía trên.
3. **Tự động nhận diện súng thứ 2 (primary + secondary) thay vì phải lăn chuột qua lại**: trước đây
   ô súng phụ CHỈ được ghi nhận sau khi bạn đã từng đổi từ súng đó sang súng khác (dựa lịch sử scroll)
   — nếu bạn cầm sẵn 2 súng từ đầu mà chưa từng đổi qua lại, ô phụ để trống dù đang cầm đủ 2 súng. Đã
   sửa: nếu chưa có "lịch sử" (hoặc súng nhớ trong lịch sử không còn trong hotbar nữa), tự động quét
   hotbar tìm khẩu súng KHÁC đang có sẵn và hiện luôn — không cần lăn chuột qua lại trước nữa.
4. Xác nhận lại: healthbar background panel đã dùng opacity phẳng (không fade) từ bản v2.8.1 — bản
   này chỉ còn sửa nốt phần kích thước bị dư khúc ở trên, không có gradient/fade nào còn sót lại
   trong code (đã rà lại toàn bộ `HudPanel`/`PlayerHudOverlay`/`BarRender`, không còn `fillGradient`).

## v2.8.1 — Fix ammo "000" cho súng inv+dummy ammo, HUD đè nhau, animation lem sang text, fade box
1. **Fix ammo hiện "000" cho súng dùng inv ammo (M134 Minigun)**: nguyên nhân thật — `magAmmo()` bị
   mất nhánh check `useDummyAmmo` (có ở v2.7.0 nhưng bị rớt mất lúc build tiếp lên 2.8.0). Đối chiếu
   ngược với source thật của TACZ (`GunHudOverlay.handleCacheCount`): một khẩu có thể VỪA
   `useInventoryAmmo` VỪA `useDummyAmmo` cùng lúc — đạn là số ảo lưu trong NBT (đúng kiểu súng
   minigun gắn xe của bạn: đạn nạp từ kho xe, không phải item thật trong túi đồ người chơi), không hề
   có item đạn thật nào trong inventory để quét. Số "158" màu cyan trong ảnh bạn gửi chính là mã màu
   riêng TACZ dùng cho đúng combo `useInventoryAmmo && useDummyAmmo` này — bằng chứng trực tiếp. Code
   cũ luôn tự quét `player.getInventory()` bất kể `useDummyAmmo`, thấy 0 đạn thật → ra "000". Đã sửa:
   check `useDummyAmmo` TRƯỚC, dùng `getDummyAmmoAmount()` khi đúng — đúng thứ tự ưu tiên TACZ dùng.
2. **Fix "HUD đôi lúc bị đè lên nhau"**: trước chỉ gọi `setVanillaGunHudEnabled(false)` MỘT LẦN lúc
   load config — nếu có gì khác (TACZ tự reload config khi đổi gunpack, addon khác đụng chung cờ
   `RenderConfig.GUN_HUD_ENABLE`, v.v.) bật lại cờ đó, ModernHUD không hề hay biết, HUD gốc TACZ hiện
   trở lại đè lên HUD của mình. Đã sửa: `WeaponHudOverlay` giờ tự re-assert lại cờ này mỗi ~1 giây khi
   đang active, HUD TACZ gốc không thể "giành lại" màn hình quá 1 giây. Thêm cả fallback reflection
   (set field trực tiếp nếu nó là boolean thường thay vì bọc trong ForgeConfigSpec ConfigValue).
3. **Fix animation "push up" lem sang cả ammo/text**: đúng như bạn báo — trước đó CẢ pose (icon, số
   đạn, reserve, fire-mode row, tên súng) bị translate theo animation cùng lúc. Giờ tách riêng: chỉ
   icon súng chính + icon phụ nằm trong pose có animation trượt, còn cột số đạn/reserve/fire-mode/tên
   súng nằm trong pose KHÔNG animation, đứng yên tuyệt đối khi đổi súng.
4. **Bỏ hẳn "fade box" (gradient), thay bằng nền phẳng chỉnh opacity 0-100%**: đúng yêu cầu — không
   còn fade/gradient hướng nào nữa. `HudPanel` giờ chỉ có 1 hàm `draw()` tô 1 màu phẳng ở opacity chỉ
   định. Thêm config mới `panelColor` (0xRRGGBB) + `panelOpacity` (0-100) cho cả HUD máu
   (`modernhud-hud.toml`, mặc định 45%) và HUD súng (`modernhud-weapon.toml`, mặc định 30% — vẫn nhạt
   hơn HUD máu theo đúng tinh thần cũ). Ô súng phụ (secondary) còn nhân thêm `secondaryAlpha` vào
   opacity nền để mờ đồng bộ với icon mờ của nó.

## v2.8.0 — Fix hướng fade health bar, tích hợp 3-way HUD selector với TaczPresence
1. **Fix fade health bar bị ngược**: đúng như bạn báo — hướng fade bị lộn ngược so với ý định (đậm
   bên trái/tên, nhạt dần sang phải mới đúng, code cũ ra ngược lại). Đã đảo lại 2 màu trong
   `HudPanel.drawHorizontal`.
2. **Tích hợp 3-way HUD selector với TaczPresence**: TaczPresence có sẵn `PresenceConfig.GUN_HUD_TYPE`
   (enum `TACZ_HUD`/`TACZ_P_HUD`) để chọn hiển thị HUD súng nào. Không thể tự thêm 1 giá trị vào enum
   của MOD KHÁC (đó là bytecode đã compile sẵn của họ), nên ModernHUD làm bên mình một enum tương ứng
   `WeaponHudSource { TACZ_HUD, TACZ_P_HUD, MODERNHUD }` (config mới, thay cho `enabled` boolean cũ) —
   chọn cái nào thì ModernHUD tự đảm bảo CHỈ đúng 1 HUD đó hiện ra: đảo `RenderConfig.GUN_HUD_ENABLE`
   của TACZ (tắt cả TACZ gốc lẫn TaczPresence — 2 cái đó dùng chung 1 cờ này để quyết định có tự vẽ
   hay không), VÀ nếu chọn TACZ_HUD/TACZ_P_HUD thì còn set luôn `PresenceConfig.GUN_HUD_TYPE` tương
   ứng (khi TaczPresence có cài) để đúng đích danh 1 trong 2 kiểu của họ hiện lên, không phải đoán.
   Toàn bộ đọc/ghi qua reflection (an toàn, không crash nếu TaczPresence đổi API hoặc không cài).
3. **Keybind giờ CYCLE qua đúng 3 lựa chọn** thay vì chỉ toggle on/off như trước: ModernHUD → TACZ
   gốc → TaczPresence (nếu có cài, bỏ qua nếu không) → quay lại ModernHUD.

## v2.7.0 — Fix bug ammo "000", luân phiên với TaczPresence, thiết kế lại fade box, cảnh báo đạn
1. **Fix bug ammo hiện "000" cho súng dùng inventory ammo**: nguyên nhân thật — code cũ tự so sánh
   `ammoId` của viên đạn với `.equals()` thay vì gọi đúng `IAmmo.isAmmoOfGun(gun, ammo)`, method
   compatibility thật sự mà TACZ dùng (không phải lúc nào cũng match kiểu so sánh id đơn giản). Đối
   chiếu lại với `AmmoCacheCounter` của TaczPresence để sửa đúng, đồng thời bỏ luôn việc tự ý giới
   hạn số hiện ra theo `magCapacity` cho nhóm súng này (chúng không có "mag" thật, số hiện ra phải
   đúng bằng tổng đạn rời trong túi, TaczPresence cũng không cắt bớt).
2. **Luân phiên (thay vì đè cứng) với TACZ / TaczPresence**: thêm keybind mới (chưa gán phím mặc
   định) để bật/tắt weapon widget của ModernHUD VÀ tự động đảo ngược `RenderConfig.GUN_HUD_ENABLE`
   của TACZ tương ứng — tạo hiệu ứng luân phiên thật giữa "HUD của ModernHUD" và "HUD TACZ đang hiện
   hành (dù là bản gốc hay của TaczPresence)", thay vì việc ModernHUD chỉ luôn đè cứng HUD kia off.
3. **Thiết kế lại hoàn toàn "fade box"**: bỏ hẳn viền góc bo màu xanh biển (corner accent brackets) —
   không còn viền/khung nào nữa, chỉ còn duy nhất phần fade. HUD máu: fade NGANG trái (nhạt) → phải
   (đậm), có 1 đường line trắng-xám RÕ dọc theo đáy. HUD súng: fade DỌC dưới (đậm, gần số đạn) → trên
   (nhạt), độ đậm nhạt hơn hẳn so với HUD máu — đúng theo ảnh tham khảo gửi kèm. `showPanel` vẫn tắt
   bật được như cũ cho cả 2 HUD (mặc định bật lại, vì giờ nó là fade thuần, không còn viền gây khó
   chịu nữa). Bỏ hẳn config `panelAccentColor` (không còn cần vì hết viền).
4. **Ô súng phụ (secondary) giờ có fade box đúng scale**: trước đó bị thiếu hoàn toàn phần nền cho
   ô phụ, giờ dùng đúng kiểu fade dọc của HUD súng, thu nhỏ theo đúng kích thước ô phụ.
5. **Bỏ ký tự "/" trên reserve ammo → đổi thành "|"** (áp dụng cả cho trường hợp đạn vô hạn "|∞").
6. **Animation "push up" khi đổi súng**: cả khung HUD súng giờ trượt lên từ dưới vào đúng vị trí
   trong ~220ms mỗi khi đổi súng (easing), dùng chung timer với toast tên súng.
7. **Cảnh báo RELOAD / NO AMMO / LOW AMMO kiểu TaczPresence**: overlay mới, chữ nhấp nháy (pulse)
   giữa màn hình khi băng đạn hết (RELOAD nếu còn đạn dự trữ, NO AMMO nếu hết sạch) hoặc dưới 20%
   (LOW AMMO, ngưỡng chỉnh được). Bỏ qua cho súng dùng inventory ammo (không có "mag" để cạn) —
   đúng như cách TaczPresence xử lý.

## v2.6.1 — Fix mất cân xứng weapon HUD, bỏ shadowbox mặc định, absorption full xanh biển, fire-mode icon
Fix trực tiếp theo ảnh chụp thực tế (M4 ADS) — soi kỹ thấy 2 lớp viền accent lồng nhau (khung ngoài +
khung icon) và số đạn dự trữ bị lệch hàng so với số đạn chính.
1. **Bỏ hẳn khung viền lồng nhau ("shadowbox")**: trước đó vẽ 2 lớp panel — 1 lớp bọc toàn bộ widget,
   1 lớp riêng bọc icon — chồng lên nhau nhìn rối và mất cân xứng. Giờ CHỈ còn tối đa 1 panel duy nhất,
   bọc riêng icon súng thôi (không có panel bọc số đạn/tên súng nữa, y hệt HUD gốc CoD — chữ chỉ có
   shadow, không có khung nền). Panel này giờ **tắt theo mặc định** (`showPanel: false` cho cả weapon
   HUD lẫn player HUD) — bật lại được qua config nếu muốn.
2. **Absorption mặc định full xanh biển đậm**: `absorptionColorMode` đổi mặc định từ `TIERED` sang
   `CUSTOM` — ra khỏi hộp là full 1 màu xanh biển đậm (`0x1C7ED6`) luôn, không cần chỉnh gì thêm.
   Xác nhận lại: màu xanh này CHỈ áp dụng cho thanh Absorption trên health bar, không liên quan gì
   đến viền khung panel (2 cái tách biệt hoàn toàn, panel giờ mặc định tắt nên càng rõ ràng).
3. **Fix lệch số đạn chính/dự trữ**: 2 số giờ luôn xếp chồng đúng 1 cột, canh phải cùng 1 mép — số
   chính ở trên, số dự trữ ngay dưới cách 1px, không còn kiểu nửa-cùng-hàng-nửa-lệch-hàng như trước.
4. **Weapon HUD cố định vị trí, chỉ chỉnh qua config**: bỏ hẳn "Weapon HUD" ra khỏi HUD Editor kéo-thả
   — vị trí giờ CHỈ chỉnh được qua `modernhud-weapon.toml` (đúng yêu cầu, vì kéo-thả bằng chuột không
   chính xác giữa các tỉ lệ màn hình khác nhau). HUD Editor có thêm dòng nhắc việc này khi TACZ có mặt.
5. **Tên súng chuyển lên TRÊN icon**: toast tên súng giờ hiện ngay phía trên khung icon (canh phải theo
   mép icon), không còn nằm phía dưới dễ đè lên các phần tử khác nữa.
6. **Fire mode giờ có ICON riêng (dùng icon bạn cung cấp)**: thêm 3 icon `fire_mode_semi/auto/burst.png`
   vào `textures/hud/`. Súng có NHIỀU chế độ bắn → hiện hàng icon, chế độ đang dùng sáng + có gạch chân,
   các chế độ khác mờ đi (y hệt TaczPresence). Súng chỉ có ĐÚNG 1 chế độ → không hiện gì cả, đúng yêu cầu.
7. **Fix bonus**: bộ 3 icon phụ (secondary weapon) giờ nằm hẳn trong 1 dải riêng NGAY DƯỚI khung icon
   chính — tách biệt hoàn toàn khỏi cột chữ (số đạn/tên/fire-mode), không còn khả năng đè lên "AUTO"
   hay bất kỳ chữ nào như trước nữa, bất kể kích thước khung.

## v2.6.0 — Bỏ compass/gunfire, weapon HUD kiểu Call of Duty, primary/secondary weapon, fix bug alpha
1. **Bỏ hẳn Compass + Gunfire detection**: xoá toàn bộ `CompassOverlay`, `CompassConfig`,
   `client/compass/*` (DiamondRender, GunfireBlips, GunfireListener), keybind toggle compass, config
   `modernhud-compass.toml`, và mọi chỗ đăng ký liên quan — vì đã có mod khác lo phần này rồi.
2. **Fix nguyên nhân thật của việc icon súng bị "lệch tệ hại"**: hoá ra TACZ có sẵn HUD súng riêng
   của nó (`GunHudOverlay`), và nó vẫn chạy song song với HUD của ModernHUD — 2 HUD đè lên nhau,
   icon to nhỏ lẫn lộn, đúng như ảnh chụp gửi kèm. Giờ ModernHUD tự tắt HUD súng gốc của TACZ (qua
   `RenderConfig.GUN_HUD_ENABLE`, đọc bằng reflection để an toàn nếu TACZ đổi API) mỗi khi
   `weapon widget` của ModernHUD bật — tắt widget của ModernHUD thì trả lại HUD gốc của TACZ, không
   bao giờ để người chơi không có gì cả.
3. **Fix bug alpha khiến toàn bộ đường viền góc (corner accent) và màu absorption custom bị VÔ
   HÌNH từ trước tới giờ**: màu lấy từ config luôn ở dạng `0xRRGGBB` (không có byte alpha), nhưng
   `GuiGraphics#fill` (khác với `drawString`) KHÔNG tự ép opaque khi alpha=0 — nghĩa là mọi đường
   viền góc accent (cả HUD người chơi lẫn HUD súng) và màu absorption custom trước giờ vẽ ra hoàn
   toàn trong suốt, không hề hiển thị. Đã fix tận gốc trong `HudPanel.draw` và `BarRender.plates`
   bằng cách luôn ép opaque ngay tại chỗ vẽ.
4. **Redesign weapon HUD theo phong cách Call of Duty (Modern Warfare)**: số đạn mag/reserve dạng
   in đậm, zero-pad 3 chữ số kiểu MW ("030"/"060", tắt được qua config), icon súng nằm trong khung
   accent-bracket cố định riêng bên phải, tên súng vẫn là toast hiện lên rồi mờ dần khi đổi súng
   (giữ đúng yêu cầu trước đó — "khung cố định, tên không làm phình khung") nhưng canh vị trí sát
   khu vực số đạn hơn cho giống ảnh tham khảo.
5. **Hệ thống 2 vũ khí (primary/secondary) kiểu COD**: súng đang cầm luôn là primary (khung lớn);
   khẩu súng KHÁC gần nhất từng cầm (còn trong hotbar) hiện thành 1 ô nhỏ, mờ hơn, nép ở góc khung
   icon chính — y hệt HUD 2 súng của Call of Duty. Cầm lại khẩu đó thì nó lại thành primary. Tự động
   ẩn nếu súng đó không còn trong hotbar nữa (không để lại "bóng ma").

## v2.5.1 — Weapon HUD fixed frame + fix icon distortion + fade box direction
Fix theo feedback thực tế từ ảnh chụp màn hình (A545 + FGM-148 Javelin) và tham khảo trực tiếp
source code của TaczPresence (github.com/AlfredoAlessandro/Tacz-Presence).
1. **Fix icon bị "biến dạng"**: nguyên nhân gốc là `TaczCompat.renderHudTexture` cũ đọc thẳng kích
   thước pixel thật của file PNG icon (qua `NativeImage`) rồi tính scale theo đó — nhưng TACZ KHÔNG
   hoạt động vậy. Đối chiếu với chính `ImmersiveGunHudOverlay` của TaczPresence (và cả HUD gốc của
   TACZ) thì icon HUD của TACZ luôn được vẽ từ một canvas cố định **39×13**, bất kể súng nào (súng
   dài như AK hay súng to như Javelin đều dùng chung 1 canvas 39×13 này, phần asset tự canh trong
   đó). Đọc kích thước file thật rồi tự tính UV là sai hoàn toàn, gây méo/lệch UV nặng — nhất là với
   Javelin vì file thật của nó lệch xa tỉ lệ 39:13. Đã bỏ hẳn cơ chế đọc `NativeImage`, giờ luôn blit
   cố định 39×13 nhân với 1 hệ số scale duy nhất (`iconScale`), y hệt cách TaczPresence làm.
2. **HUD giờ có khung cố định (fixed frame), không đổi theo tên súng hay art**: trước đây bề rộng
   panel được tính động theo độ dài tên súng + kích thước icon, nên súng có tên dài
   ("FGM-148 Javelin Anti-Tank Missile") làm cả khung phình to, đè lên crosshair, và tràn ra ngoài
   màn hình (đúng như ảnh chụp — chữ bị cắt "FGM-148 Ja"). Giờ khung dùng kích thước CỐ ĐỊNH
   (`frameWidth` / `frameHeight` trong config), y hệt hướng tiếp cận của TaczPresence: icon + số đạn
   luôn neo ở toạ độ cố định bên trong khung, chữ chỉ được đo để canh phải (right-align), không bao
   giờ dùng để phình khung.
3. **Tên súng tách thành toast riêng, tự ẩn sau 1-2 giây**: tên súng không còn nằm trong khung cố
   định nữa (đó chính là thứ từng làm khung biến dạng) — giờ nó hiện lên như một nhãn nổi riêng phía
   trên khung mỗi khi đổi súng, giữ nguyên ~1.4s rồi mờ dần trong ~0.5s (chỉnh được qua
   `nameHoldMs`/`nameFadeMs`), sau đó biến mất hẳn — khung ammo bên dưới không bao giờ bị ảnh hưởng
   bởi độ dài tên.
4. **Fix fade box bị lộn ngược**: gradient nền (`HudPanel`) trước đó đậm phía trên/nhạt phía dưới —
   ngược với cảm giác "neo" tự nhiên khi HUD nằm sát mép màn hình. Đã đổi lại: đậm ở cạnh neo gần mép
   màn hình, nhạt dần khi đi lên phía trong khung hình — dùng chung cho cả HUD người chơi lẫn HUD
   súng vì cả 2 dùng chung `HudPanel`.

## v2.5.0 — Config sync fixes, custom absorption colour, modern panel, TACZ weapon widget
1. **Config đồng bộ / fix lỗi**: đối chiếu từng field trong `HudConfig` với renderer thực tế và
   phát hiện các config đã khai báo + load nhưng KHÔNG được dùng ở đâu cả: `namePosition`
   (ABOVE/BELOW bị bỏ qua, tên luôn vẽ ở trên), `hpTextPosition` (luôn hardcode END_OF_BAR),
   `showOverflowText` (không có chỗ nào vẽ "+N"), và cả nhóm Armor Bar
   (`showArmorBar/showArmorText/armorTextFormat/armorTextPosition/armorTextColor` — tồn tại từ
   version cũ nhưng chưa từng được wire vào renderer). Đã nối lại toàn bộ các config này vào
   `PlayerHudOverlay`, bao gồm thêm một dòng Armor thật (vanilla armor points, tách biệt với
   Absorption/shield). Đồng thời bổ sung 34 translation key còn thiếu trong `en_us.json` (trước đó
   sẽ hiện ra dạng `modernhud.config.xxx` thô trong màn hình config).
2. **Absorption màu custom**: thêm `absorptionColorMode` (TIERED/CUSTOM) +
   `absorptionCustomColor`, mặc định ocean blue `0x1C7ED6` khi chọn CUSTOM — khoá cả thanh về một
   màu duy nhất thay vì đổi theo tier như trước.
3. **Fix lệch head/name/bar**: đầu người chơi trước đó được căn giữa theo TOÀN BỘ khối (kể cả dòng
   tên), nên nhìn bị lệch so với các thanh bar. Giờ đầu được căn giữa riêng theo khối bar
   (plates+health+armor), độc lập với tên ở ABOVE hay BELOW.
4. **Panel "fade box + line góc"**: `HudPanel` mới — một lớp nền gradient mờ dần (translucent fade)
   + 2 đường viền góc màu accent (corner brackets), dùng chung cho cả HUD người chơi lẫn HUD súng,
   cố tình chỉ 1 lớp phẳng — không có drop-shadow / box đôi phía sau.
5. **Tích hợp TACZ weapon HUD từ CustomHUD**: decompile `TaczCompat`/`AmmoHud` gốc (CFR) để lấy đúng
   logic mag/reserve/fire-mode, port lại vào package riêng của ModernHUD (Mojang mapping, không SRG)
   thành `TaczCompat` + `WeaponConfig` + `WeaponHudOverlay`. Hiển thị icon HUD gốc của TACZ, tên súng,
   fire mode, và đạn mag/reserve dạng SỐ THUẦN — **bỏ hẳn thanh gauge ammo bar**, không shadow box,
   dùng chung panel "modern" ở trên. Soft-dep an toàn: không TACZ thì widget không làm gì cả.

## v2.1.0 — Render-engine HUD + MW compass gunfire (làm lại từ đầu, cẩn thận)
Bản này viết lại từ source gốc v2.0.0, mỗi file whole-rewrite + verify balance, không chồng patch.

### Item 1 — Render-engine migration
Thanh bar không còn PNG texture: vẽ hoàn toàn bằng GuiGraphics fill/gradient (BarRender). Thanh máu
MW2022: mỏng, trắng, sheen nhẹ, smooth. Sắc nét mọi scale.

### Item 2 — HUD player rework
- Đầu người chơi sang CẠNH TRÁI khối bar, căn giữa chiều cao.
- Tên ABOVE/BELOW qua config `namePosition`.
- Absorption MỎNG (config `absorptionHeight`, mặc định 3px), đặt TRÊN thanh giáp, 3 tier màu
  trắng→xanh→tím/đỏ như MW.

### Item 3 — Compass gunfire detection
- Nháy đỏ theo HƯỚNG kẻ địch bắn (chiếu bearing lên bar, cùng công thức tick).
- Detect qua TACZ GunShootEvent client-side (soft-dep, chỉ register khi TACZ loaded).
- shoot_3p 256 block (to/đậm khi gần), silence_3p 32 block (mờ tới khi sát), mũi tên cao/thấp.
- **Whitelist team vanilla**: KHÔNG nháy với chính mình + người cùng team; nháy với khác team +
  mob/player không team.
- Config nhóm "Gunfire Blips": bật/tắt, tầm, màu, kích thước.

TACZ là optional — không có TACZ thì compass vẫn chạy, chỉ mất blip.

## v2.2.0 — Bar smooth & modern kiểu Damage Engine + rework layout + compass off-arc
**Bar (học Damage Engine, hoàn toàn textureless):**
- **Pill bo góc** vẽ bằng stacked fill (radius = height/2) — smooth, modern, không texture.
- **Smooth lerp**: thanh máu TRƯỢT mượt về giá trị thật (mất máu nhanh hơn, hồi máu dịu hơn) thay
  vì nhảy giật — đúng cảm giác Damage Engine.
- Sheen nhẹ nửa trên. Absorption strip 3-tier vẫn dùng pill.
**Layout HUD mới (theo yêu cầu):**
- Đầu player bên trái. Bên phải từ trên xuống: THANH MÁU (pill smooth) + SỐ máu ở cuối bar →
  GIÁP hiển thị bằng ICON khiên vẽ tay + SỐ (KHÔNG còn thanh giáp) → TÊN player dưới cùng.
- Absorption (khi có) là dải mỏng trên cùng.
**Compass off-arc indicator:**
- Tiếng súng NGOÀI cung compass giờ hiện `>>` (bên phải) hoặc `<<` (bên trái) ở mép chỉ hướng,
  fade theo cường độ; xoay vào tầm nhìn thì hiện chấm nháy như thường.

Chỉ còn sound + icon break dùng texture; toàn bộ bar đã render-engine.

## v2.3.0 — HUD MW2022 seamless + ping kim cương Ping-Wheel + chevron rõ
**HUD (theo ảnh ref MW2022/DMZ/Warzone, sửa ảnh 2 xấu):**
- Tên player TRÊN ĐẦU. Đầu bên trái, căn giữa khối.
- Thanh máu HÌNH CHỮ NHẬT mỏng (nửa chiều cao cũ), SEAMLESS — không viền nặng, chỉ hairline 1px
  + sheen nhẹ. Trượt mượt (Damage-Engine glide). Số HP ở cuối bar.
- Armor = PLATE BAR chia ô có KHOẢNG CÁCH (gap) giữa các plate, tô tier trắng→xanh→tím→đỏ. Bỏ
  hẳn armor icon + số giáp.
- Layout ỔN ĐỊNH: hàng plate luôn reserve chỗ nên khi có/mất shield thanh máu KHÔNG nhảy lệch.
**Ping / compass (theo ảnh ref MW + Ping Wheel):**
- Blip giờ là KIM CƯƠNG 2 LỚP (outline ring + filled center) đúng kỹ thuật Ping Wheel: vẽ ô vuông
  rồi xoay 45° bằng matrix, scale mượt (không còn ô vuông 30fps).
- Animation: mỗi phát bắn BOUNCE to (size theo khoảng cách — gần thì to hơn) rồi FADE + NHỎ lại.
- Off-arc: chevron `<<` / `>>` TO ĐẬM vẽ bằng fill (không còn chữ nhỏ khó thấy) chỉ hướng ở mép.

## v2.4.0 — Compass polish + HUD editor kéo thả + config đầy đủ + ổn định
**Compass / shoot indicator:**
- Marker nhỏ lại (~65% mặc định) + CONFIG được: size (%), thời gian fade (ms), độ cao trên bar.
- Marker giờ nằm CAO HƠN thanh tọa độ (vẽ ngoài scissor nên không bị cắt).
- Chevron off-arc `<<`/`>>` giờ ANIMATED (bounce-in + pulse + fade) crisp như gunfire ping, vẽ
  bằng fill (không texture), đẩy RA NGOÀI rìa thanh tọa độ.
**Absorption tier:** đúng thang XÁM → XANH DƯƠNG → TÍM → ĐỎ theo số plate đầy (1 plate = xám,
2 = xanh, 3 = tím, 4+ = đỏ). Sửa lỗi 2 vạch đã tím.
**HUD editor kéo thả (QOL kiểu COD):** keybind mở "HUD Layout Editor" — kéo Player HUD và icon
Hit/Break (gộp chung) đặt vị trí bằng CHUỘT, có lưới căn, lưu khi đóng. Hết cảnh bấm số offset.
**Config:** mỗi feature có nhóm config đồng bộ, đầy đủ; thêm setter runtime persist cho editor.
**Ổn định:** rà client/server — class server không import class client-only; gunfire listener chỉ
đăng ký client + khi TACZ có mặt; balance toàn bộ sạch.

(YACL config + live preview kiểu HaloHUD: chờ bạn gửi HaloHUD source để làm cho chuẩn.)
