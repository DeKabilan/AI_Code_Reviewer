//$Id$
package com.reviewer.ai.AIChat.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;

import com.reviewer.ai.utils.CONSTANTS;
import com.reviewer.ai.utils.CommonUtil;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AIChat {
	private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
			.connectTimeout(60, TimeUnit.SECONDS)
			.readTimeout(60, TimeUnit.SECONDS)
			.writeTimeout(60, TimeUnit.SECONDS)
			.build();

	private JSONObject history;
	private final List<String> messageHistory = new ArrayList<>();
	private String lastMessage;
	private final String endPoint;
	private final String model;
	private final String vendor;

	public AIChat(String endPoint, String vendor, String model) {
		this.endPoint = endPoint;
		this.vendor = vendor;
		this.model = model;
	}

	public List<String> getMessageHistory() {
		return messageHistory;
	}

	public String getLastMessage() {
		return lastMessage;
	}

	public String chat(String prompt) throws Exception {
		JSONObject requestBody;
		if (history == null) {
			requestBody = new JSONObject();
			JSONArray messages = new JSONArray();
			messages.put(createMessage("user", prompt));
			requestBody.put("model", model);
			requestBody.put("vendor", vendor);
			requestBody.put("messages", messages);
		} else {
			requestBody = new JSONObject(history.toString());
			JSONArray messages = requestBody.getJSONArray("messages");
			messages.put(createMessage("user", prompt));
		}

		Request request = new Request.Builder()
				.url(endPoint + "ai/chat")
				.post(RequestBody.create(
						MediaType.parse("application/json"),
						requestBody.toString()
				))
				.addHeader("Authorization", "Zoho-oauthtoken " + CommonUtil.getAccessToken())
				.addHeader("portal_id", CONSTANTS.PLATFORM_ID)
				.build();

		try (Response response = CLIENT.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				throw new RuntimeException("Unexpected response: " + response);
			}
			String responseBody = response.body().string();
			JSONObject responseJson = new JSONObject(responseBody);
			String assistantMessage = responseJson
					.getJSONObject("data")
					.getJSONArray("results")
					.getString(0);

			JSONArray messagesList = requestBody.getJSONArray("messages");
			messagesList.put(createMessage("assistant", assistantMessage));
			requestBody.put("messages", messagesList);
			this.messageHistory.add(assistantMessage);
			this.lastMessage = assistantMessage;
			this.history = requestBody;
			return this.lastMessage;
		}
	}

	private JSONObject createMessage(String role, String content) {
		try {
			JSONObject message = new JSONObject();
			message.put("role", role);
			message.put("content", content);
			return message;
		} catch (org.json.JSONException e) {
			throw new RuntimeException("Failed to create message JSON", e);
		}
	}
}
