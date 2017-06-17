import org.apache.log4j.Logger;

public class AppController {
    private final static Logger LOGGER = Logger.getLogger(AppController.class);
    private String messagesFileName = "";
    MessagesService messagesService = new MessagesService();


    public static void main(String[] args) {
        AppController appController = new AppController();
        appController.checkArgs(args);
        String fileName = appController.getMessagesFileName();
        appController.startApp(fileName);
    }

    private void checkArgs(String[] args) {
        if (args.length > 0) {
            setMessagesFileName(args[0]);
            messagesService.setPath(args[1]);
        } else {
            setMessagesFileName("test.txt");
            messagesService.setPath("test");
        }
    }

    private void startApp(String fileName) {
        if (!messagesService.isDirExists()) {
            messagesService.createDesignationDir();
        }
        messagesService.findImagesLinks(fileName);
    }

    public String getMessagesFileName() {
        return messagesFileName;
    }

    public void setMessagesFileName(String messagesFileName) {
        this.messagesFileName = messagesFileName;
    }
}
