package yuu.record

import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.UUID

/**
 * @Author      : yuu
 * @Date        : 2024-07-23
 * @Description :
 */
// ChargingRecord.kt
@Serializable
data class ChargingRecord(
    @Serializable(with = UUIDSerializer::class) val id: UUID = UUID.randomUUID(), val date: String, val chargingTime: String, val chargingCost: Double, val totalRange: Int, val notes: String, val rangeAdded: Int = 0  // 将这个字段加回来，但默认值为0
)

object UUIDSerializer: KSerializer<UUID> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): UUID {
        return UUID.fromString(decoder.decodeString())
    }
}