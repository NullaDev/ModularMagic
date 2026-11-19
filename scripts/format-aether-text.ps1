# Applies to handbook prose and displayed formulas, never to configuration or editable expressions.
function Format-AetherText([string]$text) {
    return [regex]::Replace($text, '(?<![\w.])\d{4,}(?![\d.])', {
        param($match)
        $amount = [decimal]::Parse($match.Value, [cultureinfo]::InvariantCulture)
        $suffixes = @('', 'K', 'M', 'B')
        $tier = 0
        while ($amount -ge 1000 -and $tier -lt 3) { $amount /= 1000; $tier++ }
        return $amount.ToString('0.##', [cultureinfo]::InvariantCulture) + $suffixes[$tier]
    })
}
