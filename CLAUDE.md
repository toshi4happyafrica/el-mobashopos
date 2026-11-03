# el-mobashopos — mobaSHOPOS Androidアプリ

モバイルPOS「mobaSHOPOS」の Android クライアント。サーバーは別リポジトリ `el-mobashopsrv`。
旧システムの解析・設計方針は `el-mobashopsrv/docs/`（`legacy-spec.md`, `decisions.md`）を参照。

## 作業ルール

- 回答・ドキュメント・コミットメッセージは日本語。結論を先に書く
- 事実と推定を分け、未検証のことは「未確認」と明記する
- 既存のコード・文書の書き方（命名、構成、文体）に合わせる。一貫性を優先する
- 迷ったら選択肢を示し、推奨を1つ添えて止まる
- 次の操作は実行前に必ず確認を取る：削除／履歴の書き換え（rebase, force push 等）／本番リリース・ストア公開／外部への送信・公開／課金が発生する操作
- 旧資料 `G:\マイドライブ\EL.GDR\ELEKTRO.PJ\pjREF\` は読み取り専用。書き込まない

## 技術スタック

- Kotlin + Jetpack Compose + Room + OkHttp/Retrofit + Coroutines
- 金額は「最小単位の整数＋通貨コード」。浮動小数点は使わない
- 画面の文字列はリソース化し、多言語（日本語／英語／クメール語）に対応できる形にする
- 依存バージョンは `gradle/libs.versions.toml` で一元管理（Gradle 9.6.0 / AGP 9.5.0 / Kotlin 2.2.10 / KSP 2.3.6 / Room 2.8.5）
  - 2026-11-04 に Android Studio のアップグレードで AGP 9 系へ移行。`gradle.properties` の互換フラグ（`android.builtInKotlin=false`、`android.newDsl=false` など）は Studio が追加したもので、外す場合は動作確認してから
- Room の DAO では、suspend メソッドに Unit を返させない（件数・行IDを返す）。DAO に `@Transaction` メソッドを置かず、まとめた処理は `withTransaction` で行う（KSP2 の既知の不具合 "unexpected jvm signature V" を避けるため）

## 構成

- パッケージ `jp.elektrolibera.mobashopos`（単一モジュール `app`）
- `data/api` サーバー API（Retrofit + kotlinx.serialization）／`data/db` マスターキャッシュ（Room）／`data/settings` トークン・設定（DataStore）／`data/PosRepository.kt` 窓口
- `domain` 金額の表示・入力変換、注文カート（Android 非依存。単体テストあり）
- `ui` Compose 画面。`ui/PosApp.kt` が登録状態・ログイン状態で大枠の画面を切り替える
- 金額はサーバーが確定する。端末側の合計は目安表示のみ
- 注文・会計は clientRequestId（UUID）を付けて送り、再送時は同じ値を使う
