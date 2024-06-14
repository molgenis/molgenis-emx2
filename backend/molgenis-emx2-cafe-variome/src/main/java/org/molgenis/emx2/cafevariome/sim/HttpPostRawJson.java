package org.molgenis.emx2.cafevariome.sim;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpPostRawJson {

	public static String httpJsonRawPost(String url, String jsonBody) throws IOException {
		URL postURL = new URL(url);
		HttpURLConnection connection = (HttpURLConnection) postURL.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("Accept", "application/json");
		connection.setDoOutput(true);
		OutputStream outputStream = connection.getOutputStream();
		byte[] input = jsonBody.getBytes("utf-8");
		outputStream.write(input, 0, input.length);
		BufferedReader bufferedReader =
				new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
		StringBuilder response = new StringBuilder();
		String responseLine = null;
		while ((responseLine = bufferedReader.readLine()) != null) {
			response.append(responseLine.trim());
		}
		return response.toString();
	}
}
