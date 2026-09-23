# Onboarding playbook — build the user their own workout app

## YOUR OBJECTIVE (read this first)

Turn this repo into **the user's own personally-branded Android app, built and installed on
their physical Android phone.** Concretely, you will: (0) run the preflight script to
verify/auto-install the toolchain, (1) ask a few branding + program questions, (2) apply
their branding, (3) generate their `workouts.json`, (4) build and install to their phone.
Success = the app opens on their phone with their name, color, and workouts. Treat this
file as a script; run the steps **in order**; assume the user is **not** a developer, so do
the work for them and only ask what you truly need.

> ⚠️ **ANDROID ONLY.** This project builds an Android app and installs it via USB to an
> Android phone. There is **no iOS/web build**. If the user only has an iPhone, tell them
> plainly that this template can't target iOS yet, and stop.

Tested on **macOS** (Homebrew). On Linux, use the distro package manager for JDK 17 + the
Android SDK; on Windows, use WSL2. The helper scripts assume macOS paths.

There are helper scripts in `scripts/` — prefer them over hand-running commands:
`scripts/preflight.sh` (checks + auto-installs the SDK, writes `local.properties`, checks
the phone), `scripts/set_branding.sh "Name" "#RRGGBB"` (applies name + accent), and
`scripts/build_install.sh` (builds + installs + launches on the connected phone).

Do not invent workout programming or health claims. Build exactly what the user describes;
if they want a starter routine, keep it simple and conventional and tell them to adjust it.

---

## Step 0 — Preflight (do this first)

Run it — it checks the toolchain, auto-installs the Android SDK if missing, writes
`local.properties`, and checks the phone:

```bash
scripts/preflight.sh
```

Handle what it reports:
- **JDK 17 missing** → it can't self-install (the cask needs a password). Have the user run
  it themselves; in Claude Code they type it with a leading `!`:
  `!brew install --cask temurin@17` — then rerun preflight.
- **No phone** → ask them to enable Developer Options (Settings → About phone → tap "Build
  number" 7×) and **USB debugging**, plug in via USB, tap **Allow** on the phone, then
  rerun preflight.

Be honest about time: **a few minutes if the toolchain is already installed; ~15–20 minutes
the first time** (mostly the one-time installs).

---

## Step 1 — Ask the branding + program questions

Ask these (batch them, offer sensible defaults):

1. **App name?** (e.g. "Aisha's Workout") — becomes the launcher name and in-app title.
2. **Accent color?** Offer a few (green `#2BD576`, blue `#3B82F6`, orange `#F97316`,
   violet `#8B5CF6`, red `#EF4444`) or accept any hex.
3. **Units?** lbs (default) or kg.
4. **Their program.** Ask them to describe it in plain language — days/sessions, the
   exercises in each, sets/reps or hold times, and any warm-up/cooldown. **If they don't
   have one, offer to build a simple starter**: ask goals (how many days/week), equipment
   (dumbbells only? full gym? bodyweight?), and focus (full-body, upper/lower, etc.), then
   propose a plain, conventional routine and let them tweak it. Never present invented
   programming as expert advice — say it's a reasonable starting point to adjust.

---

## Step 2 — Apply the branding

Run:

```bash
scripts/set_branding.sh "Their App Name" "#RRGGBB"
```

It sets `app_name` in `strings.xml` (the in-app header reads it automatically) and the
accent (`AppGreen` + a darker `AppGreenDim`) in `ui/theme/Theme.kt`. Optionally also update
the launcher-icon accent in `app/src/main/res/values/colors.xml` (`accent_green`).

The **package id** stays `com.workout.tracker` for everyone — that's fine for a personal
sideloaded app. Only change it (advanced) if they want to publish to the Play Store or run
two different builds side-by-side on one phone.

---

## Step 3 — Generate their program → `app/src/main/assets/workouts.json`

Convert their description into that file following **`WORKOUTS_SCHEMA.md`** (read it). Key
rules:
- Structure: one program → workouts → sections (Warm-up / Main Exercise / Cooldown) →
  groups (lettered blocks like "A") → exercises.
- Set `tracksWeight: false` for warm-up, cooldown, mobility, core, and cardio (they become
  a simple "mark done" with no weight field); `true` for weighted lifts.
- Set `timeBased: true` for holds/stretches/timed work.
- Set `tracked: false` for any playful/non-exercise item the user wants (e.g. "post a gym
  selfie") — it shows as a fun note, not logged, not part of completion.
- `targetSets` = how many set rows to show (rounds); use the rep scheme to pick it.

The app imports this on first launch. (It's git-ignored so their personal program never
gets committed back to the template.)

---

## Step 4 — Build & install to their phone

```bash
scripts/build_install.sh
```

It builds, installs (update-in-place, so re-runs keep their logged history), targets the
one connected phone, and launches the app. Confirm it opens on the phone.

---

## Step 5 — Hand off

Tell them:
- Open the app → **Today** tab shows the next workout; **Start** runs it with a timer, and
  finishing logs it (green check on the calendar; stats grow over time).
- **Programs** tab is where they add/edit workouts later (the **Today** view is read-only).
- **Settings** (gear on Programs) has **Back up my data** — recommend they back up
  periodically and before switching phones. Data is stored locally on the phone only.

Re-running this flow just re-applies branding / regenerates the program and reinstalls;
use `./gradlew :app:installDebug` (not uninstall) so their logged history is preserved.

---

## Troubleshooting

- `adb devices` shows **unauthorized** → tap "Allow USB debugging" on the phone.
- `adb devices` shows nothing → check the cable/port; some cables are charge-only.
- **"Unable to locate a Java Runtime"** → JDK 17 isn't installed (Step 0); the cask needs
  their password.
- Gradle can't find the SDK → `local.properties` is missing or `sdk.dir` is wrong.
- Install fails with a signature mismatch → a build with a different signing key is already
  installed; `adb uninstall com.workout.tracker` then reinstall (this wipes local data, so
  export a backup first if they have history).
