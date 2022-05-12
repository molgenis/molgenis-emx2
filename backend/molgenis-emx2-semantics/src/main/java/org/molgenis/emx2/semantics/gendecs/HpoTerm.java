package org.molgenis.emx2.semantics.gendecs;

import java.util.ArrayList;

/**
 * Class HpoTerm is a class which holds 2 arraylists. Parents and children which hold parents and
 * children classes of an HPO term.
 */
public class HpoTerm {
  private ArrayList<String> parents;
  private ArrayList<String> children = new ArrayList<>();
  private String hpoTerm;

  public HpoTerm(String hpoTerm) {
    this.hpoTerm = hpoTerm;
  }

  public void setParents(ArrayList<String> parents) {
    this.parents = parents;
  }

  public void addChildren(ArrayList<String> children) {
    this.children.addAll(children);
  }
}
