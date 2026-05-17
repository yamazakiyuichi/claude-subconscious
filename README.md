# Suica履歴アプリ

モバイルSuicaの利用履歴から毎日の購入内容を確認・分析するAndroidアプリ。

## 機能

- **NFC読み取り** — Suicaをかざすだけで直近20件を取得（ネット不要）
- **Web同期** — モバイルSuicaにログインして全履歴を自動取得
- **日別リスト** — 日付ごとに購入内容と合計金額を表示
- **月次グラフ** — カテゴリ別の積み上げ棒グラフ＋今月の円グラフ
- **カテゴリ分類** — コンビニ・交通・飲食・スーパー等に自動分類

## セットアップ

### 必要環境
- Android Studio Hedgehog 以降
- Android SDK 26+
- NFC対応のAndroid実機（テスト用）

### ビルド
```bash
./gradlew assembleDebug
```

### テスト
```bash
./gradlew test
```

## 使い方

### NFC読み取り（直近20件）
1. アプリを起動
2. 画面右下のFABをタップ
3. SuicaカードまたはモバイルSuicaが入ったスマホの背面を当てる

### Web同期（全履歴）
1. メニュー → 設定・ログイン
2. モバイルSuicaのメールアドレス・パスワードを入力
3. 「ログインして履歴を取得」をタップ

## アーキテクチャ

```
data/
  nfc/        FeliCaReader    — NFC FeliCa読み取り
  web/        SuicaScraper    — Webスクレイピング
  db/         Room DB         — ローカル保存
  repository/ TransactionRepository
domain/
  CategoryClassifier          — 店舗名→カテゴリ
ui/
  home/       日別リスト
  monthly/    月次グラフ
  settings/   ログイン設定
```

## 注意事項

- モバイルSuicaのWebスクレイピングは非公式な手法です。サイトの仕様変更により動作しなくなる場合があります。
- ログイン情報はデバイス内のみで使用し、外部サーバーには送信しません。
