package org.molgenis.emx2.semantics.gendecs;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import org.junit.Test;

public class OwlQuerierTest {

  @Test
  public void getSubClasses_normalInput() {
    String hpoId = "HP_0006488";
    ArrayList<String> resultExp = new ArrayList<>();
    resultExp.add("Bowing of the arm");
    resultExp.add("Bowed forearm bones");
    resultExp.add("Progressive forearm bowing");
    resultExp.add("Radial bowing");
    resultExp.add("Ulnar bowing");
    resultExp.add("Bowed humerus");

    ArrayList<String> result = OwlQuerier.getSubClasses(hpoId);
    Collections.sort(result);
    Collections.sort(resultExp);

    assertEquals(resultExp, result);
  }

  @Test
  public void getParentClasses_normalInput() {
    String hpoId = "HP_0006488";
    ArrayList<String> resultExp = new ArrayList<>();
    resultExp.add("Bowing of the long bones");
    resultExp.add("Abnormality of the upper limb");

    ArrayList<String> result = OwlQuerier.getParentClasses(hpoId);
    Collections.sort(result);
    Collections.sort(resultExp);

    assertEquals(resultExp, result);
  }

  @Test
  public void getSubClasses_nullInput() {
    ArrayList<String> resultExp = new ArrayList<>();

    ArrayList<String> result = OwlQuerier.getSubClasses(null);

    assertEquals(resultExp, result);
  }

  @Test
  public void getParentClasses_nullInput() {
    ArrayList<String> resultExp = new ArrayList<>();

    ArrayList<String> result = OwlQuerier.getParentClasses(null);

    assertEquals(resultExp, result);
  }
}
