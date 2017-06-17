import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Created by shnaps on 16.06.2017.
 */
public class MessagesWorker {

    private final static Logger LOGGER = Logger.getLogger(MessagesWorker.class);

    private ExecutorService instance = Executors.newFixedThreadPool(15);

    private final String PATH = "d:\\Needed soft\\pic history";
    private int count = 1;
    //    private final String PATH = System.getProperty("user.dir") + File.separator + "pic";

    public String getPATH() {
        return PATH;
    }

    public ExecutorService getInstance() {
        return instance;
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
        } catch (IOException e) {
            LOGGER.error("Cant create stream, error!");
            e.printStackTrace();
        } finally {
            try {
                imageDownloader.canceledImages.forEach(record -> {
                    instance.submit(() -> {
                        imageDownloader.download(record, this.count);
                    });
                });

                LOGGER.info("Attempt to shutdown executor");
                instance.shutdown();
                instance.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LOGGER.error("Tasks interrupted");
            }
        }
    }

    private void countPlus() {
        this.count = this.count + 1;
    }


}
