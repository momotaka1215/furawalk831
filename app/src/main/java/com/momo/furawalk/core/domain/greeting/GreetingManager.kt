package com.momo.furawalk.core.domain.greeting

import java.time.LocalDateTime
import java.time.Month
import java.util.Locale
import kotlin.random.Random

/**
 * ペットの状態を表す列挙型
 */
enum class PetMood {
    HAPPY, NORMAL, BORED, SLEEPY, EXCITED, LONELY
}

/**
 * 挨拶データクラス
 */
data class GreetingData(
    val title: String,
    val message: String
)

/**
 * ゲーム内の状況をまとめたコンテキスト
 */
data class GreetingContext(
    val userName: String = "冒険者",
    val dateTime: LocalDateTime = LocalDateTime.now(),
    val isBirthday: Boolean = false,
    val petName: String = "ペット",
    val petMood: PetMood = PetMood.NORMAL,
    val affectionLevel: Int = 0,
    val daysTogether: Int = 1,
    val isPetHungry: Boolean = false,
    val isPetUnhappy: Boolean = false,
    val isPetSleeping: Boolean = false,
    val lastFedHours: Int = 0,
    val lastPlayedHours: Int = 0,
    val yesterdayPassedCount: Int = 0,
    val lastWalkDistanceKm: Double = 0.0,
    val lastStepCount: Int = 0
)

/**
 * 挨拶生成マネージャー
 */
class GreetingManager {
    private var lastSelectedMessage: String? = null

    private enum class Category(val baseWeight: Double) {
        BIRTHDAY(10.0),
        PET_HUNGRY(6.0),
        PET_MOOD(6.0),
        PET_HAPPY(5.0),
        PET_SLEEPY(4.0),
        PET_GROWTH(5.0),
        PET_TALK(8.0),
        PASSING(3.0),
        ACTIVITY_DISTANCE(3.0),
        ACTIVITY_STEPS(3.0),
        GAME_TIPS(2.0),
        PET_TIPS(2.0),
        WALK_TIPS(2.0),
        NORMAL(6.0),
        DEVELOPER(0.1)
    }

    fun generateGreeting(context: GreetingContext = GreetingContext()): GreetingData {
        val availableCategories = mutableMapOf<Category, Double>()

        if (context.isBirthday) availableCategories[Category.BIRTHDAY] = Category.BIRTHDAY.baseWeight
        if (context.isPetHungry) availableCategories[Category.PET_HUNGRY] = Category.PET_HUNGRY.baseWeight
        if (context.isPetUnhappy) availableCategories[Category.PET_MOOD] = Category.PET_MOOD.baseWeight
        if (context.petMood == PetMood.HAPPY || context.petMood == PetMood.EXCITED) {
            availableCategories[Category.PET_HAPPY] = Category.PET_HAPPY.baseWeight
        }
        if (context.isPetSleeping || context.petMood == PetMood.SLEEPY) {
            availableCategories[Category.PET_SLEEPY] = Category.PET_SLEEPY.baseWeight
        }
        if (context.affectionLevel > 10 || context.daysTogether % 7 == 0) {
            availableCategories[Category.PET_GROWTH] = Category.PET_GROWTH.baseWeight
        }

        availableCategories[Category.PET_TALK] = Category.PET_TALK.baseWeight
        availableCategories[Category.PASSING] = Category.PASSING.baseWeight
        
        if (context.lastWalkDistanceKm > 0.1) availableCategories[Category.ACTIVITY_DISTANCE] = Category.ACTIVITY_DISTANCE.baseWeight
        if (context.lastStepCount > 100) availableCategories[Category.ACTIVITY_STEPS] = Category.ACTIVITY_STEPS.baseWeight
        
        availableCategories[Category.GAME_TIPS] = Category.GAME_TIPS.baseWeight
        availableCategories[Category.PET_TIPS] = Category.PET_TIPS.baseWeight
        availableCategories[Category.WALK_TIPS] = Category.WALK_TIPS.baseWeight
        availableCategories[Category.NORMAL] = Category.NORMAL.baseWeight
        availableCategories[Category.DEVELOPER] = Category.DEVELOPER.baseWeight

        val selectedCategory = selectWeightedCategory(availableCategories)
        
        val candidates = when (selectedCategory) {
            Category.BIRTHDAY -> PetGreetingRepository.getBirthdayGreetings(context)
            Category.PET_HUNGRY -> PetGreetingRepository.getHungryGreetings(context)
            Category.PET_MOOD -> PetGreetingRepository.getMoodGreetings(context)
            Category.PET_HAPPY -> PetGreetingRepository.getHappyGreetings(context)
            Category.PET_SLEEPY -> PetGreetingRepository.getSleepyGreetings(context)
            Category.PET_GROWTH -> PetGreetingRepository.getGrowthGreetings(context)
            Category.PET_TALK -> PetGreetingRepository.getTalkGreetings(context)
            Category.PASSING -> NormalGreetingRepository.getPassingGreetings(context)
            Category.ACTIVITY_DISTANCE -> NormalGreetingRepository.getDistanceGreetings(context)
            Category.ACTIVITY_STEPS -> NormalGreetingRepository.getStepGreetings(context)
            Category.GAME_TIPS -> TipsRepository.getGameTips()
            Category.PET_TIPS -> TipsRepository.getPetTips(context)
            Category.WALK_TIPS -> TipsRepository.getWalkTips()
            Category.DEVELOPER -> TipsRepository.getDevComments()
            else -> NormalGreetingRepository.getNormalGreetings(context)
        }

        val filtered = candidates.filter { it.message != lastSelectedMessage }
        val selected = if (filtered.isNotEmpty()) filtered.random() else candidates.random()
        
        lastSelectedMessage = selected.message
        return selected
    }

    private fun selectWeightedCategory(categories: Map<Category, Double>): Category {
        val total = categories.values.sum()
        if (total <= 0.0) return Category.NORMAL
        var r = Random.nextDouble() * total
        for ((cat, w) in categories) {
            r -= w
            if (r <= 0) return cat
        }
        return categories.keys.first()
    }
}

/**
 * ペット関連のメッセージリポジトリ
 */
object PetGreetingRepository {
    fun getBirthdayGreetings(c: GreetingContext) = listOf(
        GreetingData("🎂 誕生日おめでとう！", "${c.userName}さん、今日は最高の日ですね！${c.petName}もリボンをつけて待っています✨"),
        GreetingData("🎉 Happy Birthday!", "${c.petName}が朝から嬉しそうに走り回っています。${c.userName}さんの誕生日を分かっているみたい！"),
        GreetingData("🍰 特別な1日", "ハッピーバースデー！今日は${c.petName}といっぱいお祝いしましょう🎁"),
        GreetingData("🕯️ 願いを込めて", "お誕生日おめでとう！${c.petName}と一緒に素敵な1年になりますように。"),
        GreetingData("🎊 お祝いだね", "${c.userName}さんにとって笑顔あふれる誕生日になりますように。${c.petName}も隣にいるよ🐾"),
        GreetingData("💖 大切なパートナー", "${c.userName}さん、生まれてきてくれてありがとう。${c.petName}もそう言ってるみたい✨"),
        GreetingData("🎁 感謝のきもち", "今日は${c.userName}さんの大切な日。${c.petName}といっしょにゆっくり過ごしてね。"),
        GreetingData("🎈 パーティー！", "お誕生日おめでとう！${c.petName}がお祝いのダンスを踊ってるよ💃"),
        GreetingData("🌟 輝く1年を", "Happy Birthday！新しい1年も、${c.petName}との楽しい思い出をたくさん作ろうね。"),
        GreetingData("🎶 おめでとうの歌", "${c.petName}が鼻歌を歌ってるよ。${c.userName}さんへのバースデーソングかな？")
    )

    fun getHungryGreetings(c: GreetingContext) = (1..20).map { i ->
        GreetingData("🍖 お腹すいた？", "${c.petName}がお腹を空かせて${c.userName}さんを見つめているよ。ごはんをあげよう！(No.$i)")
    }

    fun getMoodGreetings(c: GreetingContext) = (1..20).map { i ->
        GreetingData("🎾 遊ぼうよ！", "${c.petName}が退屈そうに尻尾を振っているよ。一緒におもちゃで遊ぼう！(No.$i)")
    }

    fun getHappyGreetings(c: GreetingContext) = (1..20).map { i ->
        GreetingData("✨ しあわせ！", "${c.petName}はとってもご機嫌！${c.userName}さんと一緒で本当に嬉しいんだね。(No.$i)")
    }

    fun getSleepyGreetings(c: GreetingContext) = (1..15).map { i ->
        GreetingData("💤 すやすや", "${c.petName}はぐっすり夢の中。${c.userName}さんのそばが一番落ち着くんだね。(No.$i)")
    }

    fun getGrowthGreetings(c: GreetingContext) = (1..15).map { i ->
        GreetingData("📈 すくすく", "出会ってから${c.daysTogether}日目。${c.petName}との絆が深まっているよ。(No.$i)")
    }

    fun getTalkGreetings(c: GreetingContext) = listOf(
        GreetingData("🐾 ${c.petName}", "「今日はどこへ行くの？」"),
        GreetingData("🐾 ${c.petName}", "「${c.userName}さん、大好きだよ！」"),
        GreetingData("🐾 ${c.petName}", "「お外の空気、とっても気持ちいいね！」"),
        GreetingData("🐾 ${c.petName}", "「あっちに何かキラキラしたものがあるかも！」"),
        GreetingData("🐾 ${c.petName}", "「${c.userName}さんと一緒に歩くの、一番の楽しみなんだ」"),
        GreetingData("🐾 ${c.petName}", "「ねえねえ、後でいっぱいなでなでしてね！」"),
        GreetingData("🐾 ${c.petName}", "「お腹いっぱいになると、眠たくなっちゃうね」"),
        GreetingData("🐾 ${c.petName}", "「富良野って、とっても広いんだね。びっくりしちゃう」"),
        GreetingData("🐾 ${c.petName}", "「次はあそこの公園に行ってみない？」"),
        GreetingData("🐾 ${c.petName}", "「${c.userName}さんの歩くリズム、すごく落ち着くんだぁ」"),
        GreetingData("🐾 ${c.petName}", "「今日はどんな素敵なことが待ってるかな？」"),
        GreetingData("🐾 ${c.petName}", "「ずっとずっと、一緒にいてね？」"),
        GreetingData("🐾 ${c.petName}", "「ぼく、${c.userName}さんの相棒でいられて幸せだよ」"),
        GreetingData("🐾 ${c.petName}", "「お花が綺麗に咲いてるね。いい匂い！」"),
        GreetingData("🐾 ${c.petName}", "「疲れたら、ぼくの隣で休んでいいからね」"),
        GreetingData("🐾 ${c.petName}", "「お腹グーって鳴っちゃった。内緒だよ？」"),
        GreetingData("🐾 ${c.petName}", "「${c.userName}さんの笑顔が見えると、ぼくも元気になるよ」"),
        GreetingData("🐾 ${c.petName}", "「今日はなんだか、遠くまで行けそうな気がする！」"),
        GreetingData("🐾 ${c.petName}", "「雪が降ったら、いっしょに遊ぼうね！」"),
        GreetingData("🐾 ${c.petName}", "「ラベンダーの色、${c.userName}さんに似合ってるよ」")
    ) + (1..10).map { i -> GreetingData("🐾 ${c.petName}", "「もっといっぱいお話ししたいな！(No.$i)」") }
}

/**
 * Tips関連のメッセージリポジトリ
 */
object TipsRepository {
    fun getGameTips() = (1..30).map { i ->
        GreetingData("💡 Tips", "毎日少しずつ歩くことが、富良野を冒険する一番の近道だよ。(No.$i)")
    }
    fun getPetTips(c: GreetingContext) = (1..30).map { i ->
        GreetingData("💡 ペットのコツ", "${c.petName}の名前を呼んであげると、きっと喜ぶよ。(No.$i)")
    }
    fun getWalkTips() = (1..20).map { i ->
        GreetingData("💡 お散歩の知恵", "疲れたら無理せず、富良野の美味しい空気を吸ってリフレッシュしよう。(No.$i)")
    }
    fun getDevComments() = (1..10).map { i ->
        GreetingData("🛠️ 開発チーム", "Furawalkを遊んでくれてありがとう！優しい世界を楽しんでね。(No.$i)")
    }
}

/**
 * 通常挨拶リポジトリ
 */
object NormalGreetingRepository {
    fun getPassingGreetings(c: GreetingContext) = (1..20).map { i ->
        GreetingData("🤝 すれ違い", "昨日は${c.yesterdayPassedCount}人の冒険者とすれ違ったね。(No.$i)")
    }
    fun getDistanceGreetings(c: GreetingContext) = (1..15).map { i ->
        GreetingData("📏 記録", "前回は${String.format(Locale.US, "%.1f", c.lastWalkDistanceKm)}km歩いたよ！(No.$i)")
    }
    fun getStepGreetings(c: GreetingContext) = (1..15).map { i ->
        GreetingData("👟 歩数", "前回の記録は${c.lastStepCount}歩。バッチリだね！(No.$i)")
    }

    fun getNormalGreetings(c: GreetingContext): List<GreetingData> {
        val h = c.dateTime.hour
        val m = c.dateTime.month
        val name = c.userName
        val dw = c.dateTime.dayOfWeek.name
        
        val list = mutableListOf<GreetingData>()
        val timeLabel = when(h) {
            in 5..10 -> "🌅 おはよう"
            in 11..17 -> "☀️ こんにちは"
            in 18..21 -> "🌆 こんばんは"
            else -> "🌙 こんばんは"
        }
        
        val seasonMsg = when(m) {
            Month.MARCH, Month.APRIL, Month.MAY -> "春の風が気持ちいいね。🌸"
            Month.JUNE, Month.JULY, Month.AUGUST -> "夏が来た！ラベンダーが楽しみだね。🪻"
            Month.SEPTEMBER, Month.OCTOBER, Month.NOVEMBER -> "秋の景色、綺麗だね。🍂"
            else -> "寒いから温かくしてね。❄️"
        }

        repeat(40) { i ->
            list.add(GreetingData(timeLabel, "${name}さん、${timeLabel.substring(2)}！$seasonMsg (No.$i)"))
        }
        return list
    }
}
