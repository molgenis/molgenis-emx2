package org.molgenis.emx2.rdf.generators.query;

import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
import org.eclipse.rdf4j.sparqlbuilder.core.Projectable;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;

public class SparqlVariableUtil {

  public static final char CONCAT_SEPARATOR = '\u001F';

  public static final String SINGLE = "_single";

  private SparqlVariableUtil() {
    // Utility class
  }

  public static Variable singleVariable(Variable variable) {
    return SparqlBuilder.var(variable.getVarName() + SINGLE);
  }

  public static Variable subjectVariable(Variable variable) {
    return prefixVariable(TableQueryGenerator.SUBJECT_VARIABLE.getVarName(), variable);
  }

  public static Variable prefixVariable(String prefix, Variable variable) {
    return SparqlBuilder.var(prefix + variable.getVarName());
  }

  public static Projectable concatAs(Variable toConcat, Variable as) {
    return Expressions.group_concat("'" + CONCAT_SEPARATOR + "'", Expressions.str(toConcat))
        .distinct()
        .as(as);
  }

  public static GraphPattern bindAs(Variable toBind, Variable as) {
    return Expressions.bind(toBind, as);
  }
}
