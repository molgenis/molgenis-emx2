package org.molgenis.emx2.cafevariome.post.jsonrequest.beans;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class QueryComponentsGene {

	private String gene_id;
	private String[] protein_effect;
	private String af;

	public String getGene_id() {
		return gene_id;
	}

	public void setGene_id(String gene_id) {
		this.gene_id = gene_id;
	}

	public String[] getProtein_effect() {
		return protein_effect;
	}

	public void setProtein_effect(String[] protein_effect) {
		this.protein_effect = protein_effect;
	}

	public String getAf() {
		return af;
	}

	public void setAf(String af) {
		this.af = af;
	}
}
