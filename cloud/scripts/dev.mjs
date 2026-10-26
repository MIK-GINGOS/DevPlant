// Local stand-in for Vercel: serves public/ and runs api/*.js the same way. npm run dev
import { createServer } from 'node:http';
import { readFile, stat } from 'node:fs/promises';
import { join, extname, dirname } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const root = dirname(dirname(fileURLToPath(import.meta.url)));
const port = Number(process.env.PORT || 3000);
const types = { '.html': 'text/html', '.css': 'text/css', '.js': 'text/javascript', '.json': 'application/json', '.jpg': 'image/jpeg', '.png': 'image/png' };

createServer(async (req, res) => {
  try {
    const url = new URL(req.url, `http://${req.headers.host}`);
    // same rewrite as vercel.json
    const one = url.pathname.match(/^\/api\/plants\/([^/]+)$/);
    if (one) { url.pathname = '/api/plants'; url.searchParams.set('key', one[1]); }

    if (url.pathname.startsWith('/api/')) {
      const file = join(root, 'api', url.pathname.slice(5) + '.js');
      const mod = await import(pathToFileURL(file).href);
      const response = await mod.GET(new Request(url, { headers: req.headers }));
      res.writeHead(response.status, Object.fromEntries(response.headers));
      res.end(Buffer.from(await response.arrayBuffer()));
      return;
    }
    let path = join(root, 'public', decodeURIComponent(url.pathname));
    if ((await stat(path).catch(() => null))?.isDirectory()) path = join(path, 'index.html');
    const body = await readFile(path);
    res.writeHead(200, { 'Content-Type': types[extname(path)] || 'application/octet-stream' });
    res.end(body);
  } catch (e) {
    res.writeHead(e.code === 'ENOENT' || e.code === 'ERR_MODULE_NOT_FOUND' ? 404 : 500, { 'Content-Type': 'text/plain' });
    res.end(String(e.message || e));
  }
}).listen(port, () => console.log(`DevPlant cloud on http://localhost:${port}`));
