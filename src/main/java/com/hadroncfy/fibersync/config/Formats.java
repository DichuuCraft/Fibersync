package com.hadroncfy.fibersync.config;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent.Action;
import net.minecraft.util.Formatting;

public class Formats {
    private static Text red(String s){
        return new LiteralText(s).setStyle(new Style().setColor(Formatting.RED));
    }
    private static Text empty(){
        return new LiteralText("");
    }
    public Text confirmationHint = new LiteralText("[Fibersync] 使用")
        .append(new LiteralText("/fs confirm $1").setStyle(new Style().setColor(Formatting.GOLD)))
        .append(new LiteralText("以确认此次操作")),
    invalidConfirmationCode = red("[Fibersync] 无效的确认码"),
    opCancelled = new LiteralText("[Fibersync] 操作已取消"),
    nothingToConfirm = red("[Fibersync] 无待确认的操作"),
    nothingToCancel = red("[Fibersync] 无待取消的操作"),
    otherTaskRunning = red("[Fibersync] 有其他任务正在运行"),
    creatingBackup = new LiteralText("[Fibersync] $1: 正在创建备份")
        .append(new LiteralText("$2").setStyle(new Style().setColor(Formatting.GOLD))),
    backupComplete = new LiteralText("[Fibersync] $1: ")
        .append(new LiteralText("$2").setStyle(new Style().setColor(Formatting.GOLD)))
        .append(new LiteralText("已备份完成").setStyle(new Style().setColor(Formatting.RESET))),
    backupFailed = red("[Fibersync] $1: 备份失败：$2"),
    overwriteAlert = new LiteralText("[Fibersync] 此操作将会覆盖存档")
        .append(new LiteralText("$1").setStyle(new Style().setColor(Formatting.GOLD))),
    backupListTitle = new LiteralText("存档列表：").setStyle(new Style().setColor(Formatting.BLUE)),
    backupListItem = empty().append(new LiteralText("-[$1]").setStyle(new Style().setColor(Formatting.GREEN)
            .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("点击回档至该存档").setStyle(new Style().setColor(Formatting.GRAY).setItalic(true))))
            .setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/fs back $1"))))
        .append(new LiteralText(" 时间：").setStyle(new Style().setColor(Formatting.WHITE)))
        .append(new LiteralText("$3").setStyle(new Style().setColor(Formatting.GREEN)))
        .append(new LiteralText(" 描述：").setStyle(new Style().setColor(Formatting.WHITE)))
        .append(new LiteralText("$2").setStyle(new Style().setColor(Formatting.GREEN))),
    lockedBackupListItem = empty().append(new LiteralText("-[$1]").setStyle(new Style().setColor(Formatting.RED)
            .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("（此存档已锁定）点击回档至该存档").setStyle(new Style().setColor(Formatting.GRAY).setItalic(true))))
            .setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/fs back $1"))))
        .append(new LiteralText(" 时间：").setStyle(new Style().setColor(Formatting.WHITE)))
        .append(new LiteralText("$3").setStyle(new Style().setColor(Formatting.GREEN)))
        .append(new LiteralText(" 描述：").setStyle(new Style().setColor(Formatting.WHITE)))
        .append(new LiteralText("$2").setStyle(new Style().setColor(Formatting.GREEN))),
    backupListFooter = empty().append(new LiteralText("总大小：").setStyle(new Style().setColor(Formatting.BLUE)))
        .append(new LiteralText("$1M").setStyle(new Style().setColor(Formatting.GOLD))),
    backupNotExist = red("[Fibersync] 该备份不存在"),
    rollbackAlert = new LiteralText("[Fibersync] 将要回档至")
        .append(new LiteralText("$1").setStyle(new Style().setColor(Formatting.GOLD))),
    rollbackConfirmedAlert = new LiteralText("[Fibersync] $1：将要回档至")
        .append(new LiteralText("$2").setStyle(new Style().setColor(Formatting.GOLD))),
    rollbackStarted = new LiteralText("[Fibersync] 正在回档"),
    rollbackFinished = new LiteralText("[Fibersync] 回档完成"),
    startRegionBarTitle = new LiteralText("生成出生点"),
    fileCopyBarTitle = new LiteralText("复制存档文件"),
    creatingBackupTitle = new LiteralText("创建存档")
        .append(new LiteralText("$1").setStyle(new Style().setColor(Formatting.GREEN))),
    failedToCopyLevelFiles = red("[Fibersync] 复制存档文件失败：$1"),
    countDownTitle = empty().append(new LiteralText("准备回档：").setStyle(new Style().setColor(Formatting.GREEN)))
        .append(new LiteralText("$1").setStyle(new Style().setBold(true))),
    rollbackAborted = new LiteralText("[Fibersync] $1：已取消回档"),
    nothingToAbort = red("[Fibersync] 无可取消的操作"),
    reloadedConfig = new LiteralText("[Fibersync] 已加载配置"),
    failedToLoadConfig = red("[Fibersync] 加载配置失败：$1"),
    backupCountFull = red("[Fibersync] 已超出最大备份数"),
    deletedBackup = new LiteralText("[Fibersync] $1：已删除备份$2"),
    failedToDeletedBackup = red("[Fibersync] $1：删除备份$2失败：$3"),
    backupLocked = red("[Fibersync] 删除失败：此存档已锁定"),
    overwriteFailedLocked = red("[Fibersync] 此存档已存在且已锁定"),
    allBackupsLocked = red("[Fibersync] 存档数已满且找不到可覆盖的存档"),
    lockedBackup = new LiteralText("[Fibersync] $1：已锁定存档$2"),
    unlockedBackup = new LiteralText("[Fibersync] $1：已解锁存档$2"),
    failedToWriteInfo = red("[Fibersync] $1：存档信息写入失败：$2"),
    deletingBackup = new LiteralText("[Fibersync] $1：正在删除存档$2"),
    deletingBackupTitle = new LiteralText("删除存档")
        .append(new LiteralText("$1").setStyle(new Style().setColor(Formatting.GREEN))),
    failedToRetrieveList = red("[Fibersync] 获取存档列表时出错：$1"),
    failedToCopyFromTempDir = red("[Fibersync] 从临时目录复制文件时出错：$1"),
    copiedFromTempDir = new LiteralText("[Fibersync] 文件复制完成");
}