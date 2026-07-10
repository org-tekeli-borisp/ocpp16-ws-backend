import { readdirSync, readFileSync, writeFileSync, rmSync, existsSync, statSync } from 'fs';
import { resolve } from 'path';

const outDir = resolve('../src/main/resources/META-INF/resources');
const srcHtml = readFileSync(resolve('./src/index.html'), 'utf-8');
const jsDir = resolve(outDir, 'js');
const assetsDir = resolve(outDir, 'assets');

// Find the newest index-* file by mtime (Vite just wrote it)
function findNewest(dir, prefix, ext) {
  if (!existsSync(dir)) return null;
  const files = readdirSync(dir).filter(f => f.startsWith(prefix) && f.endsWith(ext));
  if (files.length === 0) return null;
  if (files.length === 1) return files[0];
  // Pick the one with the newest mtime
  return files.reduce((a, b) =>
    statSync(resolve(dir, b)).mtimeMs > statSync(resolve(dir, a)).mtimeMs ? b : a
  );
}

const jsFile = findNewest(jsDir, 'index-', '.js');
const cssFile = findNewest(assetsDir, 'index-', '.css');

if (!jsFile) throw new Error('No index JS bundle found');

// Remove old bundles (stale hashes)
const allJsFiles = existsSync(jsDir) ? readdirSync(jsDir) : [];
for (const f of allJsFiles) {
  if (f !== jsFile) rmSync(resolve(jsDir, f), { force: true });
}
if (cssFile) {
  const allCssFiles = existsSync(assetsDir) ? readdirSync(assetsDir) : [];
  for (const f of allCssFiles) {
    if (f !== cssFile) rmSync(resolve(assetsDir, f), { force: true });
  }
}

// Write index.html with correct asset references
let html = srcHtml;
if (cssFile) {
  html = html.replace(
    '<script type="module" src="/src/main.ts"></script>',
    `<link rel="stylesheet" href="/assets/${cssFile}" />
    <script type="module" src="/js/${jsFile}"></script>`
  );
} else {
  html = html.replace(
    '<script type="module" src="/src/main.ts"></script>',
    `<script type="module" src="/js/${jsFile}"></script>`
  );
}

writeFileSync(resolve(outDir, 'index.html'), html);
console.log(`Created index.html → /js/${jsFile}${cssFile ? ' + /assets/' + cssFile : ''}`);
