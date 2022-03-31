package org.molgenis.emx2.semantics.gendecs;

import java.util.ArrayList;
import java.util.HashMap;

public class Variants {
  private ArrayList<String> variants = new ArrayList<>();
  private HashMap<String, String> genesHpo = new HashMap<>();

  public void addVariant(String variants) {
    this.variants.add(variants);
  }

  public HashMap<String, String> getGeneHpo() {
    return genesHpo;
  }

  public void addGenesHpo(String gene, String hpo) {
    this.genesHpo.put(gene, hpo);
  }
}
