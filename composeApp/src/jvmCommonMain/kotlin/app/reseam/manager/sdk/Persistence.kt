package app.reseam.manager.sdk

import app.reseam.sdk.BuildRecord
import app.reseam.sdk.PatchSelection
import app.reseam.sdk.decodeBuildRecord
import app.reseam.sdk.decodeSelection
import app.reseam.sdk.encodeBuildRecord
import app.reseam.sdk.encodeSelection
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder

object BuildRecordSerializer : KSerializer<BuildRecord> {
    override val descriptor = PrimitiveSerialDescriptor("ReseamBuildRecord", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: BuildRecord) = encoder.encodeSerdeJson(encodeBuildRecord(value))
    override fun deserialize(decoder: Decoder): BuildRecord = decodeBuildRecord(decoder.decodeSerdeJson())
}

object SelectionSerializer : KSerializer<PatchSelection> {
    override val descriptor = PrimitiveSerialDescriptor("ReseamSelection", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: PatchSelection) = encoder.encodeSerdeJson(encodeSelection(value))
    override fun deserialize(decoder: Decoder): PatchSelection = decodeSelection(decoder.decodeSerdeJson())
}

private fun Encoder.encodeSerdeJson(value: String) = when (this) {
    is JsonEncoder -> encodeJsonElement(Json.parseToJsonElement(value))
    else -> encodeString(value)
}

private fun Decoder.decodeSerdeJson(): String = when (this) {
    is JsonDecoder -> decodeJsonElement().toString()
    else -> decodeString()
}
