# OpenWorkout — build your own workout app in minutes

A native Android workout tracker you make **yours** by talking to Claude Code. Point Claude
at this repo, answer a few questions (app name, color, your routine), and it builds a
personally-branded app and installs it on your Android phone. No account, no cloud — your
data lives on your phone, with a one-tap local backup.

> **📱 Android only.** This builds an Android app and installs it to an Android phone over
> USB. There is no iOS or web build. You'll need a Mac or Linux computer (tested on macOS)
> and an Android phone. If you only have an iPhone, this isn't for you yet.

**What you get**
- A **Today** view with your next workout, a live timer, and a completion **calendar**.
- A **player** that logs sets (weight × reps) for lifts and a simple "mark done" for
  warm-ups, core, mobility and cardio.
- Per-exercise **history** and **records**, plus lifetime stats (workouts, time, volume).
- A **Progress** tab: log body measurements (weight, waist, …) as trend charts, and see
  auto **training trends** (volume/duration per workout) from your logged sessions.
- Everything **on-device**; export/restore a backup file whenever you like.

## Quickstart

1. **Install the prerequisites** (one time). You need [Claude Code](https://claude.com/claude-code),
   a JDK, the Android SDK, and an Android phone with USB debugging. On macOS:
   ```sh
   brew install --cask temurin@17            # JDK (asks for your password)
   brew install --cask android-commandlinetools
   ```
   Don't worry about the details — Claude will check what's missing and walk you through it.
2. **Clone this repo** and open it in Claude Code:
   ```sh
   git clone <your-fork-url> && cd openworkout
   claude
   ```
3. **Prepare your phone** to accept the app over USB (see the next section).
4. **Say:** `set up my workout app` — Claude reads its onboarding playbook (`CLAUDE.md`),
   asks a few questions, builds your branded app, and installs it on your plugged-in phone.

⏱️ **Time:** a few minutes if the Android toolchain is already installed; ~15–20 minutes
the first time (mostly the one-time tool install).

## Prepare your phone (one-time, ~2 minutes)

The app installs over USB, so your phone has to allow it. This is the standard Android
"developer mode" — you are **not** rooting, unlocking the bootloader, or changing anything
permanent, and you don't need the Play Store.

1. **Turn on Developer Options:** Settings → **About phone** → tap **Build number** seven
   times (you'll see "You are now a developer").
2. **Turn on USB debugging:** Settings → **System → Developer options** → enable **USB
   debugging**. (On some phones it's under System → Advanced, or just search settings for
   "USB debugging".)
3. **Plug the phone into your computer** with a USB cable. Use a **data** cable — many cheap
   cables are charge-only. If it isn't detected, try a different cable or port.
4. **Allow the computer:** the phone pops up **"Allow USB debugging?"** showing your
   computer's fingerprint. Tap **Allow** and check **"Always allow from this computer."**
5. If asked, set the USB mode to **File Transfer** (not "Charging only").

**Verify it worked** — `adb devices` should list your phone as `device`:

```sh
$ adb devices
List of devices attached
A1B2C3D4E5F6     device
```

- `unauthorized` → you haven't tapped **Allow** on the phone yet (unlock it and look for the
  prompt).
- empty list → the cable/port isn't carrying data; swap the cable, then run
  `adb kill-server && adb start-server`.

Claude runs this check for you (`scripts/preflight.sh`) and tells you what's missing, but the
on-phone taps are yours to do. **macOS** works once `adb` is installed; on **Linux** you may
need a one-line `udev` rule for your phone's vendor and to be in the `plugdev` group.

## How it works

- `CLAUDE.md` is the onboarding script Claude follows (its objective, the questions, and the
  exact build steps).
- `scripts/` automates the mechanics: `preflight.sh` (checks + auto-installs the Android
  SDK, wires up `local.properties`, checks your phone), `set_branding.sh` (name + color),
  and `build_install.sh` (build + install + launch).
- `WORKOUTS_SCHEMA.md` documents the `workouts.json` format Claude generates from your
  routine (you can also hand-edit it).
- The app is Kotlin + Jetpack Compose with a local Room database. Tested on macOS; **Android
  only**. Requires JDK 17 and the Android SDK (platform 35).

## Notes

- **Bring your own program.** This template ships with **no** workout data — Claude builds
  your routine from what you describe (or a simple starter if you don't have one). Please
  don't commit programs copied from a paid coaching app; they're someone else's work.
- Weights, history, and completion are stored only on your device. Use **Settings → Back up
  my data** to save a copy (Drive/Files/email) and to move to a new phone.

## License

MIT — see [LICENSE](LICENSE).
