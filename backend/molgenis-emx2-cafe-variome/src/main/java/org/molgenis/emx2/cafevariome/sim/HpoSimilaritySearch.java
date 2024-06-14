package org.molgenis.emx2.cafevariome.sim;

import static org.molgenis.emx2.cafevariome.sim.HttpPostRawJson.httpJsonRawPost;
import static org.molgenis.emx2.graphgenome.RetrieveRefSeq.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

public class HpoSimilaritySearch {

  private static final String HPO_SIM_SERVICE_URL =
      "https://similarity.cafevariome.org/similarity/hpo";

  // output is grouped per input term: because 'minimum number of matched term' is based on this
  public static List<String> expandHPOTerm(double sim, String hpoTerm, String sep)
      throws Exception {

    // todo: better do 1 request with all inputs and then loop over each term to extract output
    String jsonRaw = "{\"hpoTerms\":[\"" + hpoTerm + "\"],\"threshold\":" + sim + "}";
    String hpoSimResponse = httpJsonRawPost(HPO_SIM_SERVICE_URL, jsonRaw);
    HpoSimServiceResponse[] responseObjArr =
        new ObjectMapper().readValue(hpoSimResponse, HpoSimServiceResponse[].class);

    List<String> result = new ArrayList<>();
    for (HpoSimServiceResponse responseObj : responseObjArr) {
      for (String expTerm : responseObj.getSimilarIDs()) {
        if (expTerm.startsWith("HP:")) {
          expTerm = expTerm.replace("HP:", "HP" + sep);
        } else {
          System.out.println("hpoTerm=" + hpoTerm);
          throw new Exception("Expected input HPO term to start with 'HP:'");
        }
        result.add(expTerm);
      }
    }
    return result;
  }

  public static void main(String[] args) throws Exception {
    List<String> expTerms = expandHPOTerm(0.5, "HP:0031058", "_");
    System.out.println(expTerms);
  }
}
