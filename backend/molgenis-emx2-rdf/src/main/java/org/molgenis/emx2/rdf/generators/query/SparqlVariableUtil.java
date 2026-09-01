package org.molgenis.emx2.rdf.generators.query;

import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
import org.eclipse.rdf4j.sparqlbuilder.core.Projectable;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.molgenis.emx2.Column;

public class SparqlVariableUtil {

  private static final String CONCAT_ARG = "'|'";
  private static final String SINGLE = "_single";
  public static final String SUBJECT_NAME = "_subject_";
  public static final Variable SUBJECT_VARIABLE = SparqlBuilder.var(SUBJECT_NAME);


  private SparqlVariableUtil() {
    // Utility class
  }

  public static Variable singleVariable(Variable variable) {
    return SparqlBuilder.var(variable.getVarName() + SINGLE);
  }

  public static Variable subjectVariable(Variable variable) {
    return prefixVariable(SUBJECT_NAME, variable);
  }

  public static Variable subjectVariable(Column column) {
    return prefixVariable(
        SUBJECT_NAME,
        ColumnNameSparqlEncoder.encodeSparqlVariable(column.getName()));
  }

  public static Variable prefixVariable(String prefix, Variable variable) {
    return SparqlBuilder.var(prefix + variable.getVarName());
  }

  public static Projectable concatAs(Variable toConcat, Variable as) {
    return Expressions.group_concat(CONCAT_ARG, Expressions.str(toConcat)).distinct().as(as);
  }

  public static GraphPattern bindAs(Variable toBind, Variable as) {
    return Expressions.bind(toBind, as);
  }
}
