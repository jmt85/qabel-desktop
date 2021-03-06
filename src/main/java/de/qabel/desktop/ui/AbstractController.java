package de.qabel.desktop.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.qabel.core.config.*;
import de.qabel.core.crypto.QblECPublicKey;
import de.qabel.core.drop.DropMessage;
import de.qabel.core.drop.DropURL;
import de.qabel.core.exceptions.QblDropInvalidURL;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.apache.log4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Function;

public class AbstractController {
	protected Alert alert;
	protected Label exceptionLabel;
	protected Gson gson;

	protected void alert(Exception e) {
		alert(e.getMessage(), e);
	}

	protected void tryOrAlert(CheckedRunnable runnable) {
		try {
			runnable.run();
		} catch (Exception e) {
			alert(e);
		}
	}

	protected String getString(ResourceBundle resources, String message, Object... params) {
		return MessageFormat.format(resources.getString(message), params);
	}

	@FunctionalInterface
	public interface CheckedRunnable {
		void run() throws Exception;
	}

	protected void alert(String message, Exception e) {
		LoggerFactory.getLogger(getClass().getSimpleName()).error(message, e);
		e.printStackTrace();

		alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle("Error");
		alert.setHeaderText(message);

		TextArea textArea = new TextArea(getTraceAsString(e));
		VBox.setMargin(textArea, new Insets(10, 0, 5, 0));
		textArea.setEditable(false);
		textArea.setWrapText(false);

		VBox.setVgrow(textArea, Priority.ALWAYS);

		exceptionLabel = new Label(e.getMessage());
		VBox expansion = new VBox();
		expansion.getChildren().add(exceptionLabel);
		expansion.getChildren().add(textArea);

		alert.getDialogPane().setContent(expansion);
		alert.setResizable(true);
		alert.showAndWait();
	}

	private String getTraceAsString(Exception e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		return sw.toString();
	}

	protected void writeStringInFile(String json, File dir) throws IOException {
		File targetFile = new File(dir.getPath());
		targetFile.createNewFile();
		OutputStream outStream = new FileOutputStream(targetFile);
		outStream.write(json.getBytes());
	}

	public String readFile(File f) throws IOException {
		FileReader fileReader = new FileReader(f);
		BufferedReader br = new BufferedReader(fileReader);

		try {
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();

			while (line != null) {
				sb.append(line);
				line = br.readLine();
				if (line != null) {
					sb.append("\n");
				}
			}
			return sb.toString();
		} finally {
			br.close();
		}
	}


	private ArrayList<DropURL> generateDropURLs(List<String> drops) throws URISyntaxException, QblDropInvalidURL {
		ArrayList<DropURL> collection = new ArrayList<>();

		for (String uri : drops) {
			DropURL dropURL = new DropURL(uri);

			collection.add(dropURL);
		}
		return collection;
	}

}
