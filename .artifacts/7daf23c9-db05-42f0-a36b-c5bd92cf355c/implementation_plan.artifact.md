# ペット登録機能の実装（カメラ撮影・情報登録）

ユーザーが自分の飼っているペット（犬・猫など）を名前、種別、サイズ、体重とともに登録し、スマートフォンのカメラで撮影した写真をペットのプロフィール画像として設定できる機能を実装します。

## Proposed Changes

### 1. データモデルの拡張
#### [MODIFY] [PetEntity.kt](file:///C:/AndroidAppFiles/furawark20260831/app/src/main/java/com/momo/furawalk/data/local/room/entity/PetEntity.kt)
- `customImageUri: String?` フィールドを追加し、撮影した写真のパスを保存できるようにします。

### 2. 設定・インフラの更新
#### [MODIFY] [file_paths.xml](file:///C:/AndroidAppFiles/furawark20260831/app/src/main/res/xml/file_paths.xml)
- カメラアプリとファイルを共有するためのパス設定を追加します。

### 3. 新規画面の作成
#### [NEW] [PetRegistrationScreen.kt](file:///C:/AndroidAppFiles/furawark20260831/app/src/main/java/com/momo/furawalk/ui/screens/PetRegistrationScreen.kt)
- ペットの名前、種別（犬・猫の選択）、身長、体重を入力するフォーム。
- カメラを起動して写真を撮影する機能。
- 撮影した写真のプレビュー表示（丸枠・四角枠の切り替え検討）。

### 4. 既存画面とナビゲーションの更新
#### [MODIFY] [Screen.kt](file:///C:/AndroidAppFiles/furawark20260831/app/src/main/java/com/momo/furawalk/ui/navigation/Screen.kt)
- `PetRegistration` ルートを追加します。

#### [MODIFY] [NavGraph.kt](file:///C:/AndroidAppFiles/furawark20260831/app/src/main/java/com/momo/furawalk/ui/navigation/NavGraph.kt)
- ペット登録画面への遷移を追加します。

#### [MODIFY] [MainActivity.kt](file:///C:/AndroidAppFiles/furawark20260831/app/src/main/java/com/momo/furawalk/MainActivity.kt)
- ペット登録完了時のDB保存ロジックを実装します。

#### [MODIFY] [PetScreen.kt](file:///C:/AndroidAppFiles/furawark20260831/app/src/main/java/com/momo/furawalk/ui/screens/PetScreen.kt)
- `customImageUri` がある場合に、撮影した画像を表示するように更新します。

## Verification Plan

### Automated Tests
- `PetDaoTest` (もしあれば) で `customImageUri` の保存と取得を確認します。

### Manual Verification
1.  ペット画面から「ペット登録」ボタン（または未登録時の案内）をタップして登録画面へ。
2.  名前、体重などを入力。
3.  「写真を撮る」ボタンを押し、カメラを起動して撮影。
4.  撮影した写真がプレビューに表示されることを確認。
5.  登録ボタンを押し、ペット画面に移動して登録した情報と写真が表示されることを確認。
