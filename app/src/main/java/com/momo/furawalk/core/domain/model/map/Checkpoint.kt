package com.momo.furawalk.core.domain.model.map

/**
 * 富良野のランドマークや店舗などのマスターデータ
 */
data class Checkpoint(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeter: Float,
    val type: CheckpointType,
    val priority: Int = 3, // 追加（デフォルトは標準の3）
    val rewards: Rewards,
    val availability: Availability = Availability()
)

data class Availability(
    val nightSafe: Boolean = true,
    val winterAccessible: Boolean = true
)

enum class CheckpointType {
    SIGHTSEEING,  // 観光地
    SHOP,         // 店舗
    PARK,         // 公園
    STATION,      // 駅
    GOVERNMENT,   // 役所・公共機関
    PUBLIC,       // 公共施設（公民館等）
    SCHOOL,       // 学校・教育
    ELEMENTARY_SCHOOL,   // 小学校
    JUNIOR_HIGH_SCHOOL,  // 中学校
    HIGH_SCHOOL,         // 高校
    NURSING_SCHOOL,      // 看護学校
    CULTURAL,     // 文化施設（図書館・劇場）
    SPORT,        // スポーツ施設
    BASEBALL_GROUND,     // 野球場
    PARK_GOLF_COURSE,    // パークゴルフ場
    POST,         // 郵便局
    TOURISM,      // 観光施設
    CONVENIENCE,  // コンビニ
    SUPERMARKET,  // スーパー
    DRUGSTORE,    // ドラッグストア
    WELFARE,      // 福祉施設
    HOSPITAL,     // 病院・クリニック
    BANK,         // 銀行・金融機関
    GAS,          // ガソリンスタンド
    MAINTENANCE,  // 整備・修理
    LIVE_HOUSE,   // ライブハウス
    SHRINE,       // 神社
    TEMPLE,       // 寺院
    RESTAURANT,   // 飲食店
    RAMEN,        // ラーメン
    SUSHI,        // 寿司
    CURRY,        // カレー
    MEAT,         // 焼肉・ステーキ
    BURGER,       // バーガー
    SOBA_UDON,    // そば・うどん
    BAKERY,       // パン
    IZAKAYA,      // 居酒屋
    SWEETS,       // スイーツ
    POLICE,       // 警察
    FIRE,         // 消防
    CAFE,         // カフェ
    COMPANY,      // 企業
    CROSSING      // 交差点・信号
}

data class Rewards(
    val exp: Int,
    val money: Int,
    val itemId: String? = null
)
