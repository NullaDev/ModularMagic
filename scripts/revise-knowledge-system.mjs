import fs from 'node:fs';
import path from 'node:path';
const assets='src/main/resources/assets/modularmagic',data='src/main/resources/data/modularmagic';
const read=p=>JSON.parse(fs.readFileSync(p,'utf8').replace(/^\uFEFF/,''));
const write=(p,v)=>fs.writeFileSync(p,JSON.stringify(v,null,2)+'\n');
const nodes=read(`${data}/node_templates.json`).templates;
const generatorIds=read(data+'/generator_defaults.json').map(r=>r.id);
if(fs.existsSync(data+'/generator_ingredients.json'))fs.unlinkSync(data+'/generator_ingredients.json');
write(`${data}/recipe/generator_housing.json`,{type:'modularmagic:generator_housing'});
const topics=read(`${data}/research/topics.json`);topics.find(t=>t.id==='K31').zh='唤物';topics.find(t=>t.id==='K31').en='Conjuration';write(`${data}/research/topics.json`,topics);
for(const locale of ['zh_cn','en_us']){
 const en=locale==='en_us',base=`${assets}/patchouli_books/assembly_guide/${locale}`;
 const walk=p=>fs.readdirSync(p,{withFileTypes:true}).flatMap(d=>d.isDirectory()?walk(path.join(p,d.name)):[path.join(p,d.name)]);
 for(const file of walk(`${base}/entries`)){
  let j=read(file);const creativeId=s=>typeof s==='string'&&/modularmagic:(?:creative_|.*_creative)/.test(s);
  if(creativeId(j.icon)||/creative_/.test(path.basename(file))){fs.unlinkSync(file);continue;}
  j.pages=j.pages.filter(p=>!creativeId(p.item)&&!creativeId(p.recipe));
  const words=s=>s.replaceAll('空的物品占位符','空白以太拟材').replaceAll('空占位符','空白以太拟材').replaceAll('占位符','以太拟材').replaceAll('物质占位','唤物').replaceAll('Empty Item Placeholder','Blank Aether Mimic').replaceAll('Empty Placeholder','Blank Aether Mimic').replaceAll('Item Placeholder','Aether Mimic').replaceAll('Placeholder','Aether Mimic').replaceAll('placeholder','aether mimic').replaceAll('Material Substitution','Conjuration');
  j.name=words(j.name);for(const pg of j.pages){if(pg.text)pg.text=words(pg.text);if(pg.title)pg.title=words(pg.title);}
  j=JSON.parse(JSON.stringify(j).replaceAll('modularmagic:item_aether mimic','modularmagic:item_placeholder').replaceAll('research/aether mimics','research/placeholders'));write(file,j);
 }
 const lang=read(`${assets}/lang/${locale}.json`);
 for(const key of Object.keys(lang)){if(key.startsWith('item.modularmagic.rune_')&&!key.startsWith('item.modularmagic.rune_plate_'))delete lang[key];else if(typeof lang[key]==='string')lang[key]=lang[key].replaceAll('空的物品占位符','空白以太拟材').replaceAll('空占位符','空白以太拟材').replaceAll('占位符','以太拟材').replaceAll('物质占位','唤物').replaceAll('Empty Item Placeholder','Blank Aether Mimic').replaceAll('Placeholder','Aether Mimic');}
 const labels={
  'item.modularmagic.item_placeholder':['空白以太拟材','Blank Aether Mimic'],
  'placeholder.modularmagic.name':['以太拟材：%s','Aether Mimic: %s'],
  'placeholder.modularmagic.title':['以太拟材','Aether Mimic'],
  'placeholder.modularmagic.blank':['空白以太拟材','Blank Aether Mimic'],
  'placeholder.modularmagic.missing':['背包中没有所需材料或对应以太拟材','Your pack has neither the required material nor its aether mimic'],
  'jade.modularmagic.state.generator_empty':['未装配节点','No nodes fitted'],
  'research.modularmagic.k31':['唤物','Conjuration'],
  'item.modularmagic.research_note':['待研究的笔记：%s','Research Notes: %s'],
  'item.modularmagic.deciphered_note':['已解读的笔记：%s','Deciphered Notes: %s'],
  'research.modularmagic.carving_help':['选择已经掌握的节点，放入空白宝石，再取走刻好的成品。','Choose a known node, insert a blank gem, then take the carving.'],
  'research.modularmagic.help':['带上笔记和墨水，点击「开始研究」。\n左键选右侧虚影，再点同色空槽填入。\n左键节点改参数；右键取回末端节点。\n「参考术式」显示连接，悬停查看参数。\n滚轮翻看；编辑参数时右键可返回。','Carry notes and ink, then click Begin Research.\nSelect a piece on the right and click a matching slot.\nLeft-click a node to edit; right-click a leaf to return it.\nView the Reference and hover nodes for their values.\nScroll to browse; right-click to leave the value editor.'],
  'research.modularmagic.begin':['开始研究','Begin Research'],
  'research.modularmagic.check':['提交研究','Submit Research'],
  'research.modularmagic.load_note':['读取笔记','Load Notes'],
  'research.modularmagic.switch_note':['切换笔记','Switch Notes'],
  'research.modularmagic.take_note':['收回','Retrieve'],
  'research.modularmagic.reference':['参考术式','Reference'],
  'research.modularmagic.draft':['我的解读','My Reading'],
  'research.modularmagic.no_note':['带上尚未读懂的研究笔记，再点击「读取笔记」。','Carry unfinished research notes, then click Load Notes.'],
  'research.modularmagic.reading_prompt':['这份笔记已经写出了答案。点击「解读」，掌握其中的知识。','These notes already hold the answer. Click Read to learn it.'],
  'research.modularmagic.start_prompt':['点击「开始研究」后装配术式','Click Begin Research to assemble the spell'],
  'research.modularmagic.ink_balance':['墨水 %s / %s','Ink %s / %s'],
  'research.modularmagic.creative_ink':['无需墨水','No ink needed'],
  'research.modularmagic.pieces_remaining':['剩余：%s','Remaining: %s'],
  'research.modularmagic.slot_help':['左键选取右侧虚影，再点此填入','Select a piece on the right, then click here'],
  'research.modularmagic.node_help':['左键调整参数；右键取回末端节点','Left-click: edit values; right-click: return a leaf'],
  'research.modularmagic.status_start_first':['先点击下方的「开始研究」','Click Begin Research below first'],
  'research.modularmagic.status_choose_piece':['先选右侧虚影，再填入同色空槽','Select a piece on the right, then a matching slot'],
  'research.modularmagic.status_wrong_type':['颜色不匹配，换一枚对应类型的节点','Colors do not match; choose the matching node type'],
  'research.modularmagic.status_piece_spent':['这枚虚影已经用完，可先从树上取回','No pieces left; return one from the tree first'],
  'research.modularmagic.status_root_fixed':['法术核心留在这里，不用取回','The spell core stays in place'],
  'research.modularmagic.status_children_first':['先取回这个节点下面的子节点','Return this node’s children first'],
  'research.modularmagic.status_reference_only':['这是参考术式；切回「我的解读」进行装配','This is the reference; switch to My Reading to assemble'],
  'research.modularmagic.status_inventory_full':['背包已满，先腾出一格再收回笔记','Make room in your pack before retrieving the notes'],
  'research.modularmagic.status_no_note':['身上没有其他未完成的研究笔记','No other unfinished notes in your pack'],
  'commands.modularmagic.unknown_knowledge':['未知知识：%s','Unknown knowledge: %s'],
  'commands.modularmagic.grant':['已为 %2$s 名玩家解锁 %1$s','Granted %1$s to %2$s player(s)'],
  'commands.modularmagic.revoke':['已为 %2$s 名玩家重新锁定 %1$s','Revoked %1$s from %2$s player(s)'],
  'commands.modularmagic.reset':['已重置 %s 名玩家的知识和研究进度','Reset knowledge and research progress for %s player(s)'],
  'commands.modularmagic.prepare':['已准备 %1$s：解锁前置、锁定目标、发放笔记和墨水（%2$s 名玩家）','Prepared %1$s: prerequisites learned, target locked, notes and ink supplied (%2$s player(s))']
 };
 for(const[k,v]of Object.entries(labels))lang[k]=v[en?1:0];
 delete lang['research.modularmagic.carve_cost'];
 const firstStudy=read(`${base}/entries/research/first_study.json`);firstStudy.pages=[
  {type:'patchouli:spotlight',item:'modularmagic:rune_research_table',link_recipe:true,text:en?'Ink follows the lines you saw through the lens. $(l)Carry your notes and ink, then open the desk.$() Your notes are laid out for you; Switch Notes turns to another unfinished study. Retrieve puts them back in your pack.':'透镜下看见的纹样，还要在纸上慢慢辨认。$(br2)$(l)带上笔记与墨水，打开研究台。$()笔记会自动铺开；「切换笔记」翻到另一份未完成的研究，「收回」则把它放回包里。'},
  {type:'patchouli:text',title:en?'Follow the Lines':'顺着纹样',text:en?'Click $(l)Begin Research$() to spend the ink once. Choose a piece on the right, then fill a slot of the same color. These are borrowed shapes; they stay on the page.$(br2)Left-click a node to change its values. Right-click a node with no children to return it.':'先点$(l)开始研究$()，墨水只在开题时扣除。选右侧的虚影，再填入同色空槽。这些只是纸上的形状，带不出研究台。$(br2)左键节点调整数值，右键取回没有子节点的节点。'},
  {type:'patchouli:text',title:en?'Compare and Understand':'对照与领悟',text:en?'The $(l)Reference$() shows how the lines connect; hover a node to inspect its values. Return to My Reading to assemble your interpretation, then Submit Research.$(br2)Unfinished work waits for your return. A failed attempt does not spend ink again.':'$(l)参考术式$()可以对照连接，悬停节点查看参数。回到「我的解读」继续装配，再提交研究。$(br2)没能读通也不必撕掉重来：离开会保留进度，重试不再多收墨水。'}
 ];write(`${base}/entries/research/first_study.json`,firstStudy);
 const carving=read(`${base}/entries/research/carving.json`);carving.pages=[{type:'patchouli:spotlight',item:'modularmagic:rune_carving_table',link_recipe:true,text:en?'Understanding guides the chisel. $(l)Only a blank gem is needed.$() Choose a known rune, then take the finished node from the output. Ink belongs on the research desk.':'理解藏在手中，凿尖只需沿着纹样走过宝石。$(br2)$(l)只需放入空白宝石。$()选一枚已经掌握的符文，再从成品槽取出节点。墨水留给纸上的研究。'}];write(`${base}/entries/research/carving.json`,carving);
 const sigilFile=`${base}/entries/research/placeholders.json`,sigil=read(sigilFile);sigil.name=en?'Aether Mimics':'以太拟材';sigil.pages[0].text=en?'An imprint remembers how matter takes shape. $(l)Use it on a place to conjure its block.$() Aether is spent, but the sigil remains.$(br2)To shape a blank sigil, place a sample in its center and materials in the ring.':'一枚小印，记着物质成形的纹样。向空处一指，便有了石头或一股水。$(br2)$(l)右键放出对应的方块$()，消耗以太，印记仍会留下。空白印记需要先定型：中心放参考，周围放材料。';write(sigilFile,sigil);
 // Keep the portable jar page focused on obtainable containers.
 const jarsFile=`${base}/entries/equipment/mana_containers.json`,jars=read(jarsFile);jars.pages=jars.pages.filter(p=>!String(p.item).includes('creative'));write(jarsFile,jars);
 fs.writeFileSync(`${assets}/lang/${locale}.json`,JSON.stringify(lang,null,4).replace(/^(\s*"[^"]+":) /gm,'$1  ')+'\n');
}
// Removed registry entries must not retain model/texture clutter in the prototype asset set.
for(const id of generatorIds)for(const file of [`${assets}/models/item/rune_${id}.json`,`${assets}/textures/item/rune_${id}.png`])if(fs.existsSync(file))fs.unlinkSync(file);
