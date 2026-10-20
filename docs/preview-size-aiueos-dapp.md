# app-preview（ページ表示）— itonami（dapp）上の分散型設計

状態: 第 1 スライス実装（2026-10-20）。正本はこの文書。
追跡: Linear JUN-128（親 JUN-116）。

## 位置づけ

app-preview は Preview —— 1 文書のページ一覧。capability は `fs/browse`
（デコードは kotoba-lang/kasane と provider）。**ページが何か、ページ一覧が
どう振る舞うか**を決める。

## このスライス: ページ表示判定コア

| 層 | 実体 | 状態 |
|---|---|---|
| 判定コア guest | `aiueos/preview_size.cljk` — size-swap（**表示サイズは回転後**。90/270/-90 で幅高さ入れ替え）/ page-id-ok（**id はページ index、印刷ラベル不可**）/ command-admissible（**読み取り専用**: quicklook/copy-path/export のみ） | **実装済み・実測** |
| 記述子 | `aiueos/preview-size.edn` | **実装済み** |
| parity | `test/app_preview/size_parity_test.cljk` — **実物の model.cljk**（mokuroku+amu+kotoba-sema クラスパス）と一致（displayed-size の回転・非回転・不明 / commands） | **実装済み・実測** |

実測: `amu test aiueos/preview_size.cljk` → **15/15**（jvm-kir / js / wasm）。
parity → 2 tests / 10 assertions / 0 failures。

## 正本の正確な規則（guest はこれに追従 —— 明文の設計意図つき）

- **表示サイズは回転後の幅高さ**: 「PDF のページは media box とは独立に回転を
  持つ。回転前の box を報告すると横長ページが縦長に見え、サムネイルグリッド
  がすべて崩れる」。rotation が 90 / 270 / -90 のとき [h w] を返す。
- **id はページ index（安定）**: 「印刷ラベルは不安定。前付けのある文書は同じ
  印刷ラベルのページが複数ある」。
- コマンドは読み取り専用 3 つ。「表示中の文書を書き換えられる viewer は
  別のプログラムで、ここには書き込み provider は無い」。

## 未実装境界

- kasane（PDF/PSD/AI デコード）と provider の page 抽出。
- rotation の unknown 値の扱い（正本は #{90 270 -90} のみ入れ替え = guest
  の test-fail-closed と一致）。
- mon（DNA）化、aiueos session 載せ（fleet 共通）。
