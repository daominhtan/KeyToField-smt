package com.vng.zshort.tools.connect.transform.normalizerfieldvalue.charfilter;

public class StripHtmlFilter implements FilterInterface{
    public static StripHtmlFilter INST = new StripHtmlFilter();
    @Override
    public String filter(String input) {
        return input.replaceAll("<[^>]*>", "");
    }
}
