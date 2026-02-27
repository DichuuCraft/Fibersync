package com.hadroncfy.fibersync.restart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.hadroncfy.fibersync.FibersyncMod;
import com.hadroncfy.fibersync.interfaces.IPlayer;
import com.hadroncfy.fibersync.interfaces.IPlayerManager;
import com.hadroncfy.fibersync.interfaces.IServer;
import com.hadroncfy.fibersync.mixin.ServerCommonNetworkHandlerAccessor;
import com.hadroncfy.fibersync.util.copy.FileOperationProgressListener;

import net.minecraft.entity.EntityPosition;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.CommonPlayerSpawnInfo;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerAbilitiesS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.NbtReadView;
import net.minecraft.storage.ReadView;
import net.minecraft.text.Text;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec2f;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

public class Limbo {
    public final Set<RegistryKey<World>> world_keys;
    private final List<AwaitingPlayer> players = new ArrayList<>();
    private final MinecraftServer server;
    private final RollBackProgressListener rollBackProgressListener = new RollBackProgressListener(this);

    public Limbo(MinecraftServer server) {
        this.server = server;
        this.world_keys = new HashSet<>();
        for (var k: server.getWorldRegistryKeys()) {
            this.world_keys.add(k);
        }
    }

    public MinecraftServer getServer(){
        return server;
    }

    public void start() {
        for (ServerPlayerEntity player : new ArrayList<>(server.getPlayerManager().getPlayerList())) {
            server.getPlayerManager().remove(player);
            var access = (ServerCommonNetworkHandlerAccessor) (Object) player.networkHandler;
            var p = new AwaitingPlayer(this, player, access.fibersync$getConnection());
            onPlayerConnect(p, false);
        }
        ((IServer) this.server).setLimbo(null, this);
    }

    public FileOperationProgressListener getFileCopyListener(){
        return rollBackProgressListener;
    }

    public void startWorldGen(){
        rollBackProgressListener.startSpawn();
    }

    public void finishWorldGen(){
        rollBackProgressListener.finishSpawn();
    }

    public void onPlayerConnect(AwaitingPlayer p, boolean sendJoin){
        if (sendJoin) {
            CommonPlayerSpawnInfo spawnInfo = createLimboSpawnInfo(GameMode.SPECTATOR);
            PlayerManager playerManager = server.getPlayerManager();
            p.connection.send(new GameJoinS2CPacket(
                0,
                server.getSaveProperties().isHardcore(),
                this.world_keys,
                playerManager.getMaxPlayerCount(),
                playerManager.getViewDistance(),
                playerManager.getSimulationDistance(),
                false,
                true,
                false,
                spawnInfo,
                false
            ));
        }
        PlayerAbilities abilities = new PlayerAbilities();
        abilities.allowFlying = true;
        abilities.allowModifyWorld = false;
        abilities.invulnerable = true;
        abilities.flying = true;
        abilities.creativeMode = false;
        p.connection.send(new PlayerAbilitiesS2CPacket(abilities));
        p.connection.send(new PlayerPositionLookS2CPacket(
            0,
            new EntityPosition(new Vec3d(0.0, 0.0, 0.0), Vec3d.ZERO, 0.0F, 0.0F),
            Collections.emptySet()
        ));
        rollBackProgressListener.onPlayerConnected(p);

        FibersyncMod.LOGGER.info("Player {} joined limbo", p.profile.name());
        addPlayer(p);
    }

    private synchronized void addPlayer(AwaitingPlayer p){
        players.add(p);
    }

    public void end() {
        final PlayerManager playerManager = server.getPlayerManager();
        final IPlayerManager pm = (IPlayerManager) playerManager;
        final ServerWorld dummy = server.getOverworld();

        this.removeRemovedPlayers();

        rollBackProgressListener.end();

        pm.reset(null);
        ((IServer) this.server).setLimbo(null, null);

        if (server.isSingleplayer() && players.isEmpty()){
            FibersyncMod.LOGGER.info("Stopping server as the server has no players");
            // server.stop(true);
        } else {
            pm.setShouldRefreshScreen(null, true);
            for (AwaitingPlayer player : players) {
                // we can't just create a new ServerPlayerEntity here since the original player
                // may
                // be the instance of an inherited class of ServerPlayerEntity, such as
                // carpet.patches.EntityPlayerMPFake, or
                // com.hadroncfy.sreplay.recording.Photographer.
                var playerEntity = player.entity;
                ConnectedClientData clientData = createClientData(player);
                if (playerEntity == null) {
                    playerEntity = new ServerPlayerEntity(this.server, dummy, player.profile, clientData.syncedOptions());
                }
                var entry = new PlayerConfigEntry(player.profile);
                Optional<NbtCompound> dataOpt = playerManager.loadPlayerData(entry);
                ReadView readView = null;
                Optional<ServerPlayerEntity.SavePos> savePosOpt = Optional.empty();
                ErrorReporter.Logging reporter = null;
                if (dataOpt.isPresent()) {
                    reporter = new ErrorReporter.Logging(playerEntity.getErrorReporterContext(), FibersyncMod.LOGGER);
                    readView = NbtReadView.create(reporter, server.getRegistryManager(), dataOpt.get());
                    savePosOpt = readView.read(ServerPlayerEntity.SavePos.CODEC);
                }

                var spawnPoint = server.getSaveProperties().getMainWorldProperties().getSpawnPoint();
                ServerWorld targetWorld = savePosOpt.flatMap(ServerPlayerEntity.SavePos::dimension)
                    .map(server::getWorld)
                    .orElseGet(() -> {
                        ServerWorld w = server.getWorld(spawnPoint.getDimension());
                        return w != null ? w : server.getOverworld();
                    });
                playerEntity.setServerWorld(targetWorld); // keep interactionManager in sync
                ((IPlayer)playerEntity).reset(null);
                if (readView != null) {
                    playerEntity.readData(readView);
                }

                Vec3d targetPos = savePosOpt.flatMap(ServerPlayerEntity.SavePos::position)
                    .orElseGet(() -> Vec3d.of(spawnPoint.getPos()));
                Vec2f targetRot = savePosOpt.flatMap(ServerPlayerEntity.SavePos::rotation)
                    .orElseGet(() -> new Vec2f(spawnPoint.yaw(), spawnPoint.pitch()));
                playerEntity.refreshPositionAndAngles(targetPos, targetRot.x, targetRot.y);

                playerManager.onPlayerConnect(player.connection, playerEntity, clientData);
                if (readView != null) {
                    playerEntity.readEnderPearls(readView);
                    playerEntity.readRootVehicle(readView);
                }
                if (reporter != null) {
                    reporter.close();
                }
            }
            pm.setShouldRefreshScreen(null, false);
        }
        players.clear();
    }

    public void sendToAll(Packet<?> packet) {
        for (AwaitingPlayer player : players) {
            player.connection.send(packet);
        }
    }

    public void broadcast(Text txt){
        sendToAll(new GameMessageS2CPacket(txt, false));
    }

    public void tick() {
        this.removeRemovedPlayers();
        this.server.getNetworkIo().tick();
    }

    public synchronized void removeRemovedPlayers(){
        for (Iterator<AwaitingPlayer> iterator = players.iterator(); iterator.hasNext();){
            AwaitingPlayer p = iterator.next();
            if (p.removed){
                iterator.remove();
                FibersyncMod.LOGGER.info("Player {} left limbo", p.profile.name());
            }
        }
    }

    private ConnectedClientData createClientData(AwaitingPlayer player) {
        if (player.entity != null) {
            return new ConnectedClientData(player.profile, 0, player.entity.getClientOptions(), false);
        }
        return ConnectedClientData.createDefault(player.profile, false);
    }

    private CommonPlayerSpawnInfo createLimboSpawnInfo(GameMode gameMode) {
        ServerWorld world = server.getOverworld();
        return new CommonPlayerSpawnInfo(
            world.getDimensionEntry(),
            world.getRegistryKey(),
            world.getSeed(),
            gameMode,
            gameMode,
            world.isDebugWorld(),
            world.isFlat(),
            Optional.empty(),
            0,
            world.getSeaLevel()
        );
    }
}
