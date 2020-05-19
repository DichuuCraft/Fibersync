package com.hadroncfy.fibersync.restart;

import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.c2s.play.AdvancementTabC2SPacket;
import net.minecraft.network.packet.c2s.play.BoatPaddleStateC2SPacket;
import net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket;
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickWindowC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientSettingsC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.network.packet.c2s.play.ConfirmGuiActionC2SPacket;
import net.minecraft.network.packet.c2s.play.CraftRequestC2SPacket;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.network.packet.c2s.play.GuiCloseC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
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
import net.minecraft.network.packet.c2s.play.RenameItemC2SPacket;
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;
import net.minecraft.network.packet.c2s.play.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.c2s.play.SelectVillagerTradeC2SPacket;
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
import net.minecraft.network.packet.s2c.play.PlayerAbilitiesS2CPacket;
import net.minecraft.text.Text;

public class ServerDummyPlayHandler implements ServerPlayPacketListener {
    private final AwaitingPlayer player;
    private final Limbo limbo;

    public ServerDummyPlayHandler(Limbo limbo, AwaitingPlayer player){
        this.player = player;
        this.limbo = limbo;
        player.getConnection().setPacketListener(this);

        final PlayerAbilities ab = new PlayerAbilities();
        // ab.allowFlying = true;
        ab.flying = true;
        player.getConnection().send(new PlayerAbilitiesS2CPacket(ab));
    }

    @Override
    public void onDisconnected(Text reason) {
        limbo.removePlayer(player);
    }

    @Override
    public ClientConnection getConnection() {
        // TODO Auto-generated method stub
        return player.getConnection();
    }

    @Override
    public void onHandSwing(HandSwingC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onChatMessage(ChatMessageC2SPacket packet) {
        // TODO Auto-generated method stub

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
    public void onConfirmTransaction(ConfirmGuiActionC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onButtonClick(ButtonClickC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onClickWindow(ClickWindowC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onCraftRequest(CraftRequestC2SPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onGuiClose(GuiCloseC2SPacket packet) {
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
        // TODO Auto-generated method stub

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
    public void onVillagerTradeSelect(SelectVillagerTradeC2SPacket packet) {
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

}