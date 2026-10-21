# Listingan Play Store — Bahasa Indonesia (id)

## Nama aplikasi (≤ 30)
MockGPS – Simulasi Lokasi

## Deskripsi singkat (≤ 80)
Simulasikan GPS, rute, dan joystick untuk menguji aplikasi lokasi. Tanpa iklan.

## Deskripsi lengkap (≤ 4000)
MockGPS adalah alat simulasi lokasi untuk developer Android dan penguji QA. Pilih satu titik, jalankan rute, atau kendalikan dengan joystick, dan setiap aplikasi di perangkat akan melihat lokasi tersebut: GPS, jaringan, dan penyedia lokasi fused dari Google Play services melaporkan posisi yang sama, sehingga aplikasi tidak kembali ke lokasi asli Anda.

TITIK
• Tekan lama peta, cari alamat, tempel koordinat atau tautan Google Maps
• Bagikan tempat dari Google Maps langsung ke MockGPS
• Simpan favorit dan langsung menuju ke sana dengan satu ketukan

RUTE
• Tambahkan titik singgah dan ikuti jalan nyata (jalan kaki, sepeda, sepeda motor, mobil) atau garis lurus
• Impor jejak GPX
• Atur kecepatan, jeda dan lanjutkan, lalu pilih berhenti, ulangi, atau bolak-balik saat tiba di akhir rute

JOYSTICK
• Joystick mengambang di atas aplikasi apa pun, dengan kecepatan dari berjalan kaki hingga berkendara

DIBUAT UNTUK DEVELOPER
• Kontrol penuh lewat adb: mulai, gerakkan, jalankan rute, ubah kecepatan, dan hentikan dari komputer atau skrip CI
• Atur akurasi, ketinggian, frekuensi pembaruan, dan jitter alami
• Pilih penyedia yang disimulasikan: GPS, jaringan, fused
• Penyiapan terpandu untuk Opsi developer, plus penyiapan adb satu baris

PRIVAT SEJAK AWAL
• Tanpa iklan, tanpa analitik, tanpa akun, tanpa server sendiri
• Favorit dan pengaturan tetap berada di perangkat Anda
• Peta dari OpenFreeMap dan OpenStreetMap; rute oleh OSRM (Anda dapat memakai server sendiri)

PERLU DIKETAHUI
• Memerlukan Opsi developer dan memilih MockGPS sebagai "aplikasi lokasi palsu".
• Android menandai lokasi simulasi sebagai mock (Location.isMock), sehingga aplikasi yang memeriksanya akan mengetahuinya. MockGPS dibuat untuk menguji aplikasi Anda sendiri; jangan gunakan untuk melanggar ketentuan layanan lain.

Data peta © OpenStreetMap contributors. Pertanyaan atau masukan: loivo@dsai.vn
GitHub: https://github.com/digitalsolutionsai/mockgps-android
