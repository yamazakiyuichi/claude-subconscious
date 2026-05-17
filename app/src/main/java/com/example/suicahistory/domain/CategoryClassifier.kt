package com.example.suicahistory.domain

object CategoryClassifier {

    private val rules = listOf(
        "コンビニ" to listOf("セブン", "7-eleven", "ローソン", "ファミマ", "ファミリーマート", "ミニストップ", "デイリー", "サークルk", "ポプラ"),
        "交通" to listOf("入場", "出場", "バス", "新幹線", "特急", "乗車", "jr", "東急", "小田急", "京王", "西武", "東武", "相鉄", "メトロ", "都営", "京急", "京成", "りんかい", "ゆりかもめ"),
        "飲食" to listOf("マクドナルド", "モスバーガー", "ケンタッキー", "スタバ", "スターバックス", "ドトール", "タリーズ", "松屋", "吉野家", "すき家", "なか卯", "サイゼ", "ガスト", "デニーズ", "ジョナサン", "バーミヤン", "餃子の王将", "日高屋"),
        "スーパー" to listOf("イオン", "イトーヨーカ", "西友", "ライフ", "マルエツ", "オーケー", "ベルク", "サミット", "まいばすけっと", "ミニピアゴ"),
        "ドラッグ" to listOf("マツキヨ", "ツルハ", "ウエルシア", "スギ薬局", "ドラッグ", "サンドラッグ", "カワチ"),
        "チャージ" to listOf("チャージ", "入金"),
    )

    fun classify(shopName: String): String {
        val lower = shopName.lowercase()
        for ((category, keywords) in rules) {
            if (keywords.any { lower.contains(it.lowercase()) }) return category
        }
        return "その他"
    }
}
