package org.molgenis.emx2.harvester;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;
import org.molgenis.emx2.*;

public class TableSparqlQuery {

  private static final ValueFactory VALUE_FACTORY = SimpleValueFactory.getInstance();

  private final SchemaMetadata schema;
  private final TableMetadata table;
  private final SelectQuery select = Queries.SELECT();

  private TriplePattern mainPattern = null;
  private final List<GraphPattern> optionalPatterns = new ArrayList<>();

  private final Variable mainVar;

  public TableSparqlQuery(SchemaMetadata schema, String tableName) {
    this.schema = schema;
    this.table = schema.getTableMetadata(tableName);
    this.mainVar = SparqlBuilder.var(tableName);
  }

  public void build() {
    for (Column column : table.getColumns()) {
      if (column.getSemantics() == null) {
        continue;
      }

      Variable columnVar = SparqlBuilder.var(column.getName());
      String semantic = Arrays.stream(column.getSemantics()).findFirst().orElseThrow();

      if (column.isReference()) {
        optionalPatterns.add(getReferenceTriplePattern(column, columnVar, semantic));
      } else if (column.isRequired()) {
        addToMainPattern(semantic, columnVar);
      } else {
        TriplePattern triple = mainVar.has(VALUE_FACTORY.createIRI(semantic), columnVar);
        optionalPatterns.add(GraphPatterns.optional(triple));
      }

      select.select(columnVar);
      select.where(mainPattern);
      for (GraphPattern pattern : optionalPatterns) {
        select.where(pattern);
      }
    }
  }

  private GraphPattern getReferenceTriplePattern(
      Column column, Variable startingPoint, String semantic) {
    Reference reference = column.getReferences().getFirst();
    TableMetadata referenceTable = schema.getTableMetadata(reference.getTargetTable());
    Column referenceColumn = referenceTable.getColumn(reference.getTargetColumn());
    if (referenceColumn.getSemantics() == null) {
      throw new MolgenisException(
          "Unable to resolve reference semantics for " + reference.getName());
    }

    Variable refVar = SparqlBuilder.var(referenceColumn.getName() + "Ref");
    TriplePattern triple = triple(startingPoint, semantic, refVar);

    String referenceSemantic = referenceColumn.getSemantics()[0];
    Variable refKeyVar = SparqlBuilder.var(referenceColumn.getName());
    TriplePattern anotherTriple = triple(refVar, referenceSemantic, refKeyVar);

    return GraphPatterns.optional(triple, anotherTriple);
  }

  private TriplePattern triple(Variable subject, String predicate, Variable object) {
    return subject.has(VALUE_FACTORY.createIRI(predicate), object);
  }

  private void addToMainPattern(String semantic, Variable var) {
    if (mainPattern == null) {
      mainPattern = triple(mainVar, semantic, var);
    } else {
      mainPattern.andHas(VALUE_FACTORY.createIRI(semantic), var);
    }
  }

  public String asString() {
    return select.getQueryString();
  }
}
