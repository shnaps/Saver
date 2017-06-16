import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by shnaps on 16.06.2017.
 */
public class ImageDownloader {

    private final static Logger LOGGER = Logger.getLogger(ImageDownloader.class);

    private final String URL_PATTERN = "(((\\s:\\s)))((?:https://)(?:[a-zA-Z0-9]{2,10})(?:.userapi.com/).{10,40}(?:.jpg))";
    private final String JSON_URL_PATTERN = "(\"type\":\"wall\"?)";
    private final String NAME_PATTERN = "(/)([A-Za-z0-9-_]+)(.jpg)";

    private static Pattern urlPattern;
    private static Pattern jsonPattern;
    private static Pattern namePattern;

    private URL url;

    public void download(String messagesSource) {
        MessagesWorker messagesWorker = new MessagesWorker();
        jsonPattern = Pattern.compile(JSON_URL_PATTERN);
        Matcher jsonMatcher = jsonPattern.matcher(messagesSource);
        if (jsonMatcher.find()) {
            LOGGER.info("This is JSON!");
        } else {
            urlPattern = Pattern.compile(URL_PATTERN);
            Matcher lineMatcher = urlPattern.matcher(messagesSource);
            while (lineMatcher.find()) {
                LOGGER.info("Line matched      " + messagesSource);
                namePattern = Pattern.compile(NAME_PATTERN);
                Matcher nameMatcher = namePattern.matcher(lineMatcher.group());
                String matchedName = "";
                if (nameMatcher.find()) {
                    matchedName = nameMatcher.group().replaceFirst("/", "");
                }
                if (!Files.exists(Paths.get(messagesWorker.getPATH() + File.separator + matchedName))) {
                    try {
                        LOGGER.info("Getting image from " + lineMatcher.group());
                        url = new URL(lineMatcher.group().replaceFirst(" : ", ""));
                    } catch (MalformedURLException e) {
                        LOGGER.error("Url not created, error!");
                        e.printStackTrace();
                    }
                    try {
                        ReadableByteChannel rb = Channels.newChannel(url.openStream());
                        FileOutputStream fos = new FileOutputStream(messagesWorker.getPATH() + File.separator + matchedName);
                        fos.getChannel().transferFrom(rb, 0, Long.MAX_VALUE);
                        LOGGER.info("Saving image " + messagesSource);
                        fos.close();

                        try {
                            LOGGER.info("Attempt to shutdown executor for " + matchedName + " image\n");
                            messagesWorker.getInstance().shutdown();
                            LOGGER.info("Sended shutdown signal" + matchedName + " image\n");
                            messagesWorker.getInstance().awaitTermination(1, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            LOGGER.error("Tasks interrupted");
                        }

                    } catch (IOException e) {
                        LOGGER.error("Image not saved properly, error!");
                        e.printStackTrace();
                    }
                } else {
                    LOGGER.info("File already exists      " + messagesSource + "\n");
                }
            }
        }
    }
}
