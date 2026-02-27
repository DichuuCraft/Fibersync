package com.hadroncfy.fibersync.config;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;

public abstract class AbstractTextRenderer<C> {
    protected abstract MutableText renderString(String s);

    public Text render(C ctx, Text template){
        MutableText ret;
        var content = template.getContent();
        if (content instanceof PlainTextContent.Literal literal){
            ret = renderString(literal.string());
        } else if (content instanceof TranslatableTextContent tc){
            Object[] args = new Object[tc.getArgs().length];
            for (int i = 0; i < args.length; i++){
                Object obj = tc.getArgs()[i];
                if (obj instanceof Text obj2){
                    obj = render(ctx, obj2);
                }
                else {
                    obj = "null";
                }
                args[i] = obj;
            }
            ret = Text.translatable(tc.getKey(), args);
        } else {
            ret = template.copyContentOnly();
        }
        ret.setStyle(renderStyle(ctx, template.getStyle()));

        for (Text t: template.getSiblings()){
            ret.append(render(ctx, t));
        }
        return ret;
    }

    private Style renderStyle(C ctx, Style style){
        Style ret = style;

        HoverEvent h = style.getHoverEvent();
        if (h instanceof HoverEvent.ShowText showText){
            Text content = showText.value();
            ret = ret.withHoverEvent(new HoverEvent.ShowText(render(ctx, content)));
        }

        ClickEvent c = style.getClickEvent();
        if (c != null){
            ret = ret.withClickEvent(renderClickEvent(ctx, c));
        }

        String i = style.getInsertion();
        if (i != null){
            ret = ret.withInsertion(renderString(i).getString());
        }

        return ret;
    }

    private ClickEvent renderClickEvent(C ctx, ClickEvent event) {
        if (event instanceof ClickEvent.RunCommand run){
            return new ClickEvent.RunCommand(renderString(run.command()).getString());
        }
        if (event instanceof ClickEvent.SuggestCommand suggest){
            return new ClickEvent.SuggestCommand(renderString(suggest.command()).getString());
        }
        if (event instanceof ClickEvent.CopyToClipboard copy){
            return new ClickEvent.CopyToClipboard(renderString(copy.value()).getString());
        }
        if (event instanceof ClickEvent.OpenFile openFile){
            String path = renderString(openFile.file().getPath()).getString();
            return new ClickEvent.OpenFile(new java.io.File(path));
        }
        if (event instanceof ClickEvent.OpenUrl openUrl){
            String url = renderString(openUrl.uri().toString()).getString();
            return new ClickEvent.OpenUrl(java.net.URI.create(url));
        }
        return event;
    }
}
