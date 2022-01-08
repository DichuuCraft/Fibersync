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

import net.minecraft.server.filter.TextStream;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

@Mixin(ServerPlayNetworkHandler.class)
public class MixinServerPlayNetworkHandler {
    private static final Pattern PREFIX = Pattern.compile("^[^ ]+");

    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "handleMessage", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Ljava/util/function/Function;Lnet/minecraft/network/MessageType;Ljava/util/UUID;)V",
        shift = At.Shift.AFTER
    ))
    private void onChat(TextStream.Message message, CallbackInfo ci){
        String msg = message.getRaw();
        Matcher m = PREFIX.matcher(msg);
        if (m.find()){
            String prefix = m.group();
            if (FibersyncMod.getConfig().alternativeCmdPrefix.contains(prefix)){
                player.getServer().getCommandManager().execute(player.getCommandSource(), "/" + BackupCommand.NAME + msg.substring(m.end()));
            }
        }
    }
}