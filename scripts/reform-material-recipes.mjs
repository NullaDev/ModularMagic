import fs from 'node:fs';
import path from 'node:path';
import {Pixels,png} from './generate-prototype-textures.mjs';
const a='src/main/resources/assets/modularmagic',d='src/main/resources/data/modularmagic';
const read=p=>JSON.parse(fs.readFileSync(p,'utf8'));
const write=(p,j)=>{fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,JSON.stringify(j,null,2)+'\n');};
const mc=id=>'minecraft:'+id,mod=id=>'modularmagic:'+id;
const ingredient=id=>({item:/^[KP]\d+$/.test(id)?mod('rune_plate_'+id.toLowerCase()):id.includes(':')?id:mc(id)});
// Inputs describe the observed source of the material, rather than another sample of its output.
const shapes={
 water:['water_bucket','water_bucket','K03','K07'],
 lava:['lava_bucket','pointed_dripstone','cauldron','K03','K06','K10'],
 cobblestone:['water_bucket','lava_bucket','K03'],stone:['water_bucket','water_bucket','lava_bucket','K03','K05'],
 basalt:['lava_bucket','soul_soil','blue_ice','K03','K08'],
 snow:['snowball','carved_pumpkin','K03','K07'],ice:['water_bucket','water_bucket','K03','K06','K08'],
 fire:['lava_bucket','oak_planks','K03','K19','K10'],
 rails:['sticky_piston','slime_ball','K22','K07','K03'],
 carpets:['sticky_piston','slime_ball','K22','K07','K03'],
 plants:['dirt','water_bucket','K03','K06','K10','K07'],
 tnt:['tnt','dead_tube_coral_fan','detector_rail','slime_block','sticky_piston','K22','K18','K03'],
 gravity:['ender_eye','sticky_piston','slime_block','K21','K22','K03'],
 powder_snow:['cauldron','bucket','snowball','K03','K06','K10'],
 obsidian_lava:['water_bucket','water_bucket','lava_bucket','K03','K08'],
 obsidian_platform:['ender_eye','obsidian','K21','K29','K03','K12'],
 obsidian_pillars:['end_crystal','end_crystal','end_crystal','end_crystal','K07','K12','K03'],
 amethyst_cluster:['amethyst_block','amethyst_shard','K03','K06','K10'],
 pointed_dripstone:['dripstone_block','water_bucket','pointed_dripstone','K03','K06','K10'],
 sculk:['sculk_catalyst','experience_bottle','K03','K32']
};
const families=['gravity','rails','plants','carpets'];
const shapeTarget=id=>id.startsWith('obsidian_')?'obsidian':id;
const machines={
 water:['K24','K03','K07','water_bucket','water_bucket'],
 stone:['K24','K03','water_bucket','lava_bucket'],
 lava:['K24','K06','K10','lava_bucket','pointed_dripstone','cauldron'],
 equipment:['K24','K03','K10','armor_stand','iron_chestplate'],
 snow:['K24','K03','K13','snow_block','snow_block','carved_pumpkin','rail'],
 wool:['K24','K06','K03','white_wool','shears','grass_block'],
 fire:['K24','K19','K10','lava_bucket','oak_planks'],
 crops:['K24','K06','K10','wheat_seeds','dirt','iron_hoe'],
 ice:['K24','K06','K08','water_bucket','ice'],
 copper:['K24','K06','K10','copper_ingot','oxidized_copper'],
 honey:['K24','K06','K03','honeycomb','glass_bottle','poppy'],
 sand:['K24','K22','K21','sand','sticky_piston','ender_eye'],
 sculk:['K24','K32','K03','sculk_catalyst','rotten_flesh'],
 iron_golem:['K24','K03','K08','iron_block','carved_pumpkin','bell'],
 rails:['K24','K22','K07','rail','slime_ball','sticky_piston'],
 amethyst:['K24','K06','K10','amethyst_shard','amethyst_block'],
 chorus:['K24','K06','K03','chorus_flower','end_stone'],
 mushrooms:['K24','K06','K10','brown_mushroom','red_mushroom','dirt'],
 music:['K24','K04','K06','jukebox','amethyst_shard'],
 egg:['K24','K06','K03','egg','wheat_seeds','feather'],
 curing:['K24','K20','K06','golden_apple','rotten_flesh','fermented_spider_eye'],
 tnt:['K24','K22','K18','tnt','dead_tube_coral_fan','detector_rail','slime_block','sticky_piston'],
 experience:['K24','K32','experience_bottle','lapis_lazuli']
};
for(const file of fs.readdirSync(d+'/recipe').filter(f=>f.startsWith('placeholder_')))fs.unlinkSync(d+'/recipe/'+file);
for(const [id,inputs]of Object.entries(shapes))write(`${d}/recipe/placeholder_${id}.json`,{type:mod('placeholder'),target:families.includes(id)?mod(id):mc(shapeTarget(id)),ingredients:inputs.map(ingredient)});
for(const [id,inputs]of Object.entries(machines))write(`${d}/recipe/generator_${id}.json`,{type:'minecraft:crafting_shapeless',category:'misc',ingredients:[ingredient(mod('generator_housing')),...inputs.map(ingredient)],result:{id:mod('aether_generator'),count:1,components:{'minecraft:custom_data':{AetherDevice:{Generator:{Rule:id}}}}}});
for(const file of ['gravity_mimic_bind.json','gravity_mimic_separate.json'])if(fs.existsSync(d+'/recipe/'+file))fs.unlinkSync(d+'/recipe/'+file);
for(const family of families)for(const separate of [false,true])write(d+'/recipe/mimic_'+family+(separate?'_separate':'_bind')+'.json',{type:mod('mimic_binding'),family,separate});
// Phenomena remain research subjects; only fundamental knowledge is carved into a recipe plate.
for(const [dir,pattern]of [[d+'/recipe/carving',/^plate_p\d+\.json$/],[a+'/models/item',/^rune_plate_p\d+\.json$/],[a+'/textures/item',/^rune_plate_p\d+\.png$/]])for(const file of fs.readdirSync(dir).filter(f=>pattern.test(f)))fs.unlinkSync(dir+'/'+file);
const colors={white:'#dce1dc',orange:'#dd852e',magenta:'#b04bba',light_blue:'#68abcb',yellow:'#d9bf39',lime:'#81b339',pink:'#d588a4',gray:'#4c5258',light_gray:'#969a92',cyan:'#39858e',purple:'#7652a4',blue:'#4149a2',brown:'#805a3c',green:'#657a3a',red:'#a3443c',black:'#303236'};
const gravity=['sand','red_sand','gravel','anvil','chipped_anvil','damaged_anvil','dragon_egg','suspicious_sand','suspicious_gravel',...Object.keys(colors).map(c=>c+'_concrete_powder')];
const woods=['oak','spruce','birch','jungle','acacia','dark_oak','mangrove','cherry'];
const plants=[...woods.flatMap(w=>[w+'_log',w+'_wood','stripped_'+w+'_log','stripped_'+w+'_wood',w+'_leaves',w==='mangrove'?'mangrove_propagule':w+'_sapling']),
 'azalea_leaves','flowering_azalea_leaves','azalea','flowering_azalea','melon','pumpkin','wheat','carrots','potatoes','beetroots','melon_stem','pumpkin_stem',
 'sugar_cane','cactus','kelp','kelp_plant','bamboo','bamboo_sapling','bamboo_block','stripped_bamboo_block','cocoa','vine','weeping_vines','weeping_vines_plant','twisting_vines','twisting_vines_plant',
 'chorus_plant','chorus_flower','nether_wart','sweet_berry_bush','brown_mushroom','red_mushroom','short_grass','tall_grass','fern','large_fern','dead_bush',
 'seagrass','tall_seagrass','sea_pickle','lily_pad','hanging_roots','mangrove_roots','muddy_mangrove_roots',
 'dandelion','poppy','blue_orchid','allium','azure_bluet','red_tulip','orange_tulip','white_tulip','pink_tulip','oxeye_daisy','cornflower','lily_of_the_valley','wither_rose',
 'sunflower','lilac','rose_bush','peony','pink_petals','torchflower','torchflower_crop','pitcher_plant','pitcher_crop'];
const rails=['rail','powered_rail','detector_rail','activator_rail'],carpets=Object.keys(colors).map(c=>c+'_carpet');
for(const [family,values]of Object.entries({gravity,rails,plants,carpets}))write(d+'/tags/block/mimic/'+family+'.json',{replace:false,values});
const targets=[...new Set([...Object.keys(shapes).filter(id=>!families.includes(id)).map(shapeTarget),...gravity,...rails,...plants,...carpets])];
write(d+'/mimic_appearances.json',targets.map(mc));
const palette={obsidian:'#382b50',powder_snow:'#e1f0ee',amethyst_cluster:'#b28de3',pointed_dripstone:'#ab8a72',sculk:'#17474d',plants:'#719f50',carpets:'#a69baf',water:'#3d9bdb',lava:'#f07c24',cobblestone:'#7a858c',stone:'#a2abb3',basalt:'#565f68',snow:'#e4f4f3',ice:'#8ad9e8',fire:'#f2b63d',rail:'#b0a291',powered_rail:'#e3bc54',detector_rail:'#be7666',activator_rail:'#a98773',tnt:'#cf574c',sand:'#ddcd8d',red_sand:'#c58045',gravel:'#939191',anvil:'#596576',chipped_anvil:'#73808c',damaged_anvil:'#89929b',dragon_egg:'#77559a',suspicious_sand:'#c8b479',suspicious_gravel:'#797c7d'};
const model=(name,texture)=>write(`${a}/models/item/${name}.json`,{parent:'minecraft:item/generated',textures:{layer0:mod('item/'+texture)}});
function texture(name,id){const p=new Pixels();p.rect(2,2,12,12,'#42576d').rect(3,3,10,10,'#a6d2d4').rect(4,4,8,8,'#203347');const c=palette[id]||colors[id.replace('_concrete_powder','').replace('_carpet','')]||(plants.includes(id)?(id.includes('log')||id.includes('wood')?'#a7774b':'#72a350'):'#95b6d7');p.rect(5,5,6,6,c);if(plants.includes(id)){let hue=[...id].reduce((v,ch)=>(v*31+ch.charCodeAt(0))>>>0,1);p.set(5+hue%6,5+(hue>>>3)%6,'#d4b38a');p.set(5+(hue>>>6)%6,5+(hue>>>9)%6,'#325d44');}
 if(id==='water'){p.line(5,6,7,6,'#b2f0ff').line(8,9,10,9,'#b2f0ff');}
 else if(id==='lava'||id==='fire'){p.line(6,9,7,6,'#ffde73').line(8,10,9,7,'#ffd268');}
 else if(id.includes('rail')){p.line(5,5,10,10,'#dde4e9').line(5,7,8,10,'#715747');}
 else if(id==='tnt'){p.rect(5,7,6,2,'#f3e9d2').line(7,7,8,7,'#50353d').line(7,8,7,9,'#50353d');}
 else if(id==='obsidian'){p.line(5,7,10,5,'#6b4a89').line(6,10,10,8,'#262138');}
 else if(id==='amethyst_cluster'){p.line(6,10,5,7,'#d5b6fa').line(8,10,8,4,'#eee1ff').line(10,10,11,6,'#9374c7');}
 else if(id==='pointed_dripstone'){p.rect(5,5,6,2,'#c4a78f').rect(6,7,4,2,'#a48469').rect(7,9,2,2,'#d4b99f');}
 else if(id==='sculk'){p.set(5,6,'#3db1a5');p.set(8,8,'#55dbcf');p.set(10,5,'#3b9c96');p.set(7,10,'#8de2d2');}
 else if(id==='plants'||plants.includes(id)&&!id.includes('log')&&!id.includes('wood')){p.line(8,10,8,5,'#bde394').line(8,7,5,5,'#2c6c3e').line(8,8,10,6,'#d3ec92');}
 else if(id==='carpets'||id.endsWith('_carpet')){p.rect(5,6,6,4,c).line(5,10,10,10,'#e0d8cf').line(5,8,10,8,'#67788d');}
 else if(id==='gravity'){p.rect(6,5,4,3,'#b7d4e4').line(8,8,8,11,'#e3f9fa').line(6,9,8,11,'#e3f9fa').line(10,9,8,11,'#e3f9fa');}
 else {let h=[...id].reduce((v,c)=>(v*31+c.charCodeAt(0))>>>0,1);for(let i=0;i<7;i++)p.set(5+((h>>>(i*3))%6),5+((h>>>(i*2))%6),i%2?'#d3dce0':'#637382');}
 p.set(3,3,'#e8fffb');p.set(12,12,'#66829b');fs.writeFileSync(`${a}/textures/item/${name}.png`,png(p));model(name,name);
}
texture('gravity_placeholder','gravity');texture('rail_placeholder','rail');texture('plant_placeholder','plants');texture('carpet_placeholder','carpets');texture('mimic_bound','bound');
const overrides=[{predicate:{'modularmagic:material':1},model:mod('item/mimic_bound')}];
for(let i=0;i<targets.length;i++){texture('mimic_'+targets[i],targets[i]);overrides.push({predicate:{'modularmagic:material':i+2},model:mod('item/mimic_'+targets[i])});}
write(a+'/models/item/item_placeholder.json',{parent:'minecraft:item/generated',textures:{layer0:mod('item/item_placeholder')},overrides});
for(const [locale,en]of [['zh_cn',false],['en_us',true]]){
 const langFile=`${a}/lang/${locale}.json`,lang=read(langFile);for(const k of Object.keys(lang))if(/^item\.modularmagic\.rune_plate_p\d+$/.test(k))delete lang[k];
 lang['research.modularmagic.k34']=en?'Dark Matter':'未元物质';lang['item.modularmagic.rune_plate_k34']=en?'Rune Plate: Dark Matter':'符文板：未元物质';lang['item.modularmagic.gravity_placeholder']=en?'Aether Mimic: Gravity Blocks':'以太拟材：重力方块';lang['jei.modularmagic.gravity']=en?'Binding Gravity Mimics':'重力拟材调制';write(langFile,lang);
 const book=`${a}/patchouli_books/assembly_guide/${locale}/entries`;
 const matter=read(`${book}/equipment/unformed.json`);matter.name=en?'Dark Matter':'未元物质';matter.pages=matter.pages.filter(p=>!p.recipe||p.recipe===mod('unformed_matter'));matter.pages[0].title=matter.name;write(`${book}/equipment/unformed.json`,matter);
 for(const r of read(d+'/generator_defaults.json')){const p=`${book}/energy/generator_${r.id}.json`,j=read(p);j.icon=mc(machines[r.id].find(s=>!s.startsWith('K')));write(p,j);}
 const studyFile=`${book}/research/first_study.json`,study=read(studyFile);study.pages[0].text=en?'$(l)Use unfinished notes on the research desk.$() Place ink beside the paper or keep it in your pack; the desk takes what it needs when you begin.$(br2)Choose a shape on the right and fit it into a slot of the same color. Left-click a node to adjust its values, or right-click a loose end to lift it away. The shapes on the paper are impressions: your actual gems stay in your pack.':'$(l)手持未解读的笔记，右键研究台$()。墨水可以放在纸旁，也可以留在背包里；开始解读时才会用上。$(br2)选右侧虚影，填入同色接口。左键节点调整数值，右键取回末端的节点。纸上的纹样只是推演，背包里的宝石不必放进去。';write(studyFile,study);
 const file=`${book}/research/placeholders.json`,j=read(file);j.pages=j.pages.filter(p=>p.recipe!==mod('item_placeholder')&&p.item!==mod('gravity_placeholder')&&!['记下它从何而来','Remember the Source'].includes(p.title));j.pages.push({type:'patchouli:text',title:en?'Remember the Source':'记下它从何而来',text:en?'Two sources teach water to return. Lava remembers a pointed dripstone and the vessel beneath it. Rails remember a piston’s repeated push; TNT remembers the moving assembly that leaves a second charge behind.$(br2)Bring those traces to the blank mimic, together with the basic rune plates that describe the change. JEI records the exact combinations.':'两格源水，记下水如何再次涌出；滴水石锥与炼药锅，记下岩浆如何一滴滴积满。铁轨留下活塞反复推动的痕迹，TNT 则记着那段会多留下一份引信的运动。$(br2)将这些线索与描述变化的基础符文板放在空白拟材周围。确切的组合可查阅 JEI。'},{type:'patchouli:spotlight',item:mod('gravity_placeholder'),text:en?'One rule carries sand, gravel, anvils and concrete powder alike. $(l)Shape a gravity mimic first.$() A falling block can serve as the central reference.$(br2)Craft it together with a gravity block, in either order, to bind that material. Craft the bound mimic alone to recover both the common mimic and the original block.':'沙、沙砾、铁砧与混凝土粉末，遵从同一条坠落的规则。$(l)先制成通用的重力方块拟材$()；定型时，中心可用任意重力方块作参考。$(br2)把它与具体的重力方块无序合成，即可选定要生成的物质。将选定后的拟材单独放回合成格，便会拆回通用拟材，并返还原方块。'},{type:'patchouli:crafting',recipe:mod('item_placeholder')});write(file,j);
}
console.log(`Material recipes: ${Object.keys(shapes).length} shapes, ${Object.keys(machines).length} devices, ${targets.length} distinct mimic appearances, one shared equipment chapter.`);

// Existing observation pages may predate a renamed discovery.
const walk=dir=>fs.readdirSync(dir,{withFileTypes:true}).flatMap(e=>e.isDirectory()?walk(path.join(dir,e.name)):[path.join(dir,e.name)]);
for(const file of walk(a+'/patchouli_books').filter(f=>f.endsWith('.json'))){const j=read(file);const visible=o=>{if(!o||typeof o!=='object')return;for(const [key,value]of Object.entries(o)){if(['name','title','text'].includes(key)&&typeof value==='string')o[key]=value.replaceAll('未定的物质','未元物质').replaceAll('Matter Unwritten','Dark Matter');else if(typeof value==='object')visible(value);}};visible(j);write(file,j);}


await import('./generate-utility-assets.mjs');

const recipeWalk=dir=>fs.readdirSync(dir,{withFileTypes:true}).flatMap(e=>e.isDirectory()?recipeWalk(path.join(dir,e.name)):[path.join(dir,e.name)]);
const usedPlates=new Set();for(const file of recipeWalk(d+'/recipe').filter(f=>!f.includes(path.sep+'carving'+path.sep)))for(const m of fs.readFileSync(file,'utf8').matchAll(/modularmagic:rune_plate_(k\d+)/g))usedPlates.add(m[1].toUpperCase());
write(d+'/rune_plates.json',[...usedPlates].sort());
for(const t of read(d+'/research/topics.json'))if(!usedPlates.has(t.id))for(const file of [d+'/recipe/carving/plate_'+t.id.toLowerCase()+'.json',a+'/models/item/rune_plate_'+t.id.toLowerCase()+'.json',a+'/textures/item/rune_plate_'+t.id.toLowerCase()+'.png'])if(fs.existsSync(file))fs.unlinkSync(file);
for(const locale of ['zh_cn','en_us']){const file=a+'/lang/'+locale+'.json',lang=read(file);for(const k of Object.keys(lang))if(k.startsWith('item.modularmagic.rune_plate_')&&!usedPlates.has(k.slice('item.modularmagic.rune_plate_'.length).toUpperCase()))delete lang[k];write(file,lang);}

await import('./reorganize-handbook.mjs');
