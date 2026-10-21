# Store listing — Tiếng Việt (vi)

## Tên ứng dụng (≤ 30)
MockGPS – Giả lập vị trí

## Mô tả ngắn (≤ 80)
Giả lập GPS, tuyến đường, joystick để test app dùng vị trí. Không quảng cáo.

## Mô tả đầy đủ (≤ 4000)
MockGPS là công cụ giả lập vị trí cho developer và tester Android. Chọn một điểm, chạy theo tuyến hoặc điều khiển bằng joystick, mọi ứng dụng trên máy đều thấy vị trí đó: GPS, mạng và nguồn fused của Google Play services cùng báo một vị trí, nên ứng dụng không bị nhảy về vị trí thật.

ĐIỂM
• Nhấn giữ trên bản đồ, tìm địa chỉ, dán tọa độ hoặc link Google Maps
• Chia sẻ địa điểm từ Google Maps sang MockGPS
• Lưu điểm yêu thích, chuyển tới chỉ với một chạm

TUYẾN ĐƯỜNG
• Thêm các điểm đi qua, bám theo đường thật (đi bộ, xe đạp, xe máy, ô tô) hoặc nối thẳng
• Nhập file GPX
• Chỉnh tốc độ, tạm dừng, tiếp tục; khi đến đích thì dừng, lặp lại hoặc khứ hồi

JOYSTICK
• Joystick nổi trên mọi ứng dụng, tốc độ từ đi bộ tới lái xe

DÀNH CHO DEVELOPER
• Điều khiển toàn bộ bằng adb: bắt đầu, di chuyển, chạy tuyến, đổi tốc độ, dừng từ máy tính hoặc script CI
• Chỉnh độ chính xác, độ cao, tần suất cập nhật và dao động tự nhiên
• Chọn nguồn vị trí cần giả lập: GPS, mạng, fused
• Hướng dẫn thiết lập Tùy chọn nhà phát triển từng bước, hoặc thiết lập nhanh bằng một lệnh adb

TÔN TRỌNG QUYỀN RIÊNG TƯ
• Không quảng cáo, không phân tích hành vi, không tài khoản, không máy chủ riêng
• Điểm yêu thích và cài đặt chỉ lưu trên máy
• Bản đồ từ OpenFreeMap và OpenStreetMap; tìm đường bằng OSRM (có thể dùng máy chủ riêng)

LƯU Ý
• Cần bật Tùy chọn nhà phát triển và chọn MockGPS làm "ứng dụng vị trí mô phỏng".
• Android đánh dấu vị trí giả lập là mock (Location.isMock), ứng dụng nào có kiểm tra sẽ nhận ra. MockGPS dùng để kiểm thử ứng dụng của bạn; đừng dùng để vi phạm điều khoản của dịch vụ khác.

Dữ liệu bản đồ © OpenStreetMap contributors. Góp ý: loivo@dsai.vn
GitHub: https://github.com/digitalsolutionsai/mockgps-android
