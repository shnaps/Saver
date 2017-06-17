import org.apache.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class Starter {
    private final static Logger LOGGER = Logger.getLogger(Starter.class);
    private String messagesFileName = "";


    public static void main(String[] args) {
        Starter starter = new Starter();
        starter.checkArgs(args);
        String fileName = starter.getMessagesFileName();
        starter.startApp(fileName);
    }

    private void checkArgs(String[] args) {
        if (args.length > 0) {
            setMessagesFileName(args[0]);
        } else {
            setMessagesFileName("test.txt");
        }
    }

    private void startApp(String fileName) {
        MessagesWorker messagesWorker = new MessagesWorker();
        if (!messagesWorker.isDirExists()) {
            messagesWorker.createDesignationDir();
        }
        messagesWorker.findImagesLinks(fileName);
    }

    public String getMessagesFileName() {
        return messagesFileName;
    }

    public void setMessagesFileName(String messagesFileName) {
        this.messagesFileName = messagesFileName;
    }
}
