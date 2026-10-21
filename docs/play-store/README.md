# Đăng MockGPS lên Google Play

Package: `vn.dsai.mockgps` · Nhà phát hành: DSAI · Liên hệ: loivo@dsai.vn · Không quảng cáo, không in-app purchase.

## 1. Khóa ký (upload key)

Chỉ làm một lần. Giữ file `.jks` và mật khẩu ở nơi an toàn; repo đã ignore `keys/` và `keystore.properties`.

```bash
mkdir -p keys
keytool -genkeypair -v -keystore keys/upload.jks -alias upload \
  -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=DSAI, O=DSAI, C=VN"
cp keystore.properties.example keystore.properties   # điền mật khẩu
```

Bật **Play App Signing** khi tạo app (mặc định). Nếu mất upload key thì Google cho reset, còn khóa ký thật do Google giữ.

## 2. Build

```bash
./gradlew bundleRelease
# → app/build/outputs/bundle/release/app-release.aab
```

Mỗi lần phát hành tăng `versionCode` (và `versionName`) trong `app/build.gradle.kts`.

- Target API 36: đạt yêu cầu Play từ 31/08/2026.
- MapLibre 13.7.1 hỗ trợ trang bộ nhớ 16 KB, đạt yêu cầu bắt buộc cho app có thư viện native.

## 3. Tạo app trên Play Console

- Tên: **MockGPS – Mock Location Tool**
- Ngôn ngữ mặc định: English (United States)
- Loại: App · Miễn phí
- Danh mục: **Tools**
- Email liên hệ: loivo@dsai.vn · Website: https://dsai.vn

Nếu tài khoản developer là **tài khoản cá nhân** tạo sau 11/2023, Play bắt buộc closed testing với ≥ 12 tester trong 14 ngày trước khi lên production. **Tài khoản tổ chức** (DSAI) thì không bị yêu cầu này.

## 4. Store listing

- Nội dung chữ: `listing/<locale>.md` (en-US là mặc định, kèm 14 ngôn ngữ khác).
- Icon 512×512: `graphics/icon-512.png`
- Feature graphic 1024×500: `graphics/feature-graphic.png`
- Ảnh chụp điện thoại (1080×2160, tỉ lệ ≤ 2:1): `graphics/screenshots/<locale>/`

Tạo lại bằng script:
- Chụp ảnh: `tools/store-screenshots.sh [locale…]`. Cần emulator hoặc máy đã cài app và chạy `tools/mockgps setup`.
- Feature graphic: `tools/make-feature-graphic.py`.
- Kiểm tra bản dịch và độ dài listing: `tools/check-translations.py`.

Đẩy lên Play bằng `tools/play-publish.py`, gồm chữ, ảnh chụp, icon, feature graphic, email và website liên hệ:
- `--status`: xem những gì đang có trên Play.
- `--validate`: chạy thử, không lưu gì.
- `--text-only` và `--only vi,ja-JP`: chỉ đẩy một phần.
- `--bundle app/build/outputs/bundle/release/app-release.aab --notes "..."`: đưa bản mới lên Internal testing. Nhớ tăng `versionCode` trước khi build.

Script không dùng file key, vì tổ chức Cloud chặn tạo key cho service account. Thay vào đó:
- Đăng nhập `gcloud auth login` bằng tài khoản có quyền *Service Account Token Creator* trên `mockgps-publisher@dsai-play-publisher.iam.gserviceaccount.com`.
- Service account này được mời trong Play Console, có quyền listing và testing, không có quyền phát hành production.
- Mỗi lần đẩy đủ ảnh mất khoảng 8 phút.
- Trong lúc script chạy, đừng sửa Store listing trên Console vì sẽ xung đột.

Tránh trong listing và tên:
- các từ "fake", "cheat", "prank", "hide location",
- tên game (Pokémon GO…) hoặc tên app khác,
- "#1", "best", "free", emoji trong tên.

App được định vị là **công cụ kiểm thử cho developer**.

## 5. App content — câu trả lời đề xuất

| Mục | Trả lời |
|---|---|
| Privacy policy | Host `docs/privacy-policy.html` tại **https://dsai.vn/mockgps/privacy** (URL này đã gắn trong app, màn Giới thiệu) |
| Ads | No, my app does not contain ads |
| App access | All functionality is available without special access. Ghi chú cho reviewer: "Enable Developer options → Select mock location app → MockGPS, or run: `adb shell appops set vn.dsai.mockgps android:mock_location allow`" |
| Content rating | Hoàn thành questionnaire: Category **Utility/Productivity/Communication/Other**; không bạo lực, không nội dung người dùng tạo, không chia sẻ vị trí với người khác → dự kiến **Everyone / PEGI 3** |
| Target audience | **18+** (công cụ cho developer; tránh chính sách Families) |
| News app / Government / Financial / Health | No |
| Data safety | Xem mục 6 |
| Foreground service | Xem mục 7 |
| Location permissions | Chỉ dùng **ACCESS_COARSE_LOCATION**, không có FINE hay BACKGROUND, nên không cần khai báo vị trí chính xác (policy mới, bắt buộc từ 27/01/2027) |

## 6. Data safety

App không có máy chủ riêng. Truy vấn được gửi thẳng từ máy tới dịch vụ bên thứ ba khi người dùng dùng tính năng. Theo định nghĩa của Play, dữ liệu rời thiết bị vẫn tính là "collected". Vì vậy khai báo thận trọng như sau:

- **Does your app collect or share any of the required user data types?** Yes
- **Is all data encrypted in transit?** Yes (chỉ dùng HTTPS)
- **Do you provide a way for users to request that their data be deleted?** No. App không lưu dữ liệu nào ngoài máy; xóa dữ liệu app là đủ.

| Data type | Collected | Shared | Ephemeral | Required | Purpose |
|---|---|---|---|---|---|
| Location → Approximate location | Yes | No (user-initiated: gửi tới dịch vụ bản đồ/định tuyến theo yêu cầu người dùng) | Yes | Optional | App functionality |
| App activity → In-app search history | Yes | No (user-initiated) | Yes | Optional | App functionality |

Không thu thập: thông tin cá nhân, tài chính, danh bạ, ảnh, định danh thiết bị, chẩn đoán/crash, analytics.

## 7. Khai báo Foreground service (Location)

- **Type:** Location (`FOREGROUND_SERVICE_LOCATION`)
- **Use case:** Other → "Developer tool: mock location provider"
- **Description:**
  > MockGPS is a developer tool that supplies a simulated location to the system after the user selects it under Developer options → Select mock location app. When the user starts a simulation (a fixed point, a route or the joystick), a foreground service pushes the simulated position to the LocationManager test providers and the fused location provider every second. A persistent notification shows the current state with Stop and Joystick actions, plus Pause during route playback. The service stops as soon as the user taps Stop.
- **User impact if deferred or interrupted:**
  > Other apps immediately fall back to the real device location, which breaks the user's test session (for example, a route being replayed stops in the middle).
- **Video:** quay màn hình 30–60 giây:
  1. mở app,
  2. chọn một điểm, bấm *Bắt đầu giả lập*,
  3. kéo thanh thông báo xuống, thấy thông báo "Đang giả lập",
  4. mở rộng thông báo đó, bấm *Dừng*, thông báo biến mất.

  Upload lên YouTube (unlisted) rồi dán link dạng `https://www.youtube.com/watch?v=<id>`. Video dọc sẽ bị YouTube xếp vào Shorts, nên nếu nhận link `/shorts/<id>` thì đổi sang dạng `watch?v=`.

  Lưu ý khi quay lại video:
  - Dùng toạ độ ở xa (ví dụ Paris) để không lộ vị trí thật.
  - Làm mờ các thông báo khác trong thanh thông báo.
  - Google Maps chỉ hiện chấm xanh nếu đã có quyền vị trí.

## 8. Điều khoản dịch vụ bên thứ ba (đã kiểm tra)

- **OpenFreeMap:** miễn phí, không giới hạn, cho phép thương mại và app di động. Bắt buộc ghi nguồn OpenMapTiles và OSM; app đã có nút attribution và màn Giới thiệu.
- **OSRM của FOSSGIS** (routing.openstreetmap.de):
  - Tối đa 1 request/giây (app tự giãn cách ≥ 1,1 giây).
  - User-Agent định danh app (`MockGPS/1.0 (...; loivo@dsai.vn)`).
  - Không hardcode URL (đổi được trong Cài đặt).
  - Ghi email nhà vận hành trên listing (đã có).
- **Nominatim:** chỉ là phương án dự phòng khi tìm kiếm, không tự động gợi ý khi gõ. Thứ tự ưu tiên: Geocoder của Android, rồi Photon, rồi Nominatim.
- **Không dùng** `tile.openstreetmap.de` (cấm app tải tile nếu chưa xin phép) và **không dùng** CARTO (cần API key).

## 9. Trước khi bấm phát hành

- [ ] Host privacy policy, mở thử https://dsai.vn/mockgps/privacy
- [ ] Hộp thư loivo@dsai.vn nhận được mail
- [ ] Tăng `versionCode`
- [ ] `./gradlew bundleRelease` ký bằng upload key
- [ ] Cài bản release lên máy thật (`bundletool` hoặc internal testing track), kiểm tra cả 3 chế độ Điểm / Tuyến / Joystick và lệnh adb
- [ ] Ảnh chụp màn hình mới nhất theo từng ngôn ngữ
