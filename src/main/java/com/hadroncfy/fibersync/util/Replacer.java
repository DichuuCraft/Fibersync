package com.hadroncfy.fibersync.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface Replacer<T> {
    T get(T a);
    
    public static String replaceAll(Pattern pattern, String s, Replacer<String> func){
        StringBuilder sb = new StringBuilder();
        int lastIndex = 0;
        Matcher m = pattern.matcher(s);
        while (m.find()){
            if (lastIndex != m.start()){
                sb.append(s.substring(lastIndex, m.start()));
            }
            String name = m.group();
            lastIndex = m.start() + name.length();
            sb.append(func.get(name));
        }
        if (lastIndex < s.length()){
            sb.append(s.substring(lastIndex));
        }
        return sb.toString();
    }
}