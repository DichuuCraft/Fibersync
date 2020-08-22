package com.hadroncfy.fibersync.command.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.IntSupplier;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.text.TranslatableText;

public abstract class VarArgParser {
    private static final SimpleCommandExceptionType COMMAND_EXCEPTION = new SimpleCommandExceptionType(new TranslatableText("command.unknown.argument"));
    protected abstract char getDelimiter();

    private final String[] words;
    private final boolean[] flags;
    private final StringBuilder sb = new StringBuilder();
    private int cursor = 0;
    private boolean expectingWord = true;
    
    protected VarArgParser(String[] words){
        this.words = words;
        this.flags = new boolean[words.length];
    }

    private ImmutableStringReader getContext(){
        return new SimpleStringContext(sb.toString(), cursor);
    }

    private void consumeWord(String word) throws CommandSyntaxException {
        int i = 0;
        for (String s: words){
            if (s.equals(word)){
                break;
            }
            i++;
        }
        if (i < words.length){
            if (flags[i]){
                throw COMMAND_EXCEPTION.createWithContext(getContext());
            }
            else {
                flags[i] = true;
            }
        }
        else {
            throw COMMAND_EXCEPTION.createWithContext(getContext());
        }
    }

    public void parse(IntSupplier source) throws CommandSyntaxException {
        int c = source.getAsInt();

        while (c != -1){
            if (c == getDelimiter()){
                if (expectingWord && sb.length() == 0){
                    throw COMMAND_EXCEPTION.createWithContext(getContext());
                }
                else {
                    consumeWord(sb.toString());
                    expectingWord = true;
                    sb.setLength(0);
                }
            }
            else {
                sb.append((char)c);
                expectingWord = false;
            }
            c = source.getAsInt();
        }
    }

    public void parse(String input) throws CommandSyntaxException {
        parse(new StringScanner(input));
    }

    public void end() throws CommandSyntaxException {
        if (expectingWord){
            throw COMMAND_EXCEPTION.createWithContext(getContext());
        }
        else {
            consumeWord(sb.toString());
        }
    }

    public Collection<String> getSuggestions(){
        List<String> ret = new ArrayList<>();
        String s = sb.toString();
        boolean hasEqual = false;

        for (int i = 0; i < words.length; i++){
            String w = words[i];
            if (!flags[i] && w.startsWith(s)){
                ret.add(w.substring(s.length()));
            }
            if (w.equals(s)){
                hasEqual = true;
            }
        }
        if (hasEqual){
            ret.add(Character.toString(getDelimiter()));
        }
        return ret;
    }

    protected boolean getFlag(int i){
        return flags[i];
    }
}