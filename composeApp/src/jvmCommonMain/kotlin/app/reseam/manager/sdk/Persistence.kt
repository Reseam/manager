package app.reseam.manager.sdk

import app.reseam.sdk.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder

/** Navigation stores stable serde JSON, never BoltFFI's version-specific wire bytes. */
object SelectionSerializer : KSerializer<PatchSelection> {
    override val descriptor = PrimitiveSerialDescriptor("ReseamSelection", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: PatchSelection) = encoder.encodeSerdeJson(encodeSelection(value))
    override fun deserialize(decoder: Decoder): PatchSelection = decodeSelection(decoder.decodeSerdeJson())
}

object PatchMetadataSerializer : KSerializer<List<PatchMetadata>> {
    override val descriptor = PrimitiveSerialDescriptor("ReseamPatchMetadata", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: List<PatchMetadata>) = encoder.encodeSerdeJson(encodePatchMetadata(value))
    override fun deserialize(decoder: Decoder): List<PatchMetadata> = decodePatchMetadata(decoder.decodeSerdeJson())
}


// JSON stores retain structured objects/arrays; other navigation encoders carry
// the same schema as a string. Neither path duplicates the SDK's field schema.
private fun Encoder.encodeSerdeJson(value: String) = when (this) {
    is JsonEncoder -> encodeJsonElement(Json.parseToJsonElement(value))
    else -> encodeString(value)
}

private fun Decoder.decodeSerdeJson(): String = when (this) {
    is JsonDecoder -> decodeJsonElement().toString()
    else -> decodeString()
}
