package org.molgenis.emx2.cafevariome.post.jsonrequest.beans;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Logic {

	@JsonProperty("-AND")
	private Object[] AND;

	@JsonProperty("-OR")
	private Object[] OR;

	public Object[] getAND() {
		return AND;
	}

	public void setAND(Object[] AND) {
		this.AND = AND;
	}

	public Object[] getOR() {
		return OR;
	}

	public void setOR(Object[] OR) {
		this.OR = OR;
	}
}
