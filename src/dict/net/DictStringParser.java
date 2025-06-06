package dict.net;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DictStringParser {

    private static Pattern stringUnit = Pattern.compile("\"([^\"]*)\"|(\\S+)");

    // input string: hello world "foo bar"
    // output: ["hello", "world", "foo bar"]
    public static String[] splitAtoms(String original) {
        List<String> list = new ArrayList<>();
        Matcher m = stringUnit.matcher(original);
        while (m.find()) {
            list.add(m.group(m.group(1) != null ? 1 : 2));
        }
        return list.toArray(new String[list.size()]);
    }
}
