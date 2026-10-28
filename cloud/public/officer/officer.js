// mixes: the officer dashboard. Talks only to the API (/api/officer, /api/reports, /api/questions).
// Every piece of farmer text is put on the page as text, never as HTML.

const API = '../api';
const TOKEN_KEY = 'devplant.officer';
const STATUS = { REPORTED: 'Reported', UNDER_REVIEW: 'Under review', FIELD_VERIFIED: 'Field verified' };
const SOURCE = { SIMULATED: 'Simulated', AI_MODEL: 'AI result', EXPERT: 'Expert checked' };
const KIND = { HEALTHY: 'Healthy', DISEASE: 'Disease', PEST: 'Pest', DEFICIENCY: 'Deficiency', UNSURE: 'Unsure' };

const $ = id => document.getElementById(id);
let session = null;
let tab = 'overview';
let showAnswered = false;

// ---- small helpers ----

/** Builds an element: h('p', { class: 'x' }, 'text', child). Strings become text nodes. */
function h(tag, attrs = {}, ...children) {
  const el = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs)) {
    if (v == null || v === false) continue;
    if (k.startsWith('on')) el.addEventListener(k.slice(2), v);
    else el.setAttribute(k, v === true ? '' : v);
  }
  for (const c of children.flat()) if (c != null && c !== false) el.append(c instanceof Node ? c : String(c));
  return el;
}

const area = x => [x.district, x.country].filter(Boolean).join(', ') || 'Area not given';
const when = iso => new Date(iso).toLocaleString(undefined, { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' });
const pct = x => `${Math.round((x || 0) * 100)}%`;
const result = r => (r.uncertain ? 'Not recognised yet' : r.resultName || KIND[r.kind] || 'Not diagnosed');
const pill = v => h('span', { class: `o-pill ${v}` }, STATUS[v] || v);

function readToken() {
  try { return JSON.parse(sessionStorage.getItem(TOKEN_KEY)); } catch { return null; }
}
function writeToken(s) {
  try { s ? sessionStorage.setItem(TOKEN_KEY, JSON.stringify(s)) : sessionStorage.removeItem(TOKEN_KEY); } catch { /* private window */ }
}

async function api(path, { method = 'GET', body } = {}) {
  const headers = { 'Content-Type': 'application/json' };
  if (session) headers.Authorization = `Bearer ${session.token}`;
  const res = await fetch(`${API}${path}`, { method, headers, body: body ? JSON.stringify(body) : undefined });
  const data = await res.json().catch(() => ({}));
  if (res.status === 401 && session) {
    signOut('Your sign-in ran out. Please sign in again.');
    throw new Error('signed out');
  }
  if (!res.ok) throw new Error(data.error || `The server answered ${res.status}`);
  return data;
}

function filters() {
  const f = new FormData($('filters'));
  const q = new URLSearchParams();
  for (const k of ['country', 'district', 'days']) if (f.get(k)) q.set(k, f.get(k).trim());
  if (tab === 'reports' && f.get('verification')) q.set('verification', f.get('verification'));
  return q.toString();
}

function failed(panel, e) {
  if (e.message === 'signed out') return;
  panel.replaceChildren(h('p', { class: 'o-empty' }, `Could not load: ${e.message}`));
}

// ---- sign in ----

function showSignedIn() {
  $('signin').hidden = !!session;
  $('board').hidden = !session;
  $('who').hidden = !session;
  $('who-name').textContent = session ? session.name : '';
  if (session) show(tab);
}

function signOut(message = '') {
  session = null;
  writeToken(null);
  $('signin-error').textContent = message;
  showSignedIn();
}

$('signin-form').addEventListener('submit', async e => {
  e.preventDefault();
  const f = new FormData(e.target);
  $('signin-error').textContent = '';
  try {
    session = await api('/officer', { method: 'POST', body: { name: f.get('name'), code: f.get('code') } });
    writeToken(session);
    e.target.reset();
    showSignedIn();
  } catch (err) {
    $('signin-error').textContent = err.message;
  }
});
$('sign-out').addEventListener('click', () => signOut());

// ---- tabs ----

function show(name) {
  tab = name;
  for (const b of document.querySelectorAll('[data-tab]')) b.setAttribute('aria-selected', String(b.dataset.tab === name));
  for (const t of ['overview', 'reports', 'questions']) $(`tab-${t}`).hidden = t !== name;
  $('filters').hidden = name === 'questions';
  for (const el of document.querySelectorAll('[data-only]')) el.hidden = el.dataset.only !== name;
  ({ overview, reports, questions })[name]();
}

for (const b of document.querySelectorAll('[data-tab]')) b.addEventListener('click', () => show(b.dataset.tab));
$('filters').addEventListener('submit', e => { e.preventDefault(); show(tab); });

// ---- overview ----

function bars(items, empty) {
  if (!items.length) return h('p', { class: 'o-note' }, empty);
  const top = items[0].count;
  return h('ul', { class: 'o-bars' }, items.map(x => h('li', {},
    h('span', {}, x.name), h('b', {}, x.count),
    h('span', { class: 'o-bar', 'aria-hidden': 'true' }, h('i', { style: `width:${Math.max(4, (x.count / top) * 100)}%` })))));
}

async function overview() {
  const panel = $('tab-overview');
  panel.replaceChildren(h('p', { class: 'o-empty' }, 'Loading…'));
  try {
    const s = await api(`/officer?${filters()}`);
    updateOpenCount(s.openQuestions);
    const delay = s.medianSyncDelayHours == null ? null
      : s.medianSyncDelayHours < 1 ? 'under an hour' : `about ${Math.round(s.medianSyncDelayHours)} hours`;
    panel.replaceChildren(
      h('div', { class: 'o-metrics' },
        metric(`Reports, last ${s.days} days`, s.reports),
        metric('Need review', s.needsReview),
        metric('Field verified', s.fieldVerified),
        metric('Open questions', s.openQuestions)),
      h('div', { class: 'o-grid' },
        h('section', { class: 'o-panel' },
          h('h2', {}, 'Possible hotspots'),
          s.hotspots.length
            ? h('ul', { class: 'o-hot' }, s.hotspots.map(x => h('li', {},
              h('b', {}, x.threat), ` in ${x.area}: ${x.reports} reports`,
              x.verified ? `, ${x.verified} field verified` : ', none verified yet',
              h('small', { class: 'o-note', style: 'display:block' }, `Latest ${when(x.last)}`))))
            : h('p', { class: 'o-note' }, 'None right now.'),
          h('p', { class: 'o-note' },
            `A possible hotspot is ${s.hotspotRule.min} or more reports of the same threat in one district within ${s.hotspotRule.days} days. `
            + 'It is not a confirmed outbreak: check the field before acting on it.')),
        h('section', { class: 'o-panel' }, h('h2', {}, 'Reports by threat'), bars(s.byThreat, 'No threats reported in this period.'),
          s.uncertain ? h('p', { class: 'o-note' }, s.uncertain === 1
            ? '1 more was a photo DevPlant could not read.'
            : `${s.uncertain} more were photos DevPlant could not read.`) : null),
        h('section', { class: 'o-panel' }, h('h2', {}, 'Reports by area'), bars(s.byArea, 'No reports in this period.'),
          h('p', { class: 'o-note' },
            'Counts show how many farmers sent a report, not how much of the crop is affected. '
            + (delay ? `Reports usually arrive ${delay} after the scan, because phones send them once they have signal.` : '')))),
    );
  } catch (e) {
    failed(panel, e);
  }
}

const metric = (label, value) => h('div', { class: 'o-metric' }, h('span', {}, label), h('b', {}, value));

function updateOpenCount(n) {
  $('open-count').hidden = !n;
  $('open-count').textContent = n || '';
}

// ---- reports ----

async function reports() {
  const panel = $('tab-reports');
  panel.replaceChildren(h('p', { class: 'o-empty' }, 'Loading…'));
  try {
    const { reports: list } = await api(`/reports?${filters()}`);
    if (!list.length) return panel.replaceChildren(h('p', { class: 'o-empty' }, 'No reports match these filters.'));
    panel.replaceChildren(h('div', { class: 'o-table-wrap' }, h('table', {},
      h('thead', {}, h('tr', {}, ['Scanned', 'Crop', 'Result', 'Area', 'Status'].map(t => h('th', { scope: 'col' }, t)))),
      h('tbody', {}, list.map(r => {
        const row = h('tr', { tabindex: '0', onclick: () => openReport(r.id), onkeydown: e => e.key === 'Enter' && openReport(r.id) },
          h('td', {}, when(r.createdAt)),
          h('td', {}, r.crop || '—'),
          h('td', {}, result(r), h('small', {}, r.uncertain
            ? `Photo not readable · ${SOURCE[r.source] || r.source}`
            : `${KIND[r.kind] || r.kind} · ${pct(r.confidence)} sure · ${SOURCE[r.source] || r.source}`)),
          h('td', {}, area(r)),
          h('td', {}, pill(r.verification)));
        return row;
      })))),
    h('p', { class: 'o-note' }, `${list.length} reports. Select one to review it.`));
  } catch (e) {
    failed(panel, e);
  }
}

async function openReport(id) {
  const dialog = $('detail');
  let r;
  try {
    r = await api(`/reports/${id}`);
  } catch (e) {
    if (e.message !== 'signed out') alert(e.message);
    return;
  }
  const facts = [
    ['Scanned', when(r.createdAt)], ['Received', when(r.receivedAt)], ['Crop', r.crop || '—'], ['Area', area(r)],
    ['Result', result(r)], ['Type', KIND[r.kind] || r.kind], ['How sure', r.uncertain ? '—' : pct(r.confidence)],
    ['Result from', SOURCE[r.source] || r.source],
  ];
  if (r.uncertainReason) facts.push(['Why unsure', r.uncertainReason]);
  if (r.updatedBy) facts.push(['Last set by', `${r.updatedBy}, ${when(r.updatedAt)}`]);

  const note = h('textarea', { name: 'note', maxlength: '1000', placeholder: 'For example: Seen in the field. Pull out and burn infected plants.' }, r.note || '');
  const error = h('p', { class: 'o-error', role: 'alert' });
  const form = h('form', { class: 'o-dialog-in', method: 'dialog' },
    h('h2', { id: 'detail-title' }, result(r)),
    h('dl', { class: 'o-facts' }, facts.flatMap(([k, v]) => [h('dt', {}, k), h('dd', {}, v)])),
    h('p', { class: 'o-note' }, 'The photo stays on the farmer\'s phone. Ask the farmer if you need to see it.'),
    h('fieldset', { class: 'o-status', style: 'border:0;padding:0;margin:0' },
      h('legend', { style: 'font-weight:600;font-size:14px;margin-bottom:6px' }, 'Status the farmer sees'),
      Object.entries(STATUS).map(([v, label]) => h('label', {},
        h('input', { type: 'radio', name: 'verification', value: v, checked: v === r.verification }), label))),
    h('label', {}, 'Note to the farmer (optional)', note),
    error,
    h('div', { class: 'o-actions' },
      h('button', { class: 'btn', type: 'submit', value: 'save' }, 'Save'),
      h('button', { class: 'btn ghost', type: 'button', onclick: () => dialog.close() }, 'Close')));

  form.addEventListener('submit', async e => {
    e.preventDefault();
    const verification = new FormData(form).get('verification');
    try {
      await api(`/reports/${id}`, { method: 'PATCH', body: { verification, note: note.value } });
      dialog.close();
      show(tab);
    } catch (err) {
      error.textContent = err.message;
    }
  });
  dialog.replaceChildren(form);
  dialog.showModal();
}

// ---- questions ----

async function questions() {
  const panel = $('tab-questions');
  panel.replaceChildren(h('p', { class: 'o-empty' }, 'Loading…'));
  try {
    const { questions: list } = await api(`/questions${showAnswered ? '' : '?open=1'}`);
    if (!showAnswered) updateOpenCount(list.length);
    const toggle = h('label', { style: 'display:flex;align-items:center;gap:8px;font-weight:500;margin-bottom:14px' },
      h('input', { type: 'checkbox', checked: showAnswered, onchange: e => { showAnswered = e.target.checked; questions(); } }),
      'Show answered questions too');
    if (!list.length) {
      return panel.replaceChildren(toggle, h('p', { class: 'o-empty' }, showAnswered ? 'No questions yet.' : 'No open questions.'));
    }
    panel.replaceChildren(toggle, h('div', { class: 'o-questions' }, list.map(question)));
  } catch (e) {
    failed(panel, e);
  }
}

function question(q) {
  const about = [q.crop, area(q), when(q.askedAt)].filter(Boolean).join(' · ');
  const card = h('article', { class: 'o-panel o-question' },
    h('h3', {}, about),
    h('p', {}, q.text),
    q.reportId ? h('button', { class: 'btn ghost small', type: 'button', onclick: () => openReport(q.reportId) }, 'See the report it is about') : null);
  if (q.answer) {
    card.append(h('p', { class: 'o-answer' }, q.answer, h('small', { class: 'o-note', style: 'display:block' }, `${q.answeredBy}, ${when(q.answeredAt)}`)));
    return card;
  }
  const answer = h('textarea', { maxlength: '2000', placeholder: 'Your answer. The farmer sees it in DevPlant the next time their phone has signal.' });
  const error = h('p', { class: 'o-error', role: 'alert' });
  const send = h('button', { class: 'btn small', type: 'button' }, 'Send answer');
  send.addEventListener('click', async () => {
    if (!answer.value.trim()) return void (error.textContent = 'Write an answer first.');
    send.disabled = true;
    try {
      await api(`/questions/${q.id}`, { method: 'PATCH', body: { answer: answer.value } });
      questions();
    } catch (e) {
      error.textContent = e.message;
      send.disabled = false;
    }
  });
  card.append(h('label', {}, 'Answer', answer), error, h('div', { class: 'o-actions' }, send));
  return card;
}

// ---- start ----

session = readToken();
showSignedIn();
