//$Id$
package com.reviewer.ai.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CommonUtil {

	private static String accessToken = null;

	public static void generateAccessToken() {
		try {
			OkHttpClient client = new OkHttpClient();
			RequestBody body = new FormBody.Builder()
					.add("grant_type", CONSTANTS.GRANT_TYPE)
					.add("client_id", CONSTANTS.CLIENT_ID)
					.add("client_secret", CONSTANTS.CLIENT_SECRET)
					.add("refresh_token", CONSTANTS.REFRESH_TOKEN)
					.build();

			Request request = new Request.Builder()
					.url(CONSTANTS.IAM_ENDPOINT)
					.post(body)
					.build();

			try (Response response = client.newCall(request).execute()) {
				String responseBody = response.body().string();
				JSONObject json = new JSONObject(responseBody);
				if (json.has("access_token")) {
					accessToken = json.getString("access_token");
				} else {
					throw new RuntimeException("Failed to retrieve access token: " + responseBody);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Error generating access token", e);
		}
	}

	public static String getAccessToken() {
		return accessToken;
	}

	public static String getRequestBody(HttpServletRequest request) throws IOException {
		StringBuilder bodyBuilder = new StringBuilder();
		try (BufferedReader reader = request.getReader()) {
			String line;
			while ((line = reader.readLine()) != null) {
				bodyBuilder.append(line);
			}
		}
		return bodyBuilder.toString();
	}

	public static JSONObject extractJSON(String text) throws Exception {
		Pattern jsonPattern = Pattern.compile("\\{.*}", Pattern.DOTALL);
		Matcher matcher = jsonPattern.matcher(text);

		if (matcher.find()) {
			String jsonString = matcher.group();
			return new JSONObject(jsonString);
		}
		return new JSONObject();
	}

	public static String extractMRId(String url) {
	    Pattern pattern = Pattern.compile("/mergerequest/(\\d+)");
	    Matcher matcher = pattern.matcher(url);
	    if (matcher.find()) {
	        return matcher.group(1);
	    }
	    return null;
	}

	public static Map<String,String> extractFilePathsWithDiff(JSONArray diffs) throws Exception {
		Map<String,String> paths = new HashMap<>();
		for (int i = 0; i < diffs.length(); i++) {
			JSONObject file = diffs.getJSONObject(i);
			if(!file.getString("path").endsWith(".xml")) {
				paths.put(file.getString("path"), file.toString());
			}
		}
		return paths;
	}
	public static String getRepositoryEndpoint() {
		return "https://repository.zoho.com/orgs/"+CONSTANTS.ORGID+"/repos/"+CONSTANTS.REPOID+"/api/v1/";
	}

	public static boolean isBackendFile(String filePath) {
		String fileExtension = filePath.substring(filePath.lastIndexOf('.') + 1).toLowerCase();
		if (CONSTANTS.BACKEND_FILES.contains(fileExtension)) {
			return true;
		}
		return false;
	}

	public static boolean isFrontendFile(String filePath) {
		String fileExtension = filePath.substring(filePath.lastIndexOf('.') + 1).toLowerCase();
		if (CONSTANTS.FRONTEND_FILES.contains(fileExtension)) {
			return true;
		}
		return false;
	}
}
