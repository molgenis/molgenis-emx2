package org.molgenis.emx2.cafevariome.post.jsonrequest.beans;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.Arrays;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class QueryComponents {

  private QueryComponentsSimilarity[] sim;
  private QueryComponentsOrdo[] ordo;
  private QueryComponentsReactome[] reactome;
  private QueryComponentsGene[] gene;
  private QueryComponentsDemography[] demography;

  public QueryComponentsSimilarity[] getSim() {
    return sim;
  }

  public void setSim(QueryComponentsSimilarity[] sim) {
    this.sim = sim;
  }

  public QueryComponentsOrdo[] getOrdo() {
    return ordo;
  }

  public void setOrdo(QueryComponentsOrdo[] ordo) {
    this.ordo = ordo;
  }

  public QueryComponentsReactome[] getReactome() {
    return reactome;
  }

  public void setReactome(QueryComponentsReactome[] reactome) {
    this.reactome = reactome;
  }

  public QueryComponentsGene[] getGene() {
    return gene;
  }

  public void setGene(QueryComponentsGene[] gene) {
    this.gene = gene;
  }

  public QueryComponentsDemography[] getDemography() {
    return demography;
  }

  public void setDemography(QueryComponentsDemography[] demography) {
    this.demography = demography;
  }

  @Override
  public String toString() {
    return "QueryComponents{"
        + "sim="
        + Arrays.toString(sim)
        + ", ordo="
        + Arrays.toString(ordo)
        + ", reactome="
        + Arrays.toString(reactome)
        + ", gene="
        + Arrays.toString(gene)
        + ", demography="
        + Arrays.toString(demography)
        + '}';
  }
}
