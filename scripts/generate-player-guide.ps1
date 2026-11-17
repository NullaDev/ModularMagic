# Invoked after the node guide. These pages teach a reproducible path before reference details.
foreach ($locale in @('zh_cn','en_us')) {
    $cn = $locale -eq 'zh_cn'
    $base = 'patchouli_books/assembly_guide/' + $locale
    function Guide-Text($titleZh,$titleEn,$textZh,$textEn) {
        return @{type='patchouli:text';title=if ($cn) {$titleZh} else {$titleEn};text=if ($cn) {$textZh} else {$textEn}}
    }
    function Guide-Item($id,$textZh,$textEn) {
        return @{type='patchouli:spotlight';item=$id;link_recipe=$true;text=if ($cn) {$textZh} else {$textEn}}
    }
    function Guide-Entry($id,$zhName,$enName,$icon,$category,$order,$pages) {
        Write-JsonFile ($base + '/entries/' + $id + '.json') ([ordered]@{
            name=if ($cn) {$zhName} else {$enName};icon=$icon;category='modularmagic:'+$category;sortnum=$order;pages=@($pages)
        })
    }
    Write-JsonFile ($base + '/categories/editor.json') (@{
        name=if ($cn) {'入门教程'} else {'Getting Started'};description=if ($cn) {'第一次游玩先读从零开始，再查操作速查。'} else {'Start with the walkthrough, then use the controls reference.'}
        icon='minecraft:writable_book';sortnum=0
    })
    Write-JsonFile ($base + '/categories/equipment.json') (@{
        name=if ($cn) {'工具与核心'} else {'Tools and Cores'};description=if ($cn) {'编写台、法杖与核心的用途和操作。'} else {'How to use the writing table, wand and assembled cores.'}
        icon='modularmagic:core_wand';sortnum=1
    })
    . (Join-Path $PSScriptRoot 'generate-cost-chapter.ps1')
    Guide-Entry 'tutorial' '从零开始：第一个法术' 'From Zero: Your First Spell' 'modularmagic:root_instant' 'editor' 0 @(
        (Guide-Text '目标与准备' 'Your goal' '本教程带你做出一颗发声的法术核心，再用法杖施放。只需三个装配物品，不必先学全部节点。$(br2)从模块化魔法创造分类取得练习用品。若要在生存模式练习，再带一个充满的魔力容器；专注会自行恢复。' 'Build a spell that plays a sound, then cast it with a wand. You need only three assembly items.$(br2)Gather practice supplies from the ModularMagic creative tab. For survival practice, also carry a charged mana container; focus recovers naturally.')
        (Guide-Text '1. 取得材料' '1. Gather supplies' '打开模块化魔法创造分类，取出：$(li)法术编写台$(li)法术核心：瞬发$(li)法术节点：自身位置$(li)法术节点：声音$(li)初学者法杖$(li)充满的小型魔力容器（生存模式）$(br2)再把本手册放在快捷栏，方便长按 Ctrl 查阅。' 'From the ModularMagic creative tab, take:$(li)Spell Writing Table$(li)Spell Core: Instant$(li)Spell Node: Self Pos$(li)Spell Node: Sound$(li)Beginner Wand$(li)Charged Small Mana Container (survival)$(br2)Keep this guide in your hotbar for Ctrl lookup.')
        (Guide-Text '2. 放入核心' '2. Insert the core' '放置编写台并右键打开。把瞬发核心放进右上角、取出核心按钮旁的物品槽。$(br2)左侧出现核心与三个接口：施法起点、主法术、可选的施放条件。右侧是参数区和背包。' 'Place and open the table. Put the Instant core in the top-right slot beside Take core.$(br2)The canvas shows a root with origin, action and optional casting-condition slots. Parameters and inventory are on the right.')
        (Guide-Text '3. 选择施法起点' '3. Choose an origin' '从背包拿起自身位置节点，左键点击画布上的施法起点空槽。$(br2)只会装入一枚宝石。这个节点让后续效果从玩家脚下开始，不需要填写参数。' 'Pick up Self Pos from your inventory and left-click the empty origin slot on the canvas.$(br2)One gem is inserted. This origin starts the spell at your feet and needs no parameters.')
        (Guide-Text '4. 加入声音动作' '4. Add the action' '拿起声音节点，左键放进主法术空槽。施放条件可以保持空白。$(br2)现在的结构是：$(li)瞬发核心$(li)施法起点：自身位置$(li)主法术：声音$(br2)声音节点没有子节点。' 'Insert Sound into the empty action slot. Leave the optional casting condition empty.$(br2)Your tree is now:$(li)Instant core$(li)Origin: Self Pos$(li)Action: Sound$(br2)Sound has no children.')
        (Guide-Text '5. 设置并检查' '5. Configure and check' '左键选择声音节点，在声音 ID 中填入 minecraft:block.note_block.pling。音量和音调均填 1，点击应用参数。$(br2)核心旁的状态按钮应显示通过。若未通过，悬停该按钮查看缺少的槽或错误参数。' 'Select Sound. Set sound to minecraft:block.note_block.pling, volume to 1 and pitch to 1. Click Apply.$(br2)The status button beside the core should show success. Hover it to inspect any missing slots or invalid parameters.')
        (Guide-Text '6. 取出成型核心' '6. Take your spell' '清空光标，点击取出核心。你会拿到成型法术核心，放入背包后关闭界面。$(br2)它包含整棵树，节点不会丢失。画布中的根核心不能右键取出；普通节点后面没有接其他宝石时，才能右键取出。' 'Empty your cursor and click Take core. Put the Formed Spell Core in your inventory and close the table.$(br2)It contains the entire tree. The root cannot be removed by right-clicking; only leaf nodes can be removed that way.')
        (Guide-Text '7. 装进法杖施放' '7. Load and cast' '手持初学者法杖，潜行并右键打开九格核心仓，装入刚才的成型核心并关闭。$(br2)普通右键应播放音符盒的声音！生存模式下，这个简单法术默认消耗 11 魔力和 5 专注。多个核心沿用 L2ItemSelector 的切换按键，默认方向键上、下，以控制设置为准。' 'Hold the Beginner Wand and sneak-right-click to open its nine core slots. Insert your formed core and close the screen.$(br2)Right-click to hear the note! In survival, this simple spell defaults to 11 mana and 5 focus. Multiple cores use L2ItemSelector controls, normally Up/Down; check Key Binds.')
        (Guide-Text '8. 再做一个变化' '8. Try a variation' '把核心从法杖取出，放回编写台。选中声音节点，把音调改为 2，再应用并取出，听听区别。$(br2)也可以取走声音节点，换成其他动作。先从少量节点开始，再学习循环、选区与实体效果。' 'Return the core from the wand to the table. Change the sound pitch to 2, apply, extract and listen again.$(br2)You can also remove Sound and try another action. Start small before adding loops, selectors and entity effects.')
        (Guide-Text '遇到问题时' 'If something goes wrong' '关闭编写台不会返还核心，它保存在台内；重新打开即可继续。$(br2)没有子节点时取出仍是对应类型的空核心。已有节点但结构或参数有误时保存为未成形核心，也能再次编辑。$(br2)接下来可读$(l:modularmagic:editor)操作速查$(/l)或$(l:modularmagic:nodes)节点图鉴$(/l)。' 'Closing saves the core inside the table; reopen to continue.$(br2)An empty tree remains a typed core. A nonempty invalid tree becomes an Unformed Spell Core that you can edit again.$(br2)Next: $(l:modularmagic:editor)Controls$(/l) or $(l:modularmagic:nodes)Node Reference$(/l).')
    )
    Guide-Entry 'editor' '编辑器与查阅速查' 'Controls and Quick Lookup' 'minecraft:writable_book' 'editor' 1 @(
        (Guide-Text '先读教程' 'Start here' '第一次使用请先读$(l:modularmagic:tutorial)从零开始：第一个法术$(/l)。$(br2)手册首页只有三个分类：入门教程、工具与核心、节点图鉴。节点放在同一目录，仍按核心、起点、动作等相邻排列，也可按名称搜索。' 'New players should read $(l:modularmagic:tutorial)From Zero: Your First Spell$(/l).$(br2)The three categories cover tutorials, equipment and all nodes. The node index keeps similar types together and supports name searches.')
        (Guide-Text '装配操作' 'Assembly controls' '持有宝石左键空槽装入；左键已有节点编辑；右键取回没有接着其他节点的宝石。要取回带着其他节点的宝石，须先把它后面的宝石取走，根核心只能整体取走。$(br2)拖空白或按住中键平移，滚轮缩放。列表末尾会保留空槽。参数区滚动查看更多，省略号展开更多参数。' 'Insert a held gem with left-click; select a node with left-click; remove a leaf with right-click. Empty child slots before removing a parent. Extract roots as a whole.$(br2)Drag space or middle-drag to pan; scroll to zoom. Scroll parameters; use the ellipsis for more options.')
        (Guide-Text '长按 Ctrl 查阅' 'Hold Ctrl to look up' '把装配指南放在快捷栏，在背包或物品栏中悬停节点、核心、编写台或法杖，长按 Ctrl，等待圆形进度完成即可跳转对应页。$(br2)这是帕秋莉自带功能；若其设置改用 Shift，则遵循该设置。手册不在快捷栏时不会触发。' 'Keep the Assembly Guide in your hotbar. Hover a node, core, table or wand and hold Ctrl until the lookup ring completes.$(br2)This uses Patchouli quick lookup. If its settings use Shift instead, follow that setting. The guide must be in your hotbar.')
        (Guide-Text '参考槽与表达式' 'References and expressions' '图标、方块、状态效果可用右侧参考槽取样：拿起对应物品后左键点击，不消耗物品；右键清空。$(br2)文本框可直接写表达式、注册 ID 或要求的 JSON。变量须在作用域内有效；可选字段留空使用引擎默认值。' 'Sample icons, blocks and potion effects using reference slots: left-click with the relevant item on your cursor without consuming it; right-click to clear.$(br2)Text fields accept expressions, IDs or the requested JSON. Variables must be in scope; optional fields may be empty.')
    )
    Guide-Entry 'equipment/writing_table' '法术编写台' 'Spell Writing Table' 'modularmagic:spell_writing_table' 'equipment' 0 @(
        (Guide-Item 'modularmagic:spell_writing_table' '编辑法术结构的工作台。放置后右键打开，将空核心或已保存核心放入右上角输入槽。详细操作见$(l:modularmagic:tutorial)从零教程$(/l)。' 'Place and open this table to edit a spell. Insert a typed or saved core in the top-right slot. See the $(l:modularmagic:tutorial)walkthrough$(/l).')
        @{type='patchouli:crafting';recipe='modularmagic:spell_writing_table';text=if ($cn) {'工作台与书与笔无序合成。'} else {'Shapeless crafting table and book and quill recipe.'}}
        (Guide-Text '保存与取出' 'Saving and extraction' '应用参数提交修改，取出核心整体取走。关闭界面会保存未提交参数，核心留在台内；重新打开或重载世界可继续。$(br2)同一张台同时只供一人编辑。拆除台子会掉落其中保存的核心。' 'Apply commits edits; Take core extracts the assembly. Closing saves pending parameters and keeps the core inside across world reloads.$(br2)One player can edit a table at a time. Breaking it drops its stored core.')
    )
    Guide-Entry 'equipment/wand' '初学者法杖' 'Beginner Wand' 'modularmagic:core_wand' 'equipment' 1 @(
        (Guide-Item 'modularmagic:core_wand' '容纳九颗法术核心，施放当前选中的法术。潜行右键打开核心仓，像背包一样装入或取出核心；关闭后内容保存在法杖内。' 'Store nine spell cores and cast the selected spell. Sneak-right-click to open its core inventory and transfer cores normally. Contents persist inside the wand.')
        (Guide-Text '施放与切换' 'Casting and selection' '普通右键施放瞬发法术；蓄力型按住后松开施放；持续型按住使用。未成形或空核心不能施法。$(br2)沿用 L2ItemSelector 的切换按键，默认方向键上、下，以控制设置为准。物品说明会显示装入数量、当前选择与消耗倍率。' 'Right-click instant spells, hold and release charged spells, and hold continuous spells. Empty or unformed cores cannot cast.$(br2)Use L2ItemSelector keys to switch, normally Up/Down. The tooltip shows core count, current selection and cost multipliers.')
        (Guide-Text '供能与专注' 'Mana and Focus' '把充过能的魔力容器放在背包或副手即可供能，多个容器会一起使用。施法不会消耗核心。$(br2)专注会自行恢复；透支后要等到回满才能再次施法。创造模式无需资源。详细规则见$(l:modularmagic:resource_costs)专注与魔力$(/l)。' 'Carry charged mana containers in your inventory or offhand; multiple containers combine automatically. Casting does not consume cores.$(br2)Focus recovers naturally. After overdraw, wait until it is full before casting again. Creative mode needs no resources. See $(l:modularmagic:resource_costs)Focus and Mana$(/l).')
    )
    Guide-Entry 'equipment/formed_core' '成型法术核心' 'Formed Spell Core' 'modularmagic:formed_spell_core' 'equipment' 2 @(
        (Guide-Item 'modularmagic:formed_spell_core' '通过结构、参数与引擎校验的完整法术。保存根核心、全部节点及参数，可放进初学者法杖施放。' 'A spell that passed structure, parameter and engine validation. It stores the root, every node and all parameters, and can be cast from a Beginner Wand.')
        (Guide-Text '修改与示例' 'Editing and examples' '放回编写台即可继续编辑。修改可能使它重新变成未成形核心，但结构会保留。$(br2)内置法术创造分类提供 28 个 L2Magic 示例，可直接装入法杖，或放进编写台研究其结构。' 'Return it to a table to edit. Changes may make it unformed again without losing its structure.$(br2)The Built-in Spells creative tab supplies 28 editable L2Magic examples for casting or studying.')
    )
    Guide-Entry 'equipment/unformed_core' '未成形法术核心' 'Unformed Spell Core' 'modularmagic:unformed_spell_core' 'equipment' 3 @(
        (Guide-Item 'modularmagic:unformed_spell_core' '非空但尚未通过校验的半成品。缺少必要子槽、错误表达式或无效 ID 都可能导致未成形。它会保留所有节点与填写的文本。' 'A nonempty assembly that has not passed validation, for example because of missing slots, invalid expressions or IDs. All nodes and entered text are preserved.')
        (Guide-Text '继续编辑' 'Continue editing' '放回编写台，悬停状态按钮查看问题，补齐节点或修正参数并应用。它可以放进法杖保存，但不能施法。$(br2)若移空所有子节点，则恢复为对应类型的空法术核心，仍保留根参数。' 'Return it to a table and hover the status button to inspect errors, then fix slots or parameters and apply. A wand can store it but cannot cast it.$(br2)Removing all children restores its typed empty core and retains root parameters.')
    )
    Guide-Entry 'equipment/creative_wand' '创造法杖' 'Creative Wand' 'modularmagic:creative_wand' 'equipment' 4 @(
        (Guide-Item 'modularmagic:creative_wand' '与初学者法杖使用相同的核心仓和切换方式，法力与专注消耗倍率固定为 0，便于独立测试法术效果。' 'Uses the beginner wand core inventory and selector. Mana and focus multipliers are fixed at zero for testing spell effects.')
        (Guide-Text '倍率与计费' 'Multipliers and billing' '初学者法杖的两种倍率默认均为 1，由 NeoForge 服务端配置控制。把成型核心的基础费用乘以对应倍率，可以比较法杖的消耗。创造法杖也能在专注熔断时施法。' 'Both beginner wand multipliers default to one and use NeoForge server settings. Multiply a core base cost by the corresponding rate to compare wands. The creative wand also works during focus lockout.')
    )
    $containerPages = [System.Collections.Generic.List[object]]::new()
    foreach ($tier in @('small','medium','large')) {
        $id='modularmagic:mana_container_'+$tier
        $containerPages.Add((Guide-Item $id '同一物品可以是空、部分充满或全满状态，储量保存在物品中。创造分类提供空、满两种状态。小、中、大型容量默认 100000、1000000、10000000，不会自行回魔。持杖时屏幕显示背包容器的总储量和玩家专注。' 'One item can be empty, partially charged or full; charge is saved on the stack. Creative mode offers empty and full states. Capacities default to 100000, 1000000 and 10000000 with no regeneration. Holding a wand shows inventory mana and player focus.'))
    }
    $containerPages.Add((Guide-Item 'modularmagic:creative_mana_container' '只有无限满容器，不会被抽空，也不接收魔力。放在背包或副手即可为法杖提供无限魔力，但不会免除专注消耗。' 'Only the full infinite variant exists. Extraction never depletes it, and it accepts no mana. Carry it in your inventory or offhand to supply unlimited wand mana. It does not remove focus costs.'))
    Guide-Entry 'equipment/mana_containers' '魔力容器' 'Mana Containers' 'modularmagic:mana_container_small' 'equipment' 5 $containerPages.ToArray()
}
