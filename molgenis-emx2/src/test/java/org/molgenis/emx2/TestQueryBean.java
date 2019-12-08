package org.molgenis.emx2;

import org.junit.Test;
import org.molgenis.emx2.beans.QueryBean;

import static org.junit.Assert.assertEquals;
import static org.molgenis.emx2.Operator.IS;

public class TestQueryBean {

  @Test
  public void testQuerySelect() {

    Query q1 =
        new QueryBean().select("First Name", "Last Name", "Father/First Name", "Father/Last Name");

    Query q2 =
        new QueryBean()
            .select("First Name")
            .select("Last Name")
            .select("Father/First Name")
            .select("Father/Last Name");

    Query q3 =
        new QueryBean()
            .select("First Name")
            .select("Last Name")
            .expand("Father")
            .select("First Name")
            .select("Last Name")
            .collapse();

    // Query q4 = new
    // QueryBean.parseSelect("First%20Name,Last%20Name,Father/First%20Name,Father/Last%20Name");
    // Query q4 = new
    // QueryBean.parseSelect("select=First%20Name,Last%20Name,Father/First%20Name,Father/Last%20Name");

    assertEquals(q1.getSelectList(), q2.getSelectList());
    assertEquals(q1.getSelectList(), q3.getSelectList());
    assertEquals(q1.getSelectList(), q3.getSelectList());
  }

  @Test
  public void testBuilder() {

    QueryBean q = null;

    // one setter per line
    System.out.println(
        "==== SELECT FirstName, LastName FILTER FirstName EQUALS Donald, LastName EQUALS Duck OR LastName EQUALS Mouse SORT ASC FirstName DESC LastName");
    q = new QueryBean();
    q.select("First Name");
    q.select("Last Name");
    q.expand("Father").select("Firs tName").select("Last Name");
    q.where("First Name", IS, "Donald")
        .and("Last Name", IS, "Duck")
        .and("Age", IS, 56)
        .or("Father/Last Name", IS, "Mouse");
    q.asc("First Name").desc("Last Name");

    assertEquals(4, q.getSelectList().size());
    assertEquals(4, q.getWhereLists().size());
    System.out.println(q);

    // or in a builder pattern
    System.out.println("==== same but in builder pattern ");
    q = new QueryBean();
    q.select("First Name")
        .select("Last Name")
        .expand("Father")
        .select("First Name")
        .select("LastName")
        .where("First Name", IS, "Donald")
        .and("Last Name", IS, "Duck")
        .or("Last Name", IS, "Mouse")
        .asc("First Name")
        .desc("Last Name");
    System.out.println(q);

    // or mixing the select and filtering, really short :-)
    System.out.println(
        "==== SELECT firstName(EQUALS Donald), LastName (EQUALS Duck) OR LastName EQUALS Mouse");
    q = new QueryBean();
    q.where("First Name", IS, "Donald")
        .where("Last Name", IS, "Duck")
        .expand("Father")
        .select("Last Name")
        .where("Father", IS, "Mouse")
        .or("Last Name", IS, "Mouse");
    System.out.println(q);
  }
}
