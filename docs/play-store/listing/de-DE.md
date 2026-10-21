# Play-Store-Eintrag — Deutsch (de-DE)

## App-Name (≤ 30)
MockGPS – Standortsimulator

## Kurzbeschreibung (≤ 80)
GPS, Routen und Joystick simulieren, um Standort-Apps zu testen. Ohne Werbung.

## Vollständige Beschreibung (≤ 4000)
MockGPS ist ein Tool zur Standortsimulation für Android-Entwickler und QA-Tester. Wähle einen Punkt, spiele eine Route ab oder steuere per Joystick – und jede App auf dem Gerät sieht diesen Standort: GPS, Netzwerk und der Fused-Location-Provider von Google Play services melden dieselbe Position, sodass Apps nicht auf deinen echten Standort zurückspringen.

PUNKT
• Karte lange drücken, Adresse suchen, Koordinaten oder einen Google-Maps-Link einfügen
• Einen Ort aus Google Maps direkt mit MockGPS teilen
• Favoriten speichern und mit einem Tipp dorthin springen

ROUTEN
• Wegpunkte hinzufügen und echten Straßen (zu Fuß, Fahrrad, Motorrad, Auto) oder geraden Linien folgen
• GPX-Tracks importieren
• Geschwindigkeit einstellen, pausieren und fortsetzen und am Ende zwischen Stopp, Wiederholung und Hin-und-zurück wählen

JOYSTICK
• Schwebender Joystick über jeder App, mit Geschwindigkeiten vom Gehen bis zum Autofahren

FÜR ENTWICKLER GEMACHT
• Volle Steuerung per adb: starten, bewegen, Routen abspielen, Geschwindigkeit ändern und stoppen – vom Computer oder aus CI-Skripten
• Genauigkeit, Höhe, Aktualisierungsrate und natürliche Abweichung einstellen
• Auswählen, welche Provider simuliert werden: GPS, Netzwerk, Fused
• Geführte Einrichtung der Entwickleroptionen sowie eine adb-Einrichtung in einer Zeile

PRIVATSPHÄRE VON ANFANG AN
• Keine Werbung, keine Analyse, kein Konto, keine eigenen Server
• Favoriten und Einstellungen bleiben auf deinem Gerät
• Karten von OpenFreeMap und OpenStreetMap; Routing über OSRM (du kannst deinen eigenen Server nutzen)

WISSENSWERT
• Erfordert die Entwickleroptionen und die Auswahl von MockGPS als „App für simulierte Standorte“.
• Android markiert simulierte Standorte als Mock (Location.isMock), Apps, die das prüfen, bemerken es also. MockGPS ist zum Testen deiner eigenen Apps gedacht; nutze es nicht, um gegen die Bedingungen anderer Dienste zu verstoßen.

Kartendaten © OpenStreetMap contributors. Fragen oder Feedback: loivo@dsai.vn
GitHub: https://github.com/digitalsolutionsai/mockgps-android
