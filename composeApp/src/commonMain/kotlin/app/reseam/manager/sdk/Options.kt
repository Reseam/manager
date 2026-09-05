package app.reseam.manager.sdk

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class OptionType {
    @SerialName("string") String,
    @SerialName("bool") Bool,
    @SerialName("int") Int,
    @SerialName("float") Float,
    @SerialName("string_list") StringList,
    @SerialName("path") Path,
}

@Serializable
sealed interface OptionValue {
    @Serializable @SerialName("string") data class Text(val value: String) : OptionValue
    @Serializable @SerialName("bool") data class Bool(val value: Boolean) : OptionValue
    @Serializable @SerialName("int") data class Int(val value: Long) : OptionValue
    @Serializable @SerialName("float") data class Float(val value: Double) : OptionValue
    @Serializable @SerialName("string_list") data class TextList(val value: List<String>) : OptionValue
    @Serializable @SerialName("path") data class Path(val value: String) : OptionValue
}

@Serializable
data class OptionDeclaration(
    val key: String,
    val title: String,
    val description: String,
    val optionType: OptionType,
    val defaultValue: OptionValue? = null,
    val validValues: List<String>? = null,
    val required: Boolean,
)

fun OptionType.emptyValue(): OptionValue = when (this) {
    OptionType.String -> OptionValue.Text("")
    OptionType.Bool -> OptionValue.Bool(false)
    OptionType.Int -> OptionValue.Int(0)
    OptionType.Float -> OptionValue.Float(0.0)
    OptionType.StringList -> OptionValue.TextList(emptyList())
    OptionType.Path -> OptionValue.Path("")
}
