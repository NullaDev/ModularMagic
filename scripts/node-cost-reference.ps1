# Documentation defaults, not registered billing rules. Keep formulas independent of item storage/config APIs.
. (Join-Path $PSScriptRoot 'format-aether-text.ps1')
function Get-NodeCostReferenceBase($node) {
    # Order: Chinese mana, English mana, Chinese focus, English focus.
    $sum = @('只计算连接在后面的节点，本身不另收。','Only the connected nodes contribute; this node adds no mana fee.','连接在后面的节点专注相加。','Add the focus costs of the connected nodes.')
    $leaf = @('0','0','0.1。伤害、等级和命中人数都不改变这个数值。','0.1, regardless of damage, effect level or number of targets.')
    switch ($node.category) {
        'root' { return @('启动 0.5，再加整套法术的费用。纯粒子或声音法术启动费为 0.01。','0.5 to start, plus the spell. A purely decorative spell starts at 0.01.','1，再加装入的节点。持续核心每次施放脉冲分别计算。','1 plus the assembled nodes. Each channeling pulse is a separate cast.') }
        'trigger' { return @('0。只决定从哪里起法。','0. This only chooses where the spell starts.','0。选择起点不额外增加结构负担。','0. Choosing an origin adds no structural cost.') }
        'modifier' { return @('0。后续效果照常计算。','0. Subsequent effects are priced normally.','0.05；偏移距离和角度不改变专注。','0.05; distance and angle do not change focus.') }
        'particle_renderer' { return @('0。粒子本身另计。','0. The particle emission is priced separately.','0；包装其他外观时，也不收取额外专注。','0, including wrappers around another appearance.') }
        'projectile_renderer' { return @('0。弹体投送和伤害另计。','0. Projectile delivery and effects are priced separately.','0。外观不会让伤害法术更难控制。','0. Appearance adds no structural burden.') }
        'motion' { return @('用于有碰撞效果的弹体时，每个弹体每 tick 计 0.001；纯装饰粒子的运动不收费。','0.001 per projectile per tick; motion of decorative particles is free.','基础运动 0.05；轨迹控制还要加上所装空间修饰的专注，只计算一次。','0.05; controlled motion also includes its modifiers, counted once.') }
        'selector' {
            if ($node.id -eq 'selector/self') { return @('0。选取自身不用扫描周围。','0. Selecting yourself needs no area scan.','0.1，不随后面的效果强度改变。','0.1, independent of the following effect strength.') }
            if ($node.id -eq 'selector/move') { return @($sum[0],$sum[1],'所装空间修饰与选区的专注相加。','Add the focus of the attached modifiers and selector.') }
            if ($node.id -eq 'selector/compound') { return @('各个选区的查询费用相加。','Add the query costs of its selectors.','0.1，再加每个选区的专注。','0.1 plus the focus of each selector.') }
            return @('每次查询 0.05 + 0.002×V。V 是扫描体积；直线和圆弧按各次采样的查询相加。','Each query costs 0.05 + 0.002*V, where V is scan volume. Lines and arcs add their sample queries.','0.1。扫描范围和找到多少人不改变专注。','0.1. Scan range and target count do not change focus.')
        }
        'predicate' {
            if ($node.id -eq 'predicate/depth_test' -or $node.id -eq 'predicate/ranged_test') {
                return @('每个实际检查的位置计 0.001，再加被检查条件的费用。','0.001 per position actually checked, plus its condition.','0.1 + L(n)×C。n 是最多检查的位置数，C 是所装条件的专注。','0.1 + L(n)*C, using the maximum position count n and attached condition cost C.')
            }
            return @('0。组合条件时，涉及的范围检查仍按各自规则计算。','0. Any nested spatial searches keep their own costs.','0.1，再加装入的条件和空间修饰；不会按判断次数重复收取整套专注。','0.1 plus attached conditions and modifiers, counted as structure rather than per test.')
        }
        'filter' { return @('0。筛选之后执行的效果另计。','0. Effects applied after filtering are priced separately.','0.1，再加装入的过滤条件；不乘被筛选人数。','0.1 plus attached filters, without multiplying by target count.') }
        'projectile' { return @('运动维护、逐次命中和实际触发的动作分别相加。每种回调都要预留足够的次数。','Add motion maintenance, hits and triggered actions. Reserve a bounded number of callbacks.','运动、外观和各处动作的结构专注相加，只计算一次。不会每飞一 tick 都再收一遍。','Add motion, appearance and event structures once, not again on every flight tick.') }
    }
    $rules = @{
        'action/list'=$sum
        'action/move'=@($sum[0],$sum[1],'所装空间修饰与后续动作的专注相加。','Add the attached modifiers and action focus.')
        'action/synced_logic'=@($sum[0],$sum[1],'0.05，再加后续动作；同一作用留下的光影不另收一遍。','0.05 plus the attached action; its accompanying appearance is not charged a second time.')
        'action/if'=@('判断所需费用，加上实际执行的那一支。预留时按更贵的一支准备。','Pay for the test and chosen branch. Reserve for the more expensive branch.','0.1 + P + max(A,B) + 0.25×min(A,B)。P 是条件专注，A、B 是两支专注。','0.1 + P + max(A,B) + 0.25*min(A,B), for condition P and branches A and B.')
        'action/delay'=@($sum[0],$sum[1],'0.15 + H(t) + C。t 是等待 tick，C 是后续动作的专注；等待期间不重复扣。','0.15 + H(t) + C, for delay t in ticks and action cost C. No repeated waiting fee.')
        'action/variable'=@($sum[0],$sum[1],'0.1，再加后续动作；变量算出多大的数字不改变专注。','0.1 plus the attached action; the variable value does not affect focus.')
        'action/random'=@($sum[0],$sum[1],'0.1 + 0.02×q，再加后续动作。q 是生成的变量个数。','0.1 + 0.02*q plus the attached action, for q declared variables.')
        'action/processor'=@('所装选区的查询费，加上每个目标实际尝试的效果费。','Selector query costs plus each attempted effect on each target.','0.1，再加选区与各效果的结构专注；不乘人数。','0.1 plus selector and effect structures; do not multiply by targets.')
        'action/sound'=@('每次播放 0.001。','0.001 per sound played.','0。纯声音法术的控制结构也享受装饰折扣。','0. Purely decorative control structures also receive the cosmetic discount.')
        'action/set_block'=@('每次尝试放置方块 0.2。方块与朝向由节点参数决定。','0.2 per placement attempt. Node settings choose the block and facing.','0.1。所选方块不改变专注。','0.1, regardless of the chosen block.')
        'action/set_block_facing'=@('每次尝试放置方块 0.2，与放置方块相同。','0.2 per placement attempt, like Place Block.','0.1。设置方块朝向不另加专注。','0.1. Block facing adds no separate focus fee.')
        'action/remove_block'=@('每次尝试移除方块 0.5。掉落方式与允许移除的方块由规则限制。','0.5 per removal attempt, subject to allowed blocks and drop rules.',$leaf[2],$leaf[3])
        'action/knock_block'=@('每块 2 + v²，再预留碰撞伤害 m(d)。v 为初速，d 为允许的最大碰撞伤害；这部分随方块投送扣除。','2 + v*v per block, plus reserved collision damage m(d), for speed v and maximum damage d; this damage allowance is charged on delivery.',$leaf[2],$leaf[3])
        'action/schedule_tick'=@('每次安排方块更新 0.1。','0.1 per scheduled block update.','0.15 + H(t)。只算这次安排，不逐 tick 重复收取。','0.15 + H(t), charged for the arrangement rather than every waiting tick.')
        'action/arrow'=@('每支 0.5 + 0.1×v²，再加箭的伤害 m(d) 和药水效果费用。','0.5 + 0.1*v*v per arrow, plus damage m(d) and potion payload costs.',$leaf[2],$leaf[3])
        'action/trident'=@('每支 0.5 + 0.1×v²，再加三叉戟伤害 m(d)。v 是发射速度。','0.5 + 0.1*v*v per trident, plus its damage m(d). v is launch speed.',$leaf[2],$leaf[3])
        'action/custom_projectile'=@('每个弹体投送 0.5 + 0.1×v²，再加弹体配置的维护、命中和动作费用。','0.5 + 0.1*v*v per projectile, plus configured maintenance, hits and actions.','0.1，再加弹体配置的结构专注；寿命和命中人数不直接增加专注。','0.1 plus configuration structure; lifetime and target count do not directly add focus.')
        'effect/damage'=@('每目标每次 m(d)=k×(0.5d+0.0125d²)。d 是请求伤害，k 为伤害类型系数，普通类型默认 1。','Per target and attempt: m(d)=k*(0.5*d+0.0125*d*d). d is requested damage; ordinary damage uses k=1.',$leaf[2],$leaf[3])
        'effect/delay'=@($sum[0],$sum[1],'0.15 + H(t) + C。与等待动作相同，不乘被保留的目标人数。','0.15 + H(t) + C, without multiplying by retained targets.')
        'effect/knockback'=@('每目标 0.5b+0.5b²。b 是击退强度，不是目标最终移动的距离。','0.5*b+0.5*b*b per target, for requested knockback strength b.',$leaf[2],$leaf[3])
        'effect/push'=@('每目标 0.5v+0.5v²。v 为推力速度。','0.5*v+0.5*v*v per target, for push speed v.',$leaf[2],$leaf[3])
        'effect/set_delta'=@('每目标 0.5v+0.5v²。v 为设置后的速度向量长度。','0.5*v+0.5*v*v per target; v is the requested velocity magnitude.',$leaf[2],$leaf[3])
        'effect/teleport'=@('每目标 2+0.15r+0.002r²。r 是本次传送距离。','2+0.15*r+0.002*r*r per target, for teleport distance r.',$leaf[2],$leaf[3])
        'effect/effect'=@('持续效果：每目标 0.02×t×(a+1)²×k。瞬间效果改为 (a+1)²×k，不按时长打折。','Timed effects: 0.02*t*(a+1)^2*k per target. Instant effects use (a+1)^2*k instead of duration.',$leaf[2],$leaf[3])
        'effect/property'=@('每目标 0.02×t。t 是要求的燃烧或冻结 tick 数。','0.02*t per target, for requested fire or freezing ticks t.',$leaf[2],$leaf[3])
        'effect/ignite'=@('每目标点燃 0.02×t。对原本已燃烧的生物执行附加效果时，再付对应效果费。','Ignition costs 0.02*t per target. Extra effects on already burning targets keep their own costs.','0.1，再加装入的附加效果；不随燃烧时长或目标人数变化。','0.1 plus attached effects, independent of burn duration or target count.')
        'effect/filtered'=@('按每个目标实际进入的分组计算效果费。预留时每人取较贵一支。','Charge the branch each target takes; reserve the more expensive branch per target.','0.1 + P + max(A,B) + 0.25×min(A,B)，与条件分支相同。','0.1 + P + max(A,B) + 0.25*min(A,B), like a conditional branch.')
        'effect/cast_at'=@('每个选中生物执行后续动作的费用相加。','Add the action costs for each selected living target.','0.1，加后续动作的结构专注一次，不乘生物数量。','0.1 plus the action structure once, without multiplying by targets.')
        'effect/projectile_hit'=@('每个非生物目标一次 0.5125，相当于普通 1 点伤害的报价。','0.5125 per non-living target, the ordinary one-damage quote.',$leaf[2],$leaf[3])
    }
    if ($rules.ContainsKey($node.id)) { return $rules[$node.id] }
    if ($node.id -in @('action/particle','action/block_particle','action/item_particle','action/dust_particle','action/transition_particle','action/custom_particle')) {
        return @('每个粒子 0.0002。纯装饰粒子的运动和外观不另收费。','0.0002 per particle. Decorative motion and appearance add no fee.','0。纯粒子展开的控制结构按通常专注的 2% 计算。','0. Purely decorative control structures use 2% of normal focus.')
    }
    if ($node.id -in @('action/iterate','action/iterate_linear','action/iterate_arc','action/random_pos_fan','action/random_pos_sphere','action/block_in_range','action/iterate_delayed','action/while_delayed')) {
        $focusZh='0.2 + L(n)×C。n 是展开次数上限，C 是后续动作的专注。'
        $focusEn='0.2 + L(n)*C, for maximum repetitions n and body focus C.'
        if ($node.id -in @('action/iterate_delayed','action/while_delayed')) {
            $focusZh='0.35 + L(n)×C + H(T)。T 为等待跨度的 tick 数。条件循环再加 0.1 和所装条件的专注。'
            $focusEn='0.35 + L(n)*C + H(T), for total delay span T. While loops also add 0.1 and the condition structure.'
        }
        if ($node.id -eq 'action/block_in_range') {
            $focusZh='0.2 + L(n)×C；n 为最多遍历的方块数。逐方块延迟不会再收一遍专注。'
            $focusEn='0.2 + L(n)*C for at most n blocks. Per-block waiting does not charge focus again.'
        }
        return @('每次真正执行的后续动作费用相加。循环不会让伤害或建材变便宜。','Add the body costs for every actual repetition. Loops do not discount damage or materials.',$focusZh,$focusEn)
    }
    throw "Missing documented costs for $($node.id)"
}

# These reference formulas share billing's normalized base units; present displayed mana to players.
function Get-NodeCostReference($node) {
    $cost = @(Get-NodeCostReferenceBase $node)
    $prices = @{'0.0002'='0.2';'0.001'='1';'0.002'='2';'0.01'='10';'0.0125'='12.5';'0.02'='20';'0.05'='50';'0.1'='100';'0.15'='150';'0.2'='200';'0.5'='500';'0.5125'='512.5';'2'='2000'}
    foreach ($i in @(0,1)) {
        $cost[$i] = [regex]::Replace($cost[$i], '(?<![\d.^])(?:0\.\d+|2)(?![\d.])', {param($match) if ($prices.ContainsKey($match.Value)) {$prices[$match.Value]} else {$match.Value}})
    }
    $cost[0]=$cost[0].Replace('2000 + v²','2000 + 1000×v²').Replace('瞬间效果改为 (a+1)²','瞬间效果改为 1000×(a+1)²')
    $cost[1]=$cost[1].Replace('2000 + v*v','2000 + 1000*v*v').Replace('Instant effects use (a+1)^2','Instant effects use 1000*(a+1)^2')
    if ($node.category -eq 'root') {
        $cost[2]='每次施法消耗 5 + 10C + 0.625C² 专注。C 是整套节点的结构复杂度；固定的 5 点不享受装饰折扣。持续施法的每次脉冲分别计算。'
        $cost[3]='Each cast costs 5 + 10*C + 0.625*C^2 focus. C is total structural complexity. The fixed 5 is never discounted. Each channeling pulse is a separate cast.'
    } else {
        $cost[2]=$cost[2].Replace('专注','复杂度')
        $cost[3]=$cost[3].Replace('focus','complexity').Replace('Focus','Complexity')
    }
    $cost[0] = Format-AetherText $cost[0]
    $cost[1] = Format-AetherText $cost[1]
    return $cost
}
