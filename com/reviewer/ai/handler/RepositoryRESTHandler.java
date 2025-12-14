//$Id$
package com.reviewer.ai.handler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import com.github.difflib.unifieddiff.UnifiedDiff;
import com.github.difflib.unifieddiff.UnifiedDiffFile;
import com.github.difflib.unifieddiff.UnifiedDiffReader;
import com.reviewer.ai.AIChat.model.AIChat;
import com.reviewer.ai.ZohoRepository.model.ZohoRepository;
import com.reviewer.ai.utils.CONSTANTS;
import com.reviewer.ai.utils.CommonUtil;
import com.reviewer.ai.utils.PromptUtil;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;



public class RepositoryRESTHandler {

	private static final MediaType JSON = MediaType.parse("application/json");
	private static final String HEADER_EMAIL = "X-ZR-Email";
	private static final String EMAIL = "YOUR_EMAIL_HERE"; // Replace with actual email or fetch from config

	private ZohoRepository repository = null;
	private final OkHttpClient client = new OkHttpClient();

	public RepositoryRESTHandler() {
		this.repository = null;
	}
	
	public RepositoryRESTHandler(ZohoRepository repository) {
		this.repository = repository;
	}

	public JSONObject getMRDetailsUsingId(String mrId) throws Exception {
		String endpoint = CommonUtil.getRepositoryEndpoint()+ "mergerequest/" + mrId;
		HttpUrl url = HttpUrl.parse(endpoint).newBuilder().build();
		return executeGetRequest(url);
	}
	public JSONObject getMRDiff() throws Exception {
		HttpUrl url = HttpUrl.parse(repository.getDiffEndpoint())
			.newBuilder().addQueryParameter("page", "1").build();
		return executeGetRequest(url);
	}
	
	public JSONObject getMRFileDiff(String path) throws Exception {
		JSONObject fullDiff = this.getMRDiff();
		JSONArray diffArray = fullDiff.getJSONArray("diffs");
		for (int i = 0; i < diffArray.length(); i++) {
			JSONObject diffObj = diffArray.getJSONObject(i);
			if (diffObj.getString("path").equals(path)) {
				return diffObj;
			}
		}
		return null;
	}

	public String getVersionID() throws Exception {
		String endpoint = CommonUtil.getRepositoryEndpoint() + "mergerequest/" +
			repository.getSequentialNumber() + "/versions";
		HttpUrl url = HttpUrl.parse(endpoint).newBuilder().build();

		JSONObject response = executeGetRequest(url);
		JSONObject versionObj = response.getJSONArray("mrVersions").getJSONObject(0);
		return String.valueOf(versionObj.getLong("id"));
	}
	
	
	public JSONObject getPostedComment() throws Exception {
		String endpoint = repository.getFetchCommentsEndpoint();
        HttpUrl url = HttpUrl.parse(endpoint).newBuilder().build();
        JSONObject input =  this.executeGetRequest(url);
        JSONArray comments = input.getJSONArray("comments");

        // Group by filePath
        Map<String, JSONArray> fileLinesMap = new HashMap<>();

        for (int i = 0; i < comments.length(); i++) {
            JSONObject comment = comments.getJSONObject(i);

            String filePath = comment.optString("filePath", "");
            int lineNo = comment.optInt("newLineNo", -1);
            String lineContent = comment.optString("lineContent", "");

            if (!filePath.isEmpty() && lineNo > 0 && !lineContent.isEmpty()) {
                JSONArray linesArray = fileLinesMap.getOrDefault(filePath, new JSONArray());

                JSONObject lineObj = new JSONObject();
                lineObj.put("lineNo", lineNo);
                lineObj.put("lineContent", lineContent);

                linesArray.put(lineObj);
                fileLinesMap.put(filePath, linesArray);
            }
        }

        JSONArray dataArray = new JSONArray();
        for (Map.Entry<String, JSONArray> entry : fileLinesMap.entrySet()) {
            JSONObject fileObj = new JSONObject();
            fileObj.put("filePath", entry.getKey());
            fileObj.put("lines", entry.getValue());
            dataArray.put(fileObj);
        }

        JSONObject output = new JSONObject();
        output.put("data", dataArray);
        return output;
	}
	
	public void postComment(JSONObject aiResponse, String diffIndex) throws Exception {
		if (!aiResponse.has("data")) return;

		JSONArray data = aiResponse.getJSONArray("data");
		for (int i = 0; i < data.length(); i++) {
			JSONObject fileObj = data.getJSONObject(i);
			String path = fileObj.getString("path");
			JSONArray comments = fileObj.getJSONArray("comments");

			for (int j = 0; j < comments.length(); j++) {
				JSONObject commentObj = comments.getJSONObject(j);
				postMRComment(
					diffIndex,
					path,
					commentObj.getString("fileType"),
					commentObj.getString("lineType"),
					commentObj.getInt("line"),
					commentObj.getString("lineContent"),
					commentObj.getString("comment")
				);
			}
		}
	}

	public void postMRComment(String diffIndex, String path, String fileType, String lineType,
					  Integer lineNumber, String lineContent, String commentText) throws Exception {
		String versionId = getVersionID();
		String endpoint = repository.getCommentEndpoint();

		JSONObject params = new JSONObject()
			.put("note", commentText)
			.put("filePath", path)
			.put("noteType", "diff_note")
			.put("lineType", lineType)
			.put("lineContent", lineContent)
			.put("newLine", lineNumber)
			.put("status", "open")
			.put("diffIndex", diffIndex)
			.put("versionId", versionId);

		JSONObject comment = new JSONObject().put("comment", params);

		HttpUrl url = HttpUrl.parse(endpoint).newBuilder().build();
		RequestBody body = RequestBody.create(JSON, comment.toString());

		Request request = new Request.Builder()
			.url(url)
			.post(body)
			.addHeader(CONSTANTS.HEADER_AUTH, CONSTANTS.AUTH_TOKEN + CommonUtil.getAccessToken())
			.addHeader(HEADER_EMAIL, EMAIL)
			.build();

		try (Response response = client.newCall(request).execute()) {
			JSONObject obj = new JSONObject(response.body().string());
		}
	}

	public JSONObject getFileDiff(String path, String source, String target) throws Exception {
		String endpoint = CommonUtil.getRepositoryEndpoint() + "diff";
		HttpUrl url = HttpUrl.parse(endpoint).newBuilder()
			.addQueryParameter("page", "1")
			.addQueryParameter("path", path)
			.addQueryParameter("source", source)
			.addQueryParameter("target", target)
			.addQueryParameter("fullDiff", "true")
			.build();

		return executeGetRequest(url);
	}

	private JSONObject executeGetRequest(HttpUrl url) throws Exception {
		Request request = new Request.Builder()
			.url(url)
			.addHeader(CONSTANTS.HEADER_AUTH, CONSTANTS.AUTH_TOKEN + CommonUtil.getAccessToken())
			.addHeader(HEADER_EMAIL, EMAIL)
			.build();

		try (Response response = client.newCall(request).execute()) {
			String responseString = response.body().string();
			return CommonUtil.extractJSON(responseString);
		}
	}
	
	public void processFileAndPostComment(AIChat bot, String path, String source, String target) throws Exception {
		JSONObject fileDiff = this.getFileDiff(path, source, target);
		JSONArray diffArray = fileDiff.getJSONArray("diffs");

		if (diffArray.length() == 0) return;

		JSONObject diffObj = diffArray.getJSONObject(0);
		JSONArray diffs = new JSONArray();
		String diffString = diffObj.getString("diff");
		InputStream stream = new ByteArrayInputStream(diffString.getBytes(StandardCharsets.UTF_8));
		UnifiedDiff unifiedDiff = UnifiedDiffReader.parseUnifiedDiff(stream);
		List<UnifiedDiffFile> unifiedFiles = unifiedDiff.getFiles();
		for (UnifiedDiffFile unifiedFile : unifiedFiles) {
			Patch<String> patch = unifiedFile.getPatch();
			for(AbstractDelta<String> delta : patch.getDeltas()) {
				JSONObject diff = new JSONObject();
				diff.put("diffLines", delta.getTarget().getLines());
				diff.put("diffPosition", delta.getTarget().getPosition());
				diffs.put(diff);
			}
		}
		JSONObject diffContent = new JSONObject()
			.put("path", diffObj.getString("path"))
			.put("diffs", diffs);
		bot.chat(diffContent.toString());
		bot.chat(PromptUtil.getFalsePositiveReductionPrompt());
		JSONObject reviewedComments = CommonUtil.extractJSON(bot.getLastMessage());
		JSONObject result = new JSONObject();
		JSONArray resultData = new JSONArray();
		JSONObject resultComment = new JSONObject();
		JSONArray resultCommentArray = new JSONArray();
		if (!reviewedComments.has("data") || reviewedComments.getJSONArray("data").length() == 0) {
			return;
		}
		JSONArray comments = reviewedComments.getJSONArray("data").getJSONObject(0).getJSONArray("comments");
		for (int i = 0; i < comments.length(); i++) {
			JSONObject commentObj = comments.getJSONObject(i);
			String lineContent = commentObj.getString("lineContent");
			for (int j = 0; j < diffContent.getJSONArray("diffs").length(); j++) {
				JSONObject lineObj = diffContent.getJSONArray("diffs").getJSONObject(j);
				Integer diffStart = lineObj.getInt("diffPosition");
				JSONArray diffLines = lineObj.getJSONArray("diffLines");
				for (int lineNo = 0; lineNo < diffLines.length(); lineNo++) {
					String diffLine = diffLines.getString(lineNo);
					if(lineContent!=null && lineContent.equals(diffLine)) {
						Integer actualLineNumber = lineNo + diffStart + 1;
						commentObj.put("line", actualLineNumber);
						//check if already commented
						JSONArray postedComments = this.getPostedComment().getJSONArray("data");
						Boolean isExist = false;
						for (int k = 0; k < postedComments.length(); k++) {
							JSONObject postedComment = postedComments.getJSONObject(k);
							if (postedComment.getString("filePath").equals(reviewedComments.getJSONArray("data").getJSONObject(0).getString("path"))){
								JSONArray lines = postedComment.getJSONArray("lines");
								for (int l = 0; l < lines.length(); l++) {
									JSONObject lineObj1 = lines.getJSONObject(l);
									if (lineObj1.getString("lineContent")!=null && lineObj1.getString("lineContent").equals(commentObj.getString("lineContent").trim())) {
										isExist = true;
									}
								}
							}
						}
						if(!isExist) {
							resultCommentArray.put(commentObj);
							break;
						}
					}
				}
			}
		}
		resultComment.put("comments", resultCommentArray);
		resultComment.put("path", reviewedComments.getJSONArray("data").getJSONObject(0).getString("path"));
		resultData.put(resultComment);
		result.put("data", resultData);
		this.postComment(result, diffObj.getString("diffIndex"));
	}
	
	public void processSnippetAndPostComment(AIChat bot, String path, String source, String target) throws Exception {
		JSONObject diffObj = this.getMRFileDiff(path);
		JSONArray diffs = new JSONArray();
		String diffString = diffObj.getString("diff");
		InputStream stream = new ByteArrayInputStream(diffString.getBytes(StandardCharsets.UTF_8));
		UnifiedDiff unifiedDiff = UnifiedDiffReader.parseUnifiedDiff(stream);
		List<UnifiedDiffFile> unifiedFiles = unifiedDiff.getFiles();
		for (UnifiedDiffFile unifiedFile : unifiedFiles) {
			Patch<String> patch = unifiedFile.getPatch();
			for(AbstractDelta<String> delta : patch.getDeltas()) {
				JSONObject diffArray = new JSONObject();
				diffArray.put("diffLines", delta.getTarget().getLines());
				diffArray.put("diffPosition", delta.getTarget().getPosition());
				diffs.put(diffArray);
			}
		}
		JSONObject diffContent = new JSONObject()
			.put("path", diffObj.getString("path"))
			.put("diffs", diffs);
		bot.chat(diffContent.toString());
		bot.chat(PromptUtil.getFalsePositiveReductionPrompt());
		JSONObject reviewedComments = CommonUtil.extractJSON(bot.getLastMessage());
		JSONObject result = new JSONObject();
		JSONArray resultData = new JSONArray();
		JSONObject resultComment = new JSONObject();
		JSONArray resultCommentArray = new JSONArray();
		if (!reviewedComments.has("data") || reviewedComments.getJSONArray("data").length() == 0) {
			return;
		}
		JSONArray comments = reviewedComments.getJSONArray("data").getJSONObject(0).getJSONArray("comments");
		for (int i = 0; i < comments.length(); i++) {
			JSONObject commentObj = comments.getJSONObject(i);
			String lineContent = commentObj.getString("lineContent");
			for (int j = 0; j < diffContent.getJSONArray("diffs").length(); j++) {
				JSONObject lineObj = diffContent.getJSONArray("diffs").getJSONObject(j);
				Integer diffStart = lineObj.getInt("diffPosition");
				JSONArray diffLines = lineObj.getJSONArray("diffLines");
				for (int lineNo = 0; lineNo < diffLines.length(); lineNo++) {
					String diffLine = diffLines.getString(lineNo);
					if(lineContent!=null && lineContent.equals(diffLine)) {
						Integer actualLineNumber = lineNo + diffStart + 1;
						commentObj.put("line", actualLineNumber);
						//check if already commented
						JSONArray postedComments = this.getPostedComment().getJSONArray("data");
						Boolean isExist = false;
						for (int k = 0; k < postedComments.length(); k++) {
							JSONObject postedComment = postedComments.getJSONObject(k);
							if (postedComment.getString("filePath").equals(reviewedComments.getJSONArray("data").getJSONObject(0).getString("path"))){
								JSONArray lines = postedComment.getJSONArray("lines");
								for (int l = 0; l < lines.length(); l++) {
									JSONObject lineObj1 = lines.getJSONObject(l);
									if (lineObj1.getString("lineContent")!=null && lineObj1.getString("lineContent").equals(commentObj.getString("lineContent").trim())) {
										isExist = true;
									}
								}
							}
						}
						if(!isExist) {
							resultCommentArray.put(commentObj);
							break;
						}
					}
				}
			}
		}
		resultComment.put("comments", resultCommentArray);
		resultComment.put("path", reviewedComments.getJSONArray("data").getJSONObject(0).getString("path"));
		resultData.put(resultComment);
		result.put("data", resultData);
		this.postComment(result, diffObj.getString("diffIndex"));
		}
}
