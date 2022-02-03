package top.kidhx.apidoc.apiwriter;

import org.apache.maven.plugin.logging.Log;

/**
 * @author HX
 * @date 2022/1/31
 */
public class MarkdownWriter {

    private Log log;

    public MarkdownWriter(Log log) {
        this.log = log;
    }

    public String h(String text, int bond) {
        if (bond > 6) {
            bond = 6;
        }

        if (bond < 1) {
            bond = 1;
        }

        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < bond; i++) {
            sb.append("#");
        }
        sb.append(" ");
        sb.append(text);
        return sb.toString();
    }

    public String tableCell(String text, String type) {
        if ("head".equalsIgnoreCase(type)) {
            return " | " + text + " | ";
        } else {
            return text + " | ";
        }
    }

    public String b(String text) {
        return "**" + text + "**";
    }
}
