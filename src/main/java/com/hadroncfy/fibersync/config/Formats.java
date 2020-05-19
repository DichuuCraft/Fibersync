package com.hadroncfy.fibersync.config;

import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class Formats {
    private static Text red(String s){
        return new LiteralText(s).setStyle(new Style().setColor(Formatting.RED));
    }
    public Text confirmationHint = new LiteralText("[Fibersync] 使用")
        .append(new LiteralText("$1").setStyle(new Style().setColor(Formatting.GOLD)))
        .append(new LiteralText("以确认此次操作")),
    invalidConfirmationCode = red("无效的确认码"),
    opCancelled = new LiteralText("操作已取消"),
    nothingToConfirm = red("[Fibersync] 无待确认的操作"),
    nothingToCancel = red("[Fibersync] 无待取消的操作"),
    otherTaskRunning = red("[Fibersync] 有其他任务正在运行"),
    creatingBackup = new LiteralText("[Fibersync] $1: 正在创建备份")
        .append(new LiteralText("$2").setStyle(new Style().setColor(Formatting.GOLD))),
    backupComplete = new LiteralText("[Fibersync] $1: ")
        .append(new LiteralText("$2").setStyle(new Style().setColor(Formatting.GOLD)))
        .append(new LiteralText("已备份完成").setStyle(new Style().setColor(Formatting.RESET))),
    backupFailed = red("[Fibersync] $1: 备份失败：$2"),
    overwriteAlert = new LiteralText("[Fibersync] 备份")
        .append(new LiteralText("$1").setStyle(new Style().setColor(Formatting.GOLD)))
        .append(new LiteralText("已存在，该操作将会覆盖这个备份")),
    backupListItem = new LiteralText("- 名称：$1  描述：$2，创建时间：$3").setStyle(new Style().setBold(true)),
    backupNotExist = red("[Fibersync] 该备份不存在"),
    rollbackAlert = new LiteralText("[Fibersync] 将要回档至")
        .append(new LiteralText("$1").setStyle(new Style().setColor(Formatting.GOLD))),
    rollbackStarted = new LiteralText("[Fibersync] 正在回档"),
    rollbackFinished = new LiteralText("[Fibersync] 回档完成"),
    startRegionBarTitle = new LiteralText("出生点生成进度");
}