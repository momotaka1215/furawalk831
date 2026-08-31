package com.momo.furawalk.data.remote.model

import com.momo.furawalk.core.domain.model.map.Availability
import com.momo.furawalk.core.domain.model.map.Checkpoint
import com.momo.furawalk.core.domain.model.map.CheckpointType
import com.momo.furawalk.core.domain.model.map.Rewards
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * サーバーから届くJSONの構造を表現するデータモデル
 */
@Serializable
data class CheckpointDto(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeter: Float,
    val type: String,
    val availability: AvailabilityDto? = null,
    val rewards: RewardsDto
) {
    fun toDomain(): Checkpoint {
        return Checkpoint(
            id = id,
            name = name,
            latitude = latitude,
            longitude = longitude,
            radiusMeter = radiusMeter,
            type = when(type) {
                "GOVERNMENT" -> CheckpointType.GOVERNMENT
                "PUBLIC" -> CheckpointType.PUBLIC
                "SCHOOL" -> CheckpointType.SCHOOL
                "ELEMENTARY_SCHOOL" -> CheckpointType.ELEMENTARY_SCHOOL
                "JUNIOR_HIGH_SCHOOL" -> CheckpointType.JUNIOR_HIGH_SCHOOL
                "HIGH_SCHOOL" -> CheckpointType.HIGH_SCHOOL
                "NURSING_SCHOOL" -> CheckpointType.NURSING_SCHOOL
                "CULTURAL" -> CheckpointType.CULTURAL
                "SPORT" -> CheckpointType.SPORT
                "BASEBALL_GROUND" -> CheckpointType.BASEBALL_GROUND
                "PARK_GOLF_COURSE" -> CheckpointType.PARK_GOLF_COURSE
                "PARK" -> CheckpointType.PARK
                "POST" -> CheckpointType.POST
                "STATION" -> CheckpointType.STATION
                "TOURISM" -> CheckpointType.TOURISM
                "SHOP" -> CheckpointType.SHOP
                "CONVENIENCE", "CVS" -> CheckpointType.CONVENIENCE
                "SUPERMARKET" -> CheckpointType.SUPERMARKET
                "DRUGSTORE" -> CheckpointType.DRUGSTORE
                "WELFARE" -> CheckpointType.WELFARE
                "HOSPITAL" -> CheckpointType.HOSPITAL
                "BANK" -> CheckpointType.BANK
                "GAS" -> CheckpointType.GAS
                "MAINTENANCE", "SERVICE" -> CheckpointType.MAINTENANCE
                "LIVE_HOUSE", "ENTERTAINMENT" -> CheckpointType.LIVE_HOUSE
                "SHRINE" -> CheckpointType.SHRINE
                "TEMPLE" -> CheckpointType.TEMPLE
                "SHRINE/TEMPLE" -> CheckpointType.SHRINE // 以前の互換性用
                "RESTAURANT" -> CheckpointType.RESTAURANT
                "RAMEN" -> CheckpointType.RAMEN
                "SUSHI" -> CheckpointType.SUSHI
                "CURRY" -> CheckpointType.CURRY
                "MEAT" -> CheckpointType.MEAT
                "BURGER" -> CheckpointType.BURGER
                "SOBA_UDON" -> CheckpointType.SOBA_UDON
                "BAKERY" -> CheckpointType.BAKERY
                "IZAKAYA" -> CheckpointType.IZAKAYA
                "SWEETS" -> CheckpointType.SWEETS
                "POLICE" -> CheckpointType.POLICE
                "FIRE" -> CheckpointType.FIRE
                "CAFE" -> CheckpointType.CAFE
                "COMPANY" -> CheckpointType.COMPANY
                "CROSSING" -> CheckpointType.CROSSING
                else -> CheckpointType.SIGHTSEEING
            },
            availability = Availability(
                nightSafe = availability?.nightSafe ?: true,
                winterAccessible = availability?.winterAccessible ?: true
            ),
            rewards = Rewards(
                exp = rewards.exp,
                money = rewards.money,
                itemId = rewards.itemId
            )
        )
    }
}

@Serializable
data class AvailabilityDto(
    @SerialName("night_safe") val nightSafe: Boolean = true,
    @SerialName("winter_accessible") val winterAccessible: Boolean = true
)

@Serializable
data class RewardsDto(
    val exp: Int,
    val money: Int,
    val itemId: String? = null
)
