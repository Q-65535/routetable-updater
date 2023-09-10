package parser;


import org.junit.jupiter.api.Test;

import java.util.regex.*;

public class ParserTest {
    @Test
    public void dateRegTest() {
        Pattern datePattern = Pattern.compile("(2\\d{3}\\.(0[1-9]|1[0-2])\\.(0[1-9]|[12]\\d|3[01]))");
        Matcher matcher = datePattern.matcher("2022.05.11");
        if (matcher.find()) {
            System.out.println("matched!");
        }
    }

    @Test
    public void hexRegTest() {
		Pattern hexPattern = Pattern.compile("0x..");
        Matcher matcher = hexPattern.matcher("0x2E");
        if (matcher.find()) {
            System.out.println("matched!");
        }
    }
    @Test
    public void unSignedRegTest() {
        Pattern dateUintPattern = Pattern.compile("\\d{1,2}u");
        Matcher matcher = dateUintPattern.matcher("23u");
        if (matcher.find()) {
        }
    }
}
