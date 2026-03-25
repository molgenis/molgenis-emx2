package org.molgenis.emx2.rdf;

import java.util.*;
import java.util.stream.Collectors;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.molgenis.emx2.*;
import org.molgenis.emx2.rdf.ReverseAnnotationMapper.ColumnMapping;
import org.molgenis.emx2.rdf.TypeDiscriminator.TableAssignment;
import org.molgenis.emx2.utils.TypeUtils;

public class RowBuilder {

  private static final String DCTERMS_IDENTIFIER = "http://purl.org/dc/terms/identifier";

  private static final Map<String, String> DEFAULT_PREFIX_MAP =
      DefaultNamespace.streamAll()
          .collect(Collectors.toMap(Namespace::getPrefix, Namespace::getName));

  public record RowBuildResult(Map<String, List<Row>> rowsByTable, List<String> warnings) {}

  private RowBuilder() {}

  public static RowBuildResult buildRows(
      Map<Resource, Map<ColumnMapping, List<Value>>> matchedData,
      Map<Resource, Set<IRI>> typeMap,
      Schema schema) {

    Map<String, List<Row>> rowsByTable = new HashMap<>();
    List<String> warnings = new ArrayList<>();
    Map<String, OntologyMapper> ontologyMappers = new HashMap<>();

    for (Map.Entry<Resource, Map<ColumnMapping, List<Value>>> entry : matchedData.entrySet()) {
      Resource subject = entry.getKey();
      Map<ColumnMapping, List<Value>> subjectData = entry.getValue();

      Set<IRI> types = typeMap.getOrDefault(subject, Set.of());
      TableAssignment assignment = TypeDiscriminator.assignTable(types);
      if (assignment == null) {
        warnings.add("Skipping subject " + subject + ": no matching table for types " + types);
        continue;
      }

      Row row = new Row();
      String identifier = extractPid(subject, subjectData);
      Table targetTable = schema.getTable(assignment.tableName());
      if (targetTable != null) {
        for (Column col : targetTable.getMetadata().getColumns()) {
          if (col.getKey() == 1) {
            row.set(col.getName(), identifier);
          }
        }
      }

      if (assignment.typeValue() != null) {
        row.set("type", assignment.typeValue());
      }

      for (Map.Entry<ColumnMapping, List<Value>> mappingEntry : subjectData.entrySet()) {
        ColumnMapping mapping = mappingEntry.getKey();
        if (!mapping.table().getTableName().equals(assignment.tableName())) {
          continue;
        }

        Column column = mapping.column();
        List<Value> values = mappingEntry.getValue();
        if (values.isEmpty()) {
          continue;
        }

        setColumnValue(row, column, values, matchedData, ontologyMappers, schema);
      }

      rowsByTable.computeIfAbsent(assignment.tableName(), k -> new ArrayList<>()).add(row);
    }

    return new RowBuildResult(rowsByTable, warnings);
  }

  private static void setColumnValue(
      Row row,
      Column column,
      List<Value> values,
      Map<Resource, Map<ColumnMapping, List<Value>>> matchedData,
      Map<String, OntologyMapper> ontologyMappers,
      Schema schema) {

    String colName = column.getName();
    ColumnType colType = column.getColumnType();

    if (column.isOntology()) {
      Table refTable = schema.getTable(column.getRefTableName());
      if (refTable == null) {
        return;
      }
      OntologyMapper mapper =
          ontologyMappers.computeIfAbsent(
              column.getRefTableName(), name -> new OntologyMapper(refTable));
      if (colType == ColumnType.ONTOLOGY_ARRAY) {
        String[] resolved =
            values.stream()
                .map(v -> mapper.resolve(v.stringValue()))
                .filter(Objects::nonNull)
                .toArray(String[]::new);
        row.set(colName, resolved);
      } else {
        String resolved = mapper.resolve(values.get(0).stringValue());
        if (resolved != null) {
          row.set(colName, resolved);
        }
      }
    } else if (column.isReference()) {
      if (colType == ColumnType.REF_ARRAY) {
        String[] pids =
            values.stream()
                .filter(Value::isIRI)
                .map(
                    v -> extractPid((Resource) v, matchedData.getOrDefault((Resource) v, Map.of())))
                .toArray(String[]::new);
        row.setRefArray(colName, (Object[]) pids);
      } else {
        Value refValue = values.get(0);
        if (refValue.isIRI()) {
          Resource refSubject = (Resource) refValue;
          String refPid = extractPid(refSubject, matchedData.getOrDefault(refSubject, Map.of()));
          row.set(colName, refPid);
        }
      }
    } else if (colType.name().endsWith("_ARRAY")) {
      String[] arr = values.stream().map(Value::stringValue).toArray(String[]::new);
      row.set(colName, arr);
    } else {
      row.set(colName, TypeUtils.getTypedValue(values.get(0).stringValue(), colType));
    }
  }

  private static String expandSemantic(String semantic) {
    int colon = semantic.indexOf(':');
    if (colon <= 0) {
      return semantic;
    }
    String prefix = semantic.substring(0, colon);
    String local = semantic.substring(colon + 1);
    String namespace = DEFAULT_PREFIX_MAP.get(prefix);
    return namespace != null ? namespace + local : semantic;
  }

  private static String extractPid(Resource subject, Map<ColumnMapping, List<Value>> data) {
    for (Map.Entry<ColumnMapping, List<Value>> entry : data.entrySet()) {
      Column column = entry.getKey().column();
      String[] semantics = column.getSemantics();
      if (semantics == null) {
        continue;
      }
      for (String semantic : semantics) {
        if (DCTERMS_IDENTIFIER.equals(expandSemantic(semantic)) && !entry.getValue().isEmpty()) {
          return entry.getValue().get(0).stringValue();
        }
      }
    }
    String uri = subject.stringValue();
    int lastSlash = uri.lastIndexOf('/');
    int lastHash = uri.lastIndexOf('#');
    int pos = Math.max(lastSlash, lastHash);
    return pos >= 0 ? uri.substring(pos + 1) : uri;
  }
}
