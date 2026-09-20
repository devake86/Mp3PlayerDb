package app.mp3playerdb;

import javafx.application.Application;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {

    /*
        APPLICATION RESOURCES
     */

    private static final String FONT_JETBRAINS_MONO_BOLD = "/font/jetbrainsmono_bold.ttf";
    private static final String FONT_JETBRAINS_MONO_BOLD_ITALIC = "/font/jetbrainsmono_bolditalic.ttf";
    private static final String FONT_JETBRAINS_MONO_ITALIC = "/font/jetbrainsmono_italic.ttf";
    private static final String FONT_JETBRAINS_MONO_REGULAR = "/font/jetbrainsmono_regular.ttf";
    private static final int FONT_LOAD_SIZE = 14;

    private static final String MAIN_VIEW_FXML = "/fxml/main-view.fxml";

    /*
        APPLICATION SETTINGS
     */

    private static final int APP_WIDTH = 1440;
    private static final int APP_HEIGHT = 810;

    private static final String APP_TITLE = "Mp3PlayerDb";

    /*
        APPLICATION LIFECYCLE
     */

    @Override
    public void start(Stage stage) throws IOException {
        Font.loadFont(getClass().getResourceAsStream(FONT_JETBRAINS_MONO_BOLD), FONT_LOAD_SIZE);
        Font.loadFont(getClass().getResourceAsStream(FONT_JETBRAINS_MONO_BOLD_ITALIC), FONT_LOAD_SIZE);
        Font.loadFont(getClass().getResourceAsStream(FONT_JETBRAINS_MONO_ITALIC), FONT_LOAD_SIZE);
        Font.loadFont(getClass().getResourceAsStream(FONT_JETBRAINS_MONO_REGULAR), FONT_LOAD_SIZE);

        FXMLLoader mainLoader = new FXMLLoader(MainApp.class.getResource(MAIN_VIEW_FXML));

        Scene mainScene = new Scene(mainLoader.load(), APP_WIDTH, APP_HEIGHT);

        stage.setTitle(APP_TITLE);
        stage.setScene(mainScene);

        stage.setResizable(false);

        stage.show();
    }

    /*
        APPLICATION ENTRY POINT
     */

    public static void main(String[] args) {
        launch(args);
    }
}
