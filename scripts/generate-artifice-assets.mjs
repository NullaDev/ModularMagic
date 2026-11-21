import fs from 'node:fs';
import path from 'node:path';
import {Pixels,png} from './generate-prototype-textures.mjs';
const assets='src/main/resources/assets/modularmagic',data='src/main/resources/data/modularmagic';
const read=p=>JSON.parse(fs.readFileSync(p,'utf8'));
const write=(p,v)=>{fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,JSON.stringify(v,null,2)+'\n');};
const M=id=>'modularmagic:'+id,V=id=>'minecraft:'+id;
const topics=read(`${data}/research/topics.json`);
const extra=[
 {id:'K32',requires:['K00'],nodes:[],sources:[V('anvil'),V('grindstone'),V('enchanting_table')],reward:.5,zh:'经验的形态',en:'The Shape of Experience'},
 {id:'K33',requires:['K32','K03'],nodes:[],sources:[V('enchanting_table')],reward:.5,zh:'附魔的纹样',en:'Patterns of Enchantment'},
 {id:'K34',requires:['K32','K33','K28'],nodes:[],sources:[V('netherite_block')],reward:.5,zh:'未定的物质',en:'Matter Unwritten'},
 {id:'P23',requires:['K24','K32'],nodes:[],sources:[V('anvil'),V('grindstone')],reward:.5,zh:'经验回流',en:'Experience Reclaimed'}
];for(const t of extra){const existing=topics.find(x=>x.id===t.id);if(existing)Object.assign(existing,t);else topics.push(t);}write(`${data}/research/topics.json`,topics);
const defaults=read(`${data}/generator_defaults.json`);if(!defaults.some(r=>r.id==='experience'))defaults.push({id:'experience',topic:'P23',icon:V('experience_bottle'),zh:'经验',en:'Experience',width:5,depth:5,height:3,period:20,batch:64,capacity:64000,output:3200,yields:[20],storage:M('mana_container_basic'),coldStart:false});write(`${data}/generator_defaults.json`,defaults);
const nodes=read(`${data}/node_templates.json`).templates;
const plate=id=>M('rune_plate_'+id.toLowerCase());
const ing=id=>typeof id==='string'?(id.startsWith('#')?{tag:id.slice(1)}:{item:id}):id;
const recipe=(id,j)=>write(`${data}/recipe/${id}.json`,j);
const shapeless=(id,items,count=1,components)=>recipe(id,{type:'minecraft:crafting_shapeless',category:'misc',ingredients:items.map(ing),result:{id:M(id),count,...(components?{components}: {})}});
const shaped=(id,pattern,key,count=1)=>recipe(id,{type:'minecraft:crafting_shaped',category:'misc',pattern,key:Object.fromEntries(Object.entries(key).map(([k,v])=>[k,ing(v)])),result:{id:M(id),count}});
const smith=(id,base)=>recipe(id,{type:'minecraft:smithing_transform',template:ing(V('netherite_upgrade_smithing_template')),base:ing(M(base)),addition:ing(V('netherite_ingot')),result:{id:M(id),count:1}});
const mineableFile='src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json';const mineable=fs.existsSync(mineableFile)?read(mineableFile):{replace:false,values:[]};if(!mineable.values.includes(M('aether_enchanter')))mineable.values.push(M('aether_enchanter'));write(mineableFile,mineable);
write(`${data}/tags/item/aether_fragments.json`,{replace:false,values:[M('aether_shard'),M('crystalline_aether_shard')]});
shapeless('aether_glass',['#modularmagic:aether_fragments',V('glass'),V('glass'),V('glass'),V('glass')],4);
shapeless('aether_gem',['#modularmagic:aether_fragments',[ing(V('diamond')),ing(V('emerald'))]]);
shapeless('unformed_matter',['#minecraft:planks',V('cobblestone'),V('leather'),V('iron_ingot'),V('gold_ingot'),V('diamond'),V('netherite_scrap'),'#modularmagic:aether_fragments']);
shaped('void_lens',[' C ','GPG',' C '],{C:V('copper_ingot'),G:V('glass'),P:V('ender_pearl')});
shaped('rune_research_table',['PIP','WWW'],{P:V('paper'),I:V('ink_sac'),W:'#minecraft:planks'});
shaped('rune_carving_table',['QFQ','WWW'],{Q:V('iron_nugget'),F:V('flint'),W:'#minecraft:planks'});
shaped('spell_writing_table',['GPG','WWW'],{G:M('aether_glass'),P:plate('K00'),W:'#minecraft:planks'});
shapeless('research_ink',[V('glass_bottle'),V('black_dye'),V('sugar')]);
shapeless('blank_spell_node',[V('amethyst_shard'),V('amethyst_shard'),'#modularmagic:aether_fragments'],4);
shaped('blank_spell_core',[' Q ','QGQ',' P '],{Q:V('gold_nugget'),G:M('aether_gem'),P:V('ender_pearl')},2);
shaped('core_wand',['  G',' P ','S  '],{G:M('aether_gem'),P:plate('K00'),S:V('stick')});
shaped('advanced_wand',['  M',' G ','G  '],{M:M('unformed_matter'),G:V('gold_ingot')});
shaped('aether_disruptor',['CBC','GRG','CLC'],{C:V('copper_ingot'),B:V('bucket'),G:V('glass'),R:V('redstone'),L:plate('K23')});
shaped('mana_container_basic',[' C ','GBG',' P '],{C:V('copper_ingot'),G:M('aether_gem'),B:V('glass_bottle'),P:plate('K03')});
shaped('mana_container_expanded',['GDG','CBC',' P '],{G:V('gold_ingot'),D:V('diamond'),C:M('aether_gem'),B:M('mana_container_basic'),P:plate('K30')});smith('mana_container_ultimate','mana_container_expanded');
const portableUpgrade=read(data+'/recipe/mana_container_expanded.json');portableUpgrade.type='modularmagic:container_upgrade';recipe('mana_container_expanded',portableUpgrade);
shaped('aether_tank',['CGC','GBG','CPC'],{C:V('copper_ingot'),G:M('aether_glass'),B:M('mana_container_basic'),P:plate('K03')});
shaped('expanded_aether_tank',['GDG','GBG',' P '],{G:V('gold_ingot'),D:V('diamond'),B:M('aether_tank'),P:plate('K30')});smith('ultimate_aether_tank','expanded_aether_tank');
shaped('aether_emitter',[' G ','CRC',' P '],{G:M('aether_gem'),C:V('copper_ingot'),R:V('redstone'),P:plate('K01')});
shaped('expanded_aether_emitter',['GDG',' B ',' P '],{G:V('gold_ingot'),D:V('diamond'),B:M('aether_emitter'),P:plate('K30')});smith('ultimate_aether_emitter','expanded_aether_emitter');
shaped('aether_condenser',['CGC','GBG',' P '],{C:V('copper_ingot'),G:M('aether_glass'),B:M('mana_container_basic'),P:plate('K23')});
shaped('aether_crystallization_chamber',['QSQ','GBG',' P '],{Q:V('quartz'),S:V('nether_star'),G:M('aether_glass'),B:M('aether_condenser'),P:plate('K28')});
shaped('aether_resonator',['AGA','CRC',' P '],{A:V('amethyst_block'),G:M('aether_gem'),C:M('aether_glass'),R:V('redstone'),P:plate('K25')});
shaped('personal_aether_anchor',[' C ','GEG','NPN'],{G:M('aether_gem'),C:M('crystalline_aether_shard'),E:V('ender_chest'),N:V('netherite_scrap'),P:plate('K26')});
shapeless('aether_receiver',[V('ender_pearl'),M('aether_gem'),V('amethyst_shard'),plate('K26')]);
shapeless('interdimensional_lens',[V('ender_eye'),M('aether_gem'),V('dragon_breath'),V('popped_chorus_fruit'),plate('K29')]);
shaped('world_anchor',['OGO','EBE','OPO'],{O:V('obsidian'),G:M('aether_gem'),E:V('ender_pearl'),B:M('mana_container_expanded'),P:plate('K27')});
shapeless('aether_tuner',[V('copper_ingot'),V('amethyst_shard'),V('redstone'),plate('K00')]);
shapeless('item_placeholder',[V('paper'),V('clay_ball'),M('aether_gem'),plate('K31')]);
// The housing contains its own empty buffer. Filled containers cannot accidentally be consumed.
recipe('generator_housing',{type:'modularmagic:generator_housing'});
if(fs.existsSync(`${data}/recipe/generator_assembly.json`))fs.unlinkSync(`${data}/recipe/generator_assembly.json`);
const samples={water:'water_bucket',stone:'cobblestone',lava:'lava_bucket',equipment:'armor_stand',snow:'snowball',wool:'shears',fire:'flint_and_steel',crops:'wheat_seeds',ice:'ice',copper:'copper_ingot',honey:'honey_bottle',sand:'sand',sculk:'sculk',iron_golem:'iron_block',rails:'rail',amethyst:'amethyst_shard',chorus:'chorus_flower',mushrooms:'red_mushroom',music:'jukebox',egg:'egg',curing:'golden_apple',tnt:'tnt',experience:'experience_bottle'};
for(const r of defaults)recipe('generator_'+r.id,{type:'minecraft:crafting_shapeless',category:'misc',ingredients:[ing(M('generator_housing')),ing(plate(r.topic)),ing(V(samples[r.id]))],result:{id:M('aether_generator'),count:1,components:{'minecraft:custom_data':{AetherDevice:{Generator:{Rule:r.id}}}}}});
shapeless('aether_enchanter',[V('enchanting_table'),V('chest'),V('lapis_block'),M('aether_gem'),M('aether_gem'),M('aether_glass'),M('aether_glass'),plate('K32'),plate('K33')]);
for(const n of nodes)recipe('carving/'+n.item,{type:'modularmagic:rune_carving',key:n.id,knowledge:n.id,ingredient:ing(M(n.category==='root'?'blank_spell_core':'blank_spell_node')),result:{id:M(n.item),count:1}});
for(const t of topics.filter(t=>t.id.startsWith('K')))recipe('carving/plate_'+t.id.toLowerCase(),{type:'modularmagic:rune_carving',key:'plate:'+t.id,knowledge:t.id,ingredient:ing(V('clay_ball')),result:{id:plate(t.id),count:1}});
for(const file of fs.readdirSync(`${data}/recipe`).filter(s=>s.startsWith('placeholder_'))){const p=`${data}/recipe/${file}`,j=read(p);for(let i=0;i<j.ingredients.length;i++){const n=nodes.find(n=>M(n.item)===j.ingredients[i].item);if(n){const t=topics.find(t=>t.nodes.includes(n.id));j.ingredients[i]=ing(plate(t.id));}}write(p,j);}
for(const [tag,names]of Object.entries({swords:['sword'],pickaxes:['pickaxe'],axes:['axe'],shovels:['shovel'],hoes:['hoe'],head_armor:['helmet'],chest_armor:['chestplate'],leg_armor:['leggings'],foot_armor:['boots'],'enchantable/durability':['sword','pickaxe','axe','shovel','hoe','helmet','chestplate','leggings','boots']}))write('src/main/resources/data/minecraft/tags/item/'+tag+'.json',{replace:false,values:names.map(n=>M('unformed_'+n))});
const toolPatterns={sword:['M','M','S'],pickaxe:['MMM',' S ',' S '],axe:['MM','MS',' S'],shovel:['M','S','S'],hoe:['MM',' S',' S'],helmet:['MMM','M M'],chestplate:['M M','MMM','MMM'],leggings:['MMM','M M','M M'],boots:['M M','M M']};
for(const [name,pattern]of Object.entries(toolPatterns)){const keys={M:M('unformed_matter')};if(pattern.join('').includes('S'))keys.S=V('stick');shaped('unformed_'+name,pattern,keys);}
const saveTexture=(name,p)=>{const f=`${assets}/textures/${name}.png`;fs.mkdirSync(path.dirname(f),{recursive:true});fs.writeFileSync(f,png(p));};
const itemModel=(id,handheld=false)=>write(`${assets}/models/item/${id}.json`,{parent:'minecraft:item/'+(handheld?'handheld':'generated'),textures:{layer0:M('item/'+id)}});
let p=new Pixels();p.rect(3,1,10,14,'#3b4c62').rect(4,2,8,12,'#81cdda').rect(5,3,6,10,'#c5f5ee').line(5,5,10,10,'#487a9c').line(10,5,5,10,'#487a9c');saveTexture('item/blank_spell_core',p);itemModel('blank_spell_core');
p=new Pixels();for(let y=1;y<15;y++)for(let x=1;x<15;x++)if(Math.abs(x-7.5)+Math.abs(y-7.5)<8)p.set(x,y,x<7?'#dafaf5':x<10?'#70cce0':'#407aa1');p.line(5,4,9,4,'#ffffff').line(4,5,4,8,'#ffffff');saveTexture('item/aether_gem',p);itemModel('aether_gem');
p=new Pixels();p.rect(2,4,12,8,'#263545').rect(3,3,9,8,'#91a7b9').rect(4,4,9,5,'#c0d6e0').line(4,5,11,9,'#5e9cae').line(6,4,8,10,'#7ef0e2');saveTexture('item/unformed_matter',p);itemModel('unformed_matter');
p=new Pixels();p.line(2,14,11,5,'#493342').line(3,14,12,5,'#b28a3e').line(4,13,12,5,'#ffe4a0').line(5,12,11,6,'#e2b655').rect(8,3,6,5,'#34495c').rect(10,1,4,5,'#a0bcca').rect(11,2,3,3,'#d8f4ee').line(10,5,13,2,'#70d8d0').rect(8,7,5,1,'#ffe4a0').rect(9,6,4,1,'#c69e4c');saveTexture('item/advanced_wand',p);itemModel('advanced_wand',true);
for(const t of topics.filter(t=>t.id.startsWith("K"))){p=new Pixels();p.rect(1,3,14,11,'#705d51').rect(2,2,12,11,'#bba487').rect(3,3,10,9,'#e0c7a1');let seed=Array.from(t.id).reduce((a,c)=>a*33+c.charCodeAt(0),1);for(let i=0;i<6;i++){const x=4+(seed>>(i*3))%7,y=4+(seed>>(i*2))%6;p.line(x,y,Math.min(11,x+2),Math.min(10,y+2),'#467b90');}saveTexture('item/rune_plate_'+t.id.toLowerCase(),p);itemModel('rune_plate_'+t.id.toLowerCase());}
for(const name of Object.keys(toolPatterns)){p=new Pixels();const color='#9ae5dc',dark='#40556e';if(['helmet','chestplate','leggings','boots'].includes(name)){if(name==='helmet')p.rect(3,3,10,9,dark).rect(4,2,8,7,color).rect(5,7,6,5,[0,0,0,0]);if(name==='chestplate')p.rect(1,3,14,5,dark).rect(3,2,10,12,color).rect(6,1,4,4,[0,0,0,0]);if(name==='leggings')p.rect(3,2,10,12,color).rect(6,6,4,9,[0,0,0,0]);if(name==='boots')p.rect(2,3,4,10,color).rect(10,3,4,10,color).rect(1,11,5,3,dark).rect(10,11,5,3,dark);p.line(4,5,11,9,'#d6fff5');}else{p.line(3,14,10,7,dark).line(4,14,11,7,'#af8e76');if(name==='sword')p.line(5,10,13,2,color).line(6,10,14,2,'#dafcf4').line(3,8,8,13,dark);else if(name==='pickaxe'||name==='hoe')p.line(3,2,13,4,color).line(12,4,14,8,dark);else if(name==='axe')p.rect(8,1,6,7,color).rect(6,3,3,4,dark);else p.rect(9,1,5,6,color);}saveTexture('item/unformed_'+name,p);itemModel('unformed_'+name,!['helmet','chestplate','leggings','boots'].includes(name));}
for(let layer=1;layer<=2;layer++){p=new Pixels(64,32);p.rect(0,0,64,32,'#607386');for(let y=0;y<32;y++)for(let x=0;x<64;x++)if(x%8===0||y%8===0)p.set(x,y,'#91d2cd');p.line(20,20,24,28,'#b5fff0').line(24,28,28,20,'#b5fff0');if(layer===1)p.rect(9,10,6,5,[0,0,0,0]);saveTexture('models/armor/unformed_layer_'+layer,p);}
p=new Pixels();p.rect(0,0,16,16,[95,180,207,25]);p.rect(0,0,16,1,'#9bd4df').rect(0,15,16,1,'#4b9bb4').rect(0,0,1,16,'#9bd4df').rect(15,0,1,16,'#4b9bb4').line(3,5,6,2,'#b4eff3').line(10,13,13,10,'#76bccd');saveTexture('block/aether_glass_block',p);
write(`${assets}/models/block/aether_glass.json`,{parent:'minecraft:block/cube_all',render_type:'minecraft:translucent',textures:{all:M('block/aether_glass_block')}});write(`${assets}/models/item/aether_glass.json`,{parent:M('block/aether_glass')});write(`${assets}/blockstates/aether_glass.json`,{variants:{'':{model:M('block/aether_glass')}}});
write(`${data}/loot_table/blocks/aether_glass.json`,{type:'minecraft:block',pools:[{rolls:1,entries:[{type:'minecraft:item',name:M('aether_glass')}],conditions:[{condition:'minecraft:survives_explosion'}]}]});
p=new Pixels();p.rect(0,0,16,16,'#25354a').rect(1,1,14,14,'#425b70').rect(2,2,12,12,'#2b6582').line(3,4,12,11,'#9be3e5').line(12,4,3,11,'#9be3e5').rect(6,6,4,4,'#d7f8e6');saveTexture('block/enchanter_top',p);
write(`${assets}/models/block/aether_enchanter.json`,{parent:'minecraft:block/cube_bottom_top',textures:{top:M('block/enchanter_top'),bottom:M('block/aether_casing'),side:M('block/frame_gold')}});write(`${assets}/models/item/aether_enchanter.json`,{parent:M('block/aether_enchanter')});write(`${assets}/blockstates/aether_enchanter.json`,{variants:{'':{model:M('block/aether_enchanter')}}});
for(const [locale,en]of [['zh_cn',false],['en_us',true]]){
 const file=`${assets}/lang/${locale}.json`,lang=read(file);for(const key of Object.keys(lang))lang[key]=lang[key].replaceAll('魔力','以太').replaceAll('法力','以太').replace(/\bMana\b/g,'Aether').replace(/\bmana\b/g,'aether');
 const set=(key,zh,eng)=>lang[key]=en?eng:zh;
 set('item.modularmagic.advanced_wand','高级法杖','Advanced Wand');
 set('book.modularmagic.assembly_guide.name','装配指南','Assembly Guide');
 set('book.modularmagic.assembly_guide.landing','两格源水之间，新的水不断涌出。若这些物质并非凭空出现，它们又从何而来？$(br2)透过一颗浑浊的末影珍珠，重新看看这个熟悉的世界。$(br2)$(l)观察纹样，解读符文，装配属于自己的魔法。$()','Water flows from the space between two sources. If this matter does not come from nothing, where does it begin?$(br2)Peer through a clouded ender pearl, and look again at the world you thought you knew.$(br2)$(l)Observe its patterns, decipher its runes, and assemble magic of your own.$()');
 if(en){lang['itemGroup.modularmagic']='Modular Magic';lang['itemGroup.modularmagic.builtin_spells']='Modular Magic: Built-in Spells';}
 set('item.modularmagic.blank_spell_core','空白法术核心','Blank Spell Core');set('item.modularmagic.aether_gem','以太宝石','Aether Gem');set('item.modularmagic.unformed_matter','未元物质','Dark Matter');set('block.modularmagic.aether_glass','以太玻璃','Aether Glass');set('block.modularmagic.aether_enchanter','以太附魔台','Aether Enchanter');
 for(const t of topics){set('research.modularmagic.'+t.id.toLowerCase(),t.zh,t.en);set('item.modularmagic.rune_plate_'+t.id.toLowerCase(),'符文板：'+t.zh,'Rune Plate: '+t.en);}
 for(const [id,zh,eng]of [['sword','剑','Sword'],['pickaxe','镐','Pickaxe'],['axe','斧','Axe'],['shovel','锹','Shovel'],['hoe','锄','Hoe'],['helmet','头盔','Helmet'],['chestplate','胸甲','Chestplate'],['leggings','护腿','Leggings'],['boots','靴子','Boots']])set('item.modularmagic.unformed_'+id,'未元'+zh,'Dark Matter '+eng);
 set('generator.modularmagic.experience','经验','Experience');
 const labels={
  'carving.modularmagic.carve':['雕刻','Carve'],
 
  'research.modularmagic.no_note':['研究完成','Research complete'],
 
  'enchanter.modularmagic.preview':['预览','Preview'],
  'enchanter.modularmagic.insert_equipment':['先放入装备','Insert equipment first'],'enchanter.modularmagic.select_sample':['选择一个附魔样本','Select an enchantment sample'],'enchanter.modularmagic.no_change':['没有可增加的附魔','No enchantment can be added'],'enchanter.modularmagic.paused':['红石信号使附魔台暂停','A redstone signal pauses the table'],'enchanter.modularmagic.waiting':['以太不足','Not enough aether'],
  'enchanter.modularmagic.apply':['附魔','Enchant'],'enchanter.modularmagic.fee':['消耗：%s 以太','Cost: %s Aether'],'enchanter.modularmagic.stored':['以太：%s','Aether: %s'],
 
  'jei.modularmagic.assembly':['法术装配','Spell Assembly'],'jei.modularmagic.assembly_example':['示例：将节点连接进核心','Example: connect nodes inside a core'],
 
  'jei.modularmagic.carving':['符文雕刻','Rune Carving'],'jei.modularmagic.sigil':['以太拟材定型','Shaping Aether Mimics'],'jei.modularmagic.enchanting':['以太附魔','Aether Enchanting'],'jei.modularmagic.condensing':['以太凝聚','Aether Condensation'],
  'jei.modularmagic.knowledge':['需要知识：%s','Knowledge: %s'],'jei.modularmagic.sample':['样本不消耗','Sample is retained'],'jei.modularmagic.aether':['%s 以太','%s Aether']
 };for(const [k,v]of Object.entries(labels))lang[k]=v[en?1:0];
 for(const k of Object.keys(lang))lang[k]=lang[k].replaceAll('唤物印','以太拟材').replaceAll('未元质','未元物质').replaceAll('Conjuration Sigils','Aether Mimics').replaceAll('Conjuration Sigil','Aether Mimic').replaceAll('conjuration sigil','aether mimic').replaceAll('Unformed Matter','Dark Matter');
 for(const k of ['carving.modularmagic.input','carving.modularmagic.help','research.modularmagic.help','research.modularmagic.carving_help','research.modularmagic.status_inventory_full','research.modularmagic.held_note_required','research.modularmagic.note_stored','research.modularmagic.slot_help','research.modularmagic.node_help','research.modularmagic.start_prompt','placeholder.modularmagic.center','placeholder.modularmagic.blank','enchanter.modularmagic.help'])delete lang[k];
 write(file,lang);
}
console.log(`Artifice: ${topics.filter(t=>t.id.startsWith("K")).length} plates, ${nodes.length} node carvings, ${defaults.length} generators.`);

// Player-facing discoveries: the explanation belongs in the book, not a paragraph on each item.
for(const [locale,en]of [['zh_cn',false],['en_us',true]]){
 const book=`${assets}/patchouli_books/assembly_guide/${locale}`;
 const page=(title,english,text,translation)=>({type:'patchouli:text',title:en?english:title,text:en?translation:text});
 const craft=id=>({type:'patchouli:crafting',recipe:M(id)});
 const entry=(id,name,english,icon,category,pages)=>write(`${book}/entries/${id}.json`,{name:en?english:name,icon,category:M(category),pages});
 entry('equipment/advanced_wand','高级法杖','Advanced Wand',M('advanced_wand'),'equipment',[
  {type:'patchouli:spotlight',item:M('advanced_wand'),link_recipe:true,text:en?'A gold handle carries the current into dark matter, where a spell can take shape with less effort.$(br2)Keep your cores inside and switch spells as before. $(l)The same spell asks for less aether and focus.$() The wand’s tooltip shows its current multipliers.':'金柄将以太引入未元物质，术式便能以更少的耗费成形。$(br2)像初学者法杖一样装入核心、切换法术。$(l)同一法术，现在只需更少的以太与专注。$()具体倍率可查看法杖上的说明。'},craft('advanced_wand')]);
 entry('research/carving','凿尖下的纹样','Patterns under the Chisel',M('rune_carving_table'),'research',[
  {type:'patchouli:spotlight',item:M('rune_carving_table'),link_recipe:true,text:en?'A pattern you understand is a pattern your hand can follow. $(l)Put a blank core, blank node or clay ball on the desk.$() The shapes you know appear beside it. Choose one and carve.':'看懂了纹样，凿尖便知道该往哪里走。$(br2)$(l)放入空白核心、空白节点或黏土球$()，旁边便会浮现你已掌握的纹样。选好一种，刻下即可。'},
  page('承载与留痕','Vessels and Impressions','空白核心容纳法术的起点；空白节点承接其中的一段。$(br2)黏土柔软，留下的符文板更适合装进器物。机器需要的是它记下的规则，不必耗费一颗宝石。','Blank cores hold a spell’s beginning; blank nodes hold its parts.$(br2)Clay takes an impression more readily. Rune plates lend their remembered rules to devices without spending a gemstone.'),craft('blank_spell_core'),craft('blank_spell_node')]);
 entry('research/first_study','纸上的疑问','Questions on Paper',M('void_lens'),'research',[
  {type:'patchouli:spotlight',item:M('rune_research_table'),link_recipe:true,text:en?'$(l)Use unfinished notes on the research desk.$() Place ink on the desk with a right-click before opening the notes, or keep it in your pack. Begin Research when ready.$(br2)Choose a shape on the right and fit it into a slot of the same color. Left-click a node to read its values; right-click a loose end to lift it away.':'$(l)手持未解读的笔记，右键研究台$()。墨水可以留在身上，也可以先手持右键，摆在台面上。摊开笔记，纸与墨便各有其位；开始解读时，才会用掉墨水。$(br2)选右侧虚影，填入同色接口。左键节点调整数值，右键取回末端的节点。'},
  page('一页重新摊开','A Fresh Page','「参考术式」可对照连接，悬停查看参数，再回到「我的解读」继续。$(br2)$(l)离开未完成的研究，笔记会回到包里，草稿与本次开题记录则会清空。$()再次铺开，要重新花费墨水。已经读懂的知识仍属于你。','View the Reference to compare connections and hover to inspect values, then return to My Reading.$(br2)$(l)Leaving unfinished work returns the notes and clears the draft and ink payment.$() A fresh attempt needs fresh ink. Knowledge already understood remains yours.')]);
 entry('research/materials','以太留下的形状','Shapes Left by Aether',M('aether_gem'),'research',[
  page('玻璃里的微光','Light in Glass','散逸的以太钻进玻璃，令它更有韧性，也留下了一点微光。敲下来的玻璃仍能再次使用。$(br2)它能围住更多以太，却经不起搬动：$(l)拆下储槽，里面的以太就会散去。$()','Stray aether settles into glass, lending it resilience and a faint glow. The glass survives being broken.$(br2)It can enclose more aether, but cannot hold it through a move: $(l)breaking a tank releases what was inside.$()'),craft('aether_glass'),
  page('整齐的晶格','An Ordered Lattice','钻石与绿宝石的晶格，比玻璃更能安稳地收住以太。小小的宝石没有储槽那样的肚量，却能做成随身携带的以太容器，也能点亮法杖与发射器的核心。','Diamond and emerald lattices hold aether more steadily than glass. Their small size limits their capacity, but makes them suited to portable containers and the hearts of wands and emitters.'),craft('aether_gem')]);
 entry('research/experience','会修补物质的光','Light That Mends Matter',V('experience_bottle'),'research',[
  page('铁砧旁的疑问','A Question by the Anvil','一点经历，竟能让破损的工具重新坚固起来。那些从指间消失的光点，究竟去了哪里？$(br2)隔着透镜再看，你认出了熟悉的流动。$(l)经验，也是以太的一种形态。$()','A little experience can make a worn tool sound again. Where do those lights go when they vanish between your fingers?$(br2)Through the lens, the flow looks familiar. $(l)Experience is another form of aether.$()'),
  page('把经历留在身边','Experience Reclaimed','把经验的纹样留在符文板上，装进外壳。站在装置附近，它便会慢慢取走你的经验，将光点重新收进以太的流里。没有经验可取，它就会停下。$(br2)不想继续时，离开它的范围，或用红石让装置歇息。','Set the experience pattern into a housing. Nearby, it draws away your experience and returns that light to the aether stream. Without experience to draw, it waits.$(br2)Step away, or give it a redstone signal, when you want it to rest.'),craft('generator_experience')]);
 entry('energy/enchanting','不再偶然的纹样','Patterns without Chance',M('aether_enchanter'),'energy',[
  {type:'patchouli:spotlight',item:M('aether_enchanter'),link_recipe:true,text:en?'Lapis settles into patterns as an enchanting table works. Once you can read those lines, chance need no longer choose the result.$(br2)The chest holds your examples. $(l)Put equipment at the upper left, then left-click a stored book or enchanted item.$() Right-click a stored example to take it out.':'附魔时，青金石在台面上铺成了纹样。看懂这些线条后，结果便不必再交给偶然。$(br2)箱子留着供你描摹的样本。$(l)装备放左上，再左键选中下方收纳的书或附魔物品。$()右键可以取回样本。'},
  page('沿着旧纹再刻一遍','Trace the Pattern Again','接上以太网络，储够所需的以太，再刻下附魔。$(l)样本始终留下，只有装备上的纹样会改变。$()$(br2)等级直接相加：锋利 I 加锋利 II 得到锋利 III，但仍不会越过锋利 V。不能共存的纹样、装备无法承载的纹样，会被略过。','Supply the table through your aether network, then trace the enchantment. $(l)The example remains; only your equipment changes.$()$(br2)Levels add: Sharpness I plus II gives III, up to its usual limit of V. Conflicting patterns and those the item cannot carry are left aside.'),
  page('铁砧之外','Beyond the Anvil','纹样越深，所需的以太越多，正如铁砧上刻下复杂附魔要花费更多经验。只是这里不再由你承担那份经历；储够以太，再复杂的纹样也能写下。','Deeper patterns need more aether, much as intricate work at an anvil takes more experience. Here you need not surrender that experience yourself. Store enough aether, and the work can proceed.'),craft('aether_enchanter')]);
 entry('equipment/unformed','还未写定的物质','Matter Not Yet Written',M('unformed_matter'),'equipment',[
  page('把差异收进同一处','Gathering Different Natures','木的柔韧、石的稳固、皮革的延展，与金属和晶石各自的秉性，被以太收拢在一小块尚未定形的物质里。$(br2)它不是没有形状，而是仍肯接受新的形状。','Wood’s flexibility, stone’s solidity, leather’s give, and the natures of metals and gems gather in a small piece of matter not yet settled.$(br2)It does have a shape. It simply remains willing to take another.'),craft('unformed_matter'),
  page('让裂痕合拢','Closing the Cracks','经验能修补工具，以太也能沿着同一条规则，令裂痕重新合拢。$(l)随身带着有以太的容器$()，未元工具与护甲便会慢慢修复；容器空了，修复也会停下。$(br2)完整的一套护甲让以太流动得更顺，施法时能省下一小部分以太。','If experience can mend a tool, aether can follow the same rule to close its cracks. $(l)Carry a charged aether container$(), and unformed tools and armor mend themselves over time. Empty containers mean no more repairs.$(br2)A complete suit guides the flow more smoothly, saving a little aether when you cast.'),craft('unformed_pickaxe'),craft('unformed_chestplate')]);
 const overview=read(`${book}/entries/energy/aether_generator.json`);overview.pages=[{type:'patchouli:spotlight',item:M('generator_housing'),link_recipe:true,text:en?'A housing holds the buffer; a $(l)rune plate$() tells it which natural rule to turn back. Add a small piece of that rule’s familiar world, then give the aether a path out.':'外壳容纳缓存，$(l)符文板$()记下要倒转的自然规则。再添上一件与它有关的寻常物品，器物便有了自己的用途。为收拢的以太接好去路，它才不会无处可去。'},craft('generator_housing')];write(`${book}/entries/energy/aether_generator.json`,overview);
 for(const r of defaults){const file=`${book}/entries/energy/generator_${r.id}.json`;const previous=fs.existsSync(file)?read(file):null;const story=r.id==='curing'?(en?'A golden apple should bring a villager back. The device takes the apple and draws away the weakness, holding that unfinished possibility in its runes. $(br2)$(l)Name a zombie villager$(), weaken it, then drop a golden apple nearby. After a full treatment’s wait, the device releases a large pulse of aether; the zombie remains. Each attempt needs fresh weakness and another apple. Keep it nearby while the runes work.':'金苹果原本会把村民带回来。器物收走苹果，也抽走了虚弱，把这份未能发生的变化留在纹样里。$(br2)先给僵尸村民$(l)命名$()，施加$(l)虚弱$()，再在附近丢下金苹果。等待一个疗程，以太便会涌出，它却仍是原来的僵尸村民。每次都需要新的虚弱和金苹果；等待时别让它走远。'):(previous?.pages.find(p=>p.text)?.text||(en?'Nearby experience returns to the aether stream.':'附近的经验光点，重新汇入以太的流里。'));entry('energy/generator_'+r.id,(en?'Aether Device: ':'以太发生器：')+(en?r.en:r.zh),'Aether Device: '+r.en,plate(r.topic),'energy',[{type:'patchouli:text',title:en?r.en:r.zh,text:story},craft('generator_'+r.id)]);}
 const mimicFile=`${book}/entries/research/placeholders.json`,mimic=read(mimicFile);mimic.name=en?'Aether Mimics':'以太拟材';mimic.pages[0].text=en?'Matter follows a pattern. Teach aether that pattern, and it can take the place of stone or water. $(l)Use a shaped mimic to place its block.$() Aether is spent; the mimic remains.$(br2)Right-click a blank mimic to open it: put a sample at the center and the required materials around it. JEI records the combinations.':'物质依照纹样成形。将纹样留给以太，它便能成为石头，也能成为一股水。$(br2)$(l)手持以太拟材右键，可放出对应方块$()；消耗的是以太，拟材仍会留下。右键打开空白拟材，中心放参考物，周围放所需材料；具体组合可查阅 JEI。';write(mimicFile,mimic);
 const observations=read(`${book}/entries/research/observations.json`);for(const t of extra)if(!observations.pages.some(p=>p.anchor===t.id.toLowerCase()))observations.pages.push(page(t.zh,t.en,en?t.en:t.zh,en?t.en:t.zh)),Object.assign(observations.pages.at(-1),{anchor:t.id.toLowerCase(),text:en?'Look through the lens at '+t.sources.map(s=>`$(item)${s.split(':')[1].replaceAll('_',' ')}$()`).join(', ')+'.':'透过虚空透镜，观察'+t.sources.map(s=>({anvil:'铁砧',grindstone:'砂轮',enchanting_table:'附魔台',netherite_block:'下界合金块'}[s.split(':')[1]])).join('、')+'。'});write(`${book}/entries/research/observations.json`,observations);
 // Keep existing chapters consistent without touching registry IDs, links or recipe names.
 const walk=d=>fs.readdirSync(d,{withFileTypes:true}).flatMap(f=>f.isDirectory()?walk(path.join(d,f.name)):[path.join(d,f.name)]);
 for(const file of walk(`${book}/entries`)){const j=read(file);j.name=j.name.replaceAll('魔力','以太').replaceAll('法力','以太').replace(/\bMana\b/g,'Aether');for(const p of j.pages||[])for(const key of ['text','title'])if(p[key])p[key]=p[key].replaceAll('魔力','以太').replaceAll('法力','以太').replace(/\bMana\b/g,'Aether').replace(/\bmana\b/g,'aether');for(const key of ['name'])j[key]=j[key].replaceAll('唤物印','以太拟材').replaceAll('未元质','未元物质').replaceAll('Conjuration Sigils','Aether Mimics').replaceAll('Conjuration Sigil','Aether Mimic');for(const p of j.pages||[])for(const key of ['text','title'])if(p[key])p[key]=p[key].replaceAll('唤物印','以太拟材').replaceAll('未元质','未元物质').replaceAll('Conjuration Sigils','Aether Mimics').replaceAll('Conjuration Sigil','Aether Mimic').replaceAll('conjuration sigil','aether mimic').replaceAll('unformed tools','dark matter tools');write(file,j);}
}

await import('./reform-material-recipes.mjs');
