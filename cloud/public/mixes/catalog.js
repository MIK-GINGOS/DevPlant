// mixes: the screens. Draws the catalogue and talks only to the API (the contract over HTTP).
// If the API can't be reached, it reads the same catalogue from the GitHub copy on jsDelivr.

const MIRROR = 'https://cdn.jsdelivr.net/gh/MIK-GINGOS/DevPlant@main/cloud';
const PAGE = 24;

const $ = id => document.getElementById(id);
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const arrow = '<span aria-hidden="true">→</span>';

let all = [];
let byKey = new Map();
let problems = new Map(); // problem key -> { name, signs, treatment, plants: [] }
let category = '';
let limit = PAGE;
let homeScroll = 0;

async function getJson(url, ms = 8000) {
  const ctrl = new AbortController();
  const timer = setTimeout(() => ctrl.abort(), ms);
  try {
    const res = await fetch(url, { signal: ctrl.signal });
    if (!res.ok) throw new Error(`${res.status}`);
    return await res.json();
  } finally {
    clearTimeout(timer);
  }
}

/** The catalogue in API shape, from the API or else from the raw data file on the mirror. */
async function loadCatalog() {
  try {
    return await getJson('api/catalog');
  } catch {
    const raw = await getJson(`${MIRROR}/gates/gm_data/catalog.json`, 15000);
    const diseases = new Map(raw.diseases.map(d => [d.key, d]));
    const plants = raw.plants.map(p => ({
      ...p,
      image_url: `${MIRROR}/public/${p.image}`,
      problems: p.diseases.map(k => ({ key: k, ...diseases.get(k) })),
    }));
    return { count: plants.length, categories: [...new Set(plants.map(p => p.category))], plants };
  }
}

// A photo that fails here (blocked or missing) is retried once from the mirror.
document.addEventListener('error', e => {
  const img = e.target;
  if (!(img instanceof HTMLImageElement) || img.dataset.retried) return;
  const path = img.getAttribute('src') || '';
  if (!path.startsWith('images/')) return;
  img.dataset.retried = '1';
  img.src = `${MIRROR}/public/${path}`;
}, true);

async function start() {
  try {
    const data = await loadCatalog();
    all = data.plants;
    byKey = new Map(all.map(p => [p.key, p]));
    for (const p of all) {
      for (const x of p.problems) {
        if (!problems.has(x.key)) problems.set(x.key, { ...x, plants: [] });
        problems.get(x.key).plants.push(p);
      }
    }
    $('stats').innerHTML = `<span><strong>${all.length}</strong> crop profiles</span>
      <span><strong>${problems.size}</strong> common problems</span>
      <span><strong>${data.categories.length}</strong> categories</span>`;
    chips(data.categories);
    render();
  } catch {
    $('stats').innerHTML = '<span>Could not load the catalogue. Check the connection and refresh.</span>';
    $('result-count').textContent = '';
    $('grid').innerHTML = '<p class="empty">The catalogue could not be loaded.</p>';
  }
  route();
}

function chips(cats) {
  const count = c => all.filter(p => p.category === c).length;
  $('cats').innerHTML = ['', ...cats].map(c =>
    `<button type="button" data-cat="${esc(c)}" aria-pressed="${c === category}">${esc(c || 'All crops')}<small>${c ? count(c) : all.length}</small></button>`).join('');
  $('cats').onclick = e => {
    const b = e.target.closest('button');
    if (!b) return;
    category = b.dataset.cat;
    for (const x of $('cats').children) x.setAttribute('aria-pressed', String(x === b));
    limit = PAGE;
    render();
  };
}

function shown() {
  const words = $('q').value.toLowerCase().split(/\s+/).filter(Boolean);
  return all.filter(p => {
    if (category && p.category !== category) return false;
    if (!words.length) return true;
    const text = [p.name, p.scientific_name, p.category, ...p.problems.map(x => x.name)].join(' ').toLowerCase();
    return words.every(w => text.includes(w));
  });
}

function card(p) {
  return `
    <a class="card" href="?plant=${encodeURIComponent(p.key)}" data-plant="${esc(p.key)}">
      <div class="card-photo">
        <img src="${esc(p.image)}" alt="${esc(p.name)}" loading="lazy" width="600" height="520">
        <span class="badge">${esc(p.category)}</span>
        <span class="open">Open crop profile ${arrow}</span>
      </div>
      <div class="card-body">
        <h3>${esc(p.name)}</h3>
        <p class="sci">${esc(p.scientific_name)}</p>
        <div class="tags">${p.problems.slice(0, 2).map(x => `<span class="tag">${esc(x.name)}</span>`).join('')}</div>
        <div class="card-foot"><span>${p.problems.length} common problems</span><b>View ${arrow}</b></div>
      </div>
    </a>`;
}

function render() {
  if (!all.length) return;
  const list = shown();
  $('result-count').textContent = list.length === all.length
    ? `Showing all ${all.length} crops`
    : `${list.length} ${list.length === 1 ? 'crop' : 'crops'} found`;
  $('grid').innerHTML = list.length
    ? list.slice(0, limit).map(card).join('')
    : '<p class="empty">No crops match. Try another word or category.</p>';
  $('more').hidden = list.length <= limit;
  $('more').innerHTML = `Show more crops (${list.length - limit} left) <span aria-hidden="true">↓</span>`;
}

function plantView(p) {
  document.title = `${p.name} · DevPlant Catalogue`;
  return `
    <button class="back" type="button" data-back>← Back to the library</button>
    <div class="panel">
      <div class="panel-photo">
        <img src="${esc(p.image)}" alt="${esc(p.name)}, ${esc(p.scientific_name)}">
        <div class="panel-caption"><p class="eyebrow">Crop profile</p><p>A closer look at ${esc(p.name.toLowerCase())}.</p></div>
      </div>
      <div class="panel-body">
        <div><span class="pill">${esc(p.category)}</span></div>
        <h1>${esc(p.name)}</h1>
        <p class="sci">${esc(p.scientific_name)}</p>
        <p class="lead">These are the problems farmers most often see on ${esc(p.name.toLowerCase())}. Tap one for the signs to look for and what to do.</p>
        <p class="eyebrow label">Common signs to watch</p>
        <div class="problems">${p.problems.map((x, i) => `
          <button class="problem" type="button" data-problem="${esc(x.key)}" data-from="${esc(p.key)}">
            <h3><span>0${i + 1}</span>${esc(x.name)}</h3>
            <p>${esc(x.signs)}</p>
            <span class="more-link">What to do ${arrow}</span>
          </button>`).join('')}
        </div>
        <p class="credit">Photo: ${esc(p.image_license)}${p.image_author ? ', ' + esc(p.image_author) : ''} ·
          <a href="${esc(p.image_source)}" target="_blank" rel="noopener">source</a>${p.wikipedia ? ` · <a href="${esc(p.wikipedia)}" target="_blank" rel="noopener">Wikipedia</a>` : ''}</p>
      </div>
    </div>`;
}

function problemView(x, from) {
  document.title = `${x.name} · DevPlant Catalogue`;
  const photo = from ?? x.plants[0];
  const others = x.plants.filter(p => p !== from);
  return `
    <button class="back" type="button" data-back>← Back to ${esc(from ? from.name : 'the library')}</button>
    <div class="panel">
      <div class="panel-photo">
        <img src="${esc(photo.image)}" alt="${esc(photo.name)}">
        <div class="panel-caption"><p class="eyebrow">Problem guide</p><p>A calmer way to read the signs.</p></div>
      </div>
      <div class="panel-body">
        <p class="eyebrow"><span class="dot" aria-hidden="true"></span> Start here</p>
        <h1>${esc(x.name)}</h1>
        <p class="lead">A visible change is the crop asking for attention, not a reason to panic. Confirm the signs, then take the first step.</p>
        <div class="boxes">
          <div class="box leaf"><span class="num">01</span><h2>Check the signs</h2><p>${esc(x.signs)}</p></div>
          <div class="box clay"><span class="num">02</span><h2>What to do</h2><p>${esc(x.treatment)}</p></div>
          <div class="box sky"><span class="num">03</span><h2>Watch and wait</h2><p>Give the crop a few days to respond before changing anything else.</p></div>
        </div>
        <div class="note">
          <p class="eyebrow">Before you spray</p>
          <p class="serif">Ask a local extension officer.</p>
          <p class="soft" style="margin-top:6px;font-size:14px">They know which products are allowed where you farm. Always follow the label.</p>
        </div>
        ${from ? `<div style="margin-top:32px"><button class="btn" type="button" data-back>Return to ${esc(from.name)} ${arrow}</button></div>` : ''}
      </div>
    </div>
    ${others.length ? `
    <section class="also">
      <p class="eyebrow">Also seen on</p>
      <h2 class="title">${others.length} other ${others.length === 1 ? 'crop' : 'crops'}</h2>
      <div class="grid">${others.map(card).join('')}</div>
    </section>` : ''}`;
}

// ---- simple routing on ?plant=key and ?problem=key&from=key ----

function route() {
  const params = new URLSearchParams(location.search);
  const plant = byKey.get(params.get('plant'));
  const problem = problems.get(params.get('problem'));
  const detail = $('detail');
  if (plant || problem) {
    detail.innerHTML = plant ? plantView(plant) : problemView(problem, byKey.get(params.get('from')));
    detail.hidden = false;
    $('home').hidden = true;
    window.scrollTo({ top: 0, behavior: 'instant' });
  } else {
    document.title = 'DevPlant Catalogue';
    const wasDetail = !detail.hidden;
    detail.hidden = true;
    detail.innerHTML = '';
    $('home').hidden = false;
    if (wasDetail) window.scrollTo({ top: homeScroll, behavior: 'instant' });
  }
}

function go(search) {
  if ($('detail').hidden) homeScroll = window.scrollY;
  history.pushState({ inApp: true }, '', search || location.pathname);
  route();
}

document.addEventListener('click', e => {
  if (e.defaultPrevented || e.button !== 0 || e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) return;
  const t = e.target.closest('[data-plant], [data-problem], [data-back], [data-home]');
  if (!t) return;
  if (t.dataset.plant) {
    e.preventDefault();
    go(`?plant=${encodeURIComponent(t.dataset.plant)}`);
  } else if (t.dataset.problem) {
    go(`?problem=${encodeURIComponent(t.dataset.problem)}&from=${encodeURIComponent(t.dataset.from)}`);
  } else if ('back' in t.dataset) {
    if (history.state?.inApp) history.back();
    else go('');
  } else if ('home' in t.dataset && !$('detail').hidden) {
    // Header links while a profile is open: back to the page, then to the section.
    e.preventDefault();
    const hash = new URL(t.href).hash;
    go('');
    if (hash) document.querySelector(hash)?.scrollIntoView();
    if ('focusSearch' in t.dataset) $('q').focus({ preventScroll: true });
  } else if ('focusSearch' in t.dataset) {
    setTimeout(() => $('q').focus({ preventScroll: true }), 300);
  }
});
window.addEventListener('popstate', route);

$('q').addEventListener('input', () => { limit = PAGE; render(); });
$('more').addEventListener('click', () => { limit += PAGE * 2; render(); });
$('hero-card').addEventListener('click', () => {
  $('q').value = 'yellow';
  category = '';
  for (const x of $('cats').children) x.setAttribute('aria-pressed', String(x.dataset.cat === ''));
  limit = PAGE;
  render();
  $('explore').scrollIntoView();
});

start();
