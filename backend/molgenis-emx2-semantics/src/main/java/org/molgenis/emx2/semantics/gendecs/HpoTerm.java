package org.molgenis.emx2.semantics.gendecs;

import java.util.ArrayList;

public class HpoTerm {
  private ArrayList<String> parents;
  private ArrayList<String> children;

  public ArrayList<String> getParents() {
    return parents;
  }

  public void setParents(ArrayList<String> parents) {
    this.parents = parents;
  }

  public ArrayList<String> getChildren() {
    return children;
  }

  public void setChildren(ArrayList<String> children) {
    this.children = children;
  }
}
