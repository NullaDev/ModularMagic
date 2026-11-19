$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$assetRoot = Join-Path $projectRoot 'src/main/resources/assets/modularmagic'
$catalog = Get-Content -Encoding UTF8 (Join-Path $projectRoot 'src/main/resources/data/modularmagic/node_templates.json') -Raw | ConvertFrom-Json
$utf8 = [System.Text.UTF8Encoding]::new($false)

function Write-JsonFile($relativePath, $value) {
    $path = Join-Path $assetRoot $relativePath
    New-Item -ItemType Directory -Force (Split-Path $path) | Out-Null
    [System.IO.File]::WriteAllText($path, ($value | ConvertTo-Json -Depth 32) + "`n", $utf8)
}

$colors = @{action='purple';modifier='cyan';selector='blue';predicate='yellow';filter='green';effect='red';motion='orange';particle_renderer='magenta';projectile_renderer='light_blue';root='white';trigger='light_gray';projectile='gray'}
$englishCategories = @{action='Action';modifier='Modifier';selector='Selector';predicate='Predicate';filter='Filter';effect='Effect';motion='Motion';particle_renderer='Particle renderer';projectile_renderer='Projectile renderer';root='Spell core';trigger='Origin';projectile='Projectile config'}
$chineseCategories = @{action='动作';modifier='空间修饰';selector='选区';predicate='条件';filter='实体过滤';effect='实体处理';motion='运动';particle_renderer='粒子渲染';projectile_renderer='弹体渲染';root='法术核心';trigger='起点';projectile='弹体配置'}
$en = [ordered]@{
    'itemGroup.modularmagic'='ModularMagic'
    'block.modularmagic.spell_writing_table'='Spell Writing Table'
    'item.modularmagic.spell_writing_table'='Spell Writing Table'
    'item.modularmagic.formed_spell_core'='Formed Spell Core'
    'item.modularmagic.unformed_spell_core'='Unformed Spell Core'
    'tooltip.modularmagic.root'='Insert into a Spell Writing Table to start assembling.'
    'tooltip.modularmagic.nodes'='%s physical nodes stored'
    'tooltip.modularmagic.reopen'='Reopen in a Spell Writing Table to continue editing.'
    'tooltip.modularmagic.unformed'='Incomplete structure or invalid parameters; all nodes are retained.'
    'tooltip.modularmagic.invalid'='Invalid or missing assembly document.'
    'gui.modularmagic.apply'='Apply'
    'gui.modularmagic.center'='Center'
    'gui.modularmagic.take'='Take core'
    'gui.modularmagic.insert_root'='Place a spell core or a saved assembly into the slot beside Take core. Pick up node gems from your inventory, then click a matching colored slot.'
    'gui.modularmagic.scroll_fields'='Scroll here for more fields'
    'gui.modularmagic.controls'='LMB: select / insert   RMB: remove   Drag: pan   Scroll: zoom'
    'gui.modularmagic.root_slot'='Spell core / saved assembly'
    'gui.modularmagic.formed'='Formed spell core'
    'gui.modularmagic.unformed'='Unformed spell core'
    'gui.modularmagic.node_count'='%s nodes; ready to save'
    'gui.modularmagic.locked'='Remove all child items before taking this node.'
}
$zh = [ordered]@{
    'itemGroup.modularmagic'='模块化魔法'
    'block.modularmagic.spell_writing_table'='法术编写台'
    'item.modularmagic.spell_writing_table'='法术编写台'
    'item.modularmagic.formed_spell_core'='成型法术核心'
    'item.modularmagic.unformed_spell_core'='未成形法术核心'
    'tooltip.modularmagic.root'='放入法术编写台开始装配。'
    'tooltip.modularmagic.nodes'='保存了 %s 个实物节点'
    'tooltip.modularmagic.reopen'='放回法术编写台可继续编辑。'
    'tooltip.modularmagic.unformed'='结构不完整或参数有误，全部节点均已保留。'
    'tooltip.modularmagic.invalid'='装配数据损坏或缺失。'
    'gui.modularmagic.apply'='应用参数'
    'gui.modularmagic.center'='居中画布'
    'gui.modularmagic.take'='取出核心'
    'gui.modularmagic.insert_root'='将法术核心或已保存的术式放入右上角核心槽。拿起背包中的节点宝石，点击匹配颜色的空槽装入。'
    'gui.modularmagic.scroll_fields'='在此滚动查看其余参数'
    'gui.modularmagic.controls'='左键选择/放入　右键取出　拖空白平移　滚轮缩放'
    'gui.modularmagic.root_slot'='法术核心 / 已保存术式'
    'gui.modularmagic.formed'='成型法术核心'
    'gui.modularmagic.unformed'='未成形法术核心'
    'gui.modularmagic.node_count'='%s 个节点，可随时保存'
    'gui.modularmagic.locked'='此节点已锁定，请先取走全部子节点物品。'
}
foreach ($category in $colors.Keys) {
    $en[('category.modularmagic.' + $category)] = $englishCategories[$category]
    $zh[('category.modularmagic.' + $category)] = $chineseCategories[$category]
}
$indices = @{}
$en['category.modularmagic.root'] = 'Spell core'
$zh['category.modularmagic.root'] = '法术核心'
$en['slot.modularmagic.root'] = 'Spell core'
$zh['slot.modularmagic.root'] = '法术核心'
$en['gui.modularmagic.empty_core'] = 'Empty spell core'
$zh['gui.modularmagic.empty_core'] = '空白法术核心'
$slotNames = @{root='术式根';action='主法术';child='子节点';children='序列项';modifiers='空间修饰';selector='选区';selectors='选区项';predicate='判断条件';condition='施放条件';fallback='否则';processors='实体效果';filter='实体过滤';list='条件项';motion='运动';renderer='外观';inner='内部外观';config='弹体配置';trigger_type='施法起点';tick='每刻动作';hit='命中效果';land='落地动作';expire='消散动作'}
foreach ($slotName in $slotNames.Keys) { $en[('slot.modularmagic.' + $slotName)] = $slotName -replace '_',' '; $zh[('slot.modularmagic.' + $slotName)] = $slotNames[$slotName] }
$en['slot.modularmagic.root'] = 'Spell core'
$zh['slot.modularmagic.root'] = '法术核心'
$en['gui.modularmagic.review'] = 'Hover to review issues'
$zh['gui.modularmagic.review'] = '悬停查看待补全内容'
$en['gui.modularmagic.lock_mark'] = 'locked'
$zh['gui.modularmagic.lock_mark'] = '锁定'
$en['item.modularmagic.core_wand'] = 'Beginner Wand'
$zh['item.modularmagic.core_wand'] = '初学者法杖'
$en['gui.modularmagic.wand_inventory'] = 'Wand Cores'
$zh['gui.modularmagic.wand_inventory'] = '法杖核心仓'
$en['gui.modularmagic.wand_invalid'] = 'Load a valid spell core first (sneak + right-click).'
$zh['gui.modularmagic.wand_invalid'] = '请先装入有效的法术核心（潜行＋右键打开核心仓）。'
$en['gui.modularmagic.cast_failed'] = 'Cast failed: %s'
$zh['gui.modularmagic.cast_failed'] = '施法失败：%s'
$en['tooltip.modularmagic.wand_count'] = 'Stored cores: %s / %s'
$zh['tooltip.modularmagic.wand_count'] = '已装核心：%s / %s'
$en['tooltip.modularmagic.wand_selected'] = 'Selected: %s'
$zh['tooltip.modularmagic.wand_selected'] = '当前选择：%s'
$en['tooltip.modularmagic.wand_controls'] = 'Sneak + right-click: manage cores. Right-click: cast. Hold to channel; release to cast charged spells. Switch spells using the L2ItemSelector controls in Key Binds.'
$zh['tooltip.modularmagic.wand_controls'] = '潜行＋右键打开核心仓；右键施法；持续法术按住施放，蓄力法术松开施放。切换法术沿用 L2ItemSelector 的选择按键，可在控制设置中查看。'
$en['gui.modularmagic.empty_canvas'] = 'No spell loaded'
$zh['gui.modularmagic.empty_canvas'] = '尚未载入术式'
$en['gui.modularmagic.help'] = "Pick up a node, then left-click a matching slot. Left-click occupied nodes to select; right-click leaves to remove. Parents with children are locked.`nDrag canvas space to pan; scroll to zoom. Scroll the parameter panel for more fields.`nReference slots sample the held item's icon, block or first potion effect without consuming it. Right-click a reference slot to clear it. Text boxes still support manual IDs and expressions.`nApply commits edits. Take core extracts the assembly; closing saves it inside the table."
$zh['gui.modularmagic.help'] = "拿起宝石，左键匹配空槽装入；左键选择节点，右键取出叶节点。有子节点的节点会锁定。`n拖动画布空白平移，滚轮缩放；在参数区滚动查看其余字段。`n参考槽可用光标上的物品选择图标、方块或药水的首个效果，不消耗物品；右键清空。旁边文本框仍可手填 ID、表达式或方块状态 JSON。`n应用参数提交编辑；取出核心主动取走；关闭界面保存在台内。"
$en['itemGroup.modularmagic.builtin_spells'] = 'ModularMagic: Built-in Spells'
$zh['itemGroup.modularmagic.builtin_spells'] = '模块化魔法：内置法术'
$en['gui.modularmagic.table_busy'] = 'Someone else is editing at this table.'
$zh['gui.modularmagic.table_busy'] = '其他玩家正在使用这张编写台。'
$en['gui.modularmagic.advanced'] = 'Show all parameters'
$zh['gui.modularmagic.advanced'] = '更多参数'
$en['gui.modularmagic.basic'] = 'Show main parameters'
$zh['gui.modularmagic.basic'] = '仅显示主要参数'
$en['gui.modularmagic.insert_node'] = 'Insert a matching gem'
$zh['gui.modularmagic.insert_node'] = '放入同色宝石'
$spellNames = @{
    absolute_zero=@('Absolute Zero','绝对零度');blazing_hammer=@('Blazing Hammer','烈焰之锤')
    circular=@('Three Bodies','三体');circular_entity=@('Three Moons','三月')
    cobwebs=@('Spider Nest','蛛巢');earthquake=@('Lava Burst','熔岩爆发');earth_spike=@('Earth Spike','地刺')
    earth_spike_field=@('Earth Spike Field','地刺阵');echo_blades=@('Echo Blades','回响环刃');echo_rings=@('Echo Rings','回响共鸣')
    flame_burst=@('Hell Mark','地狱烙印');flame_charge=@('Flame Charge','烈焰冲击');flame_pillar=@('Flame Pillar','烈焰之柱')
    floor=@('Floor','铺设地板');frost_nova=@('Frost Nova','冰霜新星');glacial_domain=@('Glacial Domain','冰川领域')
    icy_flash=@('Icy Flash','霜闪');icy_shatter=@('Icy Shatter','寒冰碎裂');magic_arrows=@('Angelic Judgement','神判')
    magic_arrow_ring=@('Sword of Seven','七星剑');magma_shield=@('Magma Shield','熔岩护盾');magnet_core=@('Magnet Core','磁铁核心')
    master_spark=@('Master Spark','极限火花');melt=@('Melt','熔化');starfall=@('Starfall','星落');swept=@('Swept','横扫')
    tornado=@('Tornado','龙卷');winter_storm=@('Winter Storm','暴风雪')
}
foreach ($spellId in $spellNames.Keys) {
    $en[('spell.modularmagic.' + $spellId)] = $spellNames[$spellId][0]
    $zh[('spell.modularmagic.' + $spellId)] = $spellNames[$spellId][1]
}
$parameterNames = @{
    action='动作';age='纹理帧';alongDir='沿朝向步进';alongOffset='沿偏移步进';ambient='环境效果';amplifier='效果等级';angle='水平角度';block='方块 ID';breaking='破碎效果';burnTicks='燃烧刻数';bypassEntity='穿透实体';bypassWall='穿透方块';center='居中';collide='碰撞';color='颜色';count='数量';damage='伤害值';damage_type='伤害类型';damagePerBlock='每方块伤害';degree='旋转角度';delay='间隔刻数';delayPerBlock='每方块间隔';dir='朝向类型';distance='偏移距离';duration='持续刻数';effect='状态效果 ID';effects='状态效果列表';end='结束颜色';face='支撑面类型';filter='目标过滤';fixFacing='固定朝向';friction='阻力';from_center='推力方向';function='集合运算';geom='纹理区域';gravity='重力';height='高度';icon='图标物品 ID';index='循环变量名';indirect='间接伤害';item='物品 ID';knockback='击退强度';life='存在刻数';max_depth='最大深度';maxAngle='最大角度';maxDamage='伤害上限';maxInclusive='包含终点';maxRadius='最大半径';method='移除方式';min_count='最小数量';minAngle='最小角度';minRadius='最小半径';name='变量名';offset='偏移向量';order='排序值';params='自定义参数';particle='粒子 ID';pitch='音调';pos='位置类型';positioned='使用命中位置';property='实体属性';r='半径';radius='半径';range='范围';renderType='渲染方式';roll='翻滚角度';scale='缩放';shape='随机形状';size='尺寸';sound='声音 ID';speed='速度';start='起始颜色';startFromOrigin='从原点开始';state='方块状态';step='循环次数';target='目标类型';test='方块性质';texture='贴图路径';tick='等待刻数';tilt='俯仰角度';time='等待刻数';total='总帧数';value='条件表达式';variable='变量值 / 名称';vertical='垂直角度';visible='显示效果';volume='音量';x='X 分量';y='Y 分量';z='Z 分量'
}
$en['gui.modularmagic.optional'] = '(optional)'
$zh['gui.modularmagic.optional'] = '留空使用默认值'
foreach ($fieldName in @($catalog.templates.fields.key | Sort-Object -Unique)) {
    $en[('parameter.modularmagic.' + $fieldName)] = $fieldName -creplace '([a-z])([A-Z])','$1 $2' -replace '_',' '
    $zh[('parameter.modularmagic.' + $fieldName)] = if ($parameterNames.ContainsKey($fieldName)) { $parameterNames[$fieldName] } else { $fieldName }
}
foreach ($template in $catalog.templates) {
    $category = $template.category
    if (!$indices.ContainsKey($category)) { $indices[$category] = 0 }
    $indices[$category]++
    $ordinal = $indices[$category]
    $elements = [System.Collections.Generic.List[object]]::new()
    # A code-native faceted gem model. Category determines color; a unique five-bit
    # engraving identifies the kind. These models need no generated bitmap textures.
    foreach ($row in 0..10) {
        $half = [Math]::Min($row, 10 - $row)
        $elements.Add(@{from=@((7-$half),(3+$row),7);to=@((9+$half),(4+$row),9);faces=@{
            north=@{texture='#base'};south=@{texture='#base'};east=@{texture='#base'};west=@{texture='#base'};up=@{texture='#base'};down=@{texture='#base'}
        }})
    }
    foreach ($bit in 0..4) {
        if (($ordinal -band (1 -shl $bit)) -ne 0) {
            $elements.Add(@{from=@((5+$bit),7,6.8);to=@((6+$bit),10,9.2);faces=@{south=@{texture='#mark'};north=@{texture='#mark'}}})
        } else {
            $elements.Add(@{from=@((5+$bit),6,6.8);to=@((6+$bit),7,9.2);faces=@{south=@{texture='#mark'};north=@{texture='#mark'}}})
        }
    }
    $model = @{
        parent='minecraft:block/block'
        gui_light='front'
        textures=@{base=('modularmagic:block/node/' + $colors[$category]);mark='modularmagic:block/node/engraving';particle=('modularmagic:block/node/' + $colors[$category])}
        elements=$elements.ToArray()
        display=@{gui=@{rotation=@(0,180,0);translation=@(0,0,0);scale=@(1,1,1)};ground=@{translation=@(0,2,0);scale=@(0.4,0.4,0.4)};fixed=@{rotation=@(0,180,0);scale=@(0.8,0.8,0.8)}}
    }
    Write-JsonFile ('models/item/' + $template.item + '.json') $model
    $title = [System.Globalization.CultureInfo]::InvariantCulture.TextInfo.ToTitleCase(($template.id.Split('/')[1] -replace '_',' '))
    $en[('item.modularmagic.' + $template.item)] = if ($category -eq 'root') { 'Spell Core: ' + $title } else { 'Spell Node: ' + $title }
    $zh[('item.modularmagic.' + $template.item)] = if ($category -eq 'root') { '法术核心：' + ($template.name -replace '法术根$','') } else { '法术节点：' + $template.name }
    $en[('node.modularmagic.' + $template.item)] = if ($category -eq 'root') { $title + ' Core' } else { $title }
    $zh[('node.modularmagic.' + $template.item)] = if ($category -eq 'root') { ($template.name -replace '法术根$','') + '核心' } else { $template.name }
}
$en['item.modularmagic.core_wand'] = 'Beginner Wand'
$zh['item.modularmagic.core_wand'] = '初学者法杖'
$en['item.modularmagic.creative_wand'] = 'Creative Wand'
$zh['item.modularmagic.creative_wand'] = '创造法杖'
$en['tooltip.modularmagic.mana_stored'] = 'Mana: %s / %s'
$zh['tooltip.modularmagic.mana_stored'] = '魔力：%s / %s'
$en['tooltip.modularmagic.mana_infinite'] = 'Mana: infinite'
$zh['tooltip.modularmagic.mana_infinite'] = '魔力：无限'
Write-JsonFile 'models/item/creative_wand.json' @{parent='minecraft:item/handheld';textures=@{layer0='l2magic:item/creative_wand'}}
$tierNames = @{basic=@('Basic','基础');expanded=@('Expanded','扩容');ultimate=@('Ultimate','终极')}
foreach ($tier in @('basic','expanded','ultimate')) {
    $id = 'mana_container_' + $tier
    $en['item.modularmagic.' + $id] = $tierNames[$tier][0] + ' Mana Container'
    $zh['item.modularmagic.' + $id] = $tierNames[$tier][1] + '魔力容器'
    Write-JsonFile ('models/item/' + $id + '.json') @{parent='minecraft:item/generated';textures=@{layer0='minecraft:item/amethyst_shard'}}
}
$en['item.modularmagic.creative_mana_container'] = 'Creative Mana Container'
$zh['item.modularmagic.creative_mana_container'] = '创造魔力容器'
$en['tooltip.modularmagic.wand_multipliers'] = 'Mana x%s | Focus x%s'
$zh['tooltip.modularmagic.wand_multipliers'] = '魔力消耗 ×%s　专注消耗 ×%s'
$en['tooltip.modularmagic.mana_quote'] = 'Base mana cost: %s'
$zh['tooltip.modularmagic.mana_quote'] = '基础魔力消耗：%s'
$en['tooltip.modularmagic.focus_quote'] = 'Base focus cost: %s'
$zh['tooltip.modularmagic.focus_quote'] = '基础专注消耗：%s'
$en['hud.modularmagic.mana'] = 'Mana %s'
$zh['hud.modularmagic.mana'] = '魔力 %s'
$en['hud.modularmagic.focus'] = 'Focus %s'
$zh['hud.modularmagic.focus'] = '专注 %s'
$en['hud.modularmagic.focus_tripped'] = 'Focus %s (recovering to full)'
$zh['hud.modularmagic.focus_tripped'] = '专注 %s（回满后解除熔断）'
$en['hud.modularmagic.focus_debt'] = 'Focus debt %s (recovering)'
$zh['hud.modularmagic.focus_debt'] = '专注负债 %s（偿还中）'
$en['gui.modularmagic.not_enough_mana'] = 'Not enough mana to reserve this spell. Carry more charged containers.'
$zh['gui.modularmagic.not_enough_mana'] = '魔力不足，无法为这次法术预留费用。请携带更多已充能的容器。'
$en['gui.modularmagic.focus_tripped'] = 'Focus is overdrawn. Wait until it fully recovers.'
$zh['gui.modularmagic.focus_tripped'] = '专注已熔断，请等待专注完全恢复。'
$en['gui.modularmagic.invalid_budget'] = 'Cannot budget this spell. Check its expressions, range and repetition counts.'
$zh['gui.modularmagic.invalid_budget'] = '无法确定这次法术的费用，请检查表达式、范围和重复次数。'
$en['gui.modularmagic.cast_cancelled'] = 'The spell ended; unused reserved mana will return to its containers.'
$zh['gui.modularmagic.cast_cancelled'] = '法术已结束，未使用的预留魔力将归还原容器。'
$en['gui.modularmagic.cast_limit'] = 'The spell exceeded its execution limits and stopped.'
$zh['gui.modularmagic.cast_limit'] = '法术超出允许的执行范围或次数，已停止。'
$en['gui.modularmagic.budget_exhausted'] = 'The spell exceeded its reserved mana and stopped.'
$zh['gui.modularmagic.budget_exhausted'] = '法术所需费用超出预留魔力，已停止。'
$en['gui.modularmagic.condition_not_met'] = 'The casting condition was not met.'
$zh['gui.modularmagic.condition_not_met'] = '未满足这个法术的施放条件。'
Write-JsonFile 'models/item/creative_mana_container.json' @{parent='minecraft:item/generated';textures=@{layer0='minecraft:item/echo_shard'}}
Write-JsonFile 'lang/en_us.json' $en
Write-JsonFile 'lang/zh_cn.json' $zh
. (Join-Path $PSScriptRoot 'generate-node-guide.ps1')
. (Join-Path $PSScriptRoot 'generate-player-guide.ps1')
& node (Join-Path $PSScriptRoot 'generate-energy-assets.mjs')
Write-Output ('Generated {0} node models and both language files.' -f $catalog.templates.Count)
