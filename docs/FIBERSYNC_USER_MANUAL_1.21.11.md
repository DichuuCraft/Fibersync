# Fibersync 使用手册（1.21.11）

本手册面向服务器管理员与玩家用户，按 `fs` 指令的功能章节说明用法与示例。
如果在 `fibersync.json` 里配置了自定义指令前缀（如 `command: "bf"`），请把所有示例里的 `/fs` 替换为 `/bf`。

**常用功能（增删改查）**

**/fs make**
用途：创建一个“自动命名”的备份。名称由系统生成（如 `b1/b2/...`）。
格式：`/fs make [描述]`
示例：
```bash
/fs make
/fs make 第一阶段完工
```

**/fs back**
用途：回档到指定备份。若不填名称，默认回档到“最新备份”。
格式：`/fs back [备份名]`
示例：
```bash
/fs back
/fs back b3
```
说明：执行回档通常需要二次确认。系统会提示一个确认码，请用 `/fs confirm <码>` 继续。

**/fs list**
用途：列出备份列表（含时间、描述、大小）。
格式：`/fs list`
示例：
```bash
/fs list
```

**/fs delete**
用途：删除指定备份（只能删除未锁定备份）。
格式：`/fs delete <备份名>`
示例：
```bash
/fs delete b2
```

**配置与管理类命令**

**/fs reload**
用途：重新加载 `fibersync.json` 配置。
格式：`/fs reload`
示例：
```bash
/fs reload
```

**/fs create**
用途：创建“自定义名称”的备份，可同时写描述。
格式：`/fs create <备份名> <描述>`
示例：
```bash
/fs create release_2026_02_21 版本发布前
```
说明：备份名建议使用字母、数字、下划线，避免空格。

**其他命令**

**/fs confirm**
用途：确认危险操作（回档、删除等）。
格式：`/fs confirm <确认码>`
示例：
```bash
/fs confirm 123
```

**/fs cancel**
用途：取消当前等待确认的操作或正在进行的倒计时回档。
格式：`/fs cancel`
示例：
```bash
/fs cancel
```

**/fs showhand**
用途：开启“无需二次确认”模式。
格式：`/fs showhand`
示例：
```bash
/fs showhand
```
说明：开启后，所有需要确认的操作将直接执行。

**/fs showhand stop**
用途：关闭“无需二次确认”模式。
格式：`/fs showhand stop`
示例：
```bash
/fs showhand stop
```

**/fs lock**
用途：锁定指定备份，防止被删除。
格式：`/fs lock <备份名>`
示例：
```bash
/fs lock b3
```
说明：需要一定权限等级（一般为 OP 权限）。

**/fs unlock**
用途：解锁指定备份。
格式：`/fs unlock <备份名>`
示例：
```bash
/fs unlock b3
```

**/fs sync**（镜像模式）
用途：镜像模式下同步指定镜像备份（需要配置 `syncDir`）。
格式：`/fs sync <备份名> [only <维度列表>]`
示例：
```bash
/fs sync b1
/fs sync b1 only overworld,nether
```

**/fs list sync**（镜像模式）
用途：列出镜像备份列表。
格式：`/fs list sync`
示例：
```bash
/fs list sync
```

