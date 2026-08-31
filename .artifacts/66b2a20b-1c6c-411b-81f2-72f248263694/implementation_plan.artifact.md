# GPS再検索ボタンの追加計画

GPS信号を待機している（位置情報が得られない）状態の時にのみ表示される「再検索ボタン」を `PlayScreen` に追加します。これにより、信号の取得が滞った場合にユーザーが手動でGPS取得をリスタートできるようになります。

## 変更点

### UIレイヤー

#### [MODIFY] [PlayScreen.kt](file:///C:/AndroidAppFiles/furawark/app/src/main/java/com/momo/furawark/ui/screens/PlayScreen.kt)
- `PlayScreen` コンポーザブルに `onRetryGPS: () -> Unit` 引数を追加します。
- `LocationStatusCard` にも `onRetryGPS` を渡し、`location == null` の時（待機中）のみボタンを表示するように変更します。

#### [MODIFY] [NavGraph.kt](file:///C:/AndroidAppFiles/furawark/app/src/main/java/com/momo/furawark/ui/navigation/NavGraph.kt)
- `AppNavGraph` に `onRetryGPS: () -> Unit` 引数を追加し、`PlayScreen` へ渡します。

### プラットフォーム・アクティビティレイヤー

#### [MODIFY] [MainActivity.kt](file:///C:/AndroidAppFiles/furawark/app/src/main/java/com/momo/furawark/MainActivity.kt)
- `AppNavGraph` の呼び出し箇所で、`locationProvider` と `headingProvider` を再起動するロジックを `onRetryGPS` として実装します。
- 位置情報取得を一度 `stopTracking()` してから `startTracking()` を呼ぶことで、Google Play Services の位置情報リクエストをリフレッシュします。

## 検証プラン

### 手動確認
1. アプリを起動し、Play画面に遷移する。
2. GPS信号がまだ取得できていない（または室内などで取得が難しい）状態で、「GPS信号を待機中...」というメッセージと共に「再検索」ボタンが表示されることを確認する。
3. ボタンを押した際、Logcat に `LocationProvider` の開始ログが再度出力されることを確認する。
4. 位置情報が取得できたら、ボタンが自動的に消えることを確認する。
