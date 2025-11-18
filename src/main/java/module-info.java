module org.example.ugplayer {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.net.http;


    opens org.example.ugplayer to javafx.fxml;
    exports org.example.ugplayer;
}