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

public class ReferenceMapper implements ColumnMapper {

  private final Variable variable;
  private final Column rootColumn;
  private final List<String> path;

  private final List<GraphPattern> patterns = new ArrayList<>();
  private final List<Projectable> selectors = new ArrayList<>();
  private final List<Groupable> groupBy = new ArrayList<>();

  public ReferenceMapper(Variable variable, Column rootColumn) {
    this(variable, rootColumn, new ArrayList<>());
  }

  private ReferenceMapper(Variable variable, Column rootColumn, List<String> path) {
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
      ColumnMapper mapper = new CollectionColumnMapper(variable, rootColumn);
      patterns.addAll(mapper.getPattern());
      selectors.addAll(mapper.getSelectors());
      groupBy.addAll(mapper.getGroupBy());
    } else {
      ColumnMapper mapper = new PlainColumnMapper(variable, rootColumn, columnVariable(), true);
      patterns.addAll(mapper.getPattern());
      selectors.add(columnVariable());
      groupBy.addAll(mapper.getGroupBy());
    }
  }

  private void mapDataColumn() {
    ColumnMapper mapper = new PlainColumnMapper(variable, rootColumn, columnVariable(), true);
    patterns.addAll(mapper.getPattern());
    mapPrimaryKeys();
  }

  private void mapPrimaryKeys() {
    TableMetadata refTable = rootColumn.getRefTable();
    for (Column column : refTable.getPrimaryKeyColumns()) {
      ArrayList<String> columnPath = columnPath();
      Variable subject = columnVariable();

      ColumnMapper mapper;
      if (column.isReference()) {
        mapper = new ReferenceMapper(subject, column, columnPath);
      } else if (Boolean.TRUE.equals(rootColumn.isArray())) {
        Variable ref = SparqlBuilder.var(String.join("_", columnPath));
        mapper = new CollectionColumnMapper(ref, column, extendVar(subject, column));
      } else {
        Variable ref = SparqlBuilder.var(String.join("_", columnPath));
        mapper = new PlainColumnMapper(ref, column, extendVar(subject, column), true);
      }

      patterns.addAll(mapper.getPattern());
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
    newPath.add(new ColumnVariableName(rootColumn).getSparqlName());
    return newPath;
  }

  private Variable extendVar(Variable toExtend, Column column) {
    return SparqlBuilder.var(
        toExtend.getVarName() + "_" + new ColumnVariableName(column).getSparqlName());
  }

  @Override
  public List<Projectable> getSelectors() {
    return new ArrayList<>(selectors);
  }

  @Override
  public List<GraphPattern> getPattern() {
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
