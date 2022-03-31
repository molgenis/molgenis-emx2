package org.molgenis.emx2.semantics.gendecs;

import java.util.ArrayList;
import java.util.HashMap;

public class Variants {
  private ArrayList<String> variants = new ArrayList<>();
  private ArrayList<String> genes = new ArrayList<>();
  private HashMap<String, String> genesHpo = new HashMap<>();

  public void addVariant(String variants) {
    this.variants.add(variants);
  }

  /**
   * Method that filters the genes from the variants if the arraylist genes is empty.
   *
   * @return ArrayList with gene symbols.
   */
  public ArrayList<String> getGenes() {
    if (this.genes.size() == 0) {
      for (String variant : variants) {
        String[] splittedLine = variant.split("\t");
        String[] infoString = splittedLine[7].split(";");
        for (String i : infoString) {
          if (i.contains("GENEINFO")) {
            this.addGene(i.split("=")[1]);
          }
        }
      }
    }
    return genes;
  }

  private void addGene(String genes) {
    this.genes.add(genes);
  }
  
  public HashMap<String, String> getGeneHpo() {
    return genesHpo;
  }

  public void addGenesHpo(String gene, String hpo) {
    this.genesHpo.put(gene, hpo);
  }
}
