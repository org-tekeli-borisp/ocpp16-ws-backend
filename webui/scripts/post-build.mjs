import { readdirSync, readFileSync, writeFileSync, rmSync, existsSync } from 'fs';
import { resolve } from 'path';

const outDir = resolve('../src/main/resources/META-INF/resources');
const srcHtml = readFileSync(resolve('./src/index.html'), 'utf-8');
const jsDir = resolve(outDir, 'js');
const assetsDir = resolve(outDir, 'assets');

// Read current files (Vite already wrote the new ones)
const allJsFiles = existsSync(jsDir) ? readdirSync(jsDir) : [];
const allCssFiles = existsSync(assetsDir) ? readdirSync(assetsDir) : [];

// Find the new index-* files
const jsFile = allJsFiles.find(f => f.startsWith('index-') && f.endsWith('.js'));
const cssFile = allCssFiles.find(f => f.startsWith('index-') && f.endsWith('.css'));

if (!jsFile) throw new Error('No index JS bundle found');

// Remove old bundles (stale hashes)
for (const f of allJsFiles) {
  if (f !== jsFile) rmSync(resolve(jsDir, f), { force: true });
}
if (cssFile) {
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
