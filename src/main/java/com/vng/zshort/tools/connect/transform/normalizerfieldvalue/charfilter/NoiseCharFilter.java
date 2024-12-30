package com.vng.zshort.tools.connect.transform.normalizerfieldvalue.charfilter;

public class NoiseCharFilter implements FilterInterface{
    public static NoiseCharFilter INST = new NoiseCharFilter();
    @Override
    public String filter(String input) {
        return input.replaceAll("[\\(\\)\\[\\{\\]\\}]+|[\\`~!@#$%^&*-+=|:;\"'“”,<>/?_.-]+", " ");
    }
}
