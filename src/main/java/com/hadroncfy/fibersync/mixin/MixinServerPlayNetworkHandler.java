package com.hadroncfy.fibersync.mixin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hadroncfy.fibersync.FibersyncMod;
import com.hadroncfy.fibersync.command.BackupCommand;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

@Mixin(ServerPlayNetworkHandler.class)
public class MixinServerPlayNetworkHandler {
    private static final Pattern PREFIX = Pattern.compile("^[^ ]+");

    @Shadow
    public ServerPlayerEntity player;
    
    @Inject(method = "onChatMessage", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/server/PlayerManager;broadcastChatMessage(Lnet/minecraft/text/Text;Z)V",
        shift = At.Shift.AFTER
    ))
    private void onChat(ChatMessageC2SPacket packet, CallbackInfo ci){
        String msg = packet.getChatMessage();
        Matcher m = PREFIX.matcher(msg);
        if (m.find()){
            String prefix = m.group();
            if (FibersyncMod.getConfig().alternativeCmdPrefix.contains(prefix)){
                player.getServer().getCommandManager().execute(player.getCommandSource(), "/" + BackupCommand.NAME + msg.substring(m.end()));
            }
        }
    }
}