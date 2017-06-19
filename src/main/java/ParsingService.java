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
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParsingService {

    private final static Logger LOGGER = Logger.getLogger(ParsingService.class);

    private final String URL_PATTERN = "(\\s:\\s)((?:https://)(?:[a-zA-Z0-9]{2,10})(?:.userapi.com/).{10,40}(?:.jpg))";
    private final String JSON_URL_PATTERN = "(\"type\":\"wall\"?)";
    private final String NAME_PATTERN = "(/)([A-Za-z0-9-_]+)(.jpg)";

    private static Pattern urlPattern;
    private static Pattern jsonPattern;
    private static Pattern namePattern;

    private URL url;
    private MessagesService messagesService = new MessagesService();

    public void download(String messagesSource, int count) {
        jsonPattern = Pattern.compile(JSON_URL_PATTERN);
        Matcher jsonMatcher = jsonPattern.matcher(messagesSource);
        if (jsonMatcher.find()) {
            ArrayList<String> resultFromJson = findInJson(messagesSource);
            String folderName = getFolderName(messagesSource);
            resultFromJson.forEach(record -> {
                saveImage(record, folderName);
            });
        } else {
            urlPattern = Pattern.compile(URL_PATTERN);
            Matcher lineMatcher = urlPattern.matcher(messagesSource);
            while (lineMatcher.find()) {
                saveImage(lineMatcher.group(), "");
            }
        }
    }

    private String getFolderName(String messagesSource) {
        JSONObject jsonObject = new JSONObject(messagesSource);
        String name = "";
        if (!jsonObject.getJSONObject("wall").isNull("date")) {
            name = jsonObject.getJSONObject("wall").get("date").toString();
            return name;
        }
        return name;
    }

    private void saveImage(String record, String folderName) {

        LOGGER.info("Line matched      \"" + record + "\"");
        namePattern = Pattern.compile(NAME_PATTERN);
        Matcher nameMatcher = namePattern.matcher(record);
        String matchedName = "";
        if (nameMatcher.find()) {
            matchedName = nameMatcher.group().replaceFirst("/", "");
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(messagesService.getPath());
        stringBuilder.append(File.separator);
        if (!folderName.equals("")) {
            stringBuilder.append(folderName + File.separator);
        }
        try {
            if (!Files.isDirectory(Paths.get(stringBuilder.toString()))) {
                Files.createDirectory(Paths.get(stringBuilder.toString()));
            }
        } catch (FileAlreadyExistsException e) {
            LOGGER.error("Folder already exists!" + "   " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            LOGGER.error("Some filesystem error!" + "   " + e.getMessage() + "\n  " + Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }
        if (!Files.exists(Paths.get(stringBuilder.toString() + matchedName))) {
            try {
                LOGGER.info("Getting image from \"" + record + "\"");
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
            } catch (IOException e) {
                Pattern keyPattern = Pattern.compile("(?:\\d*\\.)?\\d+");
                Matcher matcher = keyPattern.matcher(folderName);
                int key = Integer.valueOf(matcher.group());
                LOGGER.error("\nImage not saved properly, error! \"" + record + "\"      " + e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()) + "\n");
                WrongImage wrongImage = new WrongImage(key, record);
                MessagesService.CANCELED_IMAGES.add(wrongImage);
                e.printStackTrace();
            }
        } else {
            LOGGER.info("File already exists      " + matchedName + "");
        }
    }

    private ArrayList<String> findInJson(String source) {
        JSONObject jsonObject = new JSONObject(source);
        ArrayList<String> result = new ArrayList<>();
        if (jsonObject.getJSONObject("wall").isNull("attachments")) {
            return result;
        } else {
            JSONArray jsonArray = jsonObject.getJSONObject("wall").getJSONArray("attachments");
            for (Object object : jsonArray) {
                JSONObject tempObject = (JSONObject) object;
                if (!tempObject.isNull("photo")) {
                    JSONObject tempJsonObject = tempObject.getJSONObject("photo");
                    if (!tempJsonObject.isNull("src_xxxbig")) {
                        result.add(tempJsonObject.getString("src_xxxbig"));
                    } else {
                        if (!tempJsonObject.isNull("src_xxbig")) {
                            result.add(tempJsonObject.getString("src_xxbig"));
                        } else {
                            if (!tempJsonObject.isNull("src_xbig")) {
                                result.add(tempJsonObject.getString("src_xbig"));
                            } else {
                                if (!tempJsonObject.isNull("src_big")) {
                                    result.add(tempJsonObject.getString("src_big"));
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }
}
