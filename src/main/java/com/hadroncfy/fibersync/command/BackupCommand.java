package com.hadroncfy.fibersync.command;

import static net.minecraft.server.command.CommandManager.literal;

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

import static net.minecraft.server.command.CommandManager.argument;

import com.hadroncfy.fibersync.FibersyncMod;
import com.hadroncfy.fibersync.Mode;
import com.hadroncfy.fibersync.backup.BackupEntry;
import com.hadroncfy.fibersync.backup.BackupFactory;
import com.hadroncfy.fibersync.backup.BackupInfo;
import com.hadroncfy.fibersync.interfaces.IServer;
import com.hadroncfy.fibersync.mixin.LevelStorageAccessor;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.command.arguments.MessageArgumentType;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandSource.suggestMatching;

import static com.hadroncfy.fibersync.config.TextRenderer.render;

import static com.hadroncfy.fibersync.FibersyncMod.getFormat;
import static com.hadroncfy.fibersync.FibersyncMod.getConfig;

public class BackupCommand {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final String NAME = "fs";

    public static void register(CommandDispatcher<ServerCommandSource> cd) {
        final LiteralArgumentBuilder<ServerCommandSource> b = literal(NAME)
                .then(literal("list").executes(BackupCommand::list))
                .then(literal("create")
                    .requires(BackupCommand::isBackupMode)
                    .then(argument("name", StringArgumentType.word()).suggests(BackupCommand::suggestUnlockedBackups)
                        .then(argument("description", MessageArgumentType.message()).executes(BackupCommand::create))))
                .then(literal("make")
                    .requires(BackupCommand::isBackupMode)
                    .executes(BackupCommand::create)
                        .then(argument("description", MessageArgumentType.message()).executes(BackupCommand::create)))
                .then(literal("back")
                    .requires(BackupCommand::isBackupMode)
                    .then(argument("name", StringArgumentType.word())
                        .suggests(BackupCommand::suggestBackups)
                        .executes(BackupCommand::back)))
                .then(literal("sync")
                    .requires(BackupCommand::isMirrorMode)
                    .then(argument("name", StringArgumentType.word())
                        .suggests(BackupCommand::suggestBackups)
                        .executes(BackupCommand::sync)))
                .then(literal("confirm")
                    .then(argument("code", IntegerArgumentType.integer()).executes(BackupCommand::confirm)))
                .then(literal("cancel").executes(BackupCommand::cancel))
                .then(literal("reload").executes(BackupCommand::reload))
                .then(literal("delete")
                    .requires(BackupCommand::isBackupMode)
                    .then(argument("name", StringArgumentType.word())
                        .suggests(BackupCommand::suggestUnlockedBackups).executes(BackupCommand::delete)))
                .then(literal("lock")
                    .requires(src -> isBackupMode(src) && canLock(src))
                        .then(argument("name", StringArgumentType.word())
                        .suggests(BackupCommand::suggestUnlockedBackups).executes(ctx -> setLocked(ctx, true))))
                .then(literal("unlock")
                    .requires(src -> isBackupMode(src) && canLock(src))
                        .then(argument("name", StringArgumentType.word())
                        .suggests(BackupCommand::suggestLockedBackups).executes(ctx -> setLocked(ctx, false))));
        cd.register(b);
    }

    private static boolean isBackupMode(ServerCommandSource src){
        return getConfig().mode == Mode.BACKUP;
    }

    private static boolean isMirrorMode(ServerCommandSource src){
        return getConfig().mode == Mode.MIRROR;
    }

    private static CompletableFuture<Suggestions> suggestBackups(final CommandContext<ServerCommandSource> context,
            final SuggestionsBuilder builder) {
        final BackupFactory bf = ((IServer) context.getSource().getMinecraftServer()).getContext().getBackupFactory();
        return suggestMatching(bf.getBackups(context.getSource().getMinecraftServer().getLevelName()).stream()
                .map(e -> e.getInfo().name), builder);
    }

    private static CompletableFuture<Suggestions> suggestLockedBackups(
            final CommandContext<ServerCommandSource> context, final SuggestionsBuilder builder) {
        final BackupFactory bf = ((IServer) context.getSource().getMinecraftServer()).getContext().getBackupFactory();
        return suggestMatching(bf.getBackups(context.getSource().getMinecraftServer().getLevelName()).stream()
                .filter(p -> p.getInfo().locked).map(e -> e.getInfo().name), builder);
    }

    private static CompletableFuture<Suggestions> suggestUnlockedBackups(
            final CommandContext<ServerCommandSource> context, final SuggestionsBuilder builder) {
        final BackupFactory bf = ((IServer) context.getSource().getMinecraftServer()).getContext().getBackupFactory();
        return suggestMatching(bf.getBackups(context.getSource().getMinecraftServer().getLevelName()).stream()
                .filter(p -> !p.getInfo().locked).map(e -> e.getInfo().name), builder);
    }

    private static boolean canLock(ServerCommandSource src) {
        return src.hasPermissionLevel(1);
    }

    private static int reload(CommandContext<ServerCommandSource> ctx) {
        final ServerCommandSource src = ctx.getSource();
        try {
            FibersyncMod.loadConfig();
            src.sendFeedback(getFormat().reloadedConfig, true);
            return 0;
        } catch (Throwable e) {
            src.sendError(render(getFormat().failedToLoadConfig, e.toString()));
            return 1;
        }
    }

    private static int confirm(CommandContext<ServerCommandSource> ctx) {
        int code = IntegerArgumentType.getInteger(ctx, "code");
        final ConfirmationManager cm = ((IServer) ctx.getSource().getMinecraftServer()).getContext()
                .getConfirmationManager();
        if (!cm.confirm(ctx.getSource().getName(), code)) {
            ctx.getSource().sendError(getFormat().nothingToConfirm);
        }
        return 0;
    }

    private static int cancel(CommandContext<ServerCommandSource> ctx) {
        final ServerCommandSource src = ctx.getSource();
        final BackupCommandContext cctx = ((IServer) ctx.getSource().getMinecraftServer()).getContext();
        if (cctx.countDownTask != null) {
            cctx.countDownTask.cancel();
            src.getMinecraftServer().getPlayerManager()
                    .broadcastChatMessage(render(getFormat().rollbackAborted, src.getName()), false);
            return 0;
        } else {
            if (!cctx.getConfirmationManager().cancel(ctx.getSource().getName())) {
                src.sendError(getFormat().nothingToCancel);
                return 1;
            }
            return 0;
        }
    }

    private static UUID getSourceUUID(CommandContext<ServerCommandSource> ctx) {
        try {
            return ctx.getSource().getPlayer().getUuid();
        } catch (CommandSyntaxException e) {
            //
            return BackupInfo.CONSOLE_UUID;
        }
    }

    private static boolean getAutosave(MinecraftServer server) {
        for (ServerWorld sw : server.getWorlds()) {
            return !sw.savingDisabled;
        }
        return true;
    }

    private static void setAutosave(MinecraftServer server, boolean a) {
        for (ServerWorld w : server.getWorlds()) {
            w.savingDisabled = !a;
        }
    }

    public static Path getWorldDir(MinecraftServer server) {
        return ((LevelStorageAccessor) server.getLevelStorage()).getSavesDir().resolve(server.getLevelName());
    }

    private static synchronized int delete(CommandContext<ServerCommandSource> ctx) {
        final String name = StringArgumentType.getString(ctx, "name");
        final ServerCommandSource src = ctx.getSource();
        final MinecraftServer server = src.getMinecraftServer();
        final BackupCommandContext cctx = ((IServer) server).getContext();
        final BackupEntry b = cctx.getBackupFactory().getEntry(server.getLevelName(), name);
        if (b == null || !b.exists()) {
            src.sendError(getFormat().backupNotExist);
            return 1;
        }
        if (b.getInfo().locked) {
            src.sendError(getFormat().backupLocked);
            return 1;
        }
        cctx.getConfirmationManager().submit(src.getName(), src, s -> {
            if (cctx.tryBeginTask(src)) {
                final FileOperationProgressBar progressBar = new FileOperationProgressBar(server, render(getFormat().deletingBackupTitle, b.getInfo().name));
                try {
                    server.getPlayerManager().broadcastChatMessage(
                            render(getFormat().deletingBackup, src.getName(), b.getInfo().name), false);
                    b.delete(progressBar);
                    server.getPlayerManager().broadcastChatMessage(
                            render(getFormat().deletedBackup, src.getName(), b.getInfo().name), false);
                } catch (Throwable e) {
                    e.printStackTrace();
                    server.getPlayerManager().broadcastChatMessage(
                            render(getFormat().failedToDeletedBackup, src.getName(), b.getInfo().name, e.toString()),
                            false);
                } finally {
                    progressBar.done();
                    cctx.endTask();
                }
            }
        });
        return 0;
    }

    private static synchronized int setLocked(CommandContext<ServerCommandSource> ctx, boolean locked) {
        final ServerCommandSource src = ctx.getSource();
        final MinecraftServer server = src.getMinecraftServer();
        final BackupCommandContext cctx = ((IServer) server).getContext();
        final BackupEntry entry = cctx.getBackupFactory().getEntry(server.getLevelName(),
                StringArgumentType.getString(ctx, "name"));
        if (entry == null) {
            src.sendError(getFormat().backupNotExist);
            return 1;
        }
        if (cctx.tryBeginTask(src)) {
            try {
                entry.getInfo().locked = locked;
                entry.writeInfo();
                server.getPlayerManager()
                        .broadcastChatMessage(render(locked ? getFormat().lockedBackup : getFormat().unlockedBackup,
                                src.getName(), entry.getInfo().name), false);
            } catch (Throwable e) {
                e.printStackTrace();
                server.getPlayerManager().broadcastChatMessage(
                        render(getFormat().failedToWriteInfo, src.getName(), e.toString()), false);
            } finally {
                cctx.endTask();
            }
        }
        return 0;
    }

    private static CompletableFuture<Void> doBackup(MinecraftServer server, ServerCommandSource src,
            BackupEntry entry) {
        final boolean autosave = getAutosave(server);
        setAutosave(server, false);
        server.save(false, true, true);
        server.getPlayerManager().saveAllPlayerData();
        return CompletableFuture.runAsync(() -> {
            final FileOperationProgressBar progressBar = new FileOperationProgressBar(server, render(getFormat().creatingBackupTitle, entry.getInfo().name));
            try {
                Path worldDir = getWorldDir(server);
                LOGGER.info("world dir: " + worldDir.toString());

                entry.doBackup(worldDir, progressBar);
            } catch (Throwable e) {
                e.printStackTrace();
                progressBar.done();
                throw new CompletionException(e);
            } finally {
                setAutosave(server, autosave);
            }
        });
    }

    private static void doCopy(MinecraftServer server, BackupEntry entry, BackupEntry other)
            throws NoSuchAlgorithmException, IOException {
        final FileOperationProgressBar progressBar = new FileOperationProgressBar(server, getFormat().fileCopyBarTitle);
        try {
            entry.copyTo(other, progressBar);
        }
        finally {
            progressBar.done();
        }
    }

    private static String getEmptyBackupName(String prefix, List<BackupEntry> entries){
        String name;
        int i = 1;
        out:
        while (true){
            name = prefix + i++;
            for (BackupEntry e: entries){
                if (e.getInfo().name.equals(name)){
                    continue out;
                }
            }
            return name;
        }
    }

    private static BackupEntry getOldestUnlockedEntry(List<BackupEntry> entries){
        Collections.sort(entries);
        for (int i = entries.size() - 1; i >= 0; i--){
            BackupEntry entry = entries.get(i);
            if (!entry.getInfo().locked){
                return entry;
            }
        }
        return null;
    }

    private static synchronized int create(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        // final String name = StringArgumentType.getString(ctx, "name"),
        //         description = MessageArgumentType.getMessage(ctx, "description").asString();
        
        final ServerCommandSource src = ctx.getSource();
        final MinecraftServer server = src.getMinecraftServer();
        final BackupCommandContext cctx = ((IServer)server).getContext();
        final List<BackupEntry> entries = cctx.getBackupFactory().getBackups(server.getLevelName());
        final int maxBackups = getConfig().maxBackupCount;
        
        final String description = tryGetArg(() -> MessageArgumentType.getMessage(ctx, "description").asString(), () -> "");
        BackupEntry b2 = null;

        final String name = tryGetArg(() -> StringArgumentType.getString(ctx, "name"), () -> getEmptyBackupName("b", entries));
        final BackupEntry selected = cctx.getBackupFactory().create(server.getLevelName(), name, description, getSourceUUID(ctx));
        if (selected.exists()){
            b2 = selected;
        }
        else if (maxBackups != -1 && BackupFactory.getBackupCount(entries) >= maxBackups){
            b2 = getOldestUnlockedEntry(entries);
            if (b2 == null){
                src.sendError(getFormat().allBackupsLocked);
                return 1;
            }
        }
        

        final BackupEntry overwrite = b2;
        
        final String senderName = src.getName();
        final Runnable btask = () -> {
            if (cctx.tryBeginTask(src)){
                server.getPlayerManager().broadcastChatMessage(render(getFormat().creatingBackup, senderName, name), false);
                if (overwrite != null && overwrite != selected){
                    selected.overwriteTo(overwrite);
                }
                doBackup(server, src, selected).thenRun(() -> {
                    server.getPlayerManager().broadcastChatMessage(render(getFormat().backupComplete, senderName, name), false);
                    cctx.endTask();
                }).exceptionally(e -> {
                    server.getPlayerManager().broadcastChatMessage(render(getFormat().backupFailed, senderName, e.toString()), false);
                    cctx.endTask();
                    return null;
                });
            }
        };
        if (overwrite != null) {
            if (!overwrite.getInfo().locked){
                src.sendFeedback(render(getFormat().overwriteAlert, overwrite.getInfo().name), false);
                cctx.getConfirmationManager().submit(src.getName(), src, s -> {
                    btask.run();
                });
            }
            else {
                src.sendError(getFormat().overwriteFailedLocked);
                return 0;
            }
        }
        else {
            btask.run();
        }
        return 1;
    }

    private static int sync(CommandContext<ServerCommandSource> ctx){
        final ServerCommandSource src = ctx.getSource();
        final MinecraftServer server = src.getMinecraftServer();
        final BackupCommandContext cctx = ((IServer)server).getContext();
        final BackupEntry entry = cctx.getBackupFactory().getEntry(server.getLevelName(), StringArgumentType.getString(ctx, "name"));
        if (entry == null || !entry.exists()){
            src.sendError(getFormat().backupNotExist);
            return 0;
        }

        cctx.getConfirmationManager().submit(src.getName(), src, s -> {
            if (cctx.tryBeginTask(src)){
                server.getPlayerManager().broadcastChatMessage(render(getFormat().syncConfirmedAlert, src.getName(), entry.getInfo().name), true);

                cctx.countDownTask = new CountDownTask(getConfig().defaultCountDown);
                cctx.countDownTask.run(i -> {
                    Text title = render(getFormat().syncCountDownTitle, i.toString());
                    server.getPlayerManager().sendToAll(new TitleS2CPacket(TitleS2CPacket.Action.ACTIONBAR, title, 10, 10, -1));
                }).thenAccept(b -> {
                    cctx.countDownTask = null;
                    if (b){
                        server.getPlayerManager().sendToAll(getFormat().syncStarted);
                        ((IServer) server).reloadAll(entry, () -> {
                            server.getPlayerManager().sendToAll(getFormat().syncComplete);
                            cctx.endTask();
                        });
                    }
                });
            }
        });
        return 1;
    }

    private static synchronized int back(CommandContext<ServerCommandSource> ctx){
        final ServerCommandSource src = ctx.getSource();
        final MinecraftServer server = src.getMinecraftServer();
        final BackupCommandContext cctx = ((IServer)server).getContext();
        final BackupEntry entry = cctx.getBackupFactory().getEntry(server.getLevelName(), StringArgumentType.getString(ctx, "name"));
        if (entry == null || !entry.exists()){
            src.sendError(getFormat().backupNotExist);
            return 0;
        }

        final BackupEntry currentWorld = cctx.getBackupFactory().create(server.getLevelName(), getConfig().oldWorldName, getConfig().oldWorldDescription, getSourceUUID(ctx));
        currentWorld.getInfo().locked = true;
        currentWorld.getInfo().isOldWorld = true;

        cctx.getConfirmationManager().submit(src.getName(), src, (s) -> {
            if (cctx.tryBeginTask(src)){
                server.getPlayerManager().broadcastChatMessage(render(getFormat().rollbackConfirmedAlert, src.getName(), entry.getInfo().name), true);
                
                server.getPlayerManager().broadcastChatMessage(render(getFormat().creatingBackup, src.getName(), currentWorld.getInfo().name), false);
                BackupEntry temp = currentWorld;
                if (entry.collides(currentWorld)){
                    LOGGER.info("Backup to temp dir since we are rolling back to oldworld");
                    temp = temp.createAtNewDir(getConfig().tempDir);
                }
                final BackupEntry autoBackup = temp;
                doBackup(server, src, autoBackup).thenRun(() -> {
                    server.getPlayerManager().sendToAll(render(getFormat().backupComplete, src.getName(), currentWorld.getInfo().name));
                    
                    cctx.countDownTask = new CountDownTask(getConfig().defaultCountDown);
                    cctx.countDownTask.run(i -> {
                        Text txt = render(getFormat().countDownTitle, Integer.toString(i));
                        server.getPlayerManager().sendToAll(new TitleS2CPacket(TitleS2CPacket.Action.ACTIONBAR, txt, 10, 10, -1));
                    }).thenAccept(b -> {
                        cctx.countDownTask = null;
                        if (b){
                            server.getPlayerManager().broadcastChatMessage(getFormat().rollbackStarted, true);
                            ((IServer) server).reloadAll(entry, () -> {
                                server.getPlayerManager().broadcastChatMessage(getFormat().rollbackFinished, true);
                                if (autoBackup != currentWorld){
                                    LOGGER.info("Copying file back from temp dir");
                                    CompletableFuture.runAsync(() -> {
                                        try {
                                            doCopy(server, autoBackup, currentWorld);
                                            server.getPlayerManager().broadcastChatMessage(getFormat().copiedFromTempDir, false);
                                        } catch (Throwable e1) {
                                            e1.printStackTrace();
                                            server.getPlayerManager().broadcastChatMessage(render(getFormat().failedToCopyFromTempDir, e1.toString()), false);
                                        }
                                        finally {
                                            cctx.endTask();
                                        }
                                    });
                                }
                                else {
                                    cctx.endTask();
                                }
                            });
                        }
                        else {
                            cctx.endTask();
                        }
                    });
                }).exceptionally(e -> {
                    server.getPlayerManager().broadcastChatMessage(render(getFormat().backupFailed, src.getName(), e.toString()), false);
                    cctx.endTask();
                    return null;
                });
            }
        });
        return 1;
    }

    private static int list(CommandContext<ServerCommandSource> ctx){
        final ServerCommandSource src = ctx.getSource();
        final BackupCommandContext cctx = ((IServer)src.getMinecraftServer()).getContext();
        final List<BackupEntry> entries = cctx.getBackupFactory().getBackups(src.getMinecraftServer().getLevelName());
        Collections.sort(entries);
        CompletableFuture.runAsync(() -> {
            try {
                long totalSize = 0;
                for (BackupEntry entry: entries){
                    totalSize += entry.totalSize();
                }

                src.sendFeedback(getFormat().backupListTitle, false);
                for (BackupEntry entry: entries){
                    src.sendFeedback(render(
                        entry.getInfo().locked ? getFormat().lockedBackupListItem : getFormat().backupListItem,
                        entry.getInfo().name,
                        entry.getInfo().description,
                        getConfig().dateFormat.format(entry.getInfo().date)
                    ), false);
                }
                src.sendFeedback(render(getFormat().backupListFooter, String.format("%.2f", (float)totalSize / 1024 / 1024)), false);
            }
            catch(Throwable e){
                src.sendError(render(getFormat().failedToRetrieveList, e.toString()));
                e.printStackTrace();
            }
        });
        return 1;
    }

    @FunctionalInterface
    interface SupplierWithSyntaxException<T> {
        T get() throws IllegalArgumentException, CommandSyntaxException;
    }

    private static <T> T tryGetArg(SupplierWithSyntaxException<T> arg, Supplier<T> def){
        try {
            return arg.get();
        }
        catch(IllegalArgumentException | CommandSyntaxException e){
            return def.get();
        }
    }
}