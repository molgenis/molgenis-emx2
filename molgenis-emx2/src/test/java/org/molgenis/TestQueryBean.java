package org.molgenis;

import org.junit.Test;
import org.molgenis.query.QueryBean;

import static org.junit.Assert.assertEquals;

public class TestQueryBean {

  @Test
  public void testBuilder() {

    QueryBean q = null;

    // one setter per line
    System.out.println(
        "==== SELECT FirstName, LastName FILTER FirstName EQ Donald, LastName EQ Duck OR LastName EQ Mouse SORT ASC FirstName DESC LastName");
    q = new QueryBean();
    q.select("FirstName");
    q.select("LastName");
    q.expand("Father").include("FirstName").include("LastName");
    q.where("FirstName")
        .eq("Donald")
        .and("LastName")
        .eq("Duck")
        .and("Age")
        .eq(56)
        .or("Father", "LastName")
        .eq("Mouse");
    q.asc("FirstName").desc("LastName");

    assertEquals(4, q.getSelectList().size());
    assertEquals(5, q.getWhereLists().size());
    System.out.println(q);

    // or in a builder pattern
    System.out.println("==== same but in builder pattern ");
    q = new QueryBean();
    q.select("FirstName")
        .select("LastName")
        .expand("Father")
        .include("FirstName")
        .include("LastName")
        .where("FirstName")
        .eq("Donald")
        .and("LastName")
        .eq("Duck")
        .or("LastName")
        .eq("Mouse")
        .asc("FirstName")
        .desc("LastName");
    System.out.println(q);

    // or mixing the select and filtering, really short :-)
    System.out.println("==== SELECT firstName(EQ Donald), LastName (EQ Duck) OR LastName EQ Mouse");
    q = new QueryBean();
    q.select("FirstName")
        .eq("Donald")
        .select("LastName")
        .eq("Duck")
        .expand("Father")
        .include("LastName")
        .eq("Mouse")
        .or("LastName")
        .eq("Mouse");
    System.out.println(q);

    // aggregates
    System.out.println("==== LastName, AVG(Age), AVG(Father/Age)");
    q = new QueryBean();
    q.select("LastName");
    q.avg("Age");
    q.avg("Father", "Age");
    System.out.println(q);

    // aggregates
    System.out.println("==== LastName, AVG(Age), AVG(Father/Age)");
    q = new QueryBean();
    q.select("LastName");
    q.avg("Age");
    q.expand("Father").avg("Age");
    System.out.println(q);
  }
}
