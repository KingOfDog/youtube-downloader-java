package main;

import java.io.File;

public class FileSave {
	
	private static File filePath;

	public static File getFilePath() {
		return filePath;
	}

	public static void setFilePath(File filePath) {
		FileSave.filePath = filePath;
	}
	
}
