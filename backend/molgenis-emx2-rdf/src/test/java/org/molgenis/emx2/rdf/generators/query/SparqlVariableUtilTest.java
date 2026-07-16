package org.molgenis.emx2.rdf.generators.query;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.junit.jupiter.api.Test;

class SparqlVariableUtilTest {

  @Test
  void shouldAppendSingleSuffix() {
    Variable variable = SparqlBuilder.var("foo");
    assertEquals("foo_single", SparqlVariableUtil.singleVariable(variable).getVarName());
  }

  @Test
  void shouldPrefixWithSubjectVariable() {
    Variable variable = SparqlBuilder.var("foo");
    assertEquals(
        TableQueryGenerator.SUBJECT_VARIABLE.getVarName() + "foo",
        SparqlVariableUtil.subjectVariable(variable).getVarName());
  }

  @Test
  void shouldPrefixWithGivenPrefix() {
    Variable variable = SparqlBuilder.var("foo");
    assertEquals("bar_foo", SparqlVariableUtil.prefixVariable("bar_", variable).getVarName());
  }

  @Test
  void shouldGroupConcatDistinctAsVariable() {
    Variable toConcat = SparqlBuilder.var("foo");
    Variable as = SparqlBuilder.var("bar");
    assertEquals(
        "( GROUP_CONCAT( DISTINCT STR( ?foo ) ; SEPARATOR = ',' ) AS ?bar )",
        SparqlVariableUtil.concatAs(toConcat, as).getQueryString());
  }

  @Test
  void shouldBindVariableAsAnother() {
    Variable toBind = SparqlBuilder.var("foo");
    Variable as = SparqlBuilder.var("bar");
    assertEquals("BIND( ?foo AS ?bar )", SparqlVariableUtil.bindAs(toBind, as).getQueryString());
  }
}
