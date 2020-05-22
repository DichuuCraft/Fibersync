package com.hadroncfy.fibersync.command;

import java.util.ArrayList;
import java.util.List;

public class TaskManager {
    private boolean hasTask;
    private final List<Runnable> pendingTasks = new ArrayList<>();

    public synchronized boolean beginTask(){
        if (hasTask){
            return false;
        }
        else {
            hasTask = true;
            return true;
        }
    }

    public synchronized void endTask(){
        hasTask = false;
        for (Runnable r: pendingTasks){
            r.run();
        }
        pendingTasks.clear();
    }

    public synchronized void runWhenDone(Runnable r){
        if (hasTask){
            hasTask = false;
        }
        else {
            pendingTasks.add(r);
        }
    }
}