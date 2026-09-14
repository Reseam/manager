package app.reseam.manager.sdk

import app.reseam.sdk.OptionType
import app.reseam.sdk.OptionValue

fun OptionType.emptyValue(): OptionValue = when (this) {
    OptionType.STRING -> OptionValue.Text("")
    OptionType.BOOL -> OptionValue.Bool(false)
    OptionType.INT -> OptionValue.Int(0)
    OptionType.FLOAT -> OptionValue.Float(0.0)
    OptionType.STRING_LIST -> OptionValue.TextList(emptyList())
    OptionType.PATH -> OptionValue.Path("")
}
