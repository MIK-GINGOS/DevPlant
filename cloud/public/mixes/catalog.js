// mixes: the screens. Draws the catalogue and talks only to the API (the contract over HTTP).

const $ = id => document.getElementById(id);
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

let all = [];
let category = '';

async function start() {
  try {
    const res = await fetch('api/catalog');
    if (!res.ok) throw new Error(res.status);
    const data = await res.json();
    all = data.plants;
    $('count').textContent = `${data.count} plants · ${data.categories.length} categories`;
    chips(['', ...data.categories]);
    render();
    const key = new URLSearchParams(location.search).get('plant');
    if (key) open(key);
  } catch (e) {
    $('count').textContent = 'Could not load the catalogue. Try again later.';
  }
}

function chips(cats) {
  $('cats').innerHTML = cats.map(c =>
    `<button role="tab" data-cat="${esc(c)}" aria-selected="${c === category}">${esc(c || 'All')}</button>`).join('');
  $('cats').onclick = e => {
    const b = e.target.closest('button');
    if (!b) return;
    category = b.dataset.cat;
    for (const x of $('cats').children) x.setAttribute('aria-selected', String(x === b));
    render();
  };
}

function shown() {
  const words = $('q').value.toLowerCase().split(/\s+/).filter(Boolean);
  return all.filter(p => {
    if (category && p.category !== category) return false;
    const text = [p.name, p.scientific_name, p.category, ...p.problems.map(x => x.name)].join(' ').toLowerCase();
    return words.every(w => text.includes(w));
  });
}

function render() {
  const list = shown();
  $('grid').innerHTML = list.length ? list.map(p => `
    <button class="card" data-key="${esc(p.key)}">
      <img src="${esc(p.image)}" alt="${esc(p.name)}" loading="lazy" width="320" height="240">
      <div><b>${esc(p.name)}</b><small>${esc(p.category)}</small></div>
    </button>`).join('') : '<p class="empty">No plants match. Try another word or category.</p>';
}

function open(key) {
  const p = all.find(x => x.key === key);
  if (!p) return;
  $('detail-body').innerHTML = `
    <img class="photo" src="${esc(p.image)}" alt="${esc(p.name)}">
    <div class="head">
      <div><h2>${esc(p.name)}</h2><p class="sci soft">${esc(p.scientific_name)}</p></div>
      <button class="close" aria-label="Close">×</button>
    </div>
    <span class="pill">${esc(p.category)}</span>
    <div class="problems">${p.problems.map((x, i) => `
      <section class="problem">
        <h3><span>${i + 1}</span>${esc(x.name)}</h3>
        <p><span class="sig">Signs:</span> ${esc(x.signs)}</p>
        <p><span class="fix">Fix:</span> ${esc(x.treatment)}</p>
      </section>`).join('')}
    </div>
    <p class="credit soft">Photo: ${esc(p.image_license)}${p.image_author ? ', ' + esc(p.image_author) : ''} ·
      <a href="${esc(p.image_source)}" target="_blank" rel="noopener">${esc(p.image_source)}</a></p>`;
  $('detail-body').querySelector('.close').onclick = () => $('detail').close();
  $('detail').showModal();
  history.replaceState(null, '', `?plant=${encodeURIComponent(key)}`);
}

$('grid').onclick = e => {
  const card = e.target.closest('.card');
  if (card) open(card.dataset.key);
};
$('detail').addEventListener('close', () => history.replaceState(null, '', location.pathname));
$('detail').addEventListener('click', e => { if (e.target === $('detail')) $('detail').close(); });
$('q').addEventListener('input', render);

start();
