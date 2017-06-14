import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Main {
    public static String PATH = System.getProperty("user.dir") + File.separator + "pic";
    public static String MESSAGES = "messages.txt";
    public static String URL_PATTERN = "(https://pp.userapi.com/).+(.jpg)";
    public static String NAME_PATTERN = "(/)([A-Za-z1-9-_]+)(.jpg)";
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
        String filPathAbs = file.getAbsolutePath();
        try (Stream<String> stringStream = Files.lines(Paths.get(filPathAbs))) {
            stringStream.forEach(line -> new Thread() {
                @Override
                public void run() {
                     load(line, urlPattern);
                    Thread.currentThread().stop();
                }
            }.start());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void load(String line, Pattern urlPattern) {
        Matcher lineMatcher = urlPattern.matcher(line);
        if (lineMatcher.find()) {
            Pattern name = Pattern.compile(NAME_PATTERN);
            Matcher nameMatcher = name.matcher(lineMatcher.group());
            String matchedName = "";
            if (nameMatcher.find()) {
                matchedName = nameMatcher.group().replaceFirst("/", "");
            }
            System.out.println(matchedName);
            try {
                url = new URL(lineMatcher.group());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            try {
                ReadableByteChannel rb = Channels.newChannel(url.openStream());
                FileOutputStream fos = new FileOutputStream(PATH + matchedName);
                fos.getChannel().transferFrom(rb, 0, Long.MAX_VALUE);
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

}
