'use strict';
// Render the repository-native vector, not a real user's timetable or a copied image.
const fs=require('node:fs'),path=require('node:path'),sharp=require('sharp');
const root=path.resolve(__dirname,'../app/src/main/res');
const xml=fs.readFileSync(path.join(root,'drawable/week_widget_preview.xml'),'utf8');
const paths=[...xml.matchAll(/<path android:fillColor="([^"]+)" android:pathData="([^"]+)"/g)].map(m=>'<path fill="'+m[1]+'" d="'+m[2]+'"/>').join('');
if(!paths)throw Error('No native vector paths');
sharp(Buffer.from('<svg xmlns="http://www.w3.org/2000/svg" width="500" height="640" viewBox="0 0 250 320">'+paths+'</svg>')).png().toFile(path.join(root,'drawable-nodpi/week_widget_preview.png')).then(()=>console.log('Native weekly widget preview generated'));
