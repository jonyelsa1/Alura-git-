// Renderiza cada maqueta de src/ a capturas/ con Chromium (Playwright).
// Tamaño: 1280×800 px CSS a densidad 1,5 = 1920×1200, como la T65Max.
import { createRequire } from 'node:module';
// Playwright viene preinstalado globalmente; con npm i playwright también vale.
const { chromium } = createRequire(import.meta.url)('playwright');
import fs from 'node:fs';
import path from 'node:path';

const here = path.dirname(new URL(import.meta.url).pathname);
const iconDir = path.join(here, 'node_modules/lucide-static/icons');
const build = path.join(here, 'build');
fs.mkdirSync(build, { recursive: true });
fs.copyFileSync(path.join(here, 'src/common.css'), path.join(build, 'common.css'));

// {{i:nombre clase}} → SVG de Lucide en línea
const inline = (html) =>
  html.replace(/\{\{i:([a-z0-9-]+)(?:\s+([^}]*))?\}\}/g, (_, name, cls = '') =>
    fs.readFileSync(path.join(iconDir, `${name}.svg`), 'utf8')
      .replace(/<!--[\s\S]*?-->/g, '')
      .replace(/class="[^"]*"/, `class="ic ${cls}"`)
      .replace(/\s(width|height)="24"/g, '')
      .trim());

const pages = [
  { file: 'cristal.html', w: 1280, h: 800 },
  { file: 'cristal-cajon.html', w: 1280, h: 800 },
  { file: 'expresivo.html', w: 1280, h: 800 },
  { file: 'bento.html', w: 1280, h: 800 },
  { file: 'enfoque.html', w: 1280, h: 800 },
];
const only = process.argv.slice(2);

const browser = await chromium.launch();
for (const p of pages) {
  if (only.length && !only.some((o) => p.file.startsWith(o))) continue;
  const html = inline(fs.readFileSync(path.join(here, 'src', p.file), 'utf8'));
  const out = path.join(build, p.file);
  fs.writeFileSync(out, html);
  const page = await browser.newPage({ viewport: { width: p.w, height: p.h }, deviceScaleFactor: 1.5 });
  await page.goto('file://' + out);
  await page.evaluate(() => document.fonts.ready);
  await page.waitForTimeout(300);
  const png = path.join(here, 'capturas', p.file.replace('.html', '.png'));
  await page.screenshot({ path: png });
  console.log('ok', path.basename(png));
  await page.close();
}
await browser.close();
