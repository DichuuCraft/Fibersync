package com.hadroncfy.fibersync.command.parser;

import java.util.function.IntSupplier;

class StringScanner implements IntSupplier {
    private final String s;
    private int i = 0;

    public StringScanner(String s){
        this.s = s;
    }
    @Override
    public int getAsInt() {
        return i >= s.length() ? -1 : s.charAt(i++);
    }
    
}