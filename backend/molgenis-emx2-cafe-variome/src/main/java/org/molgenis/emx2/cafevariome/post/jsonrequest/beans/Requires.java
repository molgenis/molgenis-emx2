package org.molgenis.emx2.cafevariome.post.jsonrequest.beans;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import spark.Response;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Requires {

	private Response response;

	public Response getResponse() {
		return response;
	}

	public void setResponse(Response response) {
		this.response = response;
	}
}
