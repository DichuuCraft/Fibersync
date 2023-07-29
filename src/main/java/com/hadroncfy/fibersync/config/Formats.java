package com.hadroncfy.fibersync.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent.Action;
import net.minecraft.util.Formatting;

public class Formats {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .registerTypeHierarchyAdapter(Text.class, new Text.Serializer())
        .registerTypeHierarchyAdapter(Style.class, new Style.Serializer()).create();

    private static MutableText red(String s){
        return Text.literal(s).setStyle(Style.EMPTY.withColor(Formatting.RED));
    }
    public Text confirmationHint = Text.literal("[Fibersync] 使用")
        .append(Text.literal("/fs confirm $1").setStyle(Style.EMPTY.withColor(Formatting.GOLD)))
        .append(Text.literal("以确认此次操作"));
    public Text invalidConfirmationCode = red("[Fibersync] 无效的确认码");
    public Text opCancelled = Text.literal("[Fibersync] 操作已取消");
    public Text nothingToConfirm = red("[Fibersync] 无待确认的操作");
    public Text nothingToCancel = red("[Fibersync] 无待取消的操作");
    public Text otherTaskRunning = red("[Fibersync] 有其他任务正在运行");
    public Text creatingBackup = Text.literal("[Fibersync] $1: 正在创建备份")
        .append(Text.literal("$2").setStyle(Style.EMPTY.withColor(Formatting.GOLD)));
    public Text backupComplete = Text.literal("[Fibersync] $1: ")
        .append(Text.literal("$2").setStyle(Style.EMPTY.withColor(Formatting.GOLD)))
        .append(Text.literal("已备份完成").setStyle(Style.EMPTY.withColor(Formatting.RESET)));
    public Text backupFailed = red("[Fibersync] $1: 备份失败：$2");
    public Text overwriteAlert = Text.literal("[Fibersync] 此操作将会覆盖存档")
        .append(Text.literal("$1").setStyle(Style.EMPTY.withColor(Formatting.GOLD)));
    public Text backupListTitle = Text.literal("存档列表：").setStyle(Style.EMPTY.withColor(Formatting.BLUE));
    public Text mirrorListTitle = Text.literal("镜像存档列表：").setStyle(Style.EMPTY.withColor(Formatting.BLUE));
    public Text backupListItem = Text.empty().append(Text.literal("-[$1]").setStyle(Style.EMPTY.withColor(Formatting.GREEN)
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("点击回档至该存档").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(true))))
            .withClickEvent(new ClickEvent(Action.RUN_COMMAND, "/fs back $1"))))
        .append(Text.literal(" 时间：").setStyle(Style.EMPTY.withColor(Formatting.WHITE)))
        .append(Text.literal("$3").setStyle(Style.EMPTY.withColor(Formatting.GREEN)))
        .append(Text.literal(" 描述：").setStyle(Style.EMPTY.withColor(Formatting.WHITE)))
        .append(Text.literal("$2").setStyle(Style.EMPTY.withColor(Formatting.GREEN)));
    public Text lockedBackupListItem = Text.empty().append(Text.literal("-[$1]").setStyle(Style.EMPTY.withColor(Formatting.RED)
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("（此存档已锁定）点击回档至该存档").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(true))))
            .withClickEvent(new ClickEvent(Action.RUN_COMMAND, "/fs back $1"))))
        .append(Text.literal(" 时间：").setStyle(Style.EMPTY.withColor(Formatting.WHITE)))
        .append(Text.literal("$3").setStyle(Style.EMPTY.withColor(Formatting.GREEN)))
        .append(Text.literal(" 描述：").setStyle(Style.EMPTY.withColor(Formatting.WHITE)))
        .append(Text.literal("$2").setStyle(Style.EMPTY.withColor(Formatting.GREEN)));
    public Text backupListFooter = Text.empty().append(Text.literal("总大小：").setStyle(Style.EMPTY.withColor(Formatting.BLUE)))
        .append(Text.literal("$1M").setStyle(Style.EMPTY.withColor(Formatting.GOLD)));
    public Text backupNotExist = red("[Fibersync] 该备份不存在");
    public Text rollbackAlert = Text.literal("[Fibersync] 将要回档至")
        .append(Text.literal("$1").setStyle(Style.EMPTY.withColor(Formatting.GOLD)));
    public Text rollbackConfirmedAlert = Text.literal("[Fibersync] $1：将要回档至")
        .append(Text.literal("$2").setStyle(Style.EMPTY.withColor(Formatting.GOLD)));
    public Text rollbackStarted = Text.literal("[Fibersync] 正在回档");
    public Text rollbackFinished = Text.literal("[Fibersync] 回档完成");
    public Text startRegionBarTitle = Text.literal("生成出生点");
    public Text fileCopyBarTitle = Text.literal("复制存档文件");
    public Text creatingBackupTitle = Text.literal("创建存档")
        .append(Text.literal("$1").setStyle(Style.EMPTY.withColor(Formatting.GREEN)));
    public Text failedToCopyLevelFiles = red("[Fibersync] 复制存档文件失败：$1");
    public Text countDownTitle = Text.empty().append(Text.literal("准备回档：").setStyle(Style.EMPTY.withColor(Formatting.GREEN)))
        .append(Text.literal("$1").setStyle(Style.EMPTY.withBold(true)));
    public Text rollbackAborted = Text.literal("[Fibersync] $1：已取消回档");
    public Text nothingToAbort = red("[Fibersync] 无可取消的操作");
    public Text reloadedConfig = Text.literal("[Fibersync] 已加载配置");
    public Text failedToLoadConfig = red("[Fibersync] 加载配置失败：$1");
    public Text backupCountFull = red("[Fibersync] 已超出最大备份数");
    public Text deletedBackup = Text.literal("[Fibersync] $1：已删除备份$2");
    public Text failedToDeletedBackup = red("[Fibersync] $1：删除备份$2失败：$3");
    public Text backupLocked = red("[Fibersync] 删除失败：此存档已锁定");
    public Text overwriteFailedLocked = red("[Fibersync] 此存档已存在且已锁定");
    public Text allBackupsLocked = red("[Fibersync] 存档数已满且找不到可覆盖的存档");
    public Text lockedBackup = Text.literal("[Fibersync] $1：已锁定存档$2");
    public Text unlockedBackup = Text.literal("[Fibersync] $1：已解锁存档$2");
    public Text failedToWriteInfo = red("[Fibersync] $1：存档信息写入失败：$2");
    public Text deletingBackup = Text.literal("[Fibersync] $1：正在删除存档$2");
    public Text deletingBackupTitle = Text.literal("删除存档")
        .append(Text.literal("$1").setStyle(Style.EMPTY.withColor(Formatting.GREEN)));
    public Text failedToRetrieveList = red("[Fibersync] 获取存档列表时出错：$1");
    public Text failedToCopyFromTempDir = red("[Fibersync] 从临时目录复制文件时出错：$1");
    public Text copiedFromTempDir = Text.literal("[Fibersync] 文件复制完成");
    public Text syncComplete = Text.literal("[Fibersync] 同步完成");
    public Text syncAlert = Text.literal("[Fibersync] 将要同步至")
        .append(Text.literal("$1").setStyle(Style.EMPTY.withColor(Formatting.GOLD)));
    public Text syncConfirmedAlert = Text.literal("[Fibersync] $1：将要同步至")
        .append(Text.literal("$2").setStyle(Style.EMPTY.withColor(Formatting.GOLD)));
    public Text syncStarted = Text.literal("[Fibersync] 正在同步");
    public Text syncFinished = Text.literal("[Fibersync] 同步完成");
    public Text syncFailed = red("[Fibersync] 同步失败：$1");
    public Text syncCountDownTitle = Text.empty().append(Text.literal("准备同步：").setStyle(Style.EMPTY.withColor(Formatting.GREEN)))
        .append(Text.literal("$1").setStyle(Style.EMPTY.withBold(true)));
}