package org.molgenis.emx2.cafevariome.post.jsonrequest.beans;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class MetaComponents {

	private MetaComponentsQueryIdentification queryIdentification;

	public MetaComponentsQueryIdentification getQueryIdentification() {
		return queryIdentification;
	}

	public void setQueryIdentification(MetaComponentsQueryIdentification queryIdentification) {
		this.queryIdentification = queryIdentification;
	}
}
