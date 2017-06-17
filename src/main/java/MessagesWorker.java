import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class MessagesWorker {

    private final static Logger LOGGER = Logger.getLogger(MessagesWorker.class);

    private ExecutorService instance = Executors.newFixedThreadPool(15);

    private ArrayList<String> canceledImages = new ArrayList<>();

    private final String PATH = "d:\\Needed soft\\pic test";

    private int count = 1;

    public String getPATH() {
        return PATH;
    }

    public boolean isDirExists() {
        return Files.isDirectory(Paths.get(PATH));
    }

    public void createDesignationDir() {
        try {
            Files.createDirectory(Paths.get(PATH));
        } catch (IOException e) {
            LOGGER.error("Directory not created, error!");
            e.printStackTrace();
        }
    }

    public void findImagesLinks(String fileName) {
        canceledImages.add("https://pp.userapi.com/c413527/v413527378/8592/B2Da4pbAKHM.jpg");
        ClassLoader classLoader = getClass().getClassLoader();
        ImageDownloader imageDownloader = new ImageDownloader();
        File file = new File(classLoader.getResource(fileName).getFile());
        String fileAbsPath = file.getAbsolutePath();
        try (Stream<String> stringStream = Files.lines(Paths.get(fileAbsPath))) {
            stringStream.forEach(line -> {
                instance.submit(() -> {
                            imageDownloader.download(line, this.count);
                            countPlus();
                        }
                );
            });
            this.canceledImages.forEach(record -> {
                instance.submit(() -> {
                    LOGGER.info("Attempt to load canceled images!");
                    imageDownloader.download(record, this.count);
                    LOGGER.info("Images saved!");
                });
            });
        } catch (IOException e) {
            LOGGER.error("Cant create stream, error!");
            e.printStackTrace();
        } finally {
            try {
                LOGGER.info("Attempt to shutdown executor");
                instance.shutdown();
                instance.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LOGGER.error("Tasks interrupted");
            }
        }

    }

    public ArrayList<String> getCanceledImages() {
        return canceledImages;
    }

    private void countPlus() {
        this.count = this.count + 1;
    }


}
