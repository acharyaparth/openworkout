# `workouts.json` schema

The app seeds itself on first launch from `app/src/main/assets/workouts.json`. During setup,
Claude generates this file from the user's plain-language description. It is imported into a
local database; after that, edits happen in-app (this file is only the initial seed).

## Shape

```jsonc
{
  "program": "My Program",          // the block / program name shown on the Programs tab
  "workouts": [
    {
      "num": 1,                      // order + sequence position (1-based); "up next" cycles through these
      "name": "W01 · Legs",          // shown in lists; prefix like "W01 · " keeps repeats legible
      "title": "Legs",               // short title (optional)
      "sections": [
        {
          "name": "Warm-up",         // typically Warm-up / Main Exercise / Cooldown
          "groups": [
            {
              "label": "",           // lettered block label, e.g. "A" (blank for a simple list)
              "scheme": "Mobility",  // e.g. "5 Rounds", "4 Giant Sets", "Core", "Mobility", "Stretch"
              "note": "",            // optional coaching note shown under the block
              "exercises": [
                {
                  "label": "A",             // "A" for warm-up/cooldown items; "A1","A2" inside a lettered block
                  "name": "Shoulder Circles",
                  "prescription": "10 shoulder rotations",  // free text: reps, "10 each side", "40 secs", etc.
                  "targetSets": 1,          // number of set rows to show (rounds). 1 for warm-up/cooldown.
                  "timeBased": false,       // true for holds/stretches/timed work (label shows SECS not REPS)
                  "tracksWeight": false,    // false = "mark done" only, no weight field
                  "tracked": true           // false = a "just for fun" note (no logging, no done-gating)
                }
              ]
            }
          ]
        }
      ]
    }
  ]
}
```

## Field rules

- **`tracksWeight`**: `true` for weighted lifts (shows the SET / weight / REPS table). `false`
  for warm-up, cooldown, mobility, core, and cardio — these render as a single **Mark done**
  toggle with no weight. When in doubt for bodyweight movements, use `false`.
- **`timeBased`**: `true` for anything held/timed (planks, wall sits, stretches, cardio
  intervals). The reps column is labeled **SECS** instead of **REPS**.
- **`targetSets`**: how many set rows appear. Derive from the scheme — "5 Rounds" → 5,
  "3 sets" → 3, a "20-10-5" rep ladder → 3. Warm-up/cooldown items → 1.
- **`tracked`**: `true` for real work. Set `false` only for playful/non-exercise items
  (e.g. "post a gym selfie") — they show as a fun note with no checkbox and don't count
  toward completion. Default `true`; omit unless it's a fun item.
- **`num`** drives both display order and the rotation. "Up next" on the home screen is the
  next workout in `num` order after the last one you completed, looping back to the first.
- Weights are entered and stored in the user's chosen unit; the lbs/kg toggle is lossless.

## Minimal example

```json
{
  "program": "Starter",
  "workouts": [
    {
      "num": 1, "name": "W01 · Full Body A", "title": "Full Body A",
      "sections": [
        { "name": "Warm-up", "groups": [
          { "label": "", "scheme": "Mobility", "note": "", "exercises": [
            { "label": "A", "name": "Arm Circles", "prescription": "10 each way", "targetSets": 1, "timeBased": false, "tracksWeight": false },
            { "label": "B", "name": "Bodyweight Squat", "prescription": "15", "targetSets": 1, "timeBased": false, "tracksWeight": false }
          ] }
        ] },
        { "name": "Main Exercise", "groups": [
          { "label": "A", "scheme": "3 Sets", "note": "", "exercises": [
            { "label": "A1", "name": "Goblet Squat", "prescription": "8-10", "targetSets": 3, "timeBased": false, "tracksWeight": true },
            { "label": "A2", "name": "Dumbbell Bench Press", "prescription": "8-10", "targetSets": 3, "timeBased": false, "tracksWeight": true }
          ] }
        ] },
        { "name": "Cooldown", "groups": [
          { "label": "", "scheme": "Stretch", "note": "Hold 30s each", "exercises": [
            { "label": "A", "name": "Hamstring Stretch", "prescription": "30 secs", "targetSets": 1, "timeBased": true, "tracksWeight": false }
          ] }
        ] }
      ]
    }
  ]
}
```
