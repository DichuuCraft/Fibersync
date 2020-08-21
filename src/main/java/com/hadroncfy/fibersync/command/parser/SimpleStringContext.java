package com.hadroncfy.fibersync.command.parser;

import com.mojang.brigadier.ImmutableStringReader;

public class SimpleStringContext implements ImmutableStringReader {
    private final String s;
    private final int cursor;

    public SimpleStringContext(String s, int cursor){
        this.s = s;
        this.cursor = cursor;
    }

    @Override
    public String getString() {
        return s;
    }

    @Override
    public int getRemainingLength() {
        return 0;
    }

    @Override
    public int getTotalLength() {
        return s.length();
    }

    @Override
    public int getCursor() {
        return cursor;
    }

    @Override
    public String getRead() {
        return s;
    }

    @Override
    public String getRemaining() {
        return "";
    }

    @Override
    public boolean canRead(int length) {
        return false;
    }

    @Override
    public boolean canRead() {
        return false;
    }

    @Override
    public char peek() {
        return 0;
    }

    @Override
    public char peek(int offset) {
        return 0;
    }

}
