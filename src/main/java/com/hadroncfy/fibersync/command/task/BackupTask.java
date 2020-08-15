package com.hadroncfy.fibersync.command.task;

import com.hadroncfy.fibersync.backup.BackupEntry;
import static com.hadroncfy.fibersync.FibersyncMod.getFormat;
import static com.hadroncfy.fibersync.config.TextRenderer.render;

import net.minecraft.server.command.ServerCommandSource;

public class BackupTask extends BaseTask {
    private final BackupEntry entry;
    private final BackupEntry overwrite;

    public BackupTask(ServerCommandSource src, BackupEntry entry, BackupEntry overwrite){
        super(src);
        this.entry = entry;
        this.overwrite = overwrite;
    }

    private void runCreateBackupTask(ServerCommandSource dummy){
        String senderName = src.getName();
        if (cctx.tryBeginTask(src)){
            try {
                if (overwrite != null && overwrite != entry){
                    entry.overwriteTo(overwrite);
                }
                doBackup(entry).thenRun(cctx::endTask).exceptionally(e -> {
                    cctx.endTask();
                    return null;
                });
            } catch(Exception e){
                e.printStackTrace();
                server.getPlayerManager().broadcastChatMessage(render(getFormat().backupFailed, senderName, e), false);
            } finally {
                cctx.endTask();
            }
        }
    }

    @Override
    public int run(){
        if (overwrite != null) {
            if (!overwrite.getInfo().locked){
                src.sendFeedback(render(getFormat().overwriteAlert, overwrite.getInfo().name), false);
                cctx.getConfirmationManager().submit(src.getName(), src, this::runCreateBackupTask);
            }
            else {
                src.sendError(getFormat().overwriteFailedLocked);
                return 0;
            }
        }
        else {
            runCreateBackupTask(src);
        }
        return 1;
    }
}