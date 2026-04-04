/// usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 26+
//DEPS com.github.lalyos:jfiglet:0.0.8

import com.github.lalyos.jfiglet.FigletFont;

void main(String... a) throws Exception {
    var text = a.length > 0 ? String.join(" ", a) : "JBANG";
    System.out.println(FigletFont.convertOneLine(text));
}