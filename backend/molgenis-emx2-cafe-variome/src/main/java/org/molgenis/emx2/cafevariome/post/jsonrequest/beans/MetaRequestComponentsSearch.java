package org.molgenis.emx2.cafevariome.post.jsonrequest.beans;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class MetaRequestComponentsSearch {

	private String subjectVariant;
	private String eav;
	private String phenotype;
	private String queryIdentification;

	public String getSubjectVariant() {
		return subjectVariant;
	}

	public void setSubjectVariant(String subjectVariant) {
		this.subjectVariant = subjectVariant;
	}

	public String getEav() {
		return eav;
	}

	public void setEav(String eav) {
		this.eav = eav;
	}

	public String getPhenotype() {
		return phenotype;
	}

	public void setPhenotype(String phenotype) {
		this.phenotype = phenotype;
	}

	public String getQueryIdentification() {
		return queryIdentification;
	}

	public void setQueryIdentification(String queryIdentification) {
		this.queryIdentification = queryIdentification;
	}
}
