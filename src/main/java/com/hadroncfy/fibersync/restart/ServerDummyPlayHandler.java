package com.hadroncfy.fibersync.restart;

import com.hadroncfy.fibersync.FibersyncMod;

import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketCallbacks;
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
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.network.packet.c2s.play.CraftRequestC2SPacket;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.JigsawGeneratingC2SPacket;
import net.minecraft.network.packet.c2s.play.KeepAliveC2SPacket;
import net.minecraft.network.packet.c2s.play.MessageAcknowledgmentC2SPacket;
import net.minecraft.network.packet.c2s.play.PickFromInventoryC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayPongC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerSessionC2SPacket;
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
import net.minecraft.util.Util;

public class ServerDummyPlayHandler implements ServerPlayPacketListener {
    private final AwaitingPlayer player;
    private final Limbo limbo;

    private boolean waitingForKeepAlive;
    private long lastKeepAliveTime = Util.getMeasuringTimeMs();
    private long keepAliveId;


    public ServerDummyPlayHandler(Limbo limbo, AwaitingPlayer player){
        this.player = player;
        this.limbo = limbo;
        player.connection.setPacketListener(this);

        final PlayerAbilities ab = new PlayerAbilities();
        // ab.allowFlying = true;
        ab.flying = true;
        player.connection.send(new PlayerAbilitiesS2CPacket(ab));
    }

    private void disconnect(Text reason){
        final ClientConnection connection = player.connection;
        connection.send(new DisconnectS2CPacket(reason), PacketCallbacks.always(() -> connection.disconnect(reason)));
        connection.disableAutoRead();
        connection.handleDisconnection();
    }

    public void tick(){
        long l = Util.getMeasuringTimeMs();
        if (l - lastKeepAliveTime >= 15000L) {
           if (waitingForKeepAlive) {
              this.disconnect(Text.translatable("disconnect.timeout", new Object[0]));
           } else {
              waitingForKeepAlive = true;
              lastKeepAliveTime = l;
              keepAliveId = l;
              player.connection.send(new KeepAliveS2CPacket(keepAliveId));
           }
        }
    }

    @Override
    public void onDisconnected(Text reason) {
        player.removed = true;
        FibersyncMod.LOGGER.info("{} lost connection: {}", player.profile.getName(), reason.getString());
    }

    @Override
    public void onHandSwing(HandSwingC2SPacket packet) {

    }

    @Override
    public void onChatMessage(ChatMessageC2SPacket packet) {
        final String msg = packet.chatMessage();
        Text text = Text.translatable("chat.type.text", player.profile.getName(), msg);
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
         } else if (!limbo.getServer().isHost(player.profile)) {
            this.disconnect(Text.translatable("disconnect.timeout", new Object[0]));
         }
    }

    @Override
    public void onPlayerMove(PlayerMoveC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onUpdatePlayerAbilities(UpdatePlayerAbilitiesC2SPacket packet) {
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
    public void onUpdateSign(UpdateSignC2SPacket packet) {
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
    public void onUpdateStructureBlock(UpdateStructureBlockC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSelectMerchantTrade(SelectMerchantTradeC2SPacket packet) {
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
    public void onUpdateJigsaw(UpdateJigsawC2SPacket packet) {
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

    @Override
    public void onPong(PlayPongC2SPacket packet) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isConnectionOpen() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isConnectionOpen'");
    }

    @Override
    public void onCommandExecution(CommandExecutionC2SPacket var1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onCommandExecution'");
    }

    @Override
    public void onMessageAcknowledgment(MessageAcknowledgmentC2SPacket var1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onMessageAcknowledgment'");
    }

    @Override
    public void onPlayerSession(PlayerSessionC2SPacket var1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onPlayerSession'");
    }

}