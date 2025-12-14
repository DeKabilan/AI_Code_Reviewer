//$Id$
package com.reviewer.ai.ZohoRepository.model;

import org.json.JSONObject;

import com.reviewer.ai.utils.CommonUtil;

public class ZohoRepository {
	Long MRId = null;
	String source = null;
	String target = null;
	Long sqNumber = null;
	
	public ZohoRepository(JSONObject webHookData) throws Exception {
		this.MRId = webHookData.getLong("mergeRequestId");
		this.sqNumber = webHookData.getLong("mrSequentialNumber");
    }
	
	
	public Long getSequentialNumber() {
		return sqNumber;
	}
	
	public String getDiffEndpoint() {
		return CommonUtil.getRepositoryEndpoint() + "mergerequests/"+MRId+"/diff";
	}
	
	public String getCommentEndpoint() {
		return CommonUtil.getRepositoryEndpoint()+"mergerequest"+"/"+MRId+"/comment";
	}
	
	public String getFetchCommentsEndpoint() {
		return CommonUtil.getRepositoryEndpoint()+"mergerequests"+"/"+MRId+"/comments";
	}
}
