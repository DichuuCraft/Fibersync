package com.hadroncfy.fibersync.restart;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.c2s.play.AdvancementTabC2SPacket;
import net.minecraft.network.packet.c2s.play.BoatPaddleStateC2SPacket;
import net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket;
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientSettingsC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.ConfirmScreenActionC2SPacket;
import net.minecraft.network.packet.c2s.play.CraftRequestC2SPacket;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.JigsawGeneratingC2SPacket;
import net.minecraft.network.packet.c2s.play.KeepAliveC2SPacket;
import net.minecraft.network.packet.c2s.play.PickFromInventoryC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.QueryBlockNbtC2SPacket;
import net.minecraft.network.packet.c2s.play.QueryEntityNbtC2SPacket;
import net.minecraft.network.packet.c2s.play.RecipeBookDataC2SPacket;
import net.minecraft.network.packet.c2s.play.RecipeCategoryOptionsC2SPacket;
import net.minecraft.network.packet.c2s.play.RenameItemC2SPacket;
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;
import net.minecraft.network.packet.c2s.play.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.c2s.play.SelectMerchantTradeC2SPacket;
import net.minecraft.network.packet.c2s.play.SpectatorTeleportC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateBeaconC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateCommandBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateCommandBlockMinecartC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateDifficultyC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateDifficultyLockC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateJigsawC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdatePlayerAbilitiesC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateStructureBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.play.KeepAliveS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerAbilitiesS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Util;

public class ServerDummyPlayHandler implements ServerPlayPacketListener {
    private static final Logger LOGGER = LogManager.getLogger();
    private final AwaitingPlayer player;
    private final Limbo limbo;

    private boolean waitingForKeepAlive;
    private long lastKeepAliveTime = Util.getMeasuringTimeMs();
    private long keepAliveId;


    public ServerDummyPlayHandler(Limbo limbo, AwaitingPlayer player){
        this.player = player;
        this.limbo = limbo;
        player.getConnection().setPacketListener(this);

        final PlayerAbilities ab = new PlayerAbilities();
        // ab.allowFlying = true;
        ab.flying = true;
        player.getConnection().send(new PlayerAbilitiesS2CPacket(ab));
    }

    private void disconnect(Text reason){
        final ClientConnection connection = player.getConnection();
        connection.send(new DisconnectS2CPacket(reason), future -> {
            connection.disconnect(reason);
        });
        connection.disableAutoRead();
        connection.handleDisconnection();
    }

    public void tick(){
        long l = Util.getMeasuringTimeMs();
        if (l - lastKeepAliveTime >= 15000L) {
           if (waitingForKeepAlive) {
              this.disconnect(new TranslatableText("disconnect.timeout", new Object[0]));
           } else {
              waitingForKeepAlive = true;
              lastKeepAliveTime = l;
              keepAliveId = l;
              player.getConnection().send(new KeepAliveS2CPacket(keepAliveId));
           }
        }
    }

    @Override
    public void onDisconnected(Text reason) {
        player.markAsRemoved();
        LOGGER.info("{} lost connection: {}", player.getEntity().getGameProfile().getName(), reason.asString());
    }

    @Override
    public ClientConnection getConnection() {
        return player.getConnection();
    }

    @Override
    public void onHandSwing(HandSwingC2SPacket packet) {

    }

    @Override
    public void onGameMessage(ChatMessageC2SPacket packet) {
        final String msg = packet.getChatMessage();
        Text text = new TranslatableText("chat.type.text", player.getEntity().getGameProfile().getName(), msg);
        // player.getEntity().networkHandler.onChatMessage(packet);
        limbo.broadcast(text);
    }

    @Override
    public void onClientStatus(ClientStatusC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onClientSettings(ClientSettingsC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onConfirmScreenAction(ConfirmScreenActionC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onButtonClick(ButtonClickC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onClickSlot(ClickSlotC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onCraftRequest(CraftRequestC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onCloseHandledScreen(CloseHandledScreenC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onCustomPayload(CustomPayloadC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onPlayerInteractEntity(PlayerInteractEntityC2SPacket rpacket) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onKeepAlive(KeepAliveC2SPacket packet) {
        if (this.waitingForKeepAlive && packet.getId() == this.keepAliveId) {
            this.waitingForKeepAlive = false;
         } else if (!limbo.getServer().isHost(player.getEntity().getGameProfile())) {
            this.disconnect(new TranslatableText("disconnect.timeout", new Object[0]));
         }
    }

    @Override
    public void onPlayerMove(PlayerMoveC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onPlayerAbilities(UpdatePlayerAbilitiesC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onPlayerAction(PlayerActionC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onClientCommand(ClientCommandC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onPlayerInput(PlayerInputC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onUpdateSelectedSlot(UpdateSelectedSlotC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onCreativeInventoryAction(CreativeInventoryActionC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSignUpdate(UpdateSignC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onPlayerInteractBlock(PlayerInteractBlockC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onPlayerInteractItem(PlayerInteractItemC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSpectatorTeleport(SpectatorTeleportC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onResourcePackStatus(ResourcePackStatusC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onBoatPaddleState(BoatPaddleStateC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onVehicleMove(VehicleMoveC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onTeleportConfirm(TeleportConfirmC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onRecipeBookData(RecipeBookDataC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onAdvancementTab(AdvancementTabC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onRequestCommandCompletions(RequestCommandCompletionsC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onUpdateCommandBlock(UpdateCommandBlockC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onUpdateCommandBlockMinecart(UpdateCommandBlockMinecartC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onPickFromInventory(PickFromInventoryC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onRenameItem(RenameItemC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onUpdateBeacon(UpdateBeaconC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onStructureBlockUpdate(UpdateStructureBlockC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onMerchantTradeSelect(SelectMerchantTradeC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onBookUpdate(BookUpdateC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onQueryEntityNbt(QueryEntityNbtC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onQueryBlockNbt(QueryBlockNbtC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onJigsawUpdate(UpdateJigsawC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onUpdateDifficulty(UpdateDifficultyC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onUpdateDifficultyLock(UpdateDifficultyLockC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onJigsawGenerating(JigsawGeneratingC2SPacket packet) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onRecipeCategoryOptions(RecipeCategoryOptionsC2SPacket packet) {
        // TODO Auto-generated method stub
        
    }

}