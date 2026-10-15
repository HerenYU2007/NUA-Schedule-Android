'use strict';
// Repo-native preview artwork; optional installed font is read only, never redistributed.
const sharp=require('sharp'),path=require('node:path');
const out=path.resolve(__dirname,'../app/src/main/res/drawable-nodpi'),fontfile=process.argv[2];
const svg=(w,h,shapes)=>Buffer.from('<svg xmlns="http://www.w3.org/2000/svg" width="'+w+'" height="'+h+'"><rect width="100%" height="100%" rx="28" fill="#ffffff" stroke="#d9e2e9" stroke-width="3"/>'+shapes+'</svg>');
async function render(w,h,shapes,labels,file){
 const overlays=[];
 for(const [left,top,text,size=22,color='#213547'] of labels){
  const png=await sharp({text:{text:'<span foreground="'+color+'">'+text+'</span>',font:'Microsoft YaHei '+size,...(fontfile?{fontfile}:{}),rgba:true,dpi:72}}).png().toBuffer();
  overlays.push({input:png,left,top});
 }
 await sharp(svg(w,h,shapes)).composite(overlays).png().toFile(path.join(out,file));
}
let daily='',dailyLabels=[[28,24,'今日列表 · 白色示意',23]];
[90,200].forEach((y,i)=>{
 daily+='<rect x="24" y="'+y+'" width="452" height="94" rx="15" fill="#f0f5fa"/>';
 dailyLabels.push([42,y+16,i?'14:00—15:40':'08:55—11:40',24,'#255a84'],[275,y+18,'课程名称',21],[42,y+57,'教室地点',19,'#536b7b'],[383,y+57,'未开始',18,'#255a84']);
});
let weekly='',weekLabels=[[28,24,'竖条周课表 · 白色示意',23],[8,72,'节次',13]];
for(let day=0;day<7;day++)weekLabels.push([65+day*63,72,'一二三四五六日'[day],18]);
for(let row=0;row<12;row++){weekly+='<path d="M44 '+(112+row*41)+'H480" stroke="#dce6ef"/>';weekLabels.push([12,124+row*41,String(row+1),12,'#536b7b']);}
[[0,0,4,'#8bcae8'],[1,1,3,'#f2b7c7'],[2,0,2,'#99d6d1'],[3,4,3,'#c4b4f0'],[4,1,3,'#eac28b'],[0,5,3,'#eca891']].forEach(([day,row,length,color])=>{
 const x=48+day*63,y=114+row*41;
 weekly+='<rect x="'+x+'" y="'+y+'" width="57" height="'+(length*41-4)+'" rx="8" fill="'+color+'"/>';
 weekLabels.push([x+6,y+12,'课程',17],[x+6,y+39,'教室',14]);
});
Promise.all([render(500,320,daily,dailyLabels,'widget_picker_preview_white_bitmap.png'),render(500,640,weekly,weekLabels,'week_widget_preview_white.png')]).then(()=>console.log('Chinese white widget previews rendered from code; no real schedule data.'));
