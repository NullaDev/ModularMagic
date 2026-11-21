import fs from 'node:fs';
import path from 'node:path';
import {Pixels,png} from './generate-prototype-textures.mjs';
const a='src/main/resources/assets/modularmagic',d='src/main/resources/data/modularmagic';
const mod=s=>'modularmagic:'+s,mc=s=>'minecraft:'+s;
const read=p=>JSON.parse(fs.readFileSync(p,'utf8'));
const write=(p,j)=>{fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,JSON.stringify(j,null,2)+'\n');};
const item=s=>({item:s.includes(':')?s:mc(s)});
const mimic=s=>({type:'neoforge:components',items:mod('item_placeholder'),components:{'minecraft:custom_data':{MaterialTarget:mc(s)}},strict:false});
const shapeless=(id,ingredients)=>write(`${d}/recipe/${id}.json`,{type:'minecraft:crafting_shapeless',category:'misc',ingredients,result:{id:mod(id),count:1}});
shapeless('endless_fuel',[item(mod('carpet_placeholder')),item('coal'),item('iron_ingot')]);
shapeless('endless_fertilizer',[mimic('stone'),item('moss_block'),item('bone_meal'),item('composter'),item(mod('aether_gem'))]);
shapeless('endless_food',[item(mod('plant_placeholder')),item('carrot'),item(mod('aether_gem'))]);
shapeless('portable_tnt_cannon',[mimic('tnt'),item('water_bucket'),item('dispenser'),item('iron_ingot'),item('iron_ingot'),item(mod('aether_gem'))]);
shapeless('auto_crafter',[item('crafter'),item(mod('aether_gem')),item(mod('aether_gem')),item(mod('rune_plate_k00')),item(mod('rune_plate_k07'))]);
const names={endless_fuel:['无尽之能源','Endless Fuel'],endless_fertilizer:['无尽之肥料','Endless Fertilizer'],endless_food:['无尽之食物','Endless Food'],portable_tnt_cannon:['便携式 TNT 炮','Portable TNT Cannon'],auto_crafter:['自动合成器','Portable Auto Crafter']};
for(const id of Object.keys(names)){
 const p=new Pixels();
 if(id==='endless_fuel'){p.rect(3,3,10,11,'#2d3043').rect(4,2,8,11,'#545269').rect(5,4,6,7,'#283244').line(5,8,7,5,'#68d8e3').line(7,5,10,8,'#68d8e3').line(10,8,7,11,'#68d8e3').line(7,11,5,8,'#68d8e3').rect(3,12,10,2,'#ab6657');}
 else if(id==='endless_fertilizer'){p.rect(5,1,6,2,'#40607b').rect(4,3,8,10,'#344c60').rect(5,4,6,8,'#dce7d5').line(6,9,9,6,'#5aab78').line(6,6,9,9,'#5aab78').rect(4,12,8,2,'#86cbd5');}
 else if(id==='endless_food'){p.line(5,12,10,5,'#854f32').line(6,13,12,5,'#e7a452').line(7,11,12,6,'#f3c277').line(9,5,9,1,'#59a875').line(11,5,14,2,'#8ed39a').line(10,5,12,1,'#388f72').rect(3,11,3,3,'#6fcbd6').rect(4,12,1,1,'#e5fbf0');}
 else if(id==='portable_tnt_cannon'){p.rect(1,7,12,6,'#344b60').rect(2,8,10,3,'#85afbc').rect(4,4,9,5,'#546373').rect(10,3,4,6,'#273b50').rect(11,4,3,4,'#96dae3').rect(5,5,4,3,'#c36e63').rect(6,6,2,1,'#eee1be').rect(2,12,3,3,'#a27a59');}
 else {p.rect(2,2,12,12,'#3d5369').rect(3,3,10,10,'#a9c3bc').rect(4,4,8,8,'#345b6b');for(let y=4;y<12;y+=3)for(let x=4;x<12;x+=3)p.rect(x,y,2,2,'#8dccca');p.rect(3,12,10,2,'#b08756').rect(11,3,2,2,'#d6f7e2');}
 fs.writeFileSync(`${a}/textures/item/${id}.png`,png(p));write(`${a}/models/item/${id}.json`,{parent:'minecraft:item/'+(id==='portable_tnt_cannon'?'handheld':'generated'),textures:{layer0:mod('item/'+id)}});
}
const stories={
 endless_fuel:[
  ['留在火里的纹样','A Pattern in the Fire','推动地毯的那道规则，可以把同一片纹样一遍遍展开。若将它留在炉火中，烧去的便总是刚刚多出的那一份。$(br2)把它放进炉子的燃料槽。每轮燃烧只相当于一块地毯，却会$(l)留下自身继续燃烧，不消耗以太$()。','The carpet’s repeated pattern unfolds another copy each time. Set that rule within a furnace, and the fire always consumes the extra copy.$(br2)Place it in the fuel slot. Each cycle lasts as long as a carpet, but $(l)the fuel remains and uses no aether.$()']
 ],
 endless_fertilizer:[
  ['从岩石到新芽','From Stone to New Growth','苔藓把岩石覆成柔软的绿，堆肥又能让这些新生的痕迹留下骨粉。将这几段纹样收进同一件器物，便能借以太促成下一次生长。$(br2)手持它$(l)右键施肥$()，效果与骨粉相同。每次有效施肥会消耗随身容器中的以太，器物本身会留下。','Moss covers stone in green, and compost turns that growth into bonemeal. Gather those patterns into one tool, and aether can encourage the next growth.$(br2)$(l)Use it on a plant$() as you would bonemeal. Each valid use draws aether from your carried containers; the tool remains.']
 ],
 endless_food:[
  ['握在手里的收成','A Harvest in Your Hand','植物的拟材记住了胡萝卜的形状。每一次咀嚼，都让那份收成借以太重新出现。$(br2)像食物一样手持右键。一次使用会消耗以太，并恢复$(l)一根胡萝卜的饥饿值与饱和度$()，器物不会被吃掉。','The plant mimic remembers a carrot’s form. Each bite calls that harvest into being again.$(br2)Hold to eat it like ordinary food. One use costs aether and restores $(l)the hunger and saturation of one carrot.$() The charm remains.']
 ],
 portable_tnt_cannon:[
  ['把爆炸留给旅途','An Explosion for the Road','水幕兜住爆炸，将冲击收束成向前的一推。便携的炮身里，也能留下同样的纹样。$(br2)朝想去的方向看，再右键点火。视线越向上，升得越高。$(l)这次爆炸不会伤害自己，落地仍会受伤。$()先看好落点，再借下一次冲击赶路。','A curtain of water contains the blast and directs its force forward. The same pattern fits inside a portable barrel.$(br2)Look toward your destination and use it to fire. Aim upward for more height. $(l)The blast will not hurt you; landing still can.$() Choose your landing before you launch.']
 ],
 auto_crafter:[
  ['把配方留在掌心','A Recipe in Your Palm','合成器本就知道如何把物品排列成器物。留下配方的影像，再接上以太，它便能在背包里继续工作。$(br2)$(l)右键打开$()，用物品点击九宫格设置参考，右键可清除。参考物不会消耗。选择开启，再收起界面，它就会持续尝试合成。修改配方后，需要重新开启。','A crafter already knows how to arrange materials. Give it a recipe’s image and aether, and it can keep working inside your pack.$(br2)$(l)Right-click to open it.$() Click the grid with samples; right-click to clear them. Samples are retained. Switch it on and close the screen to begin. Editing the pattern pauses it.'],
  ['实物与拟材','Materials and Mimics','它会先取用背包中的实物，缺料时再借$(l)对应的以太拟材$()补齐；生成材料会额外消耗以太。流体拟材还需要空桶，合成后的空桶照常留下。$(br2)产物与余料都会放回背包。材料、以太不足，或背包没有位置时，它便会停下，不会扣走半份材料。','Real materials come first. $(l)Matching aether mimics$() can fill missing ingredients for their additional generation fee. Fluid mimics also need empty buckets; recipes return those buckets normally.$(br2)Results and remainders go back into your pack. Missing resources or a full pack pause the work without taking a partial payment.']
 ]
};
for(const [locale,en]of [['zh_cn',false],['en_us',true]]){
 const file=`${a}/lang/${locale}.json`,lang=read(file);for(const [id,n]of Object.entries(names))lang['item.modularmagic.'+id]=n[en?1:0];
 lang['tooltip.modularmagic.utility_cost']=en?'Aether per use: %s':'单次消耗：%s 以太';
 lang['tooltip.modularmagic.crafting_cost']=en?'Aether per craft: %s':'每次合成：%s 以太';
 lang['gui.modularmagic.autocrafter_start']=en?'Start':'开启';lang['gui.modularmagic.autocrafter_stop']=en?'Pause':'暂停';
 lang['gui.modularmagic.autocrafter_on']=en?'Running':'运转中';lang['gui.modularmagic.autocrafter_off']=en?'Paused':'已暂停';write(file,lang);
 for(const [id,n]of Object.entries(names))write(`${a}/patchouli_books/assembly_guide/${locale}/entries/equipment/${id}.json`,{name:n[en?1:0],icon:mod(id),category:mod('equipment'),pages:[{type:'patchouli:spotlight',item:mod(id),link_recipe:true,title:stories[id][0][en?1:0],text:stories[id][0][en?3:2]},...stories[id].slice(1).map(p=>({type:'patchouli:text',title:p[en?1:0],text:p[en?3:2]})),{type:'patchouli:crafting',recipe:mod(id)}]});
}
console.log('Utilities: five items, five recipes and five guide entries per language.');
