# DevPlant cloud

The plant catalogue online: 600 plants, their photos and 4 common problems each. The Android
app downloads the catalogue from here once, keeps a copy, and loads each photo from here the
first time it is shown. After that, the catalogue works offline.

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

## On your computer

Requires Node 20 or newer. Nothing to install.

```sh
npm run dev      # http://localhost:3000, works like Vercel
npm run check    # every address answers and every photo exists
npm run import   # copy a new catalogue from ../plant_catalog (catalog.json and images)
```

## Photos

From Wikipedia and Wikimedia Commons. Each plant's licence, author and source link are in the
catalogue and shown on its card. Keep the credit when reusing them.
