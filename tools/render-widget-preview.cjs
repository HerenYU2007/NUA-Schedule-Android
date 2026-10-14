// Rasterize the existing vector artwork; no new visual design or personal course data.
const fs = require('node:fs');
const path = require('node:path');
const sharp = require('sharp');
const root = path.resolve(__dirname, '..');
const res = path.join(root, 'app/src/main/res');
const vector = fs.readFileSync(path.join(res, 'drawable/widget_picker_preview.xml'), 'utf8');
const attr = (text, name) => text.match(new RegExp('android:' + name + '="([^"]+)"'))?.[1];
const width = attr(vector, 'viewportWidth'), height = attr(vector, 'viewportHeight');
const shapes = [...vector.matchAll(/<path\b[\s\S]*?\/>/g)].map(([part]) => {
  let color = attr(part, 'fillColor');
  let alpha = 1;
  if (/^#[0-9a-f]{8}$/i.test(color)) { alpha = parseInt(color.slice(1, 3), 16) / 255; color = '#' + color.slice(3); }
  return `<path d="${attr(part, 'pathData')}" fill="${color}" fill-opacity="${alpha}"/>`;
}).join('');
const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="600" height="320" viewBox="0 0 ${width} ${height}">${shapes}</svg>`;
const output = path.join(res, 'drawable-nodpi/widget_picker_preview_bitmap.png');
fs.mkdirSync(path.dirname(output), {recursive: true});
sharp(Buffer.from(svg)).png().toFile(output).then(() => console.log('Rendered bitmap widget preview: 600x320'))
  .catch(error => { console.error(error.message); process.exitCode = 1; });
