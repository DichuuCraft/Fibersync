package com.hadroncfy.fibersync.command.task;

import com.hadroncfy.fibersync.backup.BackupEntry;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static com.hadroncfy.fibersync.FibersyncMod.getFormat;

public class SyncTask extends BackTask {

    public SyncTask(ServerCommandSource src, BackupEntry selected) {
        super(src, selected);
    }
    
    @Override
    protected Text getFailedText() {
        return getFormat().syncFailed;
    }

    @Override
    protected Text getCountDownTitleText() {
        return getFormat().syncCountDownTitle;
    }

    @Override
    protected Text getStartedText() {
        return getFormat().syncStarted;
    }

    @Override
    protected Text getFinishedText() {
        return getFormat().syncFinished;
    }

    @Override
    protected Text getStartAlertText() {
        return getFormat().syncConfirmedAlert;
    }
}