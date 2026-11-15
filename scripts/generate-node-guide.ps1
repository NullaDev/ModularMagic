# Invoked by generate-node-assets.ps1. Template metadata stays the source of truth for slots and fields.
$guideDescriptions = @{}
@'
modifier/forward|沿当前前向移动法术位置。正数向前，负数向后；不会移动施法者。|Offset the spell position along its current forward direction. Negative distances move backward. This does not move the caster.
modifier/normal_offset|沿当前法向移动法术位置。可与前向偏移组合，搭建立体轨迹。|Offset the spell position along its normal axis. Combine with forward offsets to build spatial patterns.
modifier/rotate|改变法术的水平与垂直朝向，供后续偏移、展开或发射使用。|Rotate the spell orientation horizontally and vertically for subsequent offsets, patterns or shots.
modifier/offset|按世界 X、Y、Z 轴偏移法术位置，不随玩家转头而旋转。|Offset the spell position along world X, Y and Z axes, independently of the caster's facing.
modifier/set_position|把法术位置设为指定的世界坐标。坐标值可以使用表达式。|Set the spell position to absolute world coordinates. Coordinate values may be expressions.
modifier/direction|用指定向量设置法术前向，影响随后节点的朝向与展开方向。|Set the spell's forward direction from a vector, affecting subsequent orientation and patterns.
modifier/random_offset|在指定形状与尺寸内随机偏移法术位置，可用来制作散布效果。|Randomly offset the spell position within the selected shape and dimensions, creating scattered effects.
modifier/set_normal|用指定向量设置法术法向，用于控制环形展开等效果所在的平面。|Set the spell's normal vector to control the plane used by patterns such as rings.
modifier/direction_to_normal|将当前前向用作法向，改变后续节点使用的空间基准。|Use the current forward direction as the normal, changing the spatial frame for later nodes.
modifier/normal_to_direction|将当前法向用作前向，改变后续节点的前进方向。|Use the current normal as the forward direction for later nodes.
modifier/random_direction|随机选择法术前向，适合随机散射；不直接产生弹体或伤害。|Choose a random forward direction for scattered patterns. This alone creates neither projectiles nor damage.
modifier/move_to_caster|将法术位置移回施法者当前所在位置，用于让后续效果跟随施法者。|Move the spell position to the caster's current position so subsequent effects can follow them.
modifier/align_with_caster|将法术前向对齐施法者当前视线，可用于延迟后的重新瞄准。|Align the spell's forward direction with the caster's current view, including after a delay.
action/if|判断条件节点：成立时执行主动作，否则执行备用动作。两个分支均可留空。|Test a condition, running the action when true and the fallback when false. Either branch may be empty.
action/list|按装配顺序依次启动动作。各动作自身的延迟不会让后面的动作自动等待。|Start actions in assembly order. A child's scheduled delay does not automatically make later actions wait.
action/delay|等待指定游戏刻数后执行子动作。通常 20 游戏刻为 1 秒。|Schedule the child action after a delay measured in game ticks. Normally, 20 ticks equal one second.
action/random|生成 count 个 0 到 1 之间的随机数，变量名为 name0、name1 等，只在子动作作用域内提供。|Create count random values in [0, 1), named name0, name1 and so on, for the child action's scope.
action/variable|计算表达式并保存为命名变量，供子动作及其后代使用。|Evaluate an expression and bind its result to a named variable for the child action and its descendants.
action/move|依次应用空间修饰，再在变换后的位置和朝向执行子动作。|Apply spatial modifiers in order, then execute the child action at the resulting position and orientation.
action/processor|按目标类型与选区寻找实体，再对选中实体依次执行实体处理节点。伤害等效果应装在这里。|Find entities using the target type and selector, then apply entity processors in order. Put damage and similar effects here.
action/synced_logic|执行需要服务端与客户端配合的子动作。在 MM 自定义法术中由法术同步机制协调视觉与实际效果。|Execute a child action requiring server and client coordination. MM coordinates its visuals and actual effects through spell synchronization.
action/iterate|重复执行子动作，可指定循环次数与索引变量。索引从 0 开始。|Repeat the child action a set number of times. An optional index variable starts at zero.
action/iterate_delayed|按指定游戏刻间隔重复执行子动作，可在每次执行中读取循环索引。|Repeat the child action at the specified tick interval, optionally exposing the iteration index.
action/while_delayed|每轮先检查条件，成立才执行子动作并安排下一轮；step 限制循环轮数。|Check a condition before each iteration. If true, run the child and schedule the next iteration; step limits the iterations.
action/iterate_linear|沿直线逐步改变位置并执行子动作。前向步进、额外偏移与起点设置共同决定采样位置。|Execute the child at successive positions along a line, controlled by forward steps, additional offsets and the starting-point option.
action/iterate_arc|在指定半径和角度范围内均匀取点，并在各点执行子动作，可组成圆环或扇形。|Execute the child at evenly spaced points across an arc with the chosen radius and angles, forming rings or fans.
action/random_pos_fan|在指定半径与角度范围的扇面内随机取点，在每个点执行子动作。|Sample random positions in the specified annular fan and execute the child at each point.
action/random_pos_sphere|在指定球形范围内随机取点，并在各点执行子动作。|Sample random positions within a sphere and execute the child at each position.
action/block_in_range|遍历范围内的方块位置并执行子动作，可设置逐方块延迟及位置变量前缀。|Visit block positions in range and execute the child, optionally delaying between blocks and exposing position variables.
action/particle|在法术位置产生指定的原版粒子。粒子是视觉效果，不会自动造成伤害。|Spawn the specified vanilla particle at the spell position. Particles are visual and do not automatically deal damage.
action/block_particle|产生使用指定方块纹理的碎屑粒子，可调整速度、大小与寿命。|Spawn debris particles using a block's texture, with configurable speed, scale and lifetime.
action/item_particle|产生使用指定物品纹理的碎屑粒子，可调整速度、大小与寿命。|Spawn debris particles using an item's texture, with configurable speed, scale and lifetime.
action/dust_particle|产生指定颜色的尘埃粒子，可调整大小、速度与寿命。|Spawn colored dust particles with configurable scale, speed and lifetime.
action/transition_particle|产生在起始色与结束色之间渐变的尘埃粒子。|Spawn dust particles that transition between the specified start and end colors.
action/custom_particle|生成可自定义运动与外观的粒子。运动可以省略，外观节点必须提供。|Spawn a particle with a custom appearance and optional motion. An appearance node is required.
action/sound|在法术位置播放声音，可设置声音 ID、音量与音调。|Play a sound at the spell position with a configurable sound ID, volume and pitch.
action/set_block|在法术所在方块位置放置指定方块状态，受引擎方块修改规则限制。参考槽可从方块物品取样。|Place the specified block state at the spell's block position, subject to engine modification rules. Sample a block item using the reference slot.
action/set_block_facing|放置指定方块并根据法术朝向设置其方向属性，适用于具有朝向的方块。|Place a block and orient supported facing properties according to the spell's direction.
action/remove_block|按所选方式移除方块。DROP 会掉落物品，DESTROY 破坏但不掉落，REMOVE 移除，SET_AIR 设为空气。|Remove a block using DROP with drops, DESTROY without drops, REMOVE, or SET_AIR.
action/knock_block|将方块击飞为运动中的方块，并按设置计算碰撞伤害上限。|Launch a block into motion, with configurable speed and collision damage limits.
action/schedule_tick|为指定方块安排一次延迟更新；这不是让整棵法术树等待。|Schedule a delayed tick for the specified block. This does not pause the spell tree.
action/arrow|沿法术前向发射箭，可设置初速与箭上附带的状态效果列表。|Shoot an arrow along the spell's forward direction with a configurable initial speed and potion effects.
action/trident|沿法术前向发射三叉戟，可设置初速。|Shoot a trident along the spell's forward direction with a configurable initial speed.
action/custom_projectile|发射由弹体配置节点定义的弹体，设置初速、寿命、穿透选项与传入配置的变量。|Shoot a projectile defined by a configuration node, setting its speed, lifetime, piercing options and configuration parameters.
selector/self|选中施法者自身，适合为自己施加状态或移动自身。|Select the caster, for effects such as self buffs or movement.
selector/move|先应用空间修饰，再在新的空间基准下计算子选区。|Apply spatial modifiers, then evaluate the child selector in the resulting spatial frame.
selector/box|按尺寸、高度和居中选项建立盒状实体选区。选区只选目标，不自动产生效果。|Select entities in a box configured by size, height and centering. Selection alone applies no effects.
selector/compound|用集合运算组合多个子选区，结果由 function 指定的组合方式决定。|Combine multiple child selectors using the set operation specified by function.
selector/line|沿前向逐步采样，以给定尺寸建立直线实体选区。|Select entities along forward samples, using the configured step and sample size.
selector/arc|在指定半径和角度范围内采样，建立圆弧或环形实体选区。|Select entities along samples of an arc or ring with the specified radius and angle range.
selector/cylinder|建立指定半径与高度的圆柱实体选区。|Select entities inside a cylinder with the specified radius and height.
selector/ball|建立指定半径的球形实体选区。|Select entities inside a sphere with the specified radius.
predicate/and|所有子条件均成立时返回成立，用于组合多个空间或表达式要求。|Return true when every child condition is true, combining spatial or expression requirements.
predicate/or|任一子条件成立即返回成立，用于表达多种可接受情况。|Return true when any child condition is true, allowing alternative requirements.
predicate/not|反转子条件的判断结果：成立变不成立，反之亦然。|Invert the child condition's result.
predicate/move|先修改用于判断的位置与朝向，再求值子条件，不直接修改世界。|Transform the position and orientation used for testing, then evaluate the child condition without changing the world.
predicate/block|检查当前位置的方块是否匹配给定方块 ID。|Check whether the block at the current position matches the specified block ID.
predicate/surface_below|按所选支撑面类型检查当前位置下方是否有合适表面。|Check for a suitable supporting surface below the current position using the selected surface type.
predicate/block_test|检查当前位置方块是否具有所选性质，例如是否符合引擎提供的方块测试。|Test the block at the current position using the selected built-in block property test.
predicate/depth_test|从当前位置向下逐层测试子条件；在最大深度内任一层成立即通过。可提供深度索引。|Test the child condition at successive depths below the current position. Any passing depth succeeds; an optional depth index is available.
predicate/ranged_test|在范围内逐方块测试子条件，满足条件的位置数量达到最小数量时通过。|Test the child condition at block positions in range, succeeding once the number of matching positions reaches the minimum.
filter/and|只让满足全部子过滤条件的实体通过。此类节点用于实体分流，不是空间条件。|Accept entities that satisfy every child filter. These are entity filters rather than spatial conditions.
filter/or|让满足任意子过滤条件的实体通过。|Accept entities that satisfy any child filter.
filter/not|反转子实体过滤器，保留不满足该过滤条件的实体。|Invert the child entity filter, accepting entities it would reject.
filter/effect|检查实体是否拥有指定的状态效果，可用于只处理带有某种状态的目标。|Check whether an entity has the specified status effect, allowing effects to target affected entities.
effect/delay|等待指定游戏刻数后，对已选中的实体执行子处理节点。|Schedule the child processor for the selected entities after the specified tick delay.
effect/damage|对已选中的实体造成指定类型与数值的伤害，受伤害类型及原版伤害机制影响。|Deal the specified amount and type of damage to selected entities, subject to damage type and vanilla damage rules.
effect/knockback|对已选中的实体施加击退，强度、水平角度与俯仰角度共同决定击退方向。|Knock selected entities back using the configured strength, angle and tilt.
effect/push|对已选中的实体施加推力，可选择统一方向或相对中心的方向。|Push selected entities using either a uniform direction or a direction relative to the spell center.
effect/effect|为已选中的实体施加状态效果，设置持续时间、等级及显示选项。参考槽可读取药水的首个效果。|Apply a status effect with a duration, amplifier and display options. The reference slot samples a potion's first effect.
effect/property|将实体的燃烧或冻结计时至少延长至指定值；不会把更长的现有计时缩短。|Extend an entity's fire or freezing timer to at least the specified duration without shortening a longer existing timer.
effect/set_delta|直接设置实体的速度向量，而不是在当前速度上叠加推力。|Set selected entities' velocity vectors directly rather than adding a push to their existing velocity.
effect/teleport|把选中的实体传送到指定的绝对世界坐标；不是相对位移。|Teleport selected entities to absolute world coordinates rather than applying a relative offset.
effect/ignite|先对原本已燃烧的生物执行附加处理，再点燃选中的实体。附加列表不是对所有新点燃目标立即执行。|Run extra processors on living targets already on fire, then ignite selected entities. Newly ignited targets do not immediately receive those extras.
effect/filtered|按实体过滤条件分成两组，分别依次执行成立或不成立的处理列表。|Partition entities with an entity filter, then run the corresponding accepted or fallback processor list.
effect/cast_at|以选中生物的位置和指定方向执行动作子树，可制作以目标为中心的二次法术。|Run an action subtree using each selected living target's position and chosen orientation, creating target-centered secondary spells.
effect/projectile_hit|对选中的非生物实体发起一次微量弹射物伤害，触发其受弹射物命中的行为；忽略生物。|Apply a small projectile-damage hit to selected non-living entities to trigger their hit behavior. Living entities are ignored.
motion/simple|按阻力与重力更新运动，可用于普通抛物线或缓慢漂浮的粒子、弹体。|Update movement using friction and gravity for effects such as ballistic or drifting particles and projectiles.
motion/control_position|将空间修饰的结果作为每次更新的目标位置，用于直接编写运动轨迹。|Use the result of spatial modifiers as the target position on each update, directly defining a trajectory.
motion/control_velocity|将速度向量置于空间修饰中处理，用修饰结果控制每次更新的速度。|Transform the velocity vector using spatial modifiers to control movement on each update.
particle_renderer/simple|使用指定粒子的外观与渲染方式，为自定义粒子提供基础贴图。|Use the chosen particle appearance and render mode as the basic sprite for a custom particle.
particle_renderer/color|为自定义粒子提供单色外观，并选择渲染方式。|Give a custom particle a single-color appearance with the chosen render mode.
particle_renderer/transition|让自定义粒子的颜色从起始色变化到结束色。|Transition a custom particle's color from the start color to the end color.
particle_renderer/block|使用方块纹理绘制自定义粒子，可配置纹理区域与渲染方式。|Render a custom particle using a block texture with configurable geometry and render mode.
particle_renderer/item|使用物品纹理绘制自定义粒子，可配置纹理区域与渲染方式。|Render a custom particle using an item texture with configurable geometry and render mode.
particle_renderer/static|从粒子动画中选择固定帧，制作不会自动播放完整动画的外观。|Choose a fixed frame from a particle animation instead of playing the full animation.
particle_renderer/oriented|包装另一个粒子外观，使其按指定方向和翻滚角度显示。|Wrap another particle appearance to display it with a controlled facing and roll.
projectile_renderer/simple|使用指定贴图绘制定向弹体。它只控制外观，命中效果由弹体配置另行指定。|Render an oriented projectile using the specified texture. Hit effects are configured separately in the projectile configuration.
predicate/expression|将文本表达式作为条件求值。变量必须在当前作用域内有效，可用于按循环索引、蓄力时间等分支。|Evaluate a text expression as a condition. Variables must exist in the current scope, such as iteration indices or charging time.
root/instant|整棵法术的瞬发入口。装入编写台后提供起点、主动作与可选施放条件；在装配法杖中右键施放。|The root of an instant spell. Provides origin, action and optional casting-condition slots. Right-click an Assembly Wand to cast.
root/charge|整棵法术的蓄力入口。装配法杖按住使用、松开施放；表达式可读取 TickUsing 蓄力刻数。|The root of a charged spell. Hold an Assembly Wand and release to cast. Expressions can read charging ticks through TickUsing.
root/continuous|整棵法术的持续入口。装配法杖按住使用时持续执行，适合引导效果；注意重复执行的频率。|The root of a continuous spell. An Assembly Wand executes it while held in use; account for repeated execution when designing effects.
trigger/self_pos|从施法者脚下位置起法，不直接使用其视线作为前向。随后可用空间修饰改变朝向。|Start at the caster's feet using the engine's regular orientation rather than their view direction. Modifiers can change it afterward.
trigger/axis_aligned_facing|从施法者眼部所在方块中心起法，朝向吸附到最近的世界轴方向。|Start at the center of the block containing the caster's eyes, snapping the facing to the nearest world axis.
trigger/horizontal_facing|从施法者位置起法，使用去掉垂直分量后的视线方向；完全垂直看时无法确定该方向。|Start at the caster's position using their horizontal view direction. A fully vertical view cannot supply this direction.
trigger/facing_front|从眼前一格的位置起法，前向与施法者视线一致。|Start one block in front of the caster's eyes, facing along their view direction.
trigger/facing_back|从眼后方一格的位置起法，前向仍与视线一致；不是把射击方向反转。|Start one block behind the caster's eyes while still facing along their view direction. This does not reverse the firing direction.
trigger/target_pos|从视线命中的方块或实体位置起法。没有命中目标时不会施放。|Start at the block or entity hit by the caster's aiming ray. Casting fails if there is no hit.
trigger/target_entity|从当前瞄准生物的位置起法。没有有效目标生物时不会施放。|Start at the currently targeted living entity's position. Casting fails without a valid living target.
projectile/config|定义自定义弹体的运动、命中处理、外观及生命周期动作；由自定义弹体动作节点使用，不可直接作为主动作。|Define a custom projectile's motion, hit processors, appearance and lifecycle actions. Used by Custom Projectile, not directly as an action.
'@ -split "`n" | ForEach-Object {
    $parts = $_.TrimEnd("`r").Split('|')
    if ($parts.Count -ne 3 -or $guideDescriptions.ContainsKey($parts[0])) { throw "Invalid guide description: $_" }
    $guideDescriptions[$parts[0]] = @($parts[1], $parts[2])
}

$categoryOrder = @('root','trigger','action','modifier','selector','predicate','filter','effect','projectile','motion','particle_renderer','projectile_renderer')
if ($guideDescriptions.Count -ne $catalog.templates.Count) { throw 'Node guide coverage differs from the catalog' }
foreach ($locale in @('zh_cn','en_us')) {
    $isChinese = $locale -eq 'zh_cn'
    $language = if ($isChinese) { $zh } else { $en }
    $categoryNames = if ($isChinese) { $chineseCategories } else { $englishCategories }
    $base = 'patchouli_books/assembly_guide/' + $locale
    # Remove only categories previously generated by this script.
    foreach ($category in $categoryOrder) {
        $oldCategory = Join-Path $assetRoot ($base + '/categories/nodes_' + $category + '.json')
        if (Test-Path -LiteralPath $oldCategory) { Remove-Item -LiteralPath $oldCategory }
    }
    Write-JsonFile ($base + '/categories/nodes.json') ([ordered]@{
        name=if ($isChinese) {'节点图鉴'} else {'Node Reference'}
        description=if ($isChinese) {'所有节点与三种法术核心都收录在此。可按名称搜索；条目说明用途、子槽与参数。'} else {'All nodes and the three typed cores, searchable by name. Entries describe their purpose, child slots and parameters.'}
        icon='modularmagic:gem_action_iterate';sortnum=2
    })
    $ordinal = 0
    foreach ($template in $catalog.templates) {
        if (!$guideDescriptions.ContainsKey($template.id)) { throw "Missing node guide: $($template.id)" }
        $pages = [System.Collections.Generic.List[object]]::new()
        $pages.Add([ordered]@{
            type = 'patchouli:spotlight'; item = 'modularmagic:' + $template.item; link_recipe=$true
            text = $guideDescriptions[$template.id][[int](!$isChinese)]
        })
        if ($template.category -eq 'root') {
            $pages.Add(@{type='patchouli:text';title=if ($isChinese) {'核心的取放'} else {'Using this core'}
                text=if ($isChinese) {'法术核心可直接放入编写台。画布左侧的核心不能右键取出，须点击取出核心按钮整体取走。$(br2)没有任何子节点时，取出仍为对应类型的法术核心；只要已有子节点但结构尚未完整，就会保存为未成形法术核心。核心参数在空结构下也会保留。'} else {'Insert this core directly into the writing table. The canvas root cannot be removed by right-clicking; use Take core to extract the whole assembly.$(br2)With no child nodes, it remains this typed spell core. A nonempty incomplete assembly becomes an Unformed Spell Core. Parameters are retained even when empty.'}})
        }
        if ($template.slots.Count -eq 0) {
            $pages.Add(@{type='patchouli:text';title=if ($isChinese) {'子节点'} else {'Child slots'}
                text=if ($isChinese) {'没有子节点槽。这是叶节点；选中它可编辑参数。右键画布上的节点可取出宝石，根节点须通过取出核心按钮取走。'} else {'This node has no child slots. Select it to edit its parameters. Right-click a leaf on the canvas to remove it; take a root using Take core.'}})
        } else {
            for ($offset=0; $offset -lt $template.slots.Count; $offset+=2) {
                $lines = @()
                foreach ($slot in @($template.slots | Select-Object -Skip $offset -First 2)) {
                    $slotName = $language[('slot.modularmagic.' + $slot.key)]
                    if (!$slotName) { throw "Missing slot label: $($slot.key)" }
                    if ($template.id -eq 'effect/ignite' -and $slot.key -eq 'action') { $slotName = if ($isChinese) {'原本已燃烧的生物'} else {'Already burning targets'} }
                    if ($template.id -eq 'effect/filtered' -and $slot.key -eq 'action') { $slotName = if ($isChinese) {'通过过滤时'} else {'Accepted targets'} }
                    if ($template.id -eq 'effect/filtered' -and $slot.key -eq 'fallback') { $slotName = if ($isChinese) {'未通过过滤时'} else {'Rejected targets'} }
                    if ($template.id -eq 'action/if' -and $slot.key -eq 'action') { $slotName = if ($isChinese) {'条件成立时'} else {'If true'} }
                    $count = if ($slot.list) { if ($isChinese) {'列表，0~64 项'} else {'List: 0-64 nodes'} }
                        elseif ($slot.optional) { if ($isChinese) {'可选，0 或 1 个'} else {'Optional: 0 or 1 node'} }
                        else { if ($isChinese) {'必填，1 个'} else {'Required: 1 node'} }
                    $lines += '$(li)' + $slotName + '$(br)' + $categoryNames[$slot.category] + ' / ' + $count
                }
                $pages.Add(@{type='patchouli:text';title=if ($isChinese) {'子节点'} else {'Child slots'};text=$lines -join '$(br2)'})
            }
        }
        for ($offset=0; $offset -lt $template.fields.Count; $offset+=3) {
            $lines = @()
            foreach ($field in @($template.fields | Select-Object -Skip $offset -First 3)) {
                $label = $language[('parameter.modularmagic.' + $field.key)]
                $optional = if ($field.optional) { if ($isChinese) {'可留空'} else {'Optional'} } else { if ($isChinese) {'必填'} else {'Required'} }
                $lines += '$(li)' + $label + ' / ' + $optional + '$(br)$(o)' + $field.key + '$()'
            }
            $pages.Add(@{type='patchouli:text';title=if ($isChinese) {'可编辑参数'} else {'Parameters'};text=$lines -join '$(br2)'})
        }
        if ($template.fields.Count -eq 0) {
            $pages.Add(@{type='patchouli:text';title=if ($isChinese) {'可编辑参数'} else {'Parameters'}
                text=if ($isChinese) {'此节点没有文本参数，通过子节点或节点种类确定行为。'} else {'This node has no text parameters. Its kind and child nodes determine its behavior.'}})
        }
        Write-JsonFile ($base + '/entries/nodes/' + $template.id + '.json') ([ordered]@{
            name=$language[('item.modularmagic.' + $template.item)]
            icon='modularmagic:' + $template.item; category='modularmagic:nodes'
            sortnum=100 * [array]::IndexOf($categoryOrder, $template.category) + $ordinal++; pages=$pages.ToArray()
        })
    }
}
Write-Output ('Generated bilingual node guide: {0} entries per language.' -f $guideDescriptions.Count)
