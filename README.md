# MockGPS

Mock location tool for Android developers and testers, by [DSAI](https://dsai.vn). Package `vn.dsai.mockgps`, no ads, no analytics.

- **Point:**
  - long-press the map, search an address, or paste coordinates or a Google Maps link;
  - share a place from Google Maps into the app;
  - favorites.
- **Route:**
  - road-snapped waypoints (OSRM) or straight lines, or GPX import;
  - speed, pause/resume, and stop, loop or ping-pong at the end.
- **Joystick:** floating overlay that works on top of any app.
- **Mocked providers:** GPS, network, platform fused and Google Play services fused, each switchable in Settings.
- **adb control** for automation (see `tools/mockgps`).

Stack: Kotlin, Jetpack Compose (Material 3), MapLibre Native 13 + OpenFreeMap tiles. minSdk 26, targetSdk 36.

## Build and install

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
tools/mockgps setup        # select as mock location app + grant permissions over adb
```

Release: see [docs/play-store/README.md](docs/play-store/README.md).

## How it works

`MockService` is a location-type foreground service. Every tick (1 s by default) it:
- pushes one `Location` to the `LocationManager` test providers (`gps`, `network`, `fused`);
- pushes the same location to `FusedLocationProviderClient.setMockLocation`, after enabling `setMockMode(true)`.

On stop, it removes the test providers and calls `setMockMode(false)`, so nothing stays stuck on the fake position.

The service is exported behind `android.permission.DUMP`. Only the app itself and `adb shell` can drive it.

## adb control

`tools/mockgps` wraps `am start-foreground-service`:

```bash
tools/mockgps set 10.7769 106.7009            # fixed point
tools/mockgps set "Chợ Bến Thành"             # address or Google Maps link
tools/mockgps fav "Home"                      # saved favorite
tools/mockgps route 10.7798,106.6990 10.7725,106.6980 --travel motorbike --speed 35 --end loop
tools/mockgps gpx track.gpx --speed 9
tools/mockgps pause | resume | stop | speed 60 | joystick on
tools/mockgps status                          # what the system currently reports
```

Use `-s SERIAL` (or `ANDROID_SERIAL`) to pick a device. The raw intents are listed in the app under *Menu → adb control*.

## Third-party services

- Map tiles: OpenFreeMap (© OpenMapTiles, © OpenStreetMap contributors).
- Search: Android Geocoder, then Photon, then Nominatim.
- Routing: OSRM at routing.openstreetmap.de (FOSSGIS), limited to 1 request per second. The server can be changed in Settings.

Privacy policy: https://dsai.vn/mockgps/privacy

## Forks

You are welcome to fork. If you publish a build, change `applicationId`, the app name and the icon. Also put your own contact in `Places.userAgent`. Builds with another package name do not send DSAI's contact to the OSM services, and those services require one.

## Disclaimer

Android marks every simulated location as mock (`Location.isMock`). MockGPS is meant for testing your own apps. Do not use it to break the terms of other services.

## License

Apache License 2.0, see [LICENSE](LICENSE) and [NOTICE](NOTICE). Copyright 2026 Digital Solutions AI. The MockGPS and DSAI names and logos are not covered by the license.
