package org.molgenis.emx2.rdf.generators.query.generators;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.rdf4j.sparqlbuilder.core.Groupable;
import org.eclipse.rdf4j.sparqlbuilder.core.Projectable;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.rdf.generators.query.ColumnNameSparqlEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates SPARQL query components for a column that references another table.
 *
 * <p>Resolves the referenced table's primary key column(s) and delegates to the appropriate
 * generator per PK column type:
 *
 * <ul>
 *   <li>{@link ReferenceColumnSparqlQueryGenerator} - for nested references (composite keys)
 *   <li>{@link ArrayColumnSparqlQueryGenerator} - for array-typed PK columns
 *   <li>{@link LiteralColumnSparqlQueryGenerator} - for scalar PK columns
 * </ul>
 *
 * <p>Ontology columns are handled separately: array ontologies delegate to {@link
 * ArrayColumnSparqlQueryGenerator}; scalar ontologies project a single variable.
 *
 * <p>If the root column is optional, all patterns are wrapped in an OPTIONAL block.
 */
public class ReferenceColumnSparqlQueryGenerator implements ColumnSparqlQueryGenerator {

  private static final Logger logger =
      LoggerFactory.getLogger(ReferenceColumnSparqlQueryGenerator.class);

  private final Variable variable;
  private final Column rootColumn;
  private final List<String> path;

  private final List<GraphPattern> patterns = new ArrayList<>();
  private final List<Projectable> selectors = new ArrayList<>();
  private final List<Groupable> groupBy = new ArrayList<>();

  public ReferenceColumnSparqlQueryGenerator(Variable variable, Column rootColumn) {
    this(variable, rootColumn, new ArrayList<>());
  }

  private ReferenceColumnSparqlQueryGenerator(
      Variable variable, Column rootColumn, List<String> path) {
    this.variable = variable;
    this.rootColumn = rootColumn;
    this.path = path;
    map();
  }

  private void map() {
    if (rootColumn.isOntology()) {
      mapOntology();
    } else {
      mapDataColumn();
    }
  }

  private void mapOntology() {
    ColumnSparqlQueryGenerator generator;
    if (rootColumn.isArray()) {
      generator = new ArrayColumnSparqlQueryGenerator(variable, rootColumn);
      selectors.addAll(generator.getSelectors());
    } else {
      generator =
          new LiteralColumnSparqlQueryGenerator(variable, rootColumn, columnVariable(), true);

      selectors.add(columnVariable());
    }

    patterns.addAll(generator.getPatterns());
    groupBy.addAll(generator.getGroupBy());
  }

  private void mapDataColumn() {
    ColumnSparqlQueryGenerator mapper =
        new LiteralColumnSparqlQueryGenerator(variable, rootColumn, columnVariable(), true);
    patterns.addAll(mapper.getPatterns());
    mapPrimaryKeys();
  }

  private void mapPrimaryKeys() {
    TableMetadata refTable = rootColumn.getRefTable();
    for (Column column : refTable.getPrimaryKeyColumns()) {
      if (column.getSemantics() == null || column.getSemantics().length == 0) {
        logger.warn("Column {} has no semantics", column.getName());
        continue;
      }

      ArrayList<String> columnPath = columnPath();
      Variable subject = columnVariable();
      ColumnSparqlQueryGenerator mapper = getMapperForColumn(column, subject, columnPath);

      patterns.addAll(mapper.getPatterns());
      selectors.addAll(mapper.getSelectors());
      groupBy.addAll(mapper.getGroupBy());
    }
  }

  private ColumnSparqlQueryGenerator getMapperForColumn(
      Column column, Variable subject, ArrayList<String> columnPath) {
    if (column.isReference()) {
      return new ReferenceColumnSparqlQueryGenerator(subject, column, columnPath);
    }

    Variable ref = SparqlBuilder.var(ColumnNameSparqlEncoder.encodeSparqlVariable(columnPath));
    Variable extended = extendVariable(subject, column);

    if (rootColumn.isArray()) {
      return new ArrayColumnSparqlQueryGenerator(ref, column, extended);
    } else {
      return new LiteralColumnSparqlQueryGenerator(ref, column, extended, true);
    }
  }

  private Variable columnVariable() {
    return SparqlBuilder.var(ColumnNameSparqlEncoder.encodeSparqlVariable(columnPath()));
  }

  private ArrayList<String> columnPath() {
    ArrayList<String> newPath = new ArrayList<>(path);
    newPath.add(ColumnNameSparqlEncoder.encodeSparqlVariable(rootColumn));
    return newPath;
  }

  private Variable extendVariable(Variable toExtend, Column column) {
    return SparqlBuilder.var(
        ColumnNameSparqlEncoder.encodeSparqlVariable(
            List.of(toExtend.getVarName(), column.getName())));
  }

  @Override
  public List<Projectable> getSelectors() {
    return List.copyOf(selectors);
  }

  @Override
  public List<GraphPattern> getPatterns() {
    if (rootColumn.isRequired()) {
      return List.copyOf(patterns);
    } else {
      return List.of(GraphPatterns.and(patterns.toArray(new GraphPattern[0])).optional());
    }
  }

  @Override
  public List<Groupable> getGroupBy() {
    return List.copyOf(groupBy);
  }
}
