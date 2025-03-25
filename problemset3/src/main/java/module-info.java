module com.garynation {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    
    opens com.garynation to javafx.fxml;
    exports com.garynation;
}
