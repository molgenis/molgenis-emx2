package org.molgenis.emx2.graphgenome;

public class Formatting {

  /** Format node identifiers */
  public static String formatNodeId(
      String apiContext, String gene, int nodeCounter, String type, String seq) {
    return apiContext + "/" + gene + "/node" + nodeCounter + "/" + type + "/" + shorten(seq);
  }

  /** Shorten lengthy strings of DNA */
  public static String shorten(String input) {
    if (input.length() > 50) {
      return input.substring(0, 25) + "..." + input.substring(input.length() - 25);
    } else {
      return input;
    }
  }
}
