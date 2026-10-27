# DevPlant for iPhone (JavaScript / Expo)

The DevPlant app rewritten in JavaScript (TypeScript, React Native, Expo) so it runs on an
iPhone through the free **Expo Go** app. It has the same screens and rules as the Android app:
scan a leaf, diagnosis and advice, history, fields, outbreak alerts, the pest & disease guide,
and the 600-plant catalogue.

> **Early preview.** Like the Android build, diagnosis uses a simple colour-based stand-in that
> can only tell "healthy", "leaf spot" and "yellowing" apart. Do not rely on it for real crops.

## Start it (no coding needed)

You need a computer (Windows or Mac) and the iPhone.

1. On the iPhone, install **Expo Go** from the App Store.
2. On the computer, open this `mobile` folder and double-click:
   - Windows: **`START-WINDOWS.bat`**
   - Mac: **`START-MAC.command`** (if macOS blocks it: right-click → Open, or run
     `sh START-MAC.command` in Terminal)
3. The launcher installs Node.js if it is missing, installs the app's packages (first time only,
   a few minutes), then asks whether the iPhone is on the same Wi-Fi:
   - **Yes** – fastest.
   - **No** – uses a tunnel over the internet; works on any network.
4. A QR code appears. Open the iPhone **Camera**, point it at the code and tap the banner.
   DevPlant opens in Expo Go.

Keep the launcher window open while using the app; closing it stops the app. On Windows, click
**Allow** if the firewall asks about Node.js.

**Bun instead of Node.js?** The launchers use Bun automatically when Node.js is not installed but
Bun is. By hand: `bun install` then `bunx expo start`.

### If something goes wrong

- *"Project is incompatible with this version of Expo Go"*: update Expo Go from the App Store.
  This project uses Expo SDK 58.
- *The QR code opens but loading never finishes*: restart the launcher and choose **No** (tunnel).
- *Camera is black*: allow camera access for Expo Go in the iPhone's Settings.

## By hand (developers)

Requires Node.js 20.19 or newer.

```sh
npm install
npm start              # same Wi-Fi; scan the QR code with the iPhone
npm run start:tunnel   # any network
npm test               # leaf model and diagnosis rules
npm run typecheck
npm run lint
```

## Layout

The same four parts as the Android app, as folders under `src/`:

| Folder | Role | Android equivalent |
|---|---|---|
| `src/app/` | The shell (`index.tsx`: header, swipeable pages, control menu) and the detail screens | `mm_shell`, detail fragments |
| `src/mixes/` | The six main pages, the glass control menu, header, theme, shared UI | `mixes` |
| `src/contracts/` | Plain shapes shared by everything | `contracts` |
| `src/bridge/` | Diagnosis rules, the data store, the plant catalogue | `app` |
| `src/gates/` | Tools: guide data, leaf pixels, leaf model, photos, saved rows | `gates` |

- Scans, fields and alerts are saved on the phone (AsyncStorage); photos stay in the app's own
  folder.
- The plant catalogue ships inside the app (`src/gates/catalog.json`) so it works offline. On
  start it fetches a newer copy from the cloud address in `app.json` → `expo.extra.catalogUrl`,
  which also serves the photos. After the cloud catalogue changes, run `npm run sync-catalog`.
- Everything used is part of Expo Go, so no Xcode, Apple developer account or custom build is
  needed. To publish a standalone app later, use EAS Build (`npx eas-cli@latest build -p ios`).
