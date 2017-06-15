import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Main {

//    public static String PATH = System.getProperty("user.dir") + File.separator + "pic";
    public static String PATH = "d:\\Needed soft\\pic-prod";
    public static String URL_PATTERN = "(?<=((src_?(\\w){0,4}\\\\\":\\\\\")|(\\s:\\s)))((?:https://)(?:[a-zA-Z0-9]{2,10})(?:.userapi.com/).{10,40}(?:.jpg))(?!(\\\\\",\\\\\"src))";
    public static String NAME_PATTERN = "(/)([A-Za-z0-9-_]+)(.jpg)";
    public URL url = null;

    public static void main(String[] args) {
        String messagesArgs;
        if (args.length > 0) {
            messagesArgs = args[0];
        } else {
            messagesArgs = "test.txt";
        }
        ExecutorService executor = Executors.newFixedThreadPool(15);

        Main start = new Main();
        if (!start.checkDir()) {
            try {
                Files.createDirectory(Paths.get(PATH));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Pattern urlPattern = Pattern.compile(URL_PATTERN);
        start.parse(urlPattern, messagesArgs, executor);
    }

    private boolean checkDir() {
        return Files.isDirectory(Paths.get(PATH));
    }

    private void parse(Pattern urlPattern, String fileName, ExecutorService executor) {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(fileName).getFile());
        String filePathAbs = file.getAbsolutePath();
        try (Stream<String> stringStream = Files.lines(Paths.get(filePathAbs))) {
            stringStream.forEach(line -> {
                executor.submit(() -> {
                    load(line, urlPattern, executor);
                });
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void load(String line, Pattern urlPattern, ExecutorService executor) {
        Matcher lineMatcher = urlPattern.matcher(line);
        while (lineMatcher.find()) {
            Pattern name = Pattern.compile(NAME_PATTERN);
            Matcher nameMatcher = name.matcher(lineMatcher.group());
            String matchedName = "";
            if (nameMatcher.find()) {
                matchedName = nameMatcher.group().replaceFirst("/", "");
            }
            if (!Files.exists(Paths.get(PATH + File.separator + matchedName))) {
                try {
                    System.out.println("Getting image from " + lineMatcher.group());
                    url = new URL(lineMatcher.group());
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                try {
                    ReadableByteChannel rb = Channels.newChannel(url.openStream());
                    FileOutputStream fos = new FileOutputStream(PATH + File.separator + matchedName);
                    fos.getChannel().transferFrom(rb, 0, Long.MAX_VALUE);
                    System.out.println("Saving to disk image with name: " + matchedName);
                    fos.close();

                    try {
                        System.out.println("Attempt to shutdown executor for " + matchedName + " image");
                        executor.shutdown();
                        executor.awaitTermination(1, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        System.err.println("Tasks interrupted");
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
