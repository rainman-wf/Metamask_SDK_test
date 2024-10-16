package ru.rainman.metamasksdktest.network.dto.eth

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = Params.ParamsSerializer::class)
sealed interface Params {

    @Serializable
    data class Input(
        val from: String,
        val to: String,
        val input: String,
    ) : Params

    @Serializable
    data class Data(
        val from: String,
        val to: String,
        val data: String,
    ) : Params

    @Serializable
    data class Text(
        val value: String,
    ) : Params

    companion object ParamsSerializer : KSerializer<Params> {

        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("Params", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): Params = Text("")

        override fun serialize(encoder: Encoder, value: Params) {
            when (value) {
                is Text -> encoder.encodeString(value.value)
                is Input -> encoder.encodeSerializableValue(Input.serializer(), value)
                is Data -> encoder.encodeSerializableValue(Data.serializer(), value)
            }
        }
    }
}





