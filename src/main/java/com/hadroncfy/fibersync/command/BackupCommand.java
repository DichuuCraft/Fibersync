package com.hadroncfy.fibersync.command;

import static net.minecraft.server.command.CommandManager.literal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static net.minecraft.server.command.CommandManager.argument;

import com.hadroncfy.fibersync.backup.BackupEntry;
import com.hadroncfy.fibersync.backup.BackupFactory;
import com.hadroncfy.fibersync.backup.BackupInfo;
import com.hadroncfy.fibersync.config.Config;
import com.hadroncfy.fibersync.config.Formats;
import com.hadroncfy.fibersync.interfaces.IServer;
import com.hadroncfy.fibersync.mixin.LevelStorageAccessor;
import com.hadroncfy.fibersync.restart.ServerRestarter;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.util.concurrent.Promise;
import net.minecraft.command.arguments.MessageArgumentType;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTask;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandSource.suggestMatching;

import static com.hadroncfy.fibersync.config.TextRenderer.render;

public class BackupCommand implements Executor, Supplier<Path> {
    private static final Logger LOGGER = LogManager.getLogger();
    private final BackupFactory bf;
    private final ConfirmationManager cm;
    private final Supplier<Config> cProvider;
    // private Thread task = null;
    // private volatile boolean hasTask = false;
    private final AtomicBoolean hasTask = new AtomicBoolean();
    private CountDownTask countDownTask;

    @Override
    public Path get() {
        return new File(cProvider.get().backupDir).toPath();
    }

    public BackupCommand(Supplier<Config> provider) {
        cProvider = provider;
        cm = new ConfirmationManager(provider, 20000, 1000);
        this.bf = new BackupFactory(this);
    }

    private Formats getFormat() {
        return cProvider.get().formats;
    }

    public void register(CommandDispatcher<ServerCommandSource> cd) {
        final LiteralArgumentBuilder<ServerCommandSource> b = literal("fs")
            .then(literal("list").executes(this::list))
            .then(literal("create")
                .then(argument("name", StringArgumentType.word())
                    .then(argument("description", MessageArgumentType.message()).executes(this::create))))
            .then(literal("back")
                .then(argument("name", StringArgumentType.word())
                    .suggests((src, sb) -> suggestMatching(bf.getBackups(src.getSource().getMinecraftServer().getLevelName()).stream().map(e -> e.getInfo().name), sb))
                    .executes(this::back)))
            .then(literal("confirm")
                .then(argument("code", IntegerArgumentType.integer()).executes(this::confirm)))
            .then(literal("cancel").executes(this::cancel))
            .then(literal("abort").executes(this::abort));
        cd.register(b);
    }

    private int abort(CommandContext<ServerCommandSource> ctx){
        final ServerCommandSource src = ctx.getSource();
        if (countDownTask != null){
            countDownTask.cancel();
            src.getMinecraftServer().getPlayerManager().broadcastChatMessage(render(getFormat().rollbackAborted, src.getName()), false);
            return 0;
        }
        else {
            src.sendError(getFormat().nothingToAbort);
            return 1;
        }
    }

    private int confirm(CommandContext<ServerCommandSource> ctx){
        int code = IntegerArgumentType.getInteger(ctx, "code");
        cm.confirm(ctx.getSource().getName(), code);
        return 1;
    }

    private int cancel(CommandContext<ServerCommandSource> ctx){
        cm.cancel(ctx.getSource().getName());
        return 1;
    }

    private boolean tryBeginTask(ServerCommandSource src){
        if (hasTask.compareAndSet(false, true)){
            return true;
        }
        else {
            src.sendError(getFormat().otherTaskRunning);
            return false;
        }
    }

    private void endTask(){
        hasTask.set(false);
    }

    // private void runExclusively(ServerCommandSource src, Runnable r) {
    //     if (hasTask.compareAndSet(expect, update)) {
    //         src.sendError(getFormat().otherTaskRunning);
    //     } else {
    //         new Thread(r).start();
    //     }
    // }

    // private void runServerTaskExclusively(ServerCommandSource src, MinecraftServer server, Runnable r){
    //     if (hasTask){
    //         src.sendError(getFormat().otherTaskRunning);
    //     }
    //     else {
    //         server.send(new ServerTask(server.getTicks(), r));
    //     }
    // }

    private UUID getSourceUUID(CommandContext<ServerCommandSource> ctx){
        try {
            return ctx.getSource().getPlayer().getUuid();
        } catch (CommandSyntaxException e) {
            // 
            return BackupInfo.CONSOLE_UUID;
        }
    }

    private static boolean getAutosave(MinecraftServer server){
        for (ServerWorld sw: server.getWorlds()){
            return !sw.savingDisabled;
        }
        return true;
    }

    private static void setAutosave(MinecraftServer server, boolean a){
        for (ServerWorld w: server.getWorlds()){
            w.savingDisabled = !a;
        }
    }

    public static Path getWorldDir(MinecraftServer server){
        return ((LevelStorageAccessor) server.getLevelStorage()).getSavesDir().resolve(server.getLevelName());
    }

    private synchronized int create(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        final String name = StringArgumentType.getString(ctx, "name"),
                description = MessageArgumentType.getMessage(ctx, "description").asString();
        final ServerCommandSource src = ctx.getSource();
        final MinecraftServer server = src.getMinecraftServer();
        final BackupEntry b = bf.create(server.getLevelName(), name, description, getSourceUUID(ctx));
        final String senderName = src.getName();
        final Runnable btask = () -> {
            if (tryBeginTask(src)){
                final boolean autosave = getAutosave(server);
                setAutosave(server, false);
                server.send(new ServerTask(server.getTicks(), () -> {
                    new Thread(() -> {
                        server.save(false, true, true);
                        server.getPlayerManager().saveAllPlayerData();
                        final FileCopyProgressBar progressBar = new FileCopyProgressBar(server);
                        try {
                            server.getPlayerManager().sendToAll(render(getFormat().creatingBackup, senderName, name));
                            Path worldDir = getWorldDir(server);
                            LOGGER.info("world dir: " + worldDir.toString());
    
                            b.doBackup(worldDir, progressBar);
                            server.getPlayerManager().sendToAll(render(getFormat().backupComplete, senderName, name));
                        } catch (Exception e) {
                            e.printStackTrace();
                            server.getPlayerManager().sendToAll(render(getFormat().backupFailed, senderName, e.toString()));
                            progressBar.done();
                        }
                        finally {
                            setAutosave(server, autosave);
                            endTask();
                        }
                    }).start();
                }));
            }
        };
        if (b.exists()) {
            src.sendFeedback(render(getFormat().overwriteAlert, name), false);
            cm.submit(src.getName(), src, s -> {
                btask.run();
            });
        }
        else {
            btask.run();
        }
        return 1;
    }

    private synchronized int back(CommandContext<ServerCommandSource> ctx){
        final ServerCommandSource src = ctx.getSource();
        final MinecraftServer server = src.getMinecraftServer();
        final BackupEntry entry = bf.getEntry(server.getLevelName(), StringArgumentType.getString(ctx, "name"));
        if (entry == null || !entry.exists()){
            src.sendError(getFormat().backupNotExist);
            return 0;
        }
        cm.submit(src.getName(), src, (s) -> {
            if (tryBeginTask(src)){
                server.getPlayerManager().broadcastChatMessage(render(getFormat().rollbackConfirmedAlert, src.getName(), entry.getInfo().name), true);
                countDownTask = new CountDownTask(cProvider.get().defaultCountDown);
                countDownTask.run(i -> {
                    Text txt = render(getFormat().countDownTitle, Integer.toString(i));
                    server.getPlayerManager().sendToAll(new TitleS2CPacket(TitleS2CPacket.Action.ACTIONBAR, txt, 10, 10, -1));
                }).thenAccept(b -> {
                    countDownTask = null;
                    if (b){
                        server.getPlayerManager().broadcastChatMessage(getFormat().rollbackStarted, true);
                        ((IServer) server).reloadAll(entry, () -> {
                            server.getPlayerManager().broadcastChatMessage(getFormat().rollbackFinished, true);
                            endTask();
                        });
                    }
                    else {
                        endTask();
                    }
                });
            }
        });
        return 1;
    }

    private int list(CommandContext<ServerCommandSource> ctx){
        final ServerCommandSource src = ctx.getSource();
        for (BackupEntry entry: bf.getBackups(src.getMinecraftServer().getLevelName())){
            src.sendFeedback(render(
                getFormat().backupListItem,
                entry.getInfo().name,
                entry.getInfo().description,
                cProvider.get().dateFormat.format(entry.getInfo().date)
            ), false);
        }
        return 1;
    }

    @Override
    public void execute(Runnable command) {
        // TODO Auto-generated method stub

    }
}