package ru.tasp.tools;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by the28awg on 25.10.15.
 */
public class Validator {
    private Pattern pattern;
    private Matcher matcher;

    public Validator(String pattern){
        this.pattern = Pattern.compile(pattern);
    }

    public boolean validate(final String matcher){
        this.matcher = pattern.matcher(matcher);
        return this.matcher.matches();
    }
}