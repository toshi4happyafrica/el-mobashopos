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

## 技術スタック（提案段階）

- Kotlin + Jetpack Compose + Room + OkHttp/Retrofit + Coroutines
- 金額は「最小単位の整数＋通貨コード」。浮動小数点は使わない
- 画面の文字列はリソース化し、多言語（日本語／英語／クメール語）に対応できる形にする
