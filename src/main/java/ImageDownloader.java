import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by shnaps on 16.06.2017.
 */
public class ImageDownloader {

    private final static Logger LOGGER = Logger.getLogger(ImageDownloader.class);

    private final String URL_PATTERN = "(\\s:\\s)((?:https://)(?:[a-zA-Z0-9]{2,10})(?:.userapi.com/).{10,40}(?:.jpg))";
    private final String JSON_URL_PATTERN = "(\"type\":\"wall\"?)";
    private final String NAME_PATTERN = "(/)([A-Za-z0-9-_]+)(.jpg)";
    private static String folderName = "";

    private static Pattern urlPattern;
    private static Pattern jsonPattern;
    private static Pattern namePattern;

    private URL url;
    private MessagesWorker messagesWorker;

    public void download(String messagesSource) {
        folderName = "";
        messagesWorker = new MessagesWorker();
        jsonPattern = Pattern.compile(JSON_URL_PATTERN);
        Matcher jsonMatcher = jsonPattern.matcher(messagesSource);
        if (jsonMatcher.find()) {
            ArrayList<String> resultFromJson = findInJson(messagesSource);
            resultFromJson.forEach(record -> {
                saveImage(record, folderName);
            });
        } else {
            urlPattern = Pattern.compile(URL_PATTERN);
            Matcher lineMatcher = urlPattern.matcher(messagesSource);
            while (lineMatcher.find()) {
                saveImage(lineMatcher.group(), null);
            }

        }
    }

    private void saveImage(String record, String folderName) {

        LOGGER.info("Line matched      \"" + record+ "\"");
        namePattern = Pattern.compile(NAME_PATTERN);
        Matcher nameMatcher = namePattern.matcher(record);
        String matchedName = "";
        if (nameMatcher.find()) {
            matchedName = nameMatcher.group().replaceFirst("/", "");
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(messagesWorker.getPATH());
        stringBuilder.append(File.separator);
        if (!folderName.equals("")) {
            stringBuilder.append(folderName + File.separator);
        }
        try {
            if (!Files.isDirectory(Paths.get(stringBuilder.toString()))) {
                Files.createDirectory(Paths.get(stringBuilder.toString()));
            }
        } catch (IOException e) {
            LOGGER.error("Can't create folder, error!" + "   " + e.getMessage());
            e.printStackTrace();
        }
        if (!Files.exists(Paths.get(stringBuilder.toString() + matchedName))) {
            try {
                LOGGER.info("Getting image from \"" + record +"\"");
                url = new URL(record.replaceFirst(" : ", ""));
            } catch (MalformedURLException e) {
                LOGGER.error("Url not created, error!" + record + "   " + e.getMessage());
                e.printStackTrace();
            }
            try {
                ReadableByteChannel rb = Channels.newChannel(url.openStream());
                FileOutputStream fos = new FileOutputStream(stringBuilder.toString() + matchedName);
                fos.getChannel().transferFrom(rb, 0, Long.MAX_VALUE);
                LOGGER.info("Saving image " + matchedName);
                fos.close();

                try {
                    LOGGER.info("Attempt to shutdown executor for " + matchedName + " image");
                    messagesWorker.getInstance().shutdown();
                    LOGGER.info("Sended shutdown signal " + matchedName + " image");
                    messagesWorker.getInstance().awaitTermination(1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    LOGGER.error("Tasks interrupted" + "   " + e.getMessage());
                }

            } catch (IOException e) {
                LOGGER.error("Image not saved properly, error! \"" + record + "\"   " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            LOGGER.info("File already exists      " + matchedName + "");
        }

    }

    private ArrayList<String> findInJson(String source) {
        JSONObject jsonObject = new JSONObject(source);
        ArrayList<String> result = new ArrayList<>();
        JSONArray jsonArray = jsonObject.getJSONObject("wall").getJSONArray("attachments");
        folderName = (String) jsonObject.getJSONObject("wall").get("id").toString();
        for (Object object : jsonArray) {
            JSONObject tempObject = (JSONObject) object;
            JSONObject tempJsonObject = tempObject.getJSONObject("photo");
//            System.out.println(tempJsonObject);

            if (!tempJsonObject.isNull("src_xxxbig")) {
                result.add(tempJsonObject.getString("src_xxxbig"));
//                System.out.println("src_xxxbig added");
            } else {
                if (!tempJsonObject.isNull("src_xxbig")) {
                    result.add(tempJsonObject.getString("src_xxbig"));
//                    System.out.println("src_xxbig added");
                } else {
                    if (!tempJsonObject.isNull("src_xbig")) {
                        result.add(tempJsonObject.getString("src_xbig"));
//                        System.out.println("src_xbig added");
                    } else {
                        if (!tempJsonObject.isNull("src_big")) {
                            result.add(tempJsonObject.getString("src_big"));
//                            System.out.println("src_big added");
                        }
                    }
                }
            }
        }
        return result;
    }
}
