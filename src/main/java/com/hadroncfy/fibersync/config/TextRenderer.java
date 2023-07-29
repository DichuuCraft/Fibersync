package com.hadroncfy.fibersync.config;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.hadroncfy.fibersync.util.Replacer;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class TextRenderer extends AbstractTextRenderer<TextRenderer> implements Replacer<String> {
    private List<String> vars = new ArrayList<>();
    private static final Pattern VAL_EXP = Pattern.compile("\\$[0-9]");
    
    public TextRenderer var(Object ...vars){
        for (Object v: vars){
            this.vars.add(v.toString());
        }
        return this;
    }

    public Text render0(Text t){
        return render(this, t);
    }

    @Override
    protected MutableText renderString(String s) {
        return Text.literal(Replacer.replaceAll(VAL_EXP, s, this));
    }

    @Override
    public String get(String a) {
        try {
            int i = Integer.parseInt(a.substring(1));
            if (i > 0 && i <= vars.size()){
                return vars.get(i - 1);
            }
        }
        catch(NumberFormatException e){}
        return a;
    }

    public static Text render(Text template, Object ...vars){
        return new TextRenderer().var(vars).render0(template);
    }
}