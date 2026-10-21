# Store listing — English (en-US, default)

## App name (≤ 30)
MockGPS – Mock Location Tool

## Short description (≤ 80)
Simulate GPS, routes and joystick moves to test location-based apps. No ads.

## Full description (≤ 4000)
MockGPS is a mock location tool for Android developers and QA testers. Pick a point, play a route or steer with a joystick, and every app on the device sees that location: GPS, network and the fused location provider from Google Play services all report the same position, so apps don't jump back to your real location.

POINT
• Long-press the map, search an address, paste coordinates or a Google Maps link
• Share a place from Google Maps straight into MockGPS
• Save favorites and jump to them in one tap

ROUTES
• Add waypoints and follow real roads (walk, bike, motorbike, car) or straight lines
• Import GPX tracks
• Set speed, pause and resume, and choose stop, loop or ping-pong at the end

JOYSTICK
• Floating joystick over any app, with walking to driving speeds

BUILT FOR DEVELOPERS
• Full adb control: start, move, play routes, change speed and stop from your computer or CI scripts
• Tune accuracy, altitude, update rate and natural jitter
• Choose which providers to mock: GPS, network, fused
• Guided setup for Developer options, plus a one-line adb setup

PRIVATE BY DESIGN
• No ads, no analytics, no account, no servers of our own
• Favorites and settings stay on your device
• Maps by OpenFreeMap and OpenStreetMap; routing by OSRM (you can use your own server)

GOOD TO KNOW
• Requires Developer options and selecting MockGPS as the "mock location app".
• Android marks simulated locations as mock (Location.isMock), so apps that check for it will notice. MockGPS is meant for testing your own apps; don't use it to break other services' terms.

Map data © OpenStreetMap contributors. Questions or feedback: loivo@dsai.vn
GitHub: https://github.com/digitalsolutionsai/mockgps-android
