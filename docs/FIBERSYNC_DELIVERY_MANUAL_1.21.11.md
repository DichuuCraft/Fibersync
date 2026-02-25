# Fibersync 1.21.11 交付手册

本文档面向服主/运维/管理员，涵盖安装、配置、指令说明与常见问题排查。版本基于 Fabric 1.21.11 与 Fibersync `1.21.11-0.3.2`。

**文件与目录默认位置（相对服务器根目录）**
- 配置：`config/fibersync.json`
- 文本格式：`config/formats.json`
- 备份目录：`config/fibersync/backups`
- 临时目录：`config/fibersync/temp`

## 安装与升级
1. 将 `fibersync-1.21.11-0.3.2.jar` 放入服务器 `mods` 目录
2. 启动服务器，自动生成 `config/fibersync.json` 与 `config/formats.json`
3. 如需修改配置，改完后可在控制台执行 `/fs reload` 重载

## 基本概念
- **备份**：将当前世界存档复制到备份目录（默认 `config/fibersync/backups`）
- **回档**：将世界存档回滚到指定备份
- **oldworld**：回档前自动备份的默认名称（可在配置中改）
- **镜像同步（sync）**：将已存在的备份同步到另一目录（需 `syncDir` 配置）

## 指令总览（/fs）
命令名：`/fs`

**注意**
- 部分指令在镜像模式（`syncDir` 配置为非空）才可用
- `lock/unlock` 需要权限等级 ≥ 1
- `confirm/cancel` 用于二次确认敏感操作（回档/删除等）

### 1) 备份列表
```
/fs list
```
列出所有备份（名称、时间、描述、总大小）。

镜像模式下可查看镜像列表：
```
/fs list sync
```

### 2) 创建备份
```
/fs make
/fs make <描述>
/fs create <name> <描述>
```
说明：
- `/fs make` 会自动生成备份名（如 `b1`, `b2`）
- `/fs create` 可指定名称
- 若达到 `maxBackupCount`，会自动覆盖最旧的未锁定备份

### 3) 回档
```
/fs back
/fs back <name>
```
说明：
- `/fs back` 默认回到最新备份
- 会触发 **二次确认**
- 服务器会先自动备份为 `oldworld` 再回档

确认：
```
/fs confirm <code>
```
取消：
```
/fs cancel
```

### 4) 删除备份
```
/fs delete <name>
```
说明：
- 需要二次确认 `/fs confirm <code>`
- 被锁定的备份不能删除

### 5) 锁定/解锁备份
```
/fs lock <name>
/fs unlock <name>
```
说明：
- 锁定后不会被自动覆盖、不能删除
- 需要权限等级 ≥ 1

### 6) 配置重载
```
/fs reload
```
重新加载 `fibersync.json` 与 `formats.json`。

### 7) showhand 模式（跳过确认）
```
/fs showhand
/fs showhand stop
```
说明：
- `showhand` 开启后，所有需要二次确认的操作将直接执行
- `showhand stop` 关闭后，恢复确认流程
- 默认关闭（见 `fibersync.json` 的 `showhand`）

### 8) 镜像同步（需要 syncDir）
```
/fs sync <name>
/fs sync <name> only <维度列表>
```
说明：
- 仅在 `fibersync.json` 中配置了 `syncDir` 时可用
- `only` 支持维度过滤（多个维度用逗号分隔）

示例：
```
/fs sync b4
/fs sync b4 only overworld,nether
```

## 配置说明（fibersync.json）
常见字段：
- `backupDir`：备份目录
- `tempDir`：临时目录
- `maxBackupCount`：最大备份数量（-1 表示不限制）
- `defaultCountDown`：回档默认倒计时秒数
- `oldWorldName`：回档前自动备份名称
- `oldWorldDescription`：回档前自动备份说明
- `fileSkipMode`：文件跳过策略（`CHECKSUM` 为默认）
- `syncDir`：镜像同步目录（为空则关闭镜像模式）
- `excludes`：同步/备份的排除规则
- `showhand`：是否跳过所有确认（默认 `false`）
- `command`：自定义指令主名称（默认 `fs`）。系统仍保留 `/fs` 作为别名。

## 常见问题排查
### 1) 指令无回显
确认 `formats.json` 是否被破坏或为空；可删除后重启生成默认配置，或 `/fs reload`。

### 2) 回档后方块/物品/位置不一致
确保使用最新 jar。回档后玩家数据会从备份 NBT 重新加载并刷新位置/物品。

### 3) 回档后不能破坏方块
确保使用最新 jar。该问题常由玩家 `interactionManager` 没有同步世界导致。

## 交付清单
- `mods/fibersync-1.21.11-0.3.2.jar`
- `config/fibersync.json`
- `config/formats.json`
- 备份目录：`config/fibersync/backups`
