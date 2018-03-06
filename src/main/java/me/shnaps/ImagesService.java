package me.shnaps;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class ImagesService {

    private static final Logger LOGGER = Logger.getLogger(ImagesService.class);

    private ArrayList<WrongImage> canceledImages = new ArrayList<>();
    private String path = System.getProperty("user.dir") + File.separator + "pic";
    private ExecutorService executorService = Executors.newFixedThreadPool(10);
    private Collection<Callable<Boolean>> tasksList = new ArrayList<>();

    public List<WrongImage> getCanceledImages() {
        return canceledImages;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = this.path + path;
    }

    public boolean isDirExists() {
        return Paths.get(path).toFile().isDirectory();
    }

    public void createDesignationDir() {
        try {
            Files.createDirectory(Paths.get(path));
        } catch (IOException e) {
            LOGGER.error("Directory not created: " + e);
        }
    }

    public void findImagesLinks(String fileName) throws InterruptedException {
        try {
            LOGGER.info("Find images in text started");
            ClassLoader classLoader = getClass().getClassLoader();
            ParsingService parsingService = new ParsingService();
            URL resource = classLoader.getResource(fileName);
            String pathname = Objects.requireNonNull(resource).getFile().replaceAll("%20", " ");
            File file = new File(pathname);
            String fileAbsPath = file.getAbsolutePath();
            try (Stream<String> stringStream = Files.lines(Paths.get(fileAbsPath))) {
                stringStream.forEach(line ->
                        tasksList.add(() ->
                                parsingService.download(line)
                        )
                );
                executorService.invokeAll(tasksList);
            } catch (IOException e) {
                LOGGER.error("Can't create stream: " + e);
            }
            LOGGER.info("Trying to save canceled images");
            canceledImages.forEach(record ->
                    executorService.submit(() ->
                            parsingService.download(record.getRecord())
                    )
            );
        } finally {
            executorService.shutdown();
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
