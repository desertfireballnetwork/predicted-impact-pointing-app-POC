# Fireball Notification POC

Proof-of-concept Android app: simulate a fireball/re-entry event, check whether it's above your
horizon from your current GPS location, and get a compass/elevation HUD guiding you to point the
top edge of your phone at it, with a countdown to the event start.

See [PROGRESS.md](PROGRESS.md) for the decision log and milestone status, and the approved plan
this was built from for full design rationale.

## Build

No Android Studio required — command-line only. The Android SDK cmdline-tools, platform-tools,
`platforms;android-34`, and `build-tools;34.0.0` need to be installed first (already done on this
machine at `~/Android/Sdk`; `local.properties` points at it).

```bash
export ANDROID_HOME=~/Android/Sdk
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64   # any JDK 17+ works
./gradlew :app:testDebugUnitTest   # geometry unit tests
./gradlew :app:assembleDebug       # produces app/build/outputs/apk/debug/app-debug.apk
```

The debug APK is auto-signed with the debug keystore — no manual signing needed.

## Install on your phone (sideload, no Play Store)

**Option A — copy the file over:**
1. Copy `app/build/outputs/apk/debug/app-debug.apk` to your phone (USB transfer, cloud drive, email
   to yourself, whatever's easiest).
2. On your phone, open the file. Android will prompt to allow installing from this source the
   first time — allow it, then install.

**Option B — via USB with `adb`** (if you enable USB debugging in Developer Options):
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Using it

1. **Simulate** — fill in the start/end point (lat/lon/height) and timing of a fake event. Defaults
   are pre-filled with a plausible desert re-entry track so you can just tap "Simulate" to try it.
   "Start in" is seconds from now, not a wall-clock time — easier to test with outdoors.
2. **Countdown** — ticks down to the simulated start time. Tap "Show HUD now" to skip ahead for
   testing rather than waiting for the real countdown.
3. **HUD** — grant location permission when prompted. If the event is above your horizon (>1°
   elevation), you'll see the compass dial: rotate your body until the red target marker reaches
   the top of the ring (that's your current heading), and use the elevation gauge below it to tilt
   up/down. It turns green and says "ON TARGET" when you're within 5° on both axes. If the event
   isn't visible from your location, you'll see a text explanation instead of the dial.

## Known limitation

The HUD's pointing math has not yet been walk-tested against a real phone (this was built on a
headless dev machine with no device attached). Before trusting it outdoors, sanity-check it: hold
the phone with its top edge pointed at a known landmark or compass bearing (e.g. compare against a
regular compass app) and confirm the dial's numbers make sense.
