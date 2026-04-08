package org.molgenis.emx2.rag;

public record QueryResult(Double score, String source, String url) {
  public void printResults() {
    System.out.println("-------------------------------");
    System.out.println("Score: " + this.score());
    System.out.println("Source: " + this.source());
    System.out.println("URL: " + this.url);
  }
}
