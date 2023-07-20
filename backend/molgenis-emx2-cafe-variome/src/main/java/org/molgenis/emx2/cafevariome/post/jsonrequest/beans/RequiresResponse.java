package org.molgenis.emx2.cafevariome.post.jsonrequest.beans;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class RequiresResponse {

	private RequiresResponseComponents components;

	public RequiresResponseComponents getComponents() {
		return components;
	}

	public void setComponents(RequiresResponseComponents components) {
		this.components = components;
	}
}
