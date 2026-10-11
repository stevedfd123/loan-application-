#!/usr/bin/env node
/**
 * Static file server for the M Rupee loan app (local preview only).
 *
 * The app itself is a single index.html with no build step - the published
 * site is served by GitHub Pages. This server exists just so the app can be
 * previewed and the APK downloaded locally.
 *
 * Deliberately proxy-friendly:
 *   - HTTP/1.1 with keep-alive
 *   - permissive CORS / CORP headers so it can be framed in the preview
 *   - Range requests (so the .apk downloads properly)
 */
'use strict';

const http = require('http');
const fs = require('fs');
const path = require('path');
const url = require('url');

const ROOT = __dirname;
const PORT = Number(process.env.PORT) || 8080;

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.ico': 'image/x-icon',
  '.apk': 'application/vnd.android.package-archive',
  '.txt': 'text/plain; charset=utf-8',
  '.md': 'text/markdown; charset=utf-8',
};

function send(res, status, headers, body) {
  res.writeHead(status, headers);
  res.end(body || '');
}

function safeJoin(root, target) {
  const decoded = decodeURIComponent(String(target).split('?')[0].split('#')[0]);
  const resolved = path.normalize(path.join(root, decoded));
  if (resolved !== root && !resolved.startsWith(root + path.sep)) return null;
  return resolved;
}

const server = http.createServer((req, res) => {
  const started = Date.now();
  res.on('finish', function () {
    console.log(
      req.method + ' ' + req.url + ' -> ' + res.statusCode +
      '  (' + (res.getHeader('Content-Length') || '-') + 'B, ' +
      (Date.now() - started) + 'ms, host=' + (req.headers.host || '-') + ')'
    );
  });

  res.setHeader('Cache-Control', 'no-cache, no-store, must-revalidate');
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, HEAD, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', '*');
  res.setHeader('Cross-Origin-Resource-Policy', 'cross-origin');

  if (req.method === 'OPTIONS') return send(res, 204);
  if (req.method !== 'GET' && req.method !== 'HEAD') {
    return send(res, 405, { Allow: 'GET, HEAD, OPTIONS' }, 'Method Not Allowed');
  }

  let filePath = safeJoin(ROOT, url.parse(req.url).pathname || '/');
  if (!filePath) return send(res, 403, {}, 'Forbidden');

  let stat;
  try {
    stat = fs.statSync(filePath);
    if (stat.isDirectory()) {
      filePath = path.join(filePath, 'index.html');
      stat = fs.statSync(filePath);
    }
  } catch (e) {
    return send(res, 404, { 'Content-Type': 'text/plain; charset=utf-8' }, 'Not Found');
  }

  const size = stat.size;
  const headers = {
    'Content-Type': MIME[path.extname(filePath).toLowerCase()] || 'application/octet-stream',
    'Last-Modified': stat.mtime.toUTCString(),
    'Accept-Ranges': 'bytes',
    'Content-Length': size,
  };

  const range = req.headers.range;
  if (range) {
    const m = /bytes=(\d*)-(\d*)/.exec(range);
    if (m) {
      let start = m[1] === '' ? size - Number(m[2]) : Number(m[1]);
      let end = m[2] === '' ? size - 1 : Number(m[2]);
      start = Math.max(0, Math.min(start, size - 1));
      end = Math.max(start, Math.min(end, size - 1));
      headers['Content-Range'] = 'bytes ' + start + '-' + end + '/' + size;
      headers['Content-Length'] = end - start + 1;
      res.writeHead(206, headers);
      if (req.method === 'HEAD') return res.end();
      return fs.createReadStream(filePath, { start: start, end: end }).pipe(res);
    }
  }

  res.writeHead(200, headers);
  if (req.method === 'HEAD') return res.end();
  fs.createReadStream(filePath).pipe(res);
});

server.on('clientError', (err, socket) => {
  if (socket.writable) socket.end('HTTP/1.1 400 Bad Request\r\n\r\n');
});

server.keepAliveTimeout = 65000;
server.headersTimeout = 70000;

server.listen(PORT, '0.0.0.0', function () {
  console.log('M Rupee loan app preview -> http://0.0.0.0:' + PORT);
  console.log('  app : /index.html');
  console.log('  apk : /dist/loan-app-release.apk');
});
