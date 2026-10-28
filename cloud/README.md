# DevPlant cloud

The plant catalogue online: 600 plants, their photos and 4 common problems each. The Android
app downloads the catalogue from here once, keeps a copy, and loads each photo from here the
first time it is shown. After that, the catalogue works offline.

It is also the officer service (from the CropGuard design): phones send report details here
when the farmer agreed to share, and extension officers review them on `/officer/`.

It uses the same four parts as the app:

| Folder | Role | Knows |
|---|---|---|
| `public/` (mixes) | The web catalogue page: `index.html`, `mixes/` | only the API |
| `contracts/` | Plain shapes (Plant, Problem) and the CatalogStore contract | nothing |
| `app/` | The bridge: builds the CatalogStore from the gates | contracts, gates |
| `gates/` | The tools: the catalogue data file `gm_data/catalog.json` | nothing |
| `api/` | Vercel entry points, one file per address | app |

Photos are plain files in `public/images/`, served by Vercel's CDN.

## Addresses

| Address | Returns |
|---|---|
| `/` | Web catalogue: search, categories, plant details |
| `/api/catalog` | Everything in one response (what the phone downloads) |
| `/api/plants?q=blight&category=Herbs` | Search |
| `/api/plants/tomato` | One plant |
| `/api/health` | `{ ok, plants, categories }` |
| `/images/tomato.jpg` | A photo |
| `/officer/` | Officer dashboard: overview, possible hotspots, reports, questions (sign-in) |
| `POST /api/reports` | A phone sends report details (`X-Device-Key` header) |
| `GET /api/reports`, `/api/reports/12` | Officers: reports, filtered by country, district, status, days |
| `PATCH /api/reports/12` | Officers: `{ verification, note }`, which the farmer sees |
| `POST /api/questions` | A phone asks an officer |
| `GET /api/questions`, `PATCH /api/questions/7` | Officers: questions, and `{ answer }` |
| `GET /api/updates` | A phone fetches statuses, notes and answers for its own reports |
| `POST /api/officer`, `GET /api/officer` | Officer sign-in, and the overview numbers |

## When Vercel can't be reached

Some networks can't reach `*.vercel.app`. The same files are in the public GitHub repo, so the
web page and the iPhone app fall back to the jsDelivr copy of this folder:
`https://cdn.jsdelivr.net/gh/MIK-GINGOS/DevPlant@main/cloud` (data file
`gates/gm_data/catalog.json`, photos under `public/images/`). It follows `main`, so push
catalogue changes to GitHub too. jsDelivr may take up to 12 hours to pick them up.

## Deploy on Vercel

1. Push the repo to GitHub, `cloud/` folder included.
2. On vercel.com: **Add New → Project**, import the DevPlant repo.
3. Set **Root Directory** to `cloud`. Leave Framework Preset as **Other** and the build
   settings empty. Click **Deploy**.
4. Open `https://<your-project>.vercel.app/api/health`; it should say `"plants": 600`.
5. Put that address in the app's `gradle.properties`:
   `devplant.catalogUrl=https://<your-project>.vercel.app`
   Then rebuild the app and install it on the phone.

Every push to GitHub redeploys the site automatically.

## Reports and the officer dashboard

What a phone sends is text only: crop, country and district the farmer chose, result, how sure,
where the result came from, and the time. Never a photo or a GPS point. Each phone is known only
by a random key, stored as a hash. The three statuses (reported, under review, field verified)
stay apart, and a cluster is called a *possible* hotspot: 3 or more reports of the same threat
in one district within 14 days.

| Folder | Files |
|---|---|
| `contracts/` | `report.js`: the shapes, the checks every incoming record goes through, the ReportStore contract |
| `app/` | `report_store.js` (the store and the overview), `officer_auth.js` (sign-in), `report_http.js` |
| `gates/gm_reports/` | `postgres.js` (on Vercel) and `file.js` (on your computer) |
| `public/officer/` | the dashboard: `index.html`, `officer.js`, `officer.css` |

### Switching it on for Vercel

1. In the Vercel project, **Storage → Create → Neon (Postgres)** and connect it to this project.
   It sets `DATABASE_URL`. The tables are created on the first report.
2. **Settings → Environment Variables**, add:
   - `OFFICERS`: each officer and their access code, e.g.
     `Mary Banda=7hq2-river-lamp; John Phiri=k9fd-stone-moon` (long codes; give each officer theirs)
   - `SESSION_SECRET`: any long random text
3. Redeploy. Open `https://<your-project>.vercel.app/officer/` and sign in.

Removing an officer from `OFFICERS` locks them out at once. Without a database, report
addresses answer 503 and phones keep their reports queued; the catalogue is not affected.

Known limits for the pilot: anyone with the app's address could send made-up reports (there is
no farmer sign-in), and hotspots are counted from report volume, which depends on how many
farmers use DevPlant in a district.

## On your computer

Requires Node 20 or newer. `npm install` is only needed for Postgres; without `DATABASE_URL`,
reports go to `.data/reports.json` and the dashboard's test officer is "Test officer" with
code `devplant` (only on your computer, never on Vercel).

```sh
npm run dev      # http://localhost:3000, works like Vercel
npm run check    # every address answers and every photo exists
npm run check:reports   # the whole farmer-to-officer path, on a throwaway file
npm run import   # copy a new catalogue from ../plant_catalog (catalog.json and images)
```

## Photos

From Wikipedia and Wikimedia Commons. Each plant's licence, author and source link are in the
catalogue and shown on its card. Keep the credit when reusing them.
