package org.molgenis.emx2.semantics.rdf;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.semantics.QueryHelper.selectColumns;
import static org.molgenis.emx2.semantics.rdf.ColumnTypeToXSDDataType.columnTypeToXSD;
import static org.molgenis.emx2.semantics.rdf.IRIParsingEncoding.encodedIRI;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.TypeUtils;

public class ValueToRDF {

  public static void describeValues(
      ObjectMapper jsonMapper,
      ModelBuilder builder,
      Table table,
      String rowId,
      String schemaContext)
      throws Exception {
    Map<String, Column> columnMap = new HashMap<>();
    for (Column c : table.getMetadata().getColumns()) {
      columnMap.put(c.getName(), c);
    }
    String tableContext = schemaContext + "/" + table.getName();
    Query query = selectColumns(table);
    if (rowId != null) {
      // FIXME: how to support multiple PKs?
      query.where(f(table.getMetadata().getPrimaryKeyColumns().get(0).getName(), EQUALS, rowId));
    }
    String json = query.retrieveJSON();
    Map<String, List<Map<String, Object>>> jsonMap = jsonMapper.readValue(json, Map.class);
    List<Map<String, Object>> data = jsonMap.get(table.getName());

    if (data == null) {
      return;
    }

    for (Map<String, Object> row : data) {

      String pkValue =
          table.getMetadata().getPrimaryKeys().stream()
              .map(primaryKey -> row.get(primaryKey).toString())
              .collect(Collectors.joining("-"));
      IRI rowContext = encodedIRI(schemaContext + "/" + table.getName() + "/" + pkValue);

      builder.add(rowContext, RDF.TYPE, encodedIRI(tableContext));
      // SIO:001187 = database row
      builder.add(rowContext, RDF.TYPE, iri("http://semanticscience.org/resource/SIO_001187"));
      if (table.getMetadata().getTableType() == TableType.ONTOLOGIES) {
        // NCIT:C95637 = Coded Value Data Type
        builder.add(rowContext, RDF.TYPE, iri("http://purl.obolibrary.org/obo/NCIT_C95637"));
        if (row.get("ontologyTermURI") != null) {
          builder.add(
              rowContext, RDFS.ISDEFINEDBY, iri(TypeUtils.toString(row.get("ontologyTermURI"))));
        }
      } else {
        builder.add(rowContext, RDF.TYPE, iri("http://purl.org/linked-data/cube#Observation"));
      }
      builder.add(
          rowContext, iri("http://purl.org/linked-data/cube#dataSet"), encodedIRI(tableContext));

      for (String column : row.keySet()) {
        if (row.get(column) != null) {
          IRI columnContext = encodedIRI(tableContext + "/column/" + column);
          for (Value value : formatValue(row.get(column), columnMap.get(column), schemaContext)) {
            if (value != null) {
              builder.add(rowContext, columnContext, value);
            }
          }
        }
      }
    }
  }

  public static Value[] formatValue(Object object, Column column, String schemaContext)
      throws Exception {
    Value[] valList;
    if (object instanceof List) {
      List<Object> objList = (List<Object>) object;
      valList = new Value[objList.size()];
      for (int i = 0; i < valList.length; i++) {
        valList[i] = applyFormatting(objList.get(i), column, schemaContext);
      }
    } else {
      valList = new Value[1];
      valList[0] = applyFormatting(object, column, schemaContext);
    }
    return valList;
  }

  public static Value applyFormatting(Object o, Column column, String schemaContext)
      throws Exception {
    ColumnType columnType = column.getColumnType();
    CoreDatatype.XSD XSDType = columnTypeToXSD(columnType);

    if (columnType.equals(ColumnType.REF)
        || columnType.equals(ColumnType.REF_ARRAY)
        || columnType.equals(ColumnType.REFBACK)
        || columnType.equals(ColumnType.ONTOLOGY)
        || columnType.equals(ColumnType.ONTOLOGY_ARRAY)) {
      // TODO should the target IRI be resolvable here?
      TableMetadata tableMetadata = column.getRefTable();
      String primaryKey = tableMetadata.getPrimaryKeys().get(0);
      String pkValue = TypeUtils.toString(((Map) o).get(primaryKey));
      return encodedIRI(schemaContext + "/" + tableMetadata.getTableName() + "/" + pkValue);
    } else if (columnType.equals(ColumnType.FILE)) {
      Map map = ((Map) o);
      if (map.get("id") != null) {
        return encodedIRI(
            schemaContext
                + "/api/file/"
                + column.getTableName()
                + "/"
                + column.getName()
                + "/"
                + map.get("id"));
      } else {
        return null;
      }

    } else {

      if (o == null) {
        return null;
      }
      switch (XSDType) {
        case BOOLEAN:
          return literal((boolean) o);
        case DATE:
          return literal(TypeUtils.toString(o), XSDType);
        case DATETIME:
          return literal(TypeUtils.toString(o).substring(0, 19), XSDType);
        case DECIMAL:
          return literal(fixDouble(o));
        case STRING:
          return literal(TypeUtils.toString(o));
        case ANYURI:
          return encodedIRI(TypeUtils.toString(o));
        case INT:
          return literal((int) o);
        case LONG:
          return literal(fixLong(o));
        default:
          throw new Exception("XSD type formatting not supported for: " + XSDType);
      }
    }
  }

  /**
   * FIXME: apparently EMX2 can return DECIMAL data as Integers when it is a whole number?
   *
   * @param object
   * @return
   */
  private static Double fixDouble(Object object) {
    if (object instanceof Integer) {
      return Double.valueOf((int) object);
    } else {
      return (double) object;
    }
  }

  /**
   * FIXME: apparently EMX2 can return LONG data as Integers when it is a whole number?
   *
   * @param object
   * @return
   */
  private static Long fixLong(Object object) {
    if (object instanceof Integer) {
      return Long.valueOf((int) object);
    } else {
      return (long) object;
    }
  }
}
