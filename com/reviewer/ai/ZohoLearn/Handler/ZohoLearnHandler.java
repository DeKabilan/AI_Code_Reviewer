//$Id$
package com.reviewer.ai.ZohoLearn.Handler;

import java.util.List;

import org.json.JSONObject;

import com.reviewer.ai.utils.CONSTANTS;
import com.reviewer.ai.utils.CommonUtil;
import com.reviewer.ai.utils.HTMLParser;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ZohoLearnHandler {
	
	public String fetchLearnDocContent(String link) throws Exception {
		OkHttpClient client = new OkHttpClient();

        String url = getAPILink(link);

        Request request = new Request.Builder()
                .url(url)
                .addHeader(CONSTANTS.HEADER_AUTH, CONSTANTS.AUTH_TOKEN + CommonUtil.getAccessToken())
                .build();

        Response response = client.newCall(request).execute();
        JSONObject responseObj = new JSONObject(response.body().string());
        return responseObj.getJSONObject("article").getString("content");
	}
	
	
	public List<List<String>> getTableContentFromDoc(String url) throws Exception{
		String htmlContent = fetchLearnDocContent(url);
		return HTMLParser.extractLearnTableData(htmlContent);
	}
	
	
	private String getAPILink(String learnDocLink) {
		 if (learnDocLink == null || !learnDocLink.startsWith("https://learn.zoho.com/portal/")) {
	            throw new IllegalArgumentException("Invalid Learn article URL");
	        }
	        String path = learnDocLink.replace("https://learn.zoho.com/portal/", "");
	        String apiBase = "https://learn.zoho.com/learn/api/v1/portal/";
	        path = path.replace("/knowledge", "");
	        if (!path.endsWith("/info")) {
	            path += "/info";
	        }
	        return apiBase + path;
	}
}
