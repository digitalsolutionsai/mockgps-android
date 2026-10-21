# Play Store Girişi — Türkçe (tr-TR)

## Uygulama adı (≤ 30)
MockGPS – Konum Simülatörü

## Kısa açıklama (≤ 80)
GPS, rota ve joystick simülasyonuyla konum uygulamalarını test edin. Reklamsız.

## Tam açıklama (≤ 4000)
MockGPS, Android geliştiricileri ve QA test uzmanları için bir konum simülasyonu aracıdır. Bir nokta seçin, bir rota oynatın veya joystick ile yönlendirin; cihazdaki her uygulama bu konumu görür: GPS, ağ ve Google Play services'in fused konum sağlayıcısı aynı konumu bildirir, böylece uygulamalar gerçek konumunuza geri dönmez.

NOKTA
• Haritaya uzun basın, adres arayın, koordinat veya Google Maps bağlantısı yapıştırın
• Google Maps'ten bir yeri doğrudan MockGPS ile paylaşın
• Favorileri kaydedin ve tek dokunuşla onlara atlayın

ROTALAR
• Ara noktalar ekleyin ve gerçek yolları (yürüyüş, bisiklet, motosiklet, araba) veya düz çizgileri izleyin
• GPX izlerini içe aktarın
• Hızı ayarlayın, duraklatın ve devam ettirin; varışta durma, tekrar veya gidiş-dönüş seçin

JOYSTICK
• Her uygulamanın üzerinde yüzen joystick; yürüyüşten araç kullanmaya kadar hızlar

GELİŞTİRİCİLER İÇİN TASARLANDI
• Tam adb kontrolü: bilgisayarınızdan veya CI betiklerinden başlatın, hareket ettirin, rotaları oynatın, hızı değiştirin ve durdurun
• Doğruluğu, rakımı, güncelleme hızını ve doğal sapmayı ayarlayın
• Hangi sağlayıcıların simüle edileceğini seçin: GPS, ağ, fused
• Geliştirici seçenekleri için adım adım kurulum ve tek satırlık adb kurulumu

TASARIMDAN GELEN GİZLİLİK
• Reklam yok, analiz yok, hesap yok, kendi sunucumuz yok
• Favoriler ve ayarlar cihazınızda kalır
• Haritalar OpenFreeMap ve OpenStreetMap'ten; rota hesaplama OSRM ile (kendi sunucunuzu kullanabilirsiniz)

BİLMENİZ GEREKENLER
• Geliştirici seçeneklerinin açık olmasını ve MockGPS'in "sahte konum uygulaması" olarak seçilmesini gerektirir.
• Android, simüle edilen konumları mock olarak işaretler (Location.isMock); bunu kontrol eden uygulamalar fark eder. MockGPS kendi uygulamalarınızı test etmek için tasarlanmıştır; diğer hizmetlerin şartlarını ihlal etmek için kullanmayın.

Harita verileri © OpenStreetMap contributors. Sorular veya geri bildirim: loivo@dsai.vn
GitHub: https://github.com/digitalsolutionsai/mockgps-android
