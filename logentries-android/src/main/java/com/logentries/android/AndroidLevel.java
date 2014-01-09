package com.logentries.android;

import java.util.logging.Level;

/**
 * To create more logging levels, subclass AndroidLevel with a class containing
 * static Level objects.
 * <br/><i>public static Level myLevel = new ExtendedLevel(levelName, levelPriority);</i>
 * @author Caroline
 * 29/08/11
 */
class AndroidLevel extends Level {
    protected AndroidLevel(String name, int level) {
        super(name, level);
    }

    public static Level ERROR = new AndroidLevel("ERROR", 950);
    public static Level DEBUG = new AndroidLevel("DEBUG", 850);
    public static Level VERBOSE = new AndroidLevel("VERBOSE", 0);
}
