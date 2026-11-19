import fs from 'node:fs';
import path from 'node:path';
import {deflateSync} from 'node:zlib';

// Original 16px artwork, authored as pixel geometry like the existing node models.
// No Minecraft bitmap is copied, recolored or embedded by this generator.
const palettes={copper:['#302633','#78504b','#bf805c','#f0bd88'],gold:['#33283b','#8a6833','#d1a84c','#ffedac'],netherite:['#191d2c','#3d4056','#73738e','#bbb9d0']};
function rgba(hex){return [...hex.replace('#','').match(/../g).map(x=>parseInt(x,16)),255];}
class Pixels {
 constructor(w=16,h=16){this.w=w;this.h=h;this.data=Buffer.alloc(w*h*4);}
 set(x,y,color){if(x>=0&&y>=0&&x<this.w&&y<this.h)this.data.set(typeof color==='string'?rgba(color):color,(y*this.w+x)*4);}
 rect(x,y,w,h,c){for(let j=y;j<y+h;j++)for(let i=x;i<x+w;i++)this.set(i,j,c);return this;}
 line(x,y,X,Y,c){const n=Math.max(Math.abs(X-x),Math.abs(Y-y));for(let i=0;i<=n;i++)this.set(Math.round(x+(X-x)*i/(n||1)),Math.round(y+(Y-y)*i/(n||1)),c);return this;}
}
function crc(buf){let c=0xffffffff;for(const v of buf){c^=v;for(let i=0;i<8;i++)c=(c>>>1)^((c&1)?0xedb88320:0);}return (c^0xffffffff)>>>0;}
function chunk(type,data){const head=Buffer.from(type),len=Buffer.alloc(4),sum=Buffer.alloc(4);len.writeUInt32BE(data.length);sum.writeUInt32BE(crc(Buffer.concat([head,data])));return Buffer.concat([len,head,data,sum]);}
function png(p){const ihdr=Buffer.alloc(13);ihdr.writeUInt32BE(p.w);ihdr.writeUInt32BE(p.h,4);ihdr[8]=8;ihdr[9]=6;const rows=[];for(let y=0;y<p.h;y++)rows.push(Buffer.from([0]),p.data.subarray(y*p.w*4,(y+1)*p.w*4));return Buffer.concat([Buffer.from([137,80,78,71,13,10,26,10]),chunk('IHDR',ihdr),chunk('IDAT',deflateSync(Buffer.concat(rows))),chunk('IEND',Buffer.alloc(0))]);}
function gem(stable=false){const p=new Pixels();for(let y=1;y<15;y++){const half=Math.max(0,Math.floor((7-Math.abs(y-7))/1.7));for(let x=7-half;x<=8+half;x++)p.set(x,y,x===7-half||x===8+half?'#193652':x<8?'#75e4fa':x<10?'#32a9d4':'#2869a6');}p.line(7,2,5,7,'#d5ffff').line(7,3,8,10,'#a0f8ff').line(8,10,7,13,'#2e7ba6');if(stable){p.rect(3,9,2,4,'#216b94').rect(4,8,1,4,'#8af5ff').rect(10,7,2,5,'#4cbbe2').rect(11,6,1,3,'#c6ffff').line(3,13,11,13,'#4584a5');}else{p.set(2,4,'#75e4fa');p.set(12,3,'#c6ffff');p.set(12,12,'#4cbbe2');}return p;}
function container(theme){const p=new Pixels(),c=palettes[theme]??palettes.netherite;p.rect(5,1,6,2,c[0]).rect(6,0,4,1,c[2]).rect(4,3,8,1,c[0]).rect(3,4,10,9,c[0]).rect(4,4,8,9,'#286179').rect(5,4,6,8,'#64d6e5').rect(6,5,4,6,'#299bbc').rect(4,5,1,6,'#b8f7f5').rect(5,6,1,4,'#d8ffff').rect(3,12,10,2,c[1]).rect(4,14,8,1,c[0]).rect(4,12,8,1,c[3]).rect(4,3,8,2,c[2]).rect(5,3,6,1,c[3]).rect(7,7,3,3,c[0]).rect(8,7,1,3,'#bffbff');if(theme==='creative')for(let y=7;y<10;y++)for(let x=7;x<10;x++)p.set(x,y,((x>>1)+(y>>1))%2?'#d532f0':'#21122c');return p;}
function tool(kind){const p=new Pixels();if(kind==='tuner'){p.line(3,14,10,7,'#26324b').line(4,14,11,7,'#c38c5c').line(5,13,11,7,'#f2c995');p.rect(8,3,5,5,'#30314d').rect(9,2,1,5,'#81dcee').rect(12,1,1,6,'#81dcee').rect(10,6,2,2,'#5caad6').set(10,3,'#dfbbff');}
 else if(kind==='receiver'){p.rect(4,2,8,11,'#253244').rect(3,4,10,7,'#bb8a5c').rect(4,3,8,9,'#e9c18c').rect(5,4,6,7,'#294b69').rect(6,5,4,5,'#61c9e6').rect(7,6,2,3,'#d2ffff').rect(6,0,4,2,'#8e77b8').rect(6,13,4,2,'#755441');}
 else {for(let y=3;y<13;y++){let half=6-Math.abs(8-y);for(let x=7-half;x<=8+half;x++)p.set(x,y,(x+y)%3?'#8f74c9':'#e2ccff');}p.rect(5,5,6,6,'#243048').rect(6,6,4,4,'#59c8d4').rect(7,5,2,6,'#14293d').rect(7,6,1,2,'#c2ffff');p.set(3,2,'#a58cda');p.set(12,13,'#a58cda');}return p;}
function wand(creative){const p=new Pixels();p.line(2,14,11,5,'#251e36').line(3,14,12,5,creative?'#73738e':'#895c59').line(4,13,12,5,creative?'#bbb9d0':'#d6ac79').rect(9,2,5,5,'#25324d').rect(10,1,3,5,'#86e4f5').rect(11,1,1,3,'#edffff').rect(9,6,4,1,'#f0cc89');if(creative)for(let y=5;y<8;y++)for(let x=8;x<14;x++)if(p.data[(y*16+x)*4+3])p.set(x,y,((x>>1)+(y>>1))%2?'#da41f5':'#24152f');return p;}
function frame(theme){const p=new Pixels(),c=palettes[theme];for(let y=0;y<16;y++)for(let x=0;x<16;x++)p.set(x,y,c[(x===0||y===0)?3:(x===15||y===15)?0:(x+y)%7===0?2:1]);p.rect(2,2,12,1,c[2]).rect(2,3,1,10,c[2]).rect(3,12,10,1,c[0]);for(const[x,y]of[[2,2],[13,2],[2,13],[13,13]])p.set(x,y,c[3]);return p;}
function core(formed){const p=new Pixels();for(let y=2;y<14;y++)for(let x=2;x<14;x++){const d=Math.abs(x-7.5)+Math.abs(y-7.5);if(d<8)p.set(x,y,d>6?'#34435b':d>4?'#d1b989':formed?'#4fbedc':'#697486');}p.line(5,5,9,5,'#efffff').line(5,6,5,9,'#bcefff');if(formed)p.rect(7,6,2,5,'#e8ffff').rect(6,7,4,2,'#e8ffff');else p.line(8,3,7,7,'#172638').line(7,7,10,10,'#172638').set(7,13,'#79bacc');return p;}
export function generatePrototypeTextures(assets){
 const images=new Map(),save=(name,p)=>{const file=path.join(assets,'textures',name+'.png');fs.mkdirSync(path.dirname(file),{recursive:true});fs.writeFileSync(file,png(p));images.set(name,p);};
 save('item/aether_shard',gem());save('item/crystalline_aether_shard',gem(true));
 for(const[tier,theme]of[['basic','copper'],['expanded','gold'],['ultimate','netherite']])save('item/mana_container_'+tier,container(theme));
 save('item/creative_mana_container',container('creative'));save('item/aether_tuner',tool('tuner'));save('item/aether_receiver',tool('receiver'));save('item/interdimensional_lens',tool('eye'));
 save('item/core_wand',wand(false));save('item/creative_wand',wand(true));
 save('item/formed_spell_core',core(true));save('item/unformed_spell_core',core(false));
 for(const theme of Object.keys(palettes))save('block/frame_'+theme,frame(theme));
 const checker=new Pixels();for(let y=0;y<16;y++)for(let x=0;x<16;x++)checker.set(x,y,((x>>3)+(y>>3))%2?'#cc32ed':'#1a1022');save('block/creative_checker',checker);
 const casing=new Pixels();for(let y=0;y<16;y++)for(let x=0;x<16;x++)casing.set(x,y,(x%8===0||y%8===0)?'#111d2b':(x+y)%5===0?'#344252':'#263342');casing.rect(3,4,10,1,'#496575').rect(3,11,10,1,'#121d2b');save('block/aether_casing',casing);
 const port=new Pixels();port.rect(0,0,16,16,'#1c344d').rect(2,2,12,12,'#397b95').rect(4,4,8,8,'#8be4ec').rect(6,3,4,10,'#d1ffff').rect(3,6,10,4,'#d1ffff').rect(6,6,4,4,'#edfefe');save('block/aether_port',port);
 const liquid=new Pixels();for(let y=0;y<16;y++)for(let x=0;x<16;x++)liquid.set(x,y,(x+Math.floor(y/3)*3)%11===0?'#b9fbfc':y%5===0?'#76dce7':'#47b7ce');save('block/aether_liquid',liquid);
 const crystal=new Pixels();for(let y=0;y<16;y++)for(let x=0;x<16;x++)crystal.set(x,y,x<3||x>13?'#447f9e':x<6?'#8bd8e7':x<10?'#c5f6fa':'#65b4cf');save('block/aether_crystal',crystal);
 const glass=new Pixels();glass.rect(0,0,16,16,'#2c596e').line(2,13,13,2,'#8cc7d1').line(3,13,13,3,'#5e9eae');save('block/aether_glass',glass);
 const wood=new Pixels();for(let y=0;y<16;y++)for(let x=0;x<16;x++)wood.set(x,y,y%5===0?'#503538':(x+3*y)%13===0?'#d4a26a':'#92614d');save('block/writing_wood',wood);
 const desk=new Pixels();desk.rect(0,0,16,16,'#503538').rect(1,1,14,14,'#a77856').rect(2,2,12,12,'#dccda6').rect(3,3,10,10,'#bfc5b3').line(7,4,4,7,'#326a88').line(4,7,8,11,'#326a88').line(8,11,11,7,'#326a88').line(11,7,7,4,'#326a88').rect(7,6,2,3,'#4fadc3').rect(3,12,3,1,'#665e65').rect(11,3,2,1,'#665e65');save('block/writing_surface',desk);
 for(const[color,hex]of Object.entries({purple:'#8c63b9',cyan:'#49b5c6',blue:'#557bca',yellow:'#dbb64d',green:'#64b886',red:'#cf6674',orange:'#d99452',magenta:'#ba6cae',light_blue:'#84c8dd',white:'#c7dce7',light_gray:'#9cacba',gray:'#718193'})){
  const p=new Pixels();p.rect(0,0,16,16,hex).rect(0,0,16,2,'#d6f0f5').rect(0,0,2,16,'#d6f0f5').rect(14,2,2,14,'#26354b').rect(2,14,14,2,'#26354b');save('block/node/'+color,p);
 }
 const engraving=new Pixels();engraving.rect(0,0,16,16,'#d6f4f2').rect(0,14,16,2,'#96bdbf');save('block/node/engraving',engraving);
 const preview=new Pixels(640,Math.ceil(images.size/8)*80);preview.rect(0,0,preview.w,preview.h,'#16212f');let n=0;for(const p of images.values()){const X=n%8*80,Y=Math.floor(n/8)*80;for(let y=0;y<16;y++)for(let x=0;x<16;x++){const c=p.data.subarray((y*16+x)*4,(y*16+x)*4+4);if(c[3])preview.rect(X+8+x*4,Y+8+y*4,4,4,c);}n++;}
 const previewPath=path.resolve(assets,'../../../../../build/prototype-textures.png');fs.mkdirSync(path.dirname(previewPath),{recursive:true});fs.writeFileSync(previewPath,png(preview));
}
