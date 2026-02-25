# Fibersync 1.21.11 开发手册

本文档面向维护与二次开发，基于当前工作区的 1.21.11 版本适配结果整理。

**定位**
- Fabric 模组（Minecraft Java 1.21.11）
- 目标：存档备份、热回档、镜像同步（不重启服务器）

**关键词**
- 热回档（玩家不需断线）
- BossBar 进度展示
- 多备份槽
- 镜像同步

---

## 运行与构建

**要求**
- Java 21
- Gradle Wrapper（项目自带）

**构建**
```bash
./gradlew build
```

**产物**
- `build/libs/fibersync-1.21.11-0.3.2.jar`
- `build/libs/fibersync-1.21.11-0.3.2-sources.jar`

**安装**
- 将 `fibersync-1.21.11-0.3.2.jar` 放入游戏或服务端 `mods/`

---

## 配置文件

配置路径（相对于游戏/服务端根目录）：
- `config/fibersync.json`
- `config/formats.json`

### `fibersync.json`

默认字段与含义（来自 `Config.java`）：

- `backupDir`: 备份根目录  
  默认：`config/fibersync/backups`
  实际使用路径为 `backupDir/<levelName>`，其中 `<levelName>` 来自 `LevelStorage.Session.getDirectoryName()`（世界存档目录名）。
- `tempDir`: 临时目录  
  默认：`config/fibersync/temp`
- `dateFormat`: 备份时间格式  
  默认：`yyyy.MM.dd HH:mm:ss`
- `alternativeCmdPrefix`: 聊天替代前缀  
  默认：`["!!fs","!!qb"]`  
  作用：玩家聊天输入以该前缀开头时，会被转换为 `/fs` 命令执行。
- `defaultCountDown`: 回档/同步倒计时秒数  
  默认：`10`
- `maxBackupCount`: 最大备份数量  
  默认：`5`  
  `-1` 表示不限制。仅统计非 `isOldWorld` 的备份。
- `oldWorldName`: 自动备份名  
  默认：`oldworld`
- `oldWorldDescription`: 自动备份描述  
  默认：`回档前自动备份`
- `removeTargetDirBeforeCopy`: 备份前先删目标目录  
  默认：`false`
- `fileSkipMode`: 文件跳过策略  
  - `NEVER`: 不跳过
  - `MOTD`: 使用文件大小 + 修改时间判断是否跳过
  - `CHECKSUM`: MD5 校验一致则跳过
  默认：`CHECKSUM`
- `syncDir`: 镜像目录  
  默认：`null`  
  设置后开启镜像模式（`/fs sync`、`/fs list sync`）。
- `excludes`: 排除文件列表  
  结构为数组，例如：`["session.lock","some/relative/path"]`  
  匹配逻辑为**路径相等**（无通配），实现见 `SimpleFileExcluder`。

### `formats.json`

自定义消息与提示文本（`Formats.java`）。  
文本变量使用 `$1`, `$2`... 占位符（由 `TextRenderer` 替换）。  
可调整：回档/备份提示、进度标题、错误文案、列表展示等。

---

## 指令一览

根命令：`/fs`

**列表**
- `/fs list`：列出备份
- `/fs list sync`：列出镜像备份（需 `syncDir`）

**创建**
- `/fs create <name> <desc?>`：创建指定名称备份
- `/fs make <desc?>`：自动生成名称（b1/b2/...）

**回档**
- `/fs back <name>`：回档到指定备份  
  回档会进行确认 + 倒计时。

**同步（镜像模式）**
- `/fs sync <name>`：将镜像存档同步到当前世界
- `/fs sync <name> only <dims>`：仅同步指定维度  
  `dims` 支持：`overworld + nether + end + other`（用 `+` 连接）  
  例：`/fs sync myworld only overworld+nether`  
  实现逻辑：只同步列出的维度，其它维度排除。

**确认/取消**
- `/fs confirm <code>`：确认危险操作  
  确认码由系统生成，默认 20 秒内有效。
- `/fs cancel`：取消倒计时或确认流程

**删除**
- `/fs delete <name>`：删除备份（需要确认）

**锁定**
- `/fs lock <name>` / `/fs unlock <name>`  
  需要权限等级 >= 1  
  锁定备份不可覆盖、不可删除。

**重载配置**
- `/fs reload`：重载 `fibersync.json` 与 `formats.json`

---

## 备份结构

备份路径示例（默认配置）：
```
config/fibersync/backups/<levelName>/<backupName>/
  info.json
  world/
    level.dat
    region/
    poi/
    data/
    DIM-1/       (下界)
    DIM1/        (末地)
```

**info.json 字段**
- `name`, `date`, `description`, `locked`, `isOldWorld`, `creator`, `size`

**`oldworld` 备份**
- 回档前自动生成
- `isOldWorld=true`、`locked=true`
- 不计入 `maxBackupCount`

---

## 运行流程摘要

**备份**
1. 生成 `BackupEntry`
2. 按 `fileSkipMode` 复制世界文件
3. 更新 `info.json`
4. 使用 BossBar 展示进度

**回档**
1. 提示确认（`/fs confirm`）
2. 倒计时广播（`defaultCountDown`）
3. 进入 limbo（玩家留在服务器）
4. 回档复制（支持排除维度）
5. 退出 limbo，恢复玩家

**镜像同步**
流程与回档一致，但源为 `syncDir` 中的备份。

---

## 关键模块速览

**命令**
- `BackupCommand`：注册 `/fs` 命令与子命令
- `TaskManager`：单任务并发控制
- `ConfirmationManager`：确认码逻辑（默认 20s）

**备份/回档**
- `BackupEntry` / `BackupFactory` / `BackupInfo`
- `BackTask` / `BackupTask` / `SyncTask`
- `BackupExcluder`：维度过滤

**文件复制**
- `FileCopier`：复制与增量跳过逻辑
- `FileSkipMode`：NEVER / MOTD / CHECKSUM

**热回档核心**
- `Limbo` + `IReloadListener`
- `MixinMinecraftServer`：重载世界 + limbo 协调

**客户端/指令入口**
- `MixinCommandManager`：注册命令
- `MixinServerPlayNetworkHandler`：聊天前缀转命令

---

## 常见问题定位

**1. 回档/同步卡住**
- 检查是否有并发任务（`otherTaskRunning`）
- 检查倒计时是否被取消

**2. 镜像同步不可用**
- `syncDir` 未配置或路径不正确
- 镜像目录缺少 `info.json` 或 `world/`

**3. 备份数量限制**
- `maxBackupCount` 达到上限且所有备份已锁定

---

## 维护建议

- 修改 `formats.json` 时尽量保持占位符数量一致。
- `excludes` 为路径相等匹配，非通配符。需要排除目录时必须使用精确路径。
- `fileSkipMode=CHECKSUM` 会增加计算开销，适合大存档稳定备份。

