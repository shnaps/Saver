package me.shnaps;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParsingService {

    private static final Logger LOGGER = Logger.getLogger(ParsingService.class);

    private static final String URL_REGEX = "(\\s:\\s)((?:https://)(?:[a-zA-Z0-9]{2,10})(?:.userapi.com/).{10,40}(?:.jpg))";
    private static final String WALL_POST_REGEX = "(\"type\":\"wall\"?)";
    private static final String NAME_REGEX = "(/)([A-Za-z0-9-_]+)(.jpg)";
    private Pattern namePattern = Pattern.compile(NAME_REGEX);
    private Pattern urlPattern = Pattern.compile(URL_REGEX);
    private Pattern wallPostPattern = Pattern.compile(WALL_POST_REGEX);
    private Pattern keyPattern = Pattern.compile("(?:\\d*\\.)?\\d+");
    private URL url;
    private ImagesService imagesService = new ImagesService();

    boolean download(String messagesSource) {
        Matcher jsonMatcher = wallPostPattern.matcher(messagesSource);
        if (jsonMatcher.find()) {
            ArrayList<String> resultFromJson = findInJson(messagesSource);
            String folderName = getFolderName(messagesSource);
            resultFromJson.forEach(record ->
                    saveImage(record, folderName)
            );
        } else {
            Matcher lineMatcher = urlPattern.matcher(messagesSource);
            while (lineMatcher.find()) {
                saveImage(lineMatcher.group(), "");
            }
        }
        return true;
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
        LOGGER.info("Line matched: " + record);
        Matcher nameMatcher = namePattern.matcher(record);
        String matchedName = "";
        if (nameMatcher.find()) {
            matchedName = nameMatcher.group().replaceFirst("/", "");
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(imagesService.getPath());
        stringBuilder.append(File.separator);
        if (!folderName.equals("")) {
            stringBuilder.append(folderName + File.separator);
        }
        try {
            if (!Paths.get(stringBuilder.toString()).toFile().isDirectory()) {
                Files.createDirectory(Paths.get(stringBuilder.toString()));
            }
        } catch (FileAlreadyExistsException e) {
            LOGGER.error("Folder already exists: " + e);
        } catch (IOException e) {
            LOGGER.error("Filesystem error: " + e);
        }
        if (!Paths.get(stringBuilder.toString() + matchedName).toFile().exists()) {
            try {
                LOGGER.info("Getting image from \"" + record + "\"");
                url = new URL(record.replaceFirst(" : ", ""));
            } catch (MalformedURLException e) {
                LOGGER.error("Url not created for " + record + " : " + e);
            }
            try (ReadableByteChannel rb = Channels.newChannel(url.openStream());
                 FileOutputStream fos = new FileOutputStream(stringBuilder.toString() + matchedName)) {
                fos.getChannel().transferFrom(rb, 0, Long.MAX_VALUE);
                LOGGER.info("Saving image " + matchedName);
            } catch (IOException e) {
                Matcher matcher = keyPattern.matcher(folderName);
                int key = Integer.parseInt(matcher.group());
                LOGGER.error("\nImage not saved properly for " + record + " : " + e);
                WrongImage wrongImage = new WrongImage(key, record);
                imagesService.getCanceledImages().add(wrongImage);
            }
        } else {
            LOGGER.info("File already exists " + matchedName);
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
