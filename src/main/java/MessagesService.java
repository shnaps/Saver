import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

public class MessagesService {

    private final static Logger LOGGER = Logger.getLogger(MessagesService.class);

    private ExecutorService instance = Executors.newFixedThreadPool(20);

    private List<Future<Integer>> futuresList = new ArrayList<>();

    private Collection<Callable<Integer>> tasksList = new ArrayList<>();

    public static ArrayList<WrongImage> CANCELED_IMAGES = new ArrayList<>();

    public void setPath(String path) {
        this.PATH = this.PATH + path;
    }

    private static String PATH = "d:\\Needed soft\\pic ";

    private int count = 0;

    public String getPath() {
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
        try {
            LOGGER.info("Find images in text started");
            ClassLoader classLoader = getClass().getClassLoader();
            ParsingService parsingService = new ParsingService();
            File file = new File(classLoader.getResource(fileName).getFile());
            String fileAbsPath = file.getAbsolutePath();
            try (Stream<String> stringStream = Files.lines(Paths.get(fileAbsPath))) {
                stringStream.forEach(line -> {
                    tasksList.add(() -> {
                        countPlus();
                        parsingService.download(line, getCount());
                        return getCount();
                    });
                    /*instance.submit(() -> {
                        parsingService.download(line, this.count);
                        countPlus();
                    });*/
                });
                try {
                    futuresList = instance.invokeAll(tasksList);
                } catch (InterruptedException e) {
                    LOGGER.error("Something wrong with tasks, error! " + e.getMessage());
                    e.printStackTrace();
                }
            } catch (IOException e) {
                LOGGER.error("Cant create stream, error!");
                e.printStackTrace();
            }
            LOGGER.info("Trying to save canceled images");
            CANCELED_IMAGES.forEach(record -> {
                instance.submit(() -> {
                    parsingService.download(record.getRecord(),record.getKey());
                });
            });

        } finally {
            instance.shutdown();
            try {
                instance.awaitTermination(5, TimeUnit.SECONDS);
                if (instance.isTerminated()) {
                    LOGGER.info("Executor shutdowned");
                }
            } catch (InterruptedException e) {
                LOGGER.error("Tasks interrupted");
            }
        }
    }

    private int countPlus() {
        return count = count + 1;
    }

    public int getCount() {
        return count;
    }
}
