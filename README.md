# DevPlant

Smartphone-based early detection of crop pests and diseases. Photograph one leaf, and the
phone tells you whether it looks healthy, names the likely disease or pest, rates how severe
it is, and suggests treatments. Everything runs on the phone, offline.

> **Status: early preview (v0.1.1).** This build shows the architecture working end to end.
> No trained AI model is bundled yet: diagnosis uses a simple colour-based stand-in that can
> only tell "healthy", "leaf spot" and "yellowing" apart. Do not rely on it for real crops.

## Try it

Download `DevPlant-v0.1.1.apk` from the [Releases](../../releases) page and install it on an
Android 10+ phone (you may need to allow installing apps from your browser or file manager).

## Architecture

Four Gradle modules, with import rules enforced by the build:

| Module | Role | Depends on |
|---|---|---|
| `mixes` | The screens. Draws the interface and nothing else. | `contracts` |
| `contracts` | The abstraction layer: interfaces and plain shapes. | nothing |
| `app` | The bridge. Implements the contracts with the gates and picks which gate to use. | all three |
| `gates` | The tools: database, leaf photos, on-device AI model. Runs fully offline. | nothing |

`mixes` never touches `gates`; `gates` and `contracts` import nothing of ours; `app` is the
only place that knows both sides. The design decks are in the repo root
(`DevPlant_architecture (2).pptx`, `DevPlant_tree.pptx`).

### One scan, from leaf to diagnosis

1. **mm_scan**: the camera frames one leaf. The photo stays on the phone.
2. **mm_data `Store.scan`**: forwards the photo to `ScanStore`.
3. **app_rules `LeafPictures`**: turns it upright, crops it square, resizes it for the model.
4. **gates `gm_model`**: the on-device model returns top labels with confidence.
5. **app_rules `DiagnosisRules`**: picks pest or disease, severity and treatment; low
   confidence asks for a retake.
6. **app_db → gm_db**: scan and diagnosis rows are written; `Changes` refreshes History and Alerts.

The model is chosen in one line in `app/.../wiring/AppServices.java`:

```java
model = TfliteModel.open(context, "devplant_v1");
```

To use a trained model, put `devplant_v1.tflite` and `devplant_v1.labels.txt` (one label per
line, matching the guide keys such as `late_blight`, `fall_armyworm`, or `healthy`) in
`gates/src/main/assets/`.

### Screens (`mixes`)

`mm_shell` (header, pager, island) · `mm_theme` · `mm_scan` · `mm_result` · `mm_library` ·
`mm_history` · `mm_fields` · `mm_alerts` · `mm_dashboard` · `mm_search` · `mm_settings` · `mm_data`

### Database (`gates/gm_db`)

Room, 8 tables: Crop, Field, Scan, Diagnosis, Disease, Pest, Treatment, Alert. Seeded on first
run with 6 crops, 13 diseases, 7 pests and their treatments.

### Planned

`app_http` (sync, sending unsure scans to experts, regional outbreak alerts) · `app_security`
(sign-in) · `app_notify` (push alerts) · `gm_location` (GPS for fields).

## Build

Requires Android Studio (or the Android SDK) and JDK 21.

```sh
./gradlew :app:assembleDebug        # debug APK
./gradlew test                      # unit tests
```

Release builds are signed only when a `keystore.properties` file (not in the repo) provides
`storeFile`, `storePassword`, `keyAlias` and `keyPassword`.

## Disclaimer

Treatment advice is general. Confirm serious problems with a local agricultural extension
officer and always follow product labels for any chemical.
