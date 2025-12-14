//$Id$
package com.reviewer.ai.utils;

import java.util.List;

import com.reviewer.ai.ZohoLearn.Handler.ZohoLearnHandler;

public class PromptUtil {
	private static final String BASIC_PROMPT = "You are my AI Code Reviewer Engine.\n"
			+ "\n"
			+ "TASK:\n"
			+ "You will receive code changes as a unified git diff.\n"
			+ "Analyze the diff strictly against specified custom coding rules and instructions provided.\n"
			+ "\n"
			+ "You must ONLY comment if a line clearly and critically violates the rules explicitly mentioned:\n"
			+ "The diff must be provided as a JSON object. It contains:\n"
			+ "\n"
			+ "path: the file path (you can ignore this).\n"
			+ "\n"
			+ "diffs: a list of diff blocks, each containing:\n"
			+ "\n"
			+ "diffLines: a list of strings, where each string is a line from the diff.\n"
			+ "\n"
			+ "diffPosition: the starting line number for this diff block (1-indexed).\n"
			+ "\n"
			+ "Each line's actual line number in the file is calculated as:\n"
			+ "\n"
			+ "ini\n"
			+ "Copy\n"
			+ "Edit\n"
			+ "line_number = diffPosition + index_in_diffLines\n"
			+ "Here's the diff input:\n"
			+ "\n"
			+ "json\n"
			+ "Copy\n"
			+ "Edit\n"
			+ "{\n"
			+ "  \"path\": \"source/com/zoho/security/reports/rules/models/Rule.java\",\n"
			+ "  \"diffs\": [\n"
			+ "    {\n"
			+ "      \"diffPosition\": 0,\n"
			+ "      \"diffLines\": [\n"
			+ "        \"//$Id$\",\n"
			+ "        \"package com.zoho.security.reports.rules.models;\",\n"
			+ "        \"\",\n"
			+ "        \"import java.util.ArrayList;\",\n"
			+ "        \"... more lines ...\"\n"
			+ "      ]\n"
			+ "    }\n"
			+ "  ]\n"
			+ "}\n";
			
		
			private static final String RESPONSE_SCHEMA = "MANDATORY JSON RESPONSE FORMAT:\n"
			+ "Your output MUST be strictly minified JSON compliant exactly to this schema:\n"
			+ "\n"
			+ "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"title\":\"Code Review Comments Schema\",\"description\":\"Schema to validate a list of file-specific comments for code reviews.\",\"type\":\"object\",\"properties\":{\"data\":{\"type\":\"array\",\"description\":\"List of files with associated comments.\",\"items\":{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\",\"description\":\"The relative file path of the file being reviewed.\"},\"comments\":{\"type\":\"array\",\"description\":\"List of comments associated with specific lines in the file.\",\"items\":{\"type\":\"object\",\"properties\":{\"line\":{\"type\":\"integer\",\"minimum\":1,\"description\":\"The exact line number from diff additions.\"},\"comment\":{\"type\":\"string\",\"description\":\"The review comment clearly explaining what's violated.\"},\"fileType\":{\"type\":\"string\",\"enum\":[\"old\",\"new\"],\"description\":\"Indicates old or new file version.\"},\"lineType\":{\"type\":\"string\",\"enum\":[\"old\",\"new\"],\"description\":\"Specifies line belongs to original or modified version.\"},\"lineContent\":{\"type\":\"string\",\"description\":\"Exact content of diff line commented on.\"},\"confidence\":{\"type\":\"integer\",\"minimum\":0,\"maximum\":10,\"description\":\"Confidence score (0-10) on how sure the comment is a true positive.\"}},\"required\":[\"line\",\"comment\",\"fileType\",\"lineType\",\"lineContent\",\"confidence\"],\"additionalProperties\":false}},\"required\":[\"path\",\"comments\"],\"additionalProperties\":false}}},\"required\":[\"data\"],\"additionalProperties\":false}\n"
			+ "\n";
			private static final String EXTRA_DETAILS =  "IMPORTANT:\n"
			+ "- Your review MUST ONLY highlight clear, critical violations strictly according to provided rules.\n"
			+ "- NEVER include explanatory notes outside the JSON format. NO conversational text allowed.\n"
			+ "- Comment only if there are actual Violations in the rules, don't comment because you have to provide an output. I would be happy If I find no Output rather than False Positives.\n"
			+ "- Respond ONLY in minified JSON exactly matching the schema or {\"data\":[]} if no critical issues found.";
			
    public static String getBackendPrompt() throws Exception {
    	List<String> ruleList = CONSTANTS.BACKEND_RULE_DOC;
		if (ruleList != null && !ruleList.isEmpty()) {
			ZohoLearnHandler zohoLearnHandler = new ZohoLearnHandler();
			String ruleString = "";
			for(String url : ruleList) {
				ruleString = ruleString + zohoLearnHandler.getTableContentFromDoc(url).toString() + "\n\n";
			}
			String RULE_PROMPT = "\n I will be Providing the Set of Backend Rules in the Format of List<List<String> which is from a Table. Use only the Rules Marked as Enabled. In the Comment Output should be \"RULEID - COMMENT\" \n"
					+ "Here are the Rules:\n"
					+ ruleString;
			System.out.println("Backend With Custom Rule");
			return BASIC_PROMPT + RULE_PROMPT + RESPONSE_SCHEMA + EXTRA_DETAILS;
		}
		return BASIC_PROMPT + RESPONSE_SCHEMA + EXTRA_DETAILS;
    }
    
    public static String getFalsePositiveReductionPrompt() {
    	return "[IMPORTANT] Remove if it is a False Positive, if it is Disabled, If you are Unsure if it is True Positive. I want only True Positives. If the Captured Violation is in the Exception Category Remove it.";
    }
    
    public static String getMinorFalsePositiveReductionPrompt() {
    	return "Remove False Positives if Any.";
    }
    
    public static String getFrontendPrompt() throws Exception {
    	List<String> ruleList = CONSTANTS.FRONTEND_RULE_DOC;
		if (ruleList != null && !ruleList.isEmpty()) {
			ZohoLearnHandler zohoLearnHandler = new ZohoLearnHandler();
			String ruleString = "";
			for(String url : ruleList) {
				ruleString = ruleString + zohoLearnHandler.getTableContentFromDoc(url).toString() + "\n\n";
			}
			String RULE_PROMPT = "\n I will be Providing the Set Of Frontend Rules in the Format of List<List<String> which is from a Table. \n [IMPORTANT]Use only the Rules Marked as Enabled. In the Comment Output should be \"RULEID - COMMENT\" \n"
					+ "Here are the Rules:\n"
					+ ruleString;
			return BASIC_PROMPT + RULE_PROMPT + RESPONSE_SCHEMA + EXTRA_DETAILS;
		}
		return BASIC_PROMPT + RESPONSE_SCHEMA + EXTRA_DETAILS;
    }
}