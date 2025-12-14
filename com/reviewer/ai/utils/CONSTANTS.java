//$Id$
package com.reviewer.ai.utils;

import java.util.ArrayList;
import java.util.List;

public class CONSTANTS {
	public static final String IAM_ENDPOINT = "https://accounts.zoho.com/oauth/v2/token";
	public static final String CLIENT_ID = "1000.TKSBOT9AY7QDOT7HE2IHAWNC6KEHRL";
	public static final String CLIENT_SECRET = "0e2840e74326b282eda7096d67880bab9251c8069d";
	public static final String REFRESH_TOKEN = "1000.050dfab3aaa2f3644635e791d0cd59ad.52d4c13d7560a8d493cb33cf2910deb9";
	public static final String GRANT_TYPE = "refresh_token";
	public static final String HEADER_AUTH = "Authorization";
	public static final String AUTH_TOKEN = "Zoho-oauthtoken ";
	public static final String AI_BASE_URL = "https://platformai.zoho.com/internalapi/v2/";
	public static final String AI_VENDOR = "zia"; // Vendor for AI Model
	public static final String AI_MODEL_NAME = "qwen-2.5-32b-instruct-zlabs"; // Model to Use
	public static final String POST = "POST";
	public static final String ORGID = "717979022"; // From Zoho Repository
	public static final String REPOID = "4000111772343"; // From Zoho Repository
	public static final String PLATFORM_ID = "Hacksaw"; // From Platform AI
	public static final List<String> BACKEND_FILES = new ArrayList<>(List.of("java"));
	public static final List<String> FRONTEND_FILES = new ArrayList<>(List.of("html", "js", "ts", "css", "scss", "jsp", "json"));
	
	public static final List<String> BACKEND_RULE_DOC = new ArrayList<>(List.of(
			"https://learn.zoho.com/portal/zohocorp/knowledge/manual/core-development/article/backend-code-design-guidelines", 
			"https://learn.zoho.com/portal/zohocorp/knowledge/manual/core-development/article/low-level-design-guidelines"
			));
	
	public static final List<String> FRONTEND_RULE_DOC = new ArrayList<>(List.of(
			"https://learn.zoho.com/portal/zohocorp/knowledge/manual/core-development/article/ui-design-guidelines"
			));

}
 