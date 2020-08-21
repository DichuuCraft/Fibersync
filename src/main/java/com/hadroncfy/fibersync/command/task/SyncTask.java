package com.hadroncfy.fibersync.command.task;

import com.hadroncfy.fibersync.backup.BackupEntry;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static com.hadroncfy.fibersync.FibersyncMod.getFormat;

public class SyncTask extends BackTask {
    private final int mask;

    public SyncTask(ServerCommandSource src, BackupEntry selected, int mask) {
        super(src, selected);
        this.mask = mask;
    }

    @Override
    protected int getMask() {
        return mask;
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