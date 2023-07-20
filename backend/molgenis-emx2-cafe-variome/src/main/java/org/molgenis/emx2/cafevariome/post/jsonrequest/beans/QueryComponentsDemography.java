package org.molgenis.emx2.cafevariome.post.jsonrequest.beans;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class QueryComponentsDemography {

	private String[] minAge;
	private String[] maxAge;
	private String[] affected;
	private String[] gender;
	private String[] family_type;

	public String[] getMinAge() {
		return minAge;
	}

	public void setMinAge(String[] minAge) {
		this.minAge = minAge;
	}

	public String[] getMaxAge() {
		return maxAge;
	}

	public void setMaxAge(String[] maxAge) {
		this.maxAge = maxAge;
	}

	public String[] getAffected() {
		return affected;
	}

	public void setAffected(String[] affected) {
		this.affected = affected;
	}

	public String[] getGender() {
		return gender;
	}

	public void setGender(String[] gender) {
		this.gender = gender;
	}

	public String[] getFamily_type() {
		return family_type;
	}

	public void setFamily_type(String[] family_type) {
		this.family_type = family_type;
	}
}
