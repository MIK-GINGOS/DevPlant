// gates: the officer service (cloud/api/reports, questions, updates) over HTTPS, like the
// Android app's gm_http/JsonHttp. Throws when there is no signal or the server says no.

const TIMEOUT_MS = 15000;

export interface OfficerUpdates {
  reports: { phoneId: number; verification: string; note: string | null }[];
  questions: { phoneId: number; answer: string | null; answeredBy: string | null; answeredAt: string | null }[];
}

async function call<T>(url: string, deviceKey: string, body?: unknown): Promise<T> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), TIMEOUT_MS);
  try {
    const res = await fetch(url, {
      method: body === undefined ? 'GET' : 'POST',
      headers: { 'Content-Type': 'application/json', 'X-Device-Key': deviceKey },
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: controller.signal,
    });
    if (!res.ok) throw new Error(`HTTP ${res.status} for ${url}`);
    return (await res.json()) as T;
  } finally {
    clearTimeout(timer);
  }
}

/** Sends report details (never photos); returns the phone ids the server stored. */
export const sendReports = (baseUrl: string, deviceKey: string, reports: object[]) =>
  call<{ accepted: number[] }>(`${baseUrl}/api/reports`, deviceKey, { reports }).then(r => r.accepted);

export const sendQuestions = (baseUrl: string, deviceKey: string, questions: object[]) =>
  call<{ accepted: number[] }>(`${baseUrl}/api/questions`, deviceKey, { questions }).then(r => r.accepted);

export const fetchUpdates = (baseUrl: string, deviceKey: string) =>
  call<OfficerUpdates>(`${baseUrl}/api/updates`, deviceKey);
