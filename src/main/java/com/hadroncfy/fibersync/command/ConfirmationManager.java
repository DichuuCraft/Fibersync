package com.hadroncfy.fibersync.command;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Supplier;

import com.hadroncfy.fibersync.config.Config;
import com.hadroncfy.fibersync.config.Formats;

import net.minecraft.server.command.ServerCommandSource;
import static com.hadroncfy.fibersync.config.TextRenderer.render;

public class ConfirmationManager {
    private final Map<String, ConfirmationEntry> confirms = new HashMap<>();
    private final long timeout;
    private final int confirmCodeBound;
    private final Supplier<Config> cProvider;
    private static final Random random = new Random();

    public ConfirmationManager(Supplier<Config> p, long timeout, int confirmCodeBound){
        cProvider = p;
        this.timeout = timeout;
        this.confirmCodeBound = confirmCodeBound;
    }

    private Formats getFormat(){
        return cProvider.get().formats;
    }

    public synchronized void submit(String label, ServerCommandSource sender, ConfirmationHandler h){
        int code = random.nextInt(confirmCodeBound);
        confirms.put(label, new ConfirmationEntry(label, sender, code, h));
        sender.sendFeedback(render(getFormat().confirmationHint, Integer.toString(code)), false);
    }

    public synchronized boolean confirm(String label, int code){
        ConfirmationEntry h = confirms.get(label);
        if (h != null){
            if (code == h.code){
                h.t.cancel();
                h.handler.onConfirm(h.sender);
                confirms.remove(label);
                return true;
            }
        }
        return false;
    }

    public synchronized boolean cancel(String label){
        ConfirmationEntry h = confirms.get(label);
        if (h != null){
            h.t.cancel();
            confirms.remove(label);
            h.sender.sendFeedback(getFormat().opCancelled, false);
            return true;
        }
        return false;
    }

    @FunctionalInterface
    public interface ConfirmationHandler {
        void onConfirm(ServerCommandSource src);
    }

    private class ConfirmationEntry extends TimerTask {
        final String label;
        final ServerCommandSource sender;
        final ConfirmationHandler handler;
        final Timer t;
        final int code;
        public ConfirmationEntry(String label, ServerCommandSource sender, int code, ConfirmationHandler h){
            this.label = label;
            this.sender = sender;
            this.handler = h;
            t = new Timer();
            this.code = code;
            t.schedule(this, timeout);
        }

        @Override
        public void run() {
            synchronized(ConfirmationManager.this){
                confirms.remove(label);
                sender.sendFeedback(getFormat().opCancelled, false);
            }
        }
    }
}