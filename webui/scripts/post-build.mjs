import { readdirSync, readFileSync, writeFileSync } from 'fs';
import { resolve } from 'path';

const outDir = resolve('../src/main/resources/META-INF/resources');
const srcHtml = readFileSync(resolve('./src/index.html'), 'utf-8');

const jsFiles = readdirSync(resolve(outDir, 'js'));
const cssFiles = readdirSync(resolve(outDir, 'assets'));

const jsFile = jsFiles.find(f => f.startsWith('index-') && f.endsWith('.js'));
const cssFile = cssFiles.find(f => f.startsWith('index-') && f.endsWith('.css'));

if (!jsFile) throw new Error('No index JS bundle found');

const html = srcHtml
  .replace('<script type="module" src="/src/main.ts"></script>',
    `<script type="module" src="/js/${jsFile}"></script>`);

writeFileSync(resolve(outDir, 'index.html'), html);
console.log(`Created index.html → /js/${jsFile}`);
