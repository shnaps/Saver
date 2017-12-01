package me.shnaps;

import org.apache.log4j.Logger;

public class AppStarter {
    private static final Logger LOGGER = Logger.getLogger(AppStarter.class);

    private ImagesService imagesService = new ImagesService();
    private String messagesFileName = "";

    public static void main(String[] args) {
        AppStarter appStarter = new AppStarter();
        appStarter.checkArgs(args);
        String fileName = appStarter.getMessagesFileName();
        appStarter.startApp(fileName);
    }

    private void checkArgs(String[] args) {
        if (args.length > 0) {
            setMessagesFileName(args[0]);
            imagesService.setPath(args[1]);
        } else {
            setMessagesFileName("test.txt");
            imagesService.setPath("test");
        }
    }

    private void startApp(String fileName) {
        if (!imagesService.isDirExists()) {
            imagesService.createDesignationDir();
        }
        try {
            imagesService.findImagesLinks(fileName);
        } catch (InterruptedException e) {
            LOGGER.error("Images saving process interrupted: " + e);
        }
    }

    private String getMessagesFileName() {
        return messagesFileName;
    }

    private void setMessagesFileName(String messagesFileName) {
        this.messagesFileName = messagesFileName;
    }
}
