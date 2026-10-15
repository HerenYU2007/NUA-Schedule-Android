'use strict';
// Preserve generated artwork without programmatic retouching.
const fs=require('node:fs'),path=require('node:path');
const root=path.resolve(__dirname,'..');
fs.mkdirSync(path.join(root,'reports'),{recursive:true});
fs.copyFileSync(path.join(root,'app/src/main/res/drawable-nodpi/ic_launcher_art.png'),path.join(root,'reports/app-icon-preview.png'));
console.log('Original artwork copied. Android applies a 10dp inset and the launcher mask.');
