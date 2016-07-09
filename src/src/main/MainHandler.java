package main;

import java.io.File;
import java.io.IOException;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.FileChooser.ExtensionFilter;

public class MainHandler {

	public static void setPath(Stage stage, Scene scene) {
		Label file = (Label) scene.lookup("#file");

		DirectoryChooser dc = new DirectoryChooser();
		dc.setTitle("Wo soll die Datei gespeichert werden?");
		File defaultPath = new File(System.getProperty("user.home") + "\\Videos\\");
		dc.setInitialDirectory(defaultPath);
		File path = dc.showDialog(stage);
		
		if(path != null) {
			file.setText("Ordner: " + path);
		}
		
		FileSave.setFilePath(path);
	}
	
	public static void download(Scene scene) throws IOException {
		TextField videoID = (TextField) scene.lookup("#videoID");
		String id = videoID.getText();
		
		if(id.isEmpty()) {
			videoID.setStyle("-fx-outer-border: #e74c3c");
		} else {
			File file = FileSave.getFilePath();
			
			if(file == null) {
				Button btn = (Button) scene.lookup("#fileChooser");
				btn.setStyle("-fx-outer-border: #e74c3c;"
						+ "-fx-background-color:"
						+ "-fx-outer-border,"
						+ "-fx-body-color;"
						+ "-fx-background-insets: 0, 2;"
						+ "-fx-background-radius: 0px, 0px;");
			} else {
				Downloader.get("http://youtube.com/watch?v=" + id, file, scene);;
			}
		}
	}
	
}
