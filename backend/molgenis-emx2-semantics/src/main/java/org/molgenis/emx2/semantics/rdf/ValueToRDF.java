package org.molgenis.emx2.semantics.rdf;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.eclipse.rdf4j.model.vocabulary.XSD.*;
import static org.eclipse.rdf4j.model.vocabulary.XSD.LONG;
import static org.molgenis.emx2.semantics.rdf.ColumnTypeToXSDDataType.columnTypeToXSD;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.molgenis.emx2.*;
import org.molgenis.emx2.beaconv2.common.QueryHelper;

public class ValueToRDF {

  public static void describeValues(
      ObjectMapper jsonMapper, ModelBuilder builder, Table table, IRI schemaContext)
      throws Exception {
    Map<String, Column> columnMap = new HashMap<>();
    for (Column c : table.getMetadata().getColumns()) {
      columnMap.put(c.getName(), c);
    }
    IRI tableContext = iri(schemaContext + "/" + table.getName());
    Query q = table.query();
    QueryHelper.selectColumns(table, q);
    String json = q.retrieveJSON();
    Map<String, List<Map<String, Object>>> jsonMap = jsonMapper.readValue(json, Map.class);
    List<Map<String, Object>> data = jsonMap.get(table.getName());

    for (Map<String, Object> row : data) {

      TableMetadata tableMetadata = table.getMetadata();
      String primaryKey = tableMetadata.getPrimaryKeys().get(0);
      String pkValue = (String) row.get(primaryKey);
      IRI rowContext = iri(schemaContext + "/" + table.getName() + "/row/" + pkValue);

      builder.add(rowContext, RDF.TYPE, tableContext);
      builder.add(rowContext, RDF.TYPE, iri("http://semanticscience.org/resource/SIO_001187"));
      builder.add(rowContext, RDF.TYPE, iri("http://purl.org/linked-data/cube#Observation"));
      builder.add(rowContext, iri("http://purl.org/linked-data/cube#dataSet"), tableContext);

      for (String column : row.keySet()) {
        if (row.get(column) != null) {
          IRI columnContext = iri(tableContext + "/column/" + column);
          for (Value value : formatValue(row.get(column), columnMap.get(column), schemaContext)) {
            builder.add(rowContext, columnContext, value);
          }
        }
      }
    }
  }

  public static Value[] formatValue(Object o, Column column, IRI schemaContext) throws Exception {
    Value[] valList;
    if (o instanceof List) {
      List<Object> objList = (List<Object>) o;
      valList = new Value[objList.size()];
      for (int i = 0; i < valList.length; i++) {
        valList[i] = applyFormatting(objList.get(i), column, schemaContext);
      }
    } else {
      valList = new Value[1];
      valList[0] = applyFormatting(o, column, schemaContext);
    }
    return valList;
  }

  public static Value applyFormatting(Object o, Column column, IRI schemaContext) throws Exception {
    ColumnType columnType = column.getColumnType();
    IRI XSDType = columnTypeToXSD(columnType);

    if (columnType.equals(ColumnType.ONTOLOGY) || columnType.equals(ColumnType.ONTOLOGY_ARRAY)) {
      return iri((String) ((Map) o).get("ontologyTermURI"));
    } else if (columnType.equals(ColumnType.REF)
        || columnType.equals(ColumnType.REF_ARRAY)
        || columnType.equals(ColumnType.REFBACK)) {
      // TODO should the target IRI be resolvable here?
      TableMetadata tableMetadata = column.getRefTable();
      String primaryKey = tableMetadata.getPrimaryKeys().get(0);
      String pkValue = (String) ((Map) o).get(primaryKey);
      return iri(schemaContext + "/" + tableMetadata.getTableName() + "/" + pkValue);

    } else {

      if (BOOLEAN.equals(XSDType)) {
        return literal((boolean) o);
      } else if (DATE.equals(XSDType)) {
        return literal(((String) o), XSDType);
      } else if (DATETIME.equals(XSDType)) {
        return literal(((String) o).substring(0, 19), XSDType);
      } else if (DECIMAL.equals(XSDType)) {
        return literal((double) o);
      } else if (STRING.equals(XSDType)) {
        return literal((String) o);
      } else if (ANYURI.equals(XSDType)) {
        return iri((String) o);
      } else if (INT.equals(XSDType)) {
        return literal((int) o);
      } else if (LONG.equals(XSDType)) {
        return literal((long) o);
      } else {
        throw new Exception("XSD type formatting not specified: " + XSDType);
      }
    }
  }
}
