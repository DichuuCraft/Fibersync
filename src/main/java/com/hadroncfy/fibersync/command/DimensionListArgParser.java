package com.hadroncfy.fibersync.command;

import com.hadroncfy.fibersync.backup.BackupExcluder;
import com.hadroncfy.fibersync.command.parser.VarArgParser;

public class DimensionListArgParser extends VarArgParser {
    private static final String[] words = new String[]{"overworld", "nether", "end", "other"};
    private static final int[] masks = new int[]{
        BackupExcluder.MASK_OVERWORLD, BackupExcluder.MASK_NETHER, BackupExcluder.MASK_THE_END, BackupExcluder.MASK_OTHER
    };

    protected DimensionListArgParser() {
        super(words);
    }

    @Override
    protected char getDelimiter() {
        return '|';
    }
    
    public int getMask(){
        int mask = 0;
        for (int i = 0; i < words.length; i++){
            if (getFlag(i)){
                mask |= masks[i];
            }
        }
        return mask;
    }
}