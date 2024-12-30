package com.vng.zshort.tools.connect.transform.normalizerfieldvalue.charfilter;

public class UnderLineFilter implements FilterInterface{
    public static UnderLineFilter INST = new UnderLineFilter();
    @Override
    public String filter(String input) {
        return input.replace("_", " ");
    }
}
