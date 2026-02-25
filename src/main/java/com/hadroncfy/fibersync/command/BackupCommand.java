package com.hadroncfy.fibersync.command;

import static net.minecraft.server.command.CommandManager.literal;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static net.minecraft.server.command.CommandManager.argument;

import com.hadroncfy.fibersync.FibersyncMod;
import com.hadroncfy.fibersync.backup.BackupEntry;
import com.hadroncfy.fibersync.backup.BackupExcluder;
import com.hadroncfy.fibersync.backup.BackupFactory;
import com.hadroncfy.fibersync.backup.BackupInfo;
import com.hadroncfy.fibersync.command.task.BackTask;
import com.hadroncfy.fibersync.command.task.BackupTask;
import com.hadroncfy.fibersync.command.task.SyncTask;
import com.hadroncfy.fibersync.interfaces.IServer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.command.argument.MessageArgumentType;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionCheck;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.command.permission.PermissionSourcePredicate;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;

import static net.minecraft.command.CommandSource.suggestMatching;

import static com.hadroncfy.fibersync.config.TextRenderer.render;

import static com.hadroncfy.fibersync.FibersyncMod.getFormat;
import static com.hadroncfy.fibersync.FibersyncMod.getConfig;

public class BackupCommand {
    private static final String ARG_DESC = "description";
    private static final String ARG_NAME = "name";
    private static final PermissionSourcePredicate<ServerCommandSource> LOCK_PERMISSION =
        net.minecraft.server.command.CommandManager.requirePermissionLevel(
            new PermissionCheck.Require(new Permission.Level(PermissionLevel.fromLevel(1))));

    public static void register(CommandDispatcher<ServerCommandSource> cd) {
        String primary = getCommandName();
        registerRoot(cd, primary);
        if (!"fs".equals(primary)) {
            registerRoot(cd, "fs");
        }
    }

    private static void registerRoot(CommandDispatcher<ServerCommandSource> cd, String name) {
        final LiteralArgumentBuilder<ServerCommandSource> b = literal(name)
                .then(literal("list").executes(src -> list(src, false))
                    .then(literal("sync")
                        .requires(BackupCommand::isMirrorMode)
                        .executes(src -> list(src, true))))
                .then(literal("create")
                    .then(argument(ARG_NAME, StringArgumentType.word()).suggests(BackupCommand::suggestUnlockedBackups)
                        .then(argument(ARG_DESC, MessageArgumentType.message()).executes(BackupCommand::create))))
                .then(literal("make")
                    .executes(BackupCommand::create)
                        .then(argument(ARG_DESC, MessageArgumentType.message()).executes(BackupCommand::create)))
                .then(literal("back").executes(BackupCommand::back)
                    .then(argument(ARG_NAME, StringArgumentType.word())
                        .suggests(BackupCommand::suggestBackups)
                        .executes(BackupCommand::back)))
                .then(literal("sync")
                    .requires(BackupCommand::isMirrorMode)
                    .then(argument(ARG_NAME, StringArgumentType.word())
                        .suggests(BackupCommand::suggestMirrors)
                        .executes(BackupCommand::sync)
                            .then(literal("only")
                                .then(argument("only", StringArgumentType.word())
                                .suggests(BackupCommand::suggestDimOnlys)
                                .executes(BackupCommand::sync)))))
                .then(literal("confirm")
                    .then(argument("code", IntegerArgumentType.integer()).executes(BackupCommand::confirm)))
                .then(literal("cancel").executes(BackupCommand::cancel))
                .then(literal("showhand")
                    .executes(ctx -> setShowhand(ctx, true))
                        .then(literal("stop")
                            .executes(ctx -> setShowhand(ctx, false))))
                .then(literal("reload").executes(BackupCommand::reload))
                .then(literal("delete")
                    .then(argument(ARG_NAME, StringArgumentType.word())
                        .suggests(BackupCommand::suggestUnlockedBackups).executes(BackupCommand::delete)))
                .then(literal("lock")
                    .requires(BackupCommand::canLock)
                        .then(argument(ARG_NAME, StringArgumentType.word())
                        .suggests(BackupCommand::suggestUnlockedBackups).executes(ctx -> setLocked(ctx, true))))
                .then(literal("unlock")
                    .requires(BackupCommand::canLock)
                        .then(argument(ARG_NAME, StringArgumentType.word())
                        .suggests(BackupCommand::suggestLockedBackups).executes(ctx -> setLocked(ctx, false))));
        cd.register(b);
    }

    public static String getCommandName() {
        String name = getConfig().command;
        if (name == null) {
            return "fs";
        }
        name = name.trim();
        if (name.isEmpty() || name.contains(" ")) {
            return "fs";
        }
        return name;
    }

    private static boolean isMirrorMode(ServerCommandSource src){
        return getConfig().syncDir != null;
    }

    private static CompletableFuture<Suggestions> suggestBackups(final CommandContext<ServerCommandSource> context,
            final SuggestionsBuilder builder) {
        final BackupFactory bf = ((IServer) context.getSource().getServer()).getBackupCommandContext(null).getBackupFactory();
        return suggestMatching(bf.getBackups().stream()
                .map(e -> e.getInfo().name), builder);
    }

    private static CompletableFuture<Suggestions> suggestMirrors(final CommandContext<ServerCommandSource> context,
            final SuggestionsBuilder builder) {
        final BackupFactory bf = ((IServer) context.getSource().getServer()).getBackupCommandContext(null).getMirrorFactory();
        return suggestMatching(bf.getBackups().stream()
                .map(e -> e.getInfo().name), builder);
    }

    private static CompletableFuture<Suggestions> suggestLockedBackups(
            final CommandContext<ServerCommandSource> context, final SuggestionsBuilder builder) {
        final BackupFactory bf = ((IServer) context.getSource().getServer()).getBackupCommandContext(null).getBackupFactory();
        return suggestMatching(bf.getBackups().stream()
                .filter(p -> p.getInfo().locked).map(e -> e.getInfo().name), builder);
    }

    private static CompletableFuture<Suggestions> suggestUnlockedBackups(
            final CommandContext<ServerCommandSource> context, final SuggestionsBuilder builder) {
        final BackupFactory bf = ((IServer) context.getSource().getServer()).getBackupCommandContext(null).getBackupFactory();
        return suggestMatching(bf.getBackups().stream()
                .filter(p -> !p.getInfo().locked).map(e -> e.getInfo().name), builder);
    }

    private static CompletableFuture<Suggestions> suggestDimOnlys(final CommandContext<ServerCommandSource> context,
            SuggestionsBuilder builder) throws CommandSyntaxException {
        String input = builder.getRemaining();
        DimensionListArgParser parser = new DimensionListArgParser();
        parser.parse(input);

        for (String w: parser.getSuggestions()){
            builder.suggest(input + w);
        }

        return builder.buildFuture();
    }

    private static boolean canLock(ServerCommandSource src) {
        // Allow all users to lock/unlock backups.
        return true;
    }

    private static int reload(CommandContext<ServerCommandSource> ctx) {
        final ServerCommandSource src = ctx.getSource();
        try {
            FibersyncMod.loadConfig();
            src.sendFeedback(() -> getFormat().reloadedConfig, true);
            return 0;
        } catch (Exception e) {
            src.sendError(render(getFormat().failedToLoadConfig, e.toString()));
            return 1;
        }
    }

    private static int setShowhand(CommandContext<ServerCommandSource> ctx, boolean enabled) {
        final ServerCommandSource src = ctx.getSource();
        getConfig().showhand = enabled;
        try {
            FibersyncMod.saveConfig();
            src.sendFeedback(() -> enabled ? getFormat().showhandEnabled : getFormat().showhandDisabled, true);
            return 0;
        } catch (Exception e) {
            src.sendError(render(getFormat().failedToSaveConfig, e.toString()));
            return 1;
        }
    }

    private static int confirm(CommandContext<ServerCommandSource> ctx) {
        int code = IntegerArgumentType.getInteger(ctx, "code");
        final ConfirmationManager cm = ((IServer) ctx.getSource().getServer()).getBackupCommandContext(null)
                .getConfirmationManager();
        if (!cm.confirm(ctx.getSource().getName(), code)) {
            ctx.getSource().sendError(getFormat().nothingToConfirm);
        }
        return 0;
    }

    private static int cancel(CommandContext<ServerCommandSource> ctx) {
        final ServerCommandSource src = ctx.getSource();
        final BackupCommandContext cctx = ((IServer) ctx.getSource().getServer()).getBackupCommandContext(null);
        if (cctx.hasCountDownTask()) {
            cctx.cancelCountDownTask();
            src.getServer().getPlayerManager()
                    .broadcast(render(getFormat().rollbackAborted, src.getName()), false);
            return 0;
        } else {
            if (!cctx.getConfirmationManager().cancel(ctx.getSource().getName())) {
                src.sendError(getFormat().nothingToCancel);
                return 1;
            }
            return 0;
        }
    }

    public static UUID getSourceUUID(CommandContext<ServerCommandSource> ctx) {
        try {
            return ctx.getSource().getPlayer().getUuid();
        } catch (Exception e) {
            //
            return BackupInfo.CONSOLE_UUID;
        }
    }

    private static int delete(CommandContext<ServerCommandSource> ctx) {
        final String name = StringArgumentType.getString(ctx, ARG_NAME);
        final ServerCommandSource src = ctx.getSource();
        final MinecraftServer server = src.getServer();
        final BackupCommandContext cctx = ((IServer) server).getBackupCommandContext(null);
        final BackupEntry b = cctx.getBackupFactory().getEntry(name);
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
                cctx.progress_bar.set(progressBar);
                try {
                    server.getPlayerManager().broadcast(
                        render(getFormat().deletingBackup, src.getName(), b.getInfo().name), false);
                    b.delete(progressBar);
                    server.getPlayerManager().broadcast(
                        render(getFormat().deletedBackup, src.getName(), b.getInfo().name), false);
                } catch (Exception e) {
                    e.printStackTrace();
                    server.getPlayerManager().broadcast(
                        render(getFormat().failedToDeletedBackup, src.getName(), b.getInfo().name, e.toString()),
                    false);
                } finally {
                    progressBar.done();
                    cctx.endTask();
                    cctx.progress_bar.set(null);
                }
            }
        });
        return 0;
    }

    private static int setLocked(CommandContext<ServerCommandSource> ctx, boolean locked) {
        final ServerCommandSource src = ctx.getSource();
        final MinecraftServer server = src.getServer();
        final BackupCommandContext cctx = ((IServer) server).getBackupCommandContext(null);
        final BackupEntry entry = cctx.getBackupFactory().getEntry(StringArgumentType.getString(ctx, ARG_NAME));
        if (entry == null) {
            src.sendError(getFormat().backupNotExist);
            return 1;
        }
        if (cctx.tryBeginTask(src)) {
            try {
                entry.getInfo().locked = locked;
                entry.writeInfo();
                server.getPlayerManager()
                        .broadcast(
                            render(locked ? getFormat().lockedBackup : getFormat().unlockedBackup, src.getName(), entry.getInfo().name),
                            false
                        );
            } catch (Exception e) {
                e.printStackTrace();
                server.getPlayerManager().broadcast(
                        render(getFormat().failedToWriteInfo, src.getName(), e.toString()), false);
            } finally {
                cctx.endTask();
            }
        }
        return 0;
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

    private static int create(CommandContext<ServerCommandSource> ctx) {
        final ServerCommandSource src = ctx.getSource();
        final MinecraftServer server = src.getServer();
        final BackupCommandContext cctx = ((IServer)server).getBackupCommandContext(null);
        final List<BackupEntry> entries = cctx.getBackupFactory().getBackups();
        final int maxBackups = getConfig().maxBackupCount;

        final String description = tryGetArg(() -> MessageArgumentType.getMessage(ctx, ARG_DESC).getString(), () -> "");
        BackupEntry overwrite = null;

        final String name = tryGetArg(() -> StringArgumentType.getString(ctx, ARG_NAME), () -> getEmptyBackupName("b", entries));
        final BackupEntry selected = cctx.getBackupFactory().create(name, description, getSourceUUID(ctx));
        if (selected.exists()){
            overwrite = selected;
        } else if (maxBackups != -1 && BackupFactory.getBackupCount(entries) >= maxBackups){
            overwrite = getOldestUnlockedEntry(entries);
            if (overwrite == null){
                src.sendError(getFormat().allBackupsLocked);
                return 1;
            }
        }

        return new BackupTask(src, selected, overwrite).run();
    }

    private static int sync(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        final ServerCommandSource src = ctx.getSource();
        final MinecraftServer server = src.getServer();
        final BackupCommandContext cctx = ((IServer)server).getBackupCommandContext(null);
        final BackupEntry entry = cctx.getMirrorFactory().getEntry(StringArgumentType.getString(ctx, ARG_NAME));
        if (entry == null || !entry.exists()){
            src.sendError(getFormat().backupNotExist);
            return 0;
        }
        int mask = BackupExcluder.MASK_NONE;
        try {
            DimensionListArgParser parser = new DimensionListArgParser();
            parser.parse(StringArgumentType.getString(ctx, "only"));
            parser.end();
            mask = parser.getMask();
        } catch(IllegalArgumentException e){
            // ignore
        }

        return new SyncTask(src, entry, mask).run();
    }

    private static int back(CommandContext<ServerCommandSource> ctx){
        final ServerCommandSource src = ctx.getSource();
        final MinecraftServer server = src.getServer();
        final BackupCommandContext cctx = ((IServer)server).getBackupCommandContext(null);
        final BackupEntry entry = cctx.getBackupFactory().getEntry(StringArgumentType.getString(ctx, ARG_NAME));
        if (entry == null || !entry.exists()){
            src.sendError(getFormat().backupNotExist);
            return 0;
        }

        return new BackTask(src, entry).run();
    }

    private static int list(CommandContext<ServerCommandSource> ctx, boolean isMirror){
        final ServerCommandSource src = ctx.getSource();
        final MinecraftServer server = src.getServer();
        final BackupCommandContext cctx = ((IServer)server).getBackupCommandContext(null);
        final List<BackupEntry> entries = (isMirror ? cctx.getMirrorFactory() : cctx.getBackupFactory()).getBackups();
        Collections.sort(entries);
        CompletableFuture.runAsync(() -> {
            try {
                long totalSize = 0;
                for (BackupEntry entry: entries){
                    totalSize += entry.totalSize();
                }
                final long totalSize2 = totalSize;
                server.execute(() -> {
                    src.sendFeedback(() -> isMirror ? getFormat().mirrorListTitle : getFormat().backupListTitle, false);
                    for (BackupEntry entry: entries){
                        src.sendFeedback(() -> render(
                            entry.getInfo().locked ? getFormat().lockedBackupListItem : getFormat().backupListItem,
                            entry.getInfo().name,
                            entry.getInfo().description,
                            getConfig().dateFormat.format(entry.getInfo().date)
                        ), false);
                    }
                    src.sendFeedback(() -> render(getFormat().backupListFooter, String.format("%.2f", (float)totalSize2 / 1024 / 1024)), false);
                });
            } catch(Exception e) {
                server.execute(() -> src.sendError(render(getFormat().failedToRetrieveList, e.toString())));
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
        } catch(IllegalArgumentException | CommandSyntaxException e){
            return def.get();
        }
    }
}
