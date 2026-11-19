import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {generateEnergyGuide} from './generate-energy-guide.mjs';
import {generatePrototypeTextures} from './generate-prototype-textures.mjs';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const assets=path.join(root,'src/main/resources/assets/modularmagic');
generatePrototypeTextures(assets);
function write(file,value){fs.mkdirSync(path.dirname(file),{recursive:true});fs.writeFileSync(file,JSON.stringify(value,null,2)+'\n');}
const blocks={aether_disruptor:['以太扰乱器','Aether Disruptor'],aether_tank:['基础以太储槽','Basic Aether Reservoir'],expanded_aether_tank:['扩容以太储槽','Expanded Aether Reservoir'],ultimate_aether_tank:['终极以太储槽','Ultimate Aether Reservoir'],aether_condenser:['以太凝结器','Aether Condenser'],aether_crystallization_chamber:['以太结晶室','Aether Crystallization Chamber'],aether_emitter:['基础以太发射器','Basic Aether Emitter'],expanded_aether_emitter:['扩容以太发射器','Expanded Aether Emitter'],ultimate_aether_emitter:['终极以太发射器','Ultimate Aether Emitter'],creative_aether_generator:['创造以太发生器','Creative Aether Generator'],aether_resonator:['以太共鸣器','Aether Resonator'],personal_aether_anchor:['个人供能锚','Personal Aether Anchor'],world_anchor:['世界锚','World Anchor']};
const items={aether_shard:['以太碎片','Aether Fragment'],crystalline_aether_shard:['晶化以太碎片','Crystalline Aether Fragment'],aether_tuner:['以太调谐器','Aether Tuner'],aether_receiver:['随身以太接收器','Portable Aether Receiver']};
items.interdimensional_lens=['定界之眼','Worldbinding Eye'];
blocks.creative_aether_tank=['创造以太储槽','Creative Aether Reservoir'];
blocks.creative_aether_emitter=['创造以太发射器','Creative Aether Emitter'];
const messages={
 network_cycle:['以太不能沿着闭环流动，请选择另一条支路。','Aether cannot follow a closed loop. Choose another branch.'],
 network_crystal:['先装入紫水晶，并让它接上主网。','Insert Amethyst Shards and reconnect it to the main network first.'],
 network_asleep:['上游的共鸣已经沉寂，请沿着线路检查。','The upstream resonance is silent. Follow the line back and inspect it.'],
 network_target:['这件装置不能接收共鸣。','This device cannot receive resonance.'],
 network_detached:['已断开上游联系。','Upstream link severed.'],
 fragment_value:['每枚封存 %s 以太','Stores %s aether per fragment'],
 resonance_upgrades:['紫水晶碎片 %s / %s，带宽 %s/秒','Amethyst shards %s / %s; bandwidth %s/s'],
 device_status:['以太 %s / %s','Aether %s / %s'],
 frequency_bound:['已接上这条以太支路，频率 %s。','Joined this aether branch, frequency %s.'],select_resonance_target:['已记住这股共鸣，请右键下游共鸣器或接收装置。','Resonance remembered. Use on a downstream resonator or receiving device.'],selection_cleared:['已取消调谐器选择。','Tuner selection cleared.'],
 use_reservoir:['把容器放进以太储槽中充能。','Use an aether reservoir to charge containers.'],crystal_waiting:['等待以太结晶，请为结晶室供能。','Waiting for crystals. Supply aether to the chamber.'],
 output_paused:['已暂停输出','Output paused'],output_running:['正在输出','Output enabled'],
 fragment_time:['%s tick 内自动消失','Disappears in %s ticks'],denied:['这台设备属于其他玩家。','This device belongs to another player.'],
 receiver_bound:['已绑定供能端。携带接收器和魔力容器即可补能。','Source bound. Carry this receiver and a mana container to recharge.'],
 setting:['设备设置：%s','Device setting: %s'],select_target:['已选中发射器，请右键接收目标。','Emitter selected. Right-click the destination.'],
 out_of_range:['目标超出射程。','Destination is out of range.'],linked:['射流目标已绑定。','Pulse destination bound.'],
 receiver_help:['右键供能锚或共鸣器绑定，放在背包或副手接收以太。','Bind to an anchor or resonator; carry in your inventory or offhand.'],
 tuner_help:['依次右键发射器、接收端来绑定；潜行右键调整设备设置。','Select emitter, then destination. Sneak-use to configure devices.'],
 battery_busy:['已有一个容器正在充能，潜行空手右键取出。','A container is charging. Sneak-use with an empty hand to retrieve it.'],
 need_fragment:['缓存至少需要 %s 以太才能取出碎片。','Extracting a fragment requires at least %s aether.'],
 loader_enabled:['世界锚已启用，充入至少 %s 以太后开始加载。','World anchor enabled. Charge it with at least %s aether to begin.'],
 loader_disabled:['世界锚已关闭。','World anchor disabled.'],status:['以太 %s / %s　充能位：%s','Aether %s / %s | Charging: %s'],
 no_battery:['空','Empty'],supply:['补给 +%s/秒','Supply +%s/s'],disconnected:['补给未连接','Supply disconnected'],
 inspect:['%s　%s / %s','%s | %s / %s'],target:['目标：%s','Destination: %s']
};
const jadeText={
 upstream:['上游：%s','Upstream: %s'],'state.upstream_asleep':['上游共鸣沉寂','Upstream resonance silent'],
 'state.amethyst_required':['空壳：请装入紫水晶碎片','Empty shell: insert Amethyst Shards'],
 resonance_upgrades:['紫水晶碎片：%s / %s','Amethyst shards: %s / %s'],lossless_distance:['无损距离：%s 格','Lossless distance: %s blocks'],
 'state.fragment_ready':['可取出以太碎片','Ready to extract fragments'],
 products:['晶化碎片：%s / 64','Crystalline fragments: %s / 64'],'state.output_full':['结晶产物已满','Crystal output full'],'state.product_ready':['可取出晶化碎片','Crystalline fragments ready'],'state.crystallizing':['正在结晶','Crystallizing'],
 stored:['以太 %s / %s','Aether %s / %s'],battery:['魔力 %s / %s','Mana %s / %s'],infinite:['以太来源：无限','Aether source: unlimited'],
 status:['状态：%s','Status: %s'],charging_item:['充能容器：%s','Charging: %s'],reserved:['施法预留：%s','Reserved for spells: %s'],
 production:['采集上限：%s/秒','Collection limit: %s/s'],bandwidth:['设备带宽：%s/秒','Device bandwidth: %s/s'],range:['有效距离：%s 格','Range: %s blocks'],
 target:['目标：%s','Destination: %s'],in_flight:['在途以太：%s','In-flight aether: %s'],channel:['频道：%s','Channel: %s'],
 cross_dimension:['已安装跨维度升级（额外耗能 %s%%）','Interdimensional upgrade installed (+%s%% cost)'],same_dimension:['同维度供能（额外耗能 %s%%）','Same-dimension supply (+%s%% cost)'],
 player_rate:['个人接收上限：%s/秒','Personal delivery limit: %s/s'],loading:['选中 %s 个区块，维护 %s/秒','%s selected chunks, upkeep %s/s'],
 stable:['永久稳定','Permanently stable'],expires:['%s tick 内自动消失','Disappears in %s ticks'],fragment_value:['每枚封存 %s 以太','Stores %s aether per fragment'],
 wand_rates:['魔力消耗 ×%s　专注消耗 ×%s','Mana cost x%s | Focus cost x%s'],cores:['已装入 %s / 9 枚核心','Cores: %s / 9'],
 core_cost:['基础消耗：魔力 %s　专注 %s','Base cost: %s mana, %s focus'],core_unpriced:['法术费用暂不可读','Spell cost unavailable'],core_unformed:['未成形，无法施法','Unformed; cannot cast'],
 receiver_bound:['已绑定；携带后为容器补能','Bound; carry to recharge containers'],receiver_unbound:['尚未绑定供能端','No supply source bound'],carried_energy:['保存的以太：%s','Stored aether: %s'],
 'state.paused':['输出已暂停','Output paused'],'state.generating':['持续供能','Continuous supply'],'state.disabled':['尚未启用','Disabled'],
 'state.loading':['正在维持区块加载','Maintaining loaded chunks'],'state.loader_charge':['至少需要 %s 以太启动','Needs %s aether to start'],
 'state.loader_waiting':['等待启动，请检查所有者和区块配额','Waiting; check owner and chunk quota'],'state.water_required':['水源再生结构未就绪','Water regeneration structure not ready'],
 'state.full':['缓存已满','Storage full'],'state.collecting':['正在采集','Collecting'],'state.empty':['等待以太','Waiting for aether'],
 'state.charged':['容器已满或空间已预留','Container full or space reserved'],'state.charging':['正在充能','Charging'],'state.ready':['就绪','Ready'],
 'state.in_flight':['射流正在传输','Pulse in flight'],'state.unbound':['尚未绑定目标','No destination bound'],'state.out_of_range':['目标超出射程','Destination out of range'],
 'state.target_unloaded':['目标区块未加载','Destination chunk unloaded'],'state.target_missing':['目标已更换，请重新绑定','Destination replaced; bind again'],
 'state.foreign_target':['目标属于其他玩家','Destination belongs to another player'],'state.blocked':['射流路径受阻','Pulse route obstructed'],'state.target_full':['目标无法接收更多以太','Destination cannot accept more aether']
};
for(const [locale,idx] of [['zh_cn',0],['en_us',1]]){
 const file=path.join(assets,'lang',locale+'.json');const lang=JSON.parse(fs.readFileSync(file,'utf8').replace(/^\uFEFF/,''));
 delete lang['energy.modularmagic.upgrade'];
 for(const[id,names]of Object.entries(blocks))lang['block.modularmagic.'+id]=names[idx];
 for(const[id,names]of Object.entries(items))lang['item.modularmagic.'+id]=names[idx];
 for(const[id,names]of Object.entries(messages))lang['energy.modularmagic.'+id]=names[idx];
 for(const[id,names]of Object.entries(jadeText))lang['jade.modularmagic.'+id]=names[idx];
 lang['config.jade.plugin_modularmagic.aether_devices']=idx===0?'以太设备储量与工作状态':'Aether device storage and state';
 lang['config.jade.plugin_modularmagic.aether_items']=idx===0?'魔力物品与碎片信息':'Mana item and fragment information';
 fs.writeFileSync(file,'{\n'+Object.entries(lang).map(([key,value])=>'    '+JSON.stringify(key)+':  '+JSON.stringify(value)).join(',\n')+'\n}\n');
}
function box(from,to,tex){return {from,to,faces:Object.fromEntries(['down','up','north','south','west','east'].map(f=>[f,{texture:'#'+tex}]))};}
for(const id of Object.keys(blocks)){
 const crystalline=id==='aether_crystallization_chamber';
 const condenser=id==='aether_condenser';
 const tank=id.endsWith('aether_tank');
 const emitter=id.endsWith('aether_emitter');
 const creative=id.startsWith('creative');
 const tex=creative||id.startsWith('ultimate')?'modularmagic:block/frame_netherite':id.startsWith('expanded')||crystalline?'modularmagic:block/frame_gold':'modularmagic:block/frame_copper';
 const textures={frame:tex,dark:'modularmagic:block/aether_casing',glow:'modularmagic:block/aether_port',glass:'modularmagic:block/aether_glass',particle:tex,fluid:'modularmagic:block/aether_liquid',crystal:'modularmagic:block/aether_crystal',...(creative?{accent:'modularmagic:block/creative_checker'}:{})};
 let elements;
 if(crystalline||condenser){
  textures.core=crystalline?'minecraft:item/nether_star':'minecraft:item/ender_pearl';textures.trim=crystalline?'modularmagic:block/frame_gold':'modularmagic:block/frame_copper';
  elements=[box([0,0,0],[16,2,16],'dark'),box([4,2,4],[12,3,12],'trim')];
  for(const x of[0,14])for(const z of[0,14])elements.push(box([x,2,z],[x+2,16,z+2],'frame'));
  elements.push(box([0,14,0],[16,16,2],'frame'),box([0,14,14],[16,16,16],'frame'),box([0,14,2],[2,16,14],'frame'),box([14,14,2],[16,16,14],'frame'));
  // Permanent nucleus: a Nether Star stabilizes crystals; the condenser reuses the disruptor's Ender Pearl.
  elements.push({from:[4.5,4.5,8],to:[11.5,11.5,8],faces:{north:{texture:'#core',uv:[0,0,16,16]},south:{texture:'#core',uv:[0,0,16,16]}}},
                {from:[8,4.5,4.5],to:[8,11.5,11.5],faces:{west:{texture:'#core',uv:[0,0,16,16]},east:{texture:'#core',uv:[0,0,16,16]}}});
 }else if(tank){elements=[box([0,0,0],[16,2,16],'frame')];for(const x of[0,14])for(const z of[0,14])elements.push(box([x,2,z],[x+2,16,z+2],'frame'));
  elements.push(box([0,14,0],[16,16,2],'frame'),box([0,14,14],[16,16,16],'frame'),box([0,14,2],[2,16,14],'frame'),box([14,14,2],[16,16,14],'frame'));
 }else if(emitter)elements=[box([0,0,0],[16,3,16],'dark'),box([4,3,4],[12,8,12],'frame')];
 else if(id==='aether_disruptor')elements=[box([0,0,0],[16,12,16],'dark'),box([1,12,1],[15,14,15],'frame'),box([4,14,4],[12,16,12],'glow')];
 else if(id==='aether_resonator')elements=[box([1,0,1],[15,3,15],'dark'),box([6,3,6],[10,8,10],'frame')];
 else if(id==='personal_aether_anchor')elements=[box([1,0,1],[15,3,15],'frame'),box([2,3,2],[14,12,14],'dark'),box([4,12,4],[12,16,12],'glow')];
 else if(id==='world_anchor')elements=[box([0,0,0],[16,3,16],'dark'),box([3,3,3],[13,14,13],'frame'),box([5,14,5],[11,16,11],'glow')];
 else elements=[box([0,0,0],[16,3,16],'dark'),box([2,3,2],[14,14,14],'glow'),box([0,14,0],[16,16,16],'frame')];
 if(creative)for(const[from,to]of[[[6,.25,-.05],[10,1.75,.02]],[[6,.25,15.98],[10,1.75,16.05]],[[-.05,.25,6],[.02,1.75,10]],[[15.98,.25,6],[16.05,1.75,10]]]){
  const badge=box(from,to,'accent');for(const face of Object.values(badge.faces))face.uv=[0,0,16,16];elements.push(badge);
 }
 write(path.join(assets,'models/block',id+'.json'),{parent:'minecraft:block/block',...(crystalline||condenser?{render_type:'minecraft:cutout'}:{}),textures,elements});
 write(path.join(assets,'models/item',id+'.json'),emitter?{parent:'minecraft:block/block',textures,elements:[...elements,box([4,7,0],[12,12,11],'frame'),box([5,8,-.3],[11,11,0],'glow'),...(creative?[box([3.8,6.8,3],[12.2,12.2,4],'accent')]:[])]}:{parent:'modularmagic:block/'+id});
 write(path.join(assets,'blockstates',id+'.json'),{variants:Object.fromEntries([['north',0],['east',90],['south',180],['west',270]].map(([d,y])=>['facing='+d,{model:'modularmagic:block/'+id,y}]))});
}
for(const id of [...Object.keys(items),'mana_container_basic','mana_container_expanded','mana_container_ultimate','creative_mana_container','core_wand','creative_wand','formed_spell_core','unformed_spell_core'])write(path.join(assets,'models/item',id+'.json'),{parent:id.endsWith('wand')?'minecraft:item/handheld':'minecraft:item/generated',textures:{layer0:'modularmagic:item/'+id}});
const data=path.join(root,'src/main/resources/data/modularmagic');
write(path.join(data,'tags/item/aether_fragments.json'),{replace:false,values:['modularmagic:aether_shard','modularmagic:crystalline_aether_shard']});
const ingredient=x=>x==='#shard'?{tag:'modularmagic:aether_fragments'}:{item:x.includes(':')?x:'minecraft:'+x};
function recipe(id,pattern,key,result='modularmagic:'+id){write(path.join(data,'recipe',id+'.json'),{type:'minecraft:crafting_shaped',category:'misc',pattern,key:Object.fromEntries(Object.entries(key).map(([k,v])=>[k,ingredient(v)])),result:{id:result,count:1}});}
recipe('aether_disruptor',['CGC','SPS','CCC'],{C:'copper_ingot',G:'glass',S:'stone',P:'ender_pearl'});
recipe('aether_tank',['CGC','GSG','CCC'],{C:'copper_ingot',G:'glass',S:'#shard'});
recipe('expanded_aether_tank',['G G','DTD','GGG'],{G:'gold_ingot',D:'diamond',T:'modularmagic:aether_tank'});
recipe('aether_condenser',['CGC','GDG','CCC'],{C:'copper_ingot',G:'glass',D:'modularmagic:aether_disruptor'});
recipe('aether_crystallization_chamber',['AGA','ENE','CCC'],{A:'iron_ingot',G:'glass',E:'ender_pearl',N:'nether_star',C:'copper_block'});
recipe('aether_emitter',[' GC','SQC','CCC'],{G:'glass',C:'copper_ingot',S:'#shard',Q:'quartz'});
recipe('expanded_aether_emitter',['GQG','DED','GGG'],{G:'gold_ingot',D:'diamond',E:'modularmagic:aether_emitter',Q:'quartz'});
for(const device of ['tank','emitter'])write(path.join(data,'recipe','ultimate_aether_'+device+'.json'),{type:'minecraft:smithing_transform',template:{item:'minecraft:netherite_upgrade_smithing_template'},base:{item:'modularmagic:expanded_aether_'+device},addition:{item:'minecraft:netherite_ingot'},result:{id:'modularmagic:ultimate_aether_'+device,count:1}});
recipe('aether_tuner',['  Q',' S ','C  '],{Q:'quartz',S:'#shard',C:'copper_ingot'});
recipe('aether_receiver',['CAC','ASA','CAC'],{C:'copper_ingot',A:'amethyst_shard',S:'#shard'});
recipe('aether_resonator',[' G ','GSG','CCC'],{G:'glass',S:'#shard',C:'copper_ingot'});
recipe('personal_aether_anchor',['DSD','AEA','CCC'],{D:'diamond',S:'modularmagic:crystalline_aether_shard',A:'amethyst_shard',E:'ender_chest',C:'copper_ingot'});
recipe('interdimensional_lens',['PDP','CEC','PSP'],{P:'popped_chorus_fruit',D:'dragon_breath',C:'modularmagic:crystalline_aether_shard',E:'ender_eye',S:'netherite_scrap'});
recipe('world_anchor',['OSO','OEO','CCC'],{O:'obsidian',S:'#shard',E:'ender_pearl',C:'copper_ingot'});
recipe('mana_container_basic',[' G ','GSG',' C '],{G:'glass',S:'#shard',C:'copper_ingot'});
recipe('mana_container_expanded',['G G','DCD','GGG'],{G:'gold_ingot',D:'diamond',C:'modularmagic:mana_container_basic'});
write(path.join(data,'recipe/mana_container_ultimate.json'),{type:'minecraft:smithing_transform',template:{item:'minecraft:netherite_upgrade_smithing_template'},base:{item:'modularmagic:mana_container_expanded'},addition:{item:'minecraft:netherite_ingot'},result:{id:'modularmagic:mana_container_ultimate',count:1}});
generateEnergyGuide(assets,write);
const tableFile=path.join(assets,'models/block/spell_writing_table.json');
const table=JSON.parse(fs.readFileSync(tableFile,'utf8'));
table.textures={particle:'modularmagic:block/writing_wood',wood:'modularmagic:block/writing_wood',top:'modularmagic:block/writing_surface'};
write(tableFile,table);
console.log('Generated original pixel textures, energy models, recipes and localized guides.');
