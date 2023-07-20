package org.molgenis.emx2.cafevariome.sim;

import static org.molgenis.emx2.semantics.graphgenome.RetrieveRefSeq.httpGet;

import java.util.*;

public class HpoSimilaritySearch {

  // output is grouped per input term: because 'minimum number of matched term' is based on this
  public static Map<String, Set<String>> refineHPOSearchTerms(
      int depth, double sim, String... hpoTerms) throws Exception {

    Map<String, Set<String>> result = new HashMap<>();

    for (String hpoTerm : hpoTerms) {
      // start by adding the input term to its set of output terms
      Set<String> refinedTerms = new HashSet<>();
      refinedTerms.add(hpoTerm);
      result.put(hpoTerm, refinedTerms);

      // step 1: replace obsolete terms with current version
      // <there will be another endpoint for this>

      // step 2: perform term similarity search
      String simResponse =
          httpGet(
              "https://www597.lamp.le.ac.uk/testing/Sim/query/?hpoterm=HP:0001258,HP:0010550&sim=0.9");

      // step 3: expand all children of the term and its similar terms
      String childrResponse =
          httpGet("https://www597.lamp.le.ac.uk/testing/Hpo/children/hpo=HP:0031058;depth=3");
    }

    return result;
  }

  public static void main(String[] args) throws Exception {
    Map<String, Set<String>> terms = refineHPOSearchTerms(3, 0.9, "HP:0031058", "HP:0000002");

    // step 4:  query individuals
    // for each original input term, use the refined terms, and report matched individuals
    // then merge the results, counting how many original terms matched an individual, that is the
    // 'minimum number of matched term'

  }
}
