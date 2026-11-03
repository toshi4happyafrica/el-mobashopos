# el-mobashopos

mobaSHOPOS（モバイルPOS）の Android アプリ。Kotlin + Jetpack Compose + Room + Retrofit。

サーバーは `el-mobashopsrv`（API 仕様：`el-mobashopsrv/docs/api-spec.md`）。

## 画面の流れ（MVP）

```
端末登録（登録コード） → 店員ログイン（店員を選んで PIN） → メイン
   メイン ─ 注文を受ける → テーブル選択 → メニュー選択（カテゴリー別・数量±） → 確認 → 送信
         ─ 会計する     → テーブル選択 → 伝票表示 → お預かり金額 → 会計確定（お釣り表示）
         ─ マスターを更新 ／ ログアウト
```

- 金額（単価・合計・お釣り）はサーバーが計算する。画面の合計は目安表示。
- 注文・会計の送信には毎回 UUID（clientRequestId）を付け、通信失敗で再送しても二重登録されない。
- マスター（テーブル・カテゴリー・メニュー）は端末内（Room）に保存し、ETag で差分がある時だけ取り直す。
- 店員トークンが無効（期限切れ・端末の失効など）になると、自動でログイン画面に戻る。

## 動かし方（開発）

1. サーバーをローカルで起動する（`el-mobashopsrv` の README 参照）
   ```powershell
   cd C:\devapp\el-mobashopsrv
   npm run db:migrate:local
   npm run db:seed:local
   npm run dev          # エミュレータから使う場合
   npm run dev:lan      # 実機（同じ Wi-Fi）から使う場合
   ```
2. Android Studio で `C:\devapp\el-mobashopos` を開き、Gradle 同期後に `app` を実行する
3. 端末登録画面で入力する
   - サーバーURL：エミュレータは既定の `http://10.0.2.2:8787/` のまま。実機は `http://<PCのIPアドレス>:8787/`
   - 登録コード：`DEVC-0DE1`（開発用 seed）
4. 店員「店長（開発用）」を選び PIN `1234` でログイン

HTTP（非暗号化）通信はデバッグビルドだけ許可している（`app/src/debug/AndroidManifest.xml`）。本番は HTTPS のみ。

## 単体テスト

```powershell
.\gradlew.bat testDebugUnitTest
```

## 未対応・今後

- 端末トークンの暗号化保存（Android Keystore）
- 多通貨会計（USD/KHR の預り・釣銭の組み合わせ）、伝票の明細取消
- オフライン時の注文の一時保存と再送
