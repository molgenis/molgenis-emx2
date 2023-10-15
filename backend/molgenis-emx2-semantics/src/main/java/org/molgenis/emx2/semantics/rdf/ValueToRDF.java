package org.molgenis.emx2.semantics.rdf;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.molgenis.emx2.FilterBean.and;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.semantics.QueryHelper.selectColumns;
import static org.molgenis.emx2.semantics.rdf.ColumnTypeToXSDDataType.columnTypeToXSD;
import static org.molgenis.emx2.semantics.rdf.IRIParsingEncoding.encodedIRI;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.molgenis.emx2.*;

public class ValueToRDF {

  public static void describeValues(
      ModelBuilder builder, Table table, String rowId, String schemaContext) {
    Map<String, Column> columnMap = new HashMap<>();
    for (Column c : table.getMetadata().getColumns()) {
      columnMap.put(c.getName(), c);
    }
    String tableContext = schemaContext + "/" + table.getName();
    Query query = selectColumns(table);
    if (rowId != null) {
      if (table.getMetadata().getPrimaryKeyFields().size() > 1) {
        query.where(decodeRowIdToFilter(rowId));
      } else {
        query.where(f(table.getMetadata().getPrimaryKeyColumns().get(0).getName(), EQUALS, rowId));
      }
    }
    List<Row> rows = query.retrieveRows();
    if (rows.isEmpty()) {
      return;
    }

    for (Row row : rows) {
      IRI rowContext =
          getIriValuesBasedOnPkey(schemaContext, table.getMetadata(), row, "")
              .get(0); // note the prefix
      builder.add(rowContext, RDF.TYPE, encodedIRI(tableContext));
      // SIO:001187 = database row
      builder.add(rowContext, RDF.TYPE, iri("http://semanticscience.org/resource/SIO_001187"));
      if (table.getMetadata().getTableType() == TableType.ONTOLOGIES) {
        // NCIT:C95637 = Coded Value Data Type
        builder.add(rowContext, RDF.TYPE, iri("http://purl.obolibrary.org/obo/NCIT_C95637"));
        if (row.getString("ontologyTermURI") != null) {
          builder.add(rowContext, RDFS.ISDEFINEDBY, iri(row.getString("ontologyTermURI")));
        }
      } else {
        builder.add(rowContext, RDF.TYPE, iri("http://purl.org/linked-data/cube#Observation"));
      }
      builder.add(
          rowContext, iri("http://purl.org/linked-data/cube#dataSet"), encodedIRI(tableContext));

      for (Column column : table.getMetadata().getColumns()) {
        if (row.getString(column.getName()) != null) { // empty lists we don't want
          IRI columnContext = encodedIRI(tableContext + "/column/" + column.getName());
          for (Value value : formatValue(row, column, schemaContext)) {
            if (value != null) {
              builder.add(rowContext, columnContext, value);
            }
          }
        }
      }
    }
  }

  private static Filter decodeRowIdToFilter(String rowId) {
    try {
      List<NameValuePair> params = URLEncodedUtils.parse(new URI(rowId), StandardCharsets.UTF_8);
      List<Filter> filters = new ArrayList<>();
      params.forEach(param -> filters.add(f(param.getName(), EQUALS, param.getValue())));
      return and(filters);
    } catch (Exception e) {
      throw new MolgenisException("Decode row to filter failed for id " + rowId);
    }
  }

  private static List<IRI> getIriValuesBasedOnPkey(
      String schemaContext, TableMetadata tableMetadata, Row row, String prefix) {
    String[] keys = row.getStringArray(prefix + tableMetadata.getPrimaryKeys().get(0));
    // check null
    if (keys == null) {
      return List.of();
    }
    // simple keys get the key value
    if (tableMetadata.getPrimaryKeyFields().size() == 1) {
      return Arrays.stream(keys)
          .map(
              value -> encodedIRI(schemaContext + "/" + tableMetadata.getTableName() + "/" + value))
          .toList();
    }
    // composite keys get pattern of part1=a&part2=b
    else {
      List<List<NameValuePair>> keyValuePairList = new ArrayList<>();
      tableMetadata.getPrimaryKeyFields().stream()
          .forEach(
              field -> {
                String[] keyParts = row.getStringArray(prefix + field.getName());
                for (int i = 0; i < keyParts.length; i++) {
                  if (keyValuePairList.get(i) == null) keyValuePairList.add(new ArrayList<>());
                  keyValuePairList
                      .get(i)
                      .add(new BasicNameValuePair(prefix + field.getName(), keyParts[i]));
                }
              });
      return keyValuePairList.stream()
          .map(
              valuePairList ->
                  encodedIRI(
                      schemaContext
                          + "/"
                          + tableMetadata.getTableName()
                          + "/"
                          + URLEncodedUtils.format(valuePairList, StandardCharsets.UTF_8)))
          .toList();
    }
  }

  public static List<Value> formatValue(Row row, Column column, String schemaContext) {
    List<Value> values = new ArrayList<>();

    ColumnType columnType = column.getColumnType();
    if (columnType.isReference()) {
      values.addAll(
          getIriValuesBasedOnPkey(
              schemaContext, column.getRefTable(), row, column.getName() + "_"));
    } else if (columnType.equals(ColumnType.FILE)) {
      if (row.getString(column.getName() + "_id") != null) {
        values.add(
            encodedIRI(
                schemaContext
                    + "/api/file/"
                    + column.getTableName()
                    + "/"
                    + column.getName()
                    + "/"));
      }
    } else {
      values.addAll(getLiteralValues(row, column));
    }
    return values;
  }

  public static List<? extends Value> getLiteralValues(Row row, Column column) {
    CoreDatatype.XSD XSDType = columnTypeToXSD(column.getColumnType());
    switch (XSDType) {
      case BOOLEAN:
        return Arrays.stream(row.getBooleanArray(column.getName())).map(Values::literal).toList();
      case DATE:
        return Arrays.stream(row.getDateArray(column.getName()))
            .map(value -> literal(value.toString(), XSDType))
            .toList();
      case DATETIME:
        return Arrays.stream(row.getDateTimeArray(column.getName()))
            .map(value -> literal(value.toString().substring(0, 19), XSDType))
            .toList();
      case DECIMAL:
        return Arrays.stream(row.getDecimalArray(column.getName())).map(Values::literal).toList();
      case STRING:
        return Arrays.stream(row.getStringArray(column.getName())).map(Values::literal).toList();
      case ANYURI:
        return Arrays.stream(row.getStringArray(column.getName()))
            .map(
                value -> {
                  System.out.println("found " + value);
                  return encodedIRI(value);
                })
            .toList();
      case INT:
        return Arrays.stream(row.getIntegerArray(column.getName())).map(Values::literal).toList();
      case LONG:
        return Arrays.stream(row.getLongArray(column.getName())).map(Values::literal).toList();
      default:
        throw new MolgenisException("XSD type formatting not supported for: " + XSDType);
    }
  }
}
