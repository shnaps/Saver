import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Main {
    public static String PATH = "d:\\Needed soft\\pic\\";
    public static String MESSAGES = "messages.txt";
    public static String URL_PATTERN = "(https://pp.userapi.com/).+(.jpg)";
    public static String NAME_PATTERN = "(/)([A-Za-z1-9-_]+)(.jpg)";
    public InputStream in = null;
    public OutputStream out = null;
    public URL url = null;

    public static void main(String[] args) {
        String messagesArgs = args[0];
        Main start = new Main();
        Pattern urlPattern = Pattern.compile(URL_PATTERN);
        start.parse(urlPattern, messagesArgs);
    }

    private void parse(Pattern urlPattern, String fileName) {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(fileName).getFile());
        String filePathAbs = file.getAbsolutePath();
        try (Stream<String> stringStream = Files.lines(Paths.get(filePathAbs))) {
            stringStream.forEach(line -> load(line, urlPattern));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void load(String line, Pattern urlPattern) {
        Matcher lineMatcher = urlPattern.matcher(line);
        if (lineMatcher.find()) {
            Pattern name = Pattern.compile(NAME_PATTERN);
            Matcher nameMatcher = name.matcher(lineMatcher.group().toString());
            String matchedName = "";
            if (nameMatcher.find()) {
                matchedName = nameMatcher.group().toString().replaceFirst("/", "");
            }
            System.out.println(matchedName);
            try {
                url = new URL(lineMatcher.group().toString());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            try {
                in = new BufferedInputStream(url.openStream());
                try {
                    out = new BufferedOutputStream(new FileOutputStream(PATH + matchedName));
                    for (int i; (i = in.read()) != -1; ) {
                        out.write(i);
                    }
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
