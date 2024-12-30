package com.vng.zshort.tools.connect.transform.normalizerfieldvalue.charfilter;

public class LongSpaceFilter implements FilterInterface{
    public static LongSpaceFilter INST = new LongSpaceFilter();
    @Override
    public String filter(String input) {
        return input.replaceAll("\\s+", " ");
    }
}
