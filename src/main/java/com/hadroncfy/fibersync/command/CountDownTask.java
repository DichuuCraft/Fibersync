package com.hadroncfy.fibersync.command;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class CountDownTask {
    private int fuse;
    private volatile boolean cancelled = false;

    public CountDownTask(int fuse) {
        this.fuse = fuse;
    }

    public void cancel() {
        if (cancelled) {
            throw new IllegalStateException("Task already cancelled");
        }
        cancelled = true;
    }

    public CompletableFuture<Boolean> run(Consumer<Integer> onCountDown) {
        return CompletableFuture.supplyAsync(() -> {
            do {
                onCountDown.accept(fuse);
                if (cancelled) {
                    return false;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while(fuse --> 0);
            return true;
        });
    }
}