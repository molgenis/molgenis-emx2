package org.molgenis.emx2.cafevariome.post.request.gql;

import static org.molgenis.emx2.beaconv2.endpoints.QueryHelper.finalizeFilter;
import static org.molgenis.emx2.beaconv2.endpoints.individuals.ejp_rd_vp.EJP_VP_IndividualsQuery.valueArrayFilterBuilder;

import java.util.ArrayList;
import java.util.List;

import org.molgenis.emx2.cafevariome.post.request.query.DemographyQuery;
import org.molgenis.emx2.cafevariome.post.request.query.GeneReactomeQuery;
import org.molgenis.emx2.cafevariome.post.request.query.HPOQuery;
import org.molgenis.emx2.cafevariome.post.request.query.ORDOQuery;

public class Filters {

  public static List<String> makeHPOFilter(HPOQuery hpoQuery) throws Exception {
    List<String> filters = new ArrayList<>();
    for (String hpoTerm : hpoQuery.getSearchTerms()) {
      if (hpoTerm.startsWith("HP:")) {
        hpoTerm = hpoTerm.replace("HP:", "HP_");
      } else {
        System.out.println("hpoTerm=" + hpoTerm);
        throw new Exception("Expected input HPO term to start with 'HP:'");
      }
      String filter =
          "{phenotypicFeatures: { featureType: { ontologyTermURI: {like:\"" + hpoTerm + "\"";
      filter = finalizeFilter(filter);
      filters.add(filter);
    }
    return filters;
  }

  public static List<String> makeORDOFilter(ORDOQuery ordoQuery) throws Exception {
    List<String> filters = new ArrayList<>();
    String ordoTerm = ordoQuery.getSearchTerm();
    if (ordoTerm.startsWith("ORPHA:")) {
      ordoTerm = ordoTerm.replace("ORPHA:", "ORPHA_");
    } else {
      System.out.println("ordoTerm=" + ordoTerm);
      throw new Exception("Expected input ORDO term to start with 'ORPHA:'");
    }
    String filter = "{diseases: { diseaseCode: { ontologyTermURI: {like:\"" + ordoTerm + "\"";
    filter = finalizeFilter(filter);
    filters.add(filter);
    return filters;
  }

  /**
   * No support yet
   * @param reactomeQuery
   * @return
   * @throws Exception
   */
  public static List<String> makeReactomeFilter(GeneReactomeQuery reactomeQuery) throws Exception {
    List<String> filters = new ArrayList<>();
    return filters;
  }

  /**
   * Only support direct match on HGNC symbol
   * @param geneQuery
   * @return
   * @throws Exception
   */
  public static List<String> makeGeneFilter(GeneReactomeQuery[] geneQuery) throws Exception {
    List<String> filters = new ArrayList<>();
    String[] values = new String[geneQuery.length];
    for(int i = 0; i < geneQuery.length; i ++)
    {
      values[i] = geneQuery[i].getId();
    }
    filters.add(valueArrayFilterBuilder("{diseaseCausalGenes: {name: {equals:", values));
    return filters;
  }

  /**
   * TODO: most of this
   * minAge and maxAge are really tricky since there are multiple ways to store age, some of which (i.e. ISO8601) need to be fully parsed first (see EJP_VP_IndividualsQuery) before logic can be applied
   * @param demographyQuery
   * @return
   * @throws Exception
   */
  public static List<String> makeDemographyFilter(DemographyQuery demographyQuery) throws Exception {
    List<String> filters = new ArrayList<>();
    filters.add(valueArrayFilterBuilder("{sex: {ontologyTermURI: {like:", demographyQuery.getGender()));
    return filters;
  }
}
