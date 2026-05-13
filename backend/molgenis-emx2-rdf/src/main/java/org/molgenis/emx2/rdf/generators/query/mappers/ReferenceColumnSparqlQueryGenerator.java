package org.molgenis.emx2.rdf.generators.query.mappers;

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
public class ReferenceColumnSparqlQueryGenerator implements SparqlQueryGenerator {

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
    if (Boolean.TRUE.equals(rootColumn.isArray())) {
      SparqlQueryGenerator mapper = new ArrayColumnSparqlQueryGenerator(variable, rootColumn);
      patterns.addAll(mapper.getPatterns());
      selectors.addAll(mapper.getSelectors());
      groupBy.addAll(mapper.getGroupBy());
    } else {
      SparqlQueryGenerator mapper =
          new LiteralColumnSparqlQueryGenerator(variable, rootColumn, columnVariable(), true);
      patterns.addAll(mapper.getPatterns());
      selectors.add(columnVariable());
      groupBy.addAll(mapper.getGroupBy());
    }
  }

  private void mapDataColumn() {
    SparqlQueryGenerator mapper =
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

      SparqlQueryGenerator mapper;
      if (column.isReference()) {
        mapper = new ReferenceColumnSparqlQueryGenerator(subject, column, columnPath);
      } else if (Boolean.TRUE.equals(rootColumn.isArray())) {
        Variable ref = SparqlBuilder.var(String.join("_", columnPath));
        mapper = new ArrayColumnSparqlQueryGenerator(ref, column, extendVar(subject, column));
      } else {
        Variable ref = SparqlBuilder.var(String.join("_", columnPath));
        mapper =
            new LiteralColumnSparqlQueryGenerator(ref, column, extendVar(subject, column), true);
      }

      patterns.addAll(mapper.getPatterns());
      selectors.addAll(mapper.getSelectors());
      groupBy.addAll(mapper.getGroupBy());
    }
  }

  private Variable columnVariable() {
    ArrayList<String> newPath = columnPath();
    return SparqlBuilder.var(String.join("_", newPath));
  }

  private ArrayList<String> columnPath() {
    ArrayList<String> newPath = new ArrayList<>(path);
    newPath.add(ColumnVariableNameMapper.columnToSparql(rootColumn));
    return newPath;
  }

  private Variable extendVar(Variable toExtend, Column column) {
    return SparqlBuilder.var(
        toExtend.getVarName() + "_" + ColumnVariableNameMapper.columnToSparql(column));
  }

  @Override
  public List<Projectable> getSelectors() {
    return new ArrayList<>(selectors);
  }

  @Override
  public List<GraphPattern> getPatterns() {
    if (rootColumn.isRequired()) {
      return new ArrayList<>(patterns);
    } else {
      return List.of(GraphPatterns.and(patterns.toArray(new GraphPattern[0])).optional());
    }
  }

  @Override
  public List<Groupable> getGroupBy() {
    return new ArrayList<>(groupBy);
  }
}
