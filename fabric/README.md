# GeyserSkinManager — Fabric

Fabric サーバーサイド Mod として動作する GeyserSkinManager の実装モジュールです。  
Geyser（Bedrock クライアント対応プロキシ）経由で接続した統合版プレイヤーのスキンを、Java Edition プレイヤーに対して自動的に適用します。

---

## 前提条件

| 項目 | バージョン |
|------|-----------|
| Java | 21 以上 |
| Minecraft | ≥ 26.1 |
| Fabric Loader | ≥ 0.18.5 |
| Fabric API | 0.144.4+26.1 |
| Geyser API | 2.9.0-SNAPSHOT |
| Floodgate API | 2.2.4-SNAPSHOT（任意） |

> **Floodgate について**  
> Floodgate が導入されている場合は、スキン適用処理が Floodgate に委譲されます（`FabricSkinEventListener` 内で `provideTargetSkins` フラグによって制御）。Floodgate なし構成でも動作します。

---

## ビルド方法

リポジトリルートで以下を実行します。

```bash
# Fabric モジュールのみビルド（common を自動的に含む）
./gradlew :fabric:build

# 出力先
# fabric/build/libs/GeyserSkinManager-Fabric-<version>.jar
```

ビルド成果物は `fabric/build/libs/` に生成されます。ソースJARも同時出力されます。

### Gradle マルチプロジェクト構成

```
settings.gradle
├── :common   — プラットフォーム共通ロジック
└── :fabric   — Fabric 固有の実装（common を include して fat-jar 化）
```

`:fabric` は `:common` を `include project(':common')` により **fat-jar** に同梱するため、サーバーには `GeyserSkinManager-Fabric-*.jar` 1ファイルを配置するだけで動作します。

---

## インストール

1. ビルドした `GeyserSkinManager-Fabric-*.jar` を Fabric サーバーの `mods/` フォルダに配置します。
2. Fabric API が `mods/` に存在することを確認します。
3. サーバーを起動すると `config/GeyserSkinManager/` に設定ファイルが生成されます。

---

## モジュール構成

```
fabric/src/main/java/.../fabric/
├── GeyserSkinManager.java              — エントリーポイント（DedicatedServerModInitializer）
├── FabricSkinEventListener.java        — プレイヤー JOIN/DISCONNECT イベント処理
├── FabricBedrockSkinUtilityListener.java — カスタムペイロード通信（Mod クライアント検出・スキン送信）
└── FabricSkinApplier.java              — GameProfile へのスキン適用とパケット同期

fabric/src/main/resources/
└── fabric.mod.json                     — Mod メタデータ（environment: server）
```

---

## Fabric 固有の実装ポイント

### 1. エントリーポイントと初期化

`GeyserSkinManager` が `DedicatedServerModInitializer` を実装しており、`fabric.mod.json` の `"server"` エントリーポイントとして登録されています。クライアントサイドのコードは一切含まれません。

```json
"environment": "server",
"entrypoints": {
  "server": ["com.github.camotoy.geyserskinmanager.fabric.GeyserSkinManager"]
}
```

`onInitializeServer()` では以下を行います。

1. `FabricLoader.getInstance().getConfigDir()` から設定ディレクトリを解決
2. `Configuration.create()` で設定ファイルをロード
3. Floodgate の有無を確認して `FabricSkinEventListener` を初期化

---

### 2. プレイヤーイベント（JOIN / DISCONNECT）

`FabricSkinEventListener` が `ServerPlayConnectionEvents` を使用してプレイヤーの入退場を検知します。

```java
ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
    onPlayerJoin(handler.getPlayer(), server);
});

ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
    modListener.onPlayerLeave(handler.getPlayer());
});
```

JOIN 時の処理フロー:

1. `BedrockSkinRetriever` で統合版プレイヤーかどうかを判定
2. スキンが取得できた場合、`GameProfile` に既存のテクスチャプロパティがなければスキンをアップロード/キャッシュ取得
3. Mod クライアント（BedrockSkinUtility）検出用のリスナーに通知

---

### 3. カスタムペイロード通信（Fabric Networking v1 / 1.21.2+ 新API）

Minecraft 1.21.2 以降、プラグインメッセージの API が刷新されました。`FabricBedrockSkinUtilityListener` ではこの新 API を使用しています。

#### カスタムペイロードの定義

`CustomPayload` インターフェースを実装した `record` を定義します。チャンネル識別子は `Constants.MOD_PLUGIN_MESSAGE_NAME`（`"bedrockskin:data"`）を使用します。

```java
public record BedrockSkinPayload(byte[] data) implements CustomPayload {
    public static final Id<BedrockSkinPayload> ID =
        new Id<>(Identifier.of(Constants.MOD_PLUGIN_MESSAGE_NAME));

    public static final PacketCodec<PacketByteBuf, BedrockSkinPayload> CODEC =
        CustomPayload.codecOf(
            (payload, buf) -> buf.writeBytes(payload.data()),
            buf -> {
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                return new BedrockSkinPayload(bytes);
            }
        );
}
```

#### ペイロード型の登録

`PayloadTypeRegistry.playS2C()` に登録することで、S2C（サーバー→クライアント）方向の送信が可能になります。

```java
PayloadTypeRegistry.playS2C().register(BedrockSkinPayload.ID, BedrockSkinPayload.CODEC);
```

> **注意**: ペイロード登録は Mod 初期化時（コンストラクタ内）に 1 回だけ行う必要があります。

#### Mod クライアントの検出

クライアントが `bedrockskin:data` チャンネルの受信を登録（`REGISTER` パケット送信）した時点で、そのプレイヤーが Mod 対応クライアントだと判断します。

```java
S2CPlayChannelEvents.REGISTER.register((handler, sender, server, channels) -> {
    if (channels.contains(BedrockSkinPayload.ID.id())) {
        server.execute(() -> onModdedPlayerConfirm(handler.getPlayer()));
    }
});
```

`server.execute()` でメインスレッドに処理を移してから `onModdedPlayerConfirm()` を呼ぶことで、スレッドセーフティを確保しています。

#### ペイロードの送信

```java
@Override
public void sendPluginMessage(byte[] payload, ServerPlayerEntity player) {
    ServerPlayNetworking.send(player, new BedrockSkinPayload(payload));
}
```

---

### 4. スキン適用とパケット同期（FabricSkinApplier）

`FabricSkinApplier.setSkin()` は以下の手順でスキンを適用します。

#### 1. GameProfile へのテクスチャ設定

```java
GameProfile profile = player.getGameProfile();
profile.getProperties().removeAll("textures");
profile.getProperties().put("textures",
    new Property("textures", skinEntry.getJavaSkinValue(), skinEntry.getJavaSkinSignature()));
```

`GameProfile` は Authlib が管理するため、直接プロパティを書き換えます。

#### 2. 他プレイヤーへの同期

Minecraft 1.21.2 以降、プロフィール変更は自動では伝播しないため、手動でパケットを送信する必要があります。

| パケット | 目的 |
|---------|------|
| `PlayerListS2CPacket`（`UPDATE_LISTED`） | プレイヤーリストのテクスチャを更新 |
| `EntitiesDestroyS2CPacket` | エンティティを一時的に非表示にする |
| `EntitySpawnS2CPacket` | エンティティを再スポーンして新スキンを反映 |

> Mixin を使用していないため、`onCanSee()` などのフックなしに純粋なパケット操作のみでスキン同期を実現しています。

---

## 依存ライブラリの同梱

`:common` モジュールに加え、設定ファイルのパース（YAML）に使用するライブラリも fat-jar に同梱されます（`include` ディレクティブ）。

| ライブラリ | バージョン | 用途 |
|-----------|-----------|------|
| `jackson-dataformat-yaml` | 2.16.0 | YAML 設定ファイルのパース |
| `snakeyaml` | 2.2 | YAML エンジン |
| `jackson-databind/core/annotations` | 2.16.0 | JSON/YAML データバインディング |

Geyser API・Floodgate API は `compileOnly`（実行環境に存在することを前提）です。

---

## 設定ファイル

初回起動時に `config/GeyserSkinManager/config.yml` が生成されます。詳細はリポジトリルートの `README.md` を参照してください。
