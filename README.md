--------------------------------------------------------------------------------

Fibersync

[中文README](https://github.com/DichuuCraft/Fibersync/blob/master/README_zh.md)

**A high-performance "Hot Restore" backup solution for Fabric.**

Fibersync is a backup and restoration tool specifically designed for the Fabric ecosystem. Compared to other backup plugins, Fibersync's standout feature is its core **"Hot Restore"** technology, aimed at providing a seamless, intuitive, and secure server management experience.

--------------------------------------------------------------------------------

✨ Key Features
- 🔥 **True "Hot Restore"** Restoration operations **do not require a server restart**, and online players **do not need to disconnect**. This perfectly solves the issue of having to re-spawn Carpet Bots after a rollback.
- 📊 **Visual Progress Management** All backup and restoration operations are equipped with **progress bar displays**, making complex operations clearly visible.
- 💾 **Multi-Slot & Locking Mechanism** Supports the creation of multiple backup slots and provides a **locking function** to prevent important backups from being accidentally deleted.
- 🌍 **Universal Support** Fibersync runs stably on both **multiplayer servers** and **single-player worlds**.
- 🛡️ **Security Confirmation Workflow** When performing dangerous operations like restoring or deleting, the system generates a **random confirmation code** to ensure every action is intentional.

--------------------------------------------------------------------------------

🚀 Quick Start

Installation

1. Ensure **Fabric Loader** is installed in your environment.
2. Place the downloaded `.jar` file into the `mods` folder of your server or client.
3. Launch the game or server; the program will automatically generate the `fibersync.json` configuration file.

Basic Commands

| Command              | Description                                                                         |
| -------------------- | ----------------------------------------------------------------------------------- |
| `/fs make [desc]`    | Creates an "auto-named" backup (e.g., b1, b2...).                                   |
| `/fs back [name]`    | Restores to a specific backup. If left blank, it defaults to the "latest backup".   |
| `/fs list`           | Lists backups including time, description, and file size.                           |
| `/fs lock <name>`    | Locks a specific backup to prevent deletion.                                        |
| `/fs confirm <code>` | Enter the system-prompted code to execute a restore or delete.                      |
| `/fs showhand`       | Enables "Showhand" mode; all operations will execute directly without confirmation. |

--------------------------------------------------------------------------------

🛠️ Advanced Features

Custom Backups

If you need to give a backup a specific name, use: `/fs create <name> <description>` _Tip: It is recommended to use letters, numbers, and underscores for backup names; avoid using spaces._

Administrator Mode (Showhand)

If you are debugging and find secondary confirmations tedious, you can enable "No-Confirmation" mode:

- **Enable:** `/fs showhand` — All operations will execute immediately without a confirmation code.
- **Disable:** `/fs showhand stop` — Returns to secure confirmation mode.

Mirror Mode

For advanced users, Fibersync supports synchronization with mirror servers by configuring `syncDir`:

- Use `/fs sync <name> [only <dimensions>]` to synchronize a specific mirror, with support for dimension-specific syncing.
- Use `/fs list sync` to view available mirror backups.

--------------------------------------------------------------------------------

⚙️ Configuration

In `fibersync.json`, you can customize the command prefix. For example, if you change the prefix to `bf`, all `/fs` commands will change to `/bf`.

- **Compatibility:** Supports `!!qb` and `!!pb` command styles.
- **Reload:** Use `/fs reload` to apply changes made to the config file.

--------------------------------------------------------------------------------

🤝 Contribution & Feedback

This project has been rewritten and adapted for the latest Minecraft versions. If you encounter any bugs or have functional suggestions during use, please feel free to submit an **Issue** or a **Pull Request**.
