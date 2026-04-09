package org.molgenis.emx2.rdf;

import java.util.*;
import java.util.stream.Collectors;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;
import org.molgenis.emx2.*;
import org.molgenis.emx2.rdf.ReverseAnnotationMapper.ColumnMapping;
import org.molgenis.emx2.rdf.TypeDiscriminator.TableAssignment;
import org.molgenis.emx2.rdf.mappers.NamespaceMapper;
import org.molgenis.emx2.utils.TypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RowBuilder {

  private static final Logger log = LoggerFactory.getLogger(RowBuilder.class);
  private static final IRI DCTERMS_IDENTIFIER_IRI =
      Values.iri("http://purl.org/dc/terms/identifier");

  public record RowBuildResult(Map<String, List<Row>> rowsByTable, List<String> warnings) {}

  private record ParentRef(Resource parentSubject, Column column) {}

  private RowBuilder() {}

  public static RowBuildResult buildRows(
      Map<Resource, Map<ColumnMapping, List<Value>>> matchedData,
      Map<Resource, Set<IRI>> typeMap,
      Schema schema) {

    Map<String, List<Row>> rowsByTable = new HashMap<>();
    Map<Resource, Row> rowBySubject = new HashMap<>();
    Map<Resource, String> tableBySubject = new HashMap<>();
    List<String> warnings = new ArrayList<>();
    Map<String, OntologyMapper> ontologyMappers = new HashMap<>();
    NamespaceMapper namespaceMapper = new NamespaceMapper("http://localhost/", schema);
    TypeDiscriminator typeDiscriminator = new TypeDiscriminator(schema);

    for (Map.Entry<Resource, Map<ColumnMapping, List<Value>>> entry : matchedData.entrySet()) {
      Resource subject = entry.getKey();
      Map<ColumnMapping, List<Value>> subjectData = entry.getValue();

      Set<IRI> types = typeMap.getOrDefault(subject, Set.of());
      TableAssignment assignment = typeDiscriminator.assignTable(types);
      if (assignment == null) {
        warnings.add("Skipping subject " + subject + ": no matching table for types " + types);
        continue;
      }

      Row row = new Row();
      Table targetTable = schema.getTable(assignment.tableName());
      if (targetTable != null) {
        setInitialPkeyValues(
            row, subject, subjectData, targetTable.getMetadata(), namespaceMapper, schema);
      }

      if (assignment.typeColumnName() != null && assignment.typeValue() != null) {
        row.set(assignment.typeColumnName(), assignment.typeValue());
      }

      for (Map.Entry<ColumnMapping, List<Value>> mappingEntry : subjectData.entrySet()) {
        ColumnMapping mapping = mappingEntry.getKey();
        if (!isTableOrAncestor(mapping.table().getTableName(), assignment.tableName(), schema)) {
          continue;
        }

        Column column = mapping.column();
        List<Value> values = mappingEntry.getValue();
        if (values.isEmpty()) {
          continue;
        }

        setColumnValue(row, column, values, matchedData, ontologyMappers, schema, namespaceMapper);
      }

      rowsByTable.computeIfAbsent(assignment.tableName(), k -> new ArrayList<>()).add(row);
      rowBySubject.put(subject, row);
      tableBySubject.put(subject, assignment.tableName());
    }

    discoverReferencedSubjects(
        matchedData,
        typeMap,
        rowsByTable,
        rowBySubject,
        tableBySubject,
        schema,
        namespaceMapper,
        typeDiscriminator);

    resolveCompositePkeys(
        rowsByTable, rowBySubject, tableBySubject, matchedData, schema, namespaceMapper);

    filterRowsMissingRequiredColumns(rowsByTable, schema, warnings);
    filterRowsWithUnresolvableForeignKeys(rowsByTable, schema, warnings);

    return new RowBuildResult(rowsByTable, warnings);
  }

  private static void discoverReferencedSubjects(
      Map<Resource, Map<ColumnMapping, List<Value>>> matchedData,
      Map<Resource, Set<IRI>> typeMap,
      Map<String, List<Row>> rowsByTable,
      Map<Resource, Row> rowBySubject,
      Map<Resource, String> tableBySubject,
      Schema schema,
      NamespaceMapper namespaceMapper,
      TypeDiscriminator typeDiscriminator) {

    for (Map<ColumnMapping, List<Value>> subjectData : matchedData.values()) {
      for (Map.Entry<ColumnMapping, List<Value>> mappingEntry : subjectData.entrySet()) {
        if (!mappingEntry.getKey().column().isReference()) {
          continue;
        }
        for (Value val : mappingEntry.getValue()) {
          if (!val.isIRI()) {
            continue;
          }
          Resource refSubject = (Resource) val;
          if (rowBySubject.containsKey(refSubject)) {
            continue;
          }
          Set<IRI> types = typeMap.getOrDefault(refSubject, Set.of());
          TableAssignment assignment = typeDiscriminator.assignTable(types);
          if (assignment == null) {
            continue;
          }
          Table targetTable = schema.getTable(assignment.tableName());
          if (targetTable == null) {
            continue;
          }
          Row row = new Row();
          setInitialPkeyValues(
              row, refSubject, Map.of(), targetTable.getMetadata(), namespaceMapper, schema);
          if (assignment.typeColumnName() != null && assignment.typeValue() != null) {
            row.set(assignment.typeColumnName(), assignment.typeValue());
          }
          rowsByTable.computeIfAbsent(assignment.tableName(), k -> new ArrayList<>()).add(row);
          rowBySubject.put(refSubject, row);
          tableBySubject.put(refSubject, assignment.tableName());
        }
      }
    }
  }

  private static void filterRowsMissingRequiredColumns(
      Map<String, List<Row>> rowsByTable, Schema schema, List<String> warnings) {
    for (Map.Entry<String, List<Row>> tableEntry : new HashMap<>(rowsByTable).entrySet()) {
      String tableName = tableEntry.getKey();
      Table table = schema.getTable(tableName);
      if (table == null) {
        continue;
      }
      List<Column> requiredNonKeyColumns =
          table.getMetadata().getColumns().stream()
              .filter(
                  col ->
                      col.isRequired()
                          && col.getKey() == 0
                          && col.getComputed() == null
                          && col.getDefaultValue() == null
                          && col.getColumnType() != ColumnType.REFBACK)
              .toList();
      if (requiredNonKeyColumns.isEmpty()) {
        continue;
      }
      List<Row> validRows =
          tableEntry.getValue().stream()
              .filter(
                  row ->
                      requiredNonKeyColumns.stream()
                          .allMatch(col -> row.getString(col.getName()) != null))
              .toList();
      int skipped = tableEntry.getValue().size() - validRows.size();
      if (skipped > 0) {
        warnings.add(
            "Skipped " + skipped + " rows in '" + tableName + "' with missing required columns");
        rowsByTable.put(tableName, validRows);
      }
    }
  }

  private static void filterRowsWithUnresolvableForeignKeys(
      Map<String, List<Row>> rowsByTable, Schema schema, List<String> warnings) {
    for (Map.Entry<String, List<Row>> tableEntry : new HashMap<>(rowsByTable).entrySet()) {
      String tableName = tableEntry.getKey();
      Table table = schema.getTable(tableName);
      if (table == null) {
        continue;
      }
      List<Column> unannotatedRefPkeys = findUnannotatedRefPkeyColumns(table);
      if (unannotatedRefPkeys.isEmpty()) {
        continue;
      }
      for (Column refCol : unannotatedRefPkeys) {
        List<Reference> references = refCol.getReferences();
        if (references.isEmpty()) {
          continue;
        }
        String refTableName = refCol.getRefTableName();
        List<Row> parentRows = rowsByTable.get(refTableName);
        if (parentRows == null) {
          continue;
        }
        String parentPkColName = references.get(0).getRefTo();
        String childFkColName = references.get(0).getName();
        Set<String> validParentIds =
            parentRows.stream()
                .map(r -> r.getString(parentPkColName))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<Row> validRows =
            tableEntry.getValue().stream()
                .filter(r -> validParentIds.contains(r.getString(childFkColName)))
                .toList();
        int skipped = tableEntry.getValue().size() - validRows.size();
        if (skipped > 0) {
          warnings.add(
              "Skipped " + skipped + " rows in '" + tableName + "' with unresolvable parent FK");
          rowsByTable.put(tableName, validRows);
        }
      }
    }
  }

  private static boolean isTableOrAncestor(
      String candidateTable, String targetTableName, Schema schema) {
    Table table = schema.getTable(targetTableName);
    if (table == null) {
      return false;
    }
    TableMetadata meta = table.getMetadata();
    while (meta != null) {
      if (candidateTable.equals(meta.getTableName())) {
        return true;
      }
      meta = meta.getInheritedTable();
    }
    return false;
  }

  private static void setInitialPkeyValues(
      Row row,
      Resource subject,
      Map<ColumnMapping, List<Value>> subjectData,
      TableMetadata table,
      NamespaceMapper namespaceMapper,
      Schema schema) {
    List<Column> pkeyColumns =
        table.getColumns().stream().filter(col -> col.getKey() == 1).toList();
    String encodedKey = extractUriLocalName(subject.stringValue());
    try {
      PrimaryKey pk = PrimaryKey.fromEncodedString(table, encodedKey);
      pk.getKeys()
          .forEach(
              (colName, value) -> {
                Column col = table.getColumn(colName);
                if (col != null && col.getKey() == 1) {
                  row.set(colName, value);
                }
              });
    } catch (Exception e) {
      log.trace("Could not decode pkey from URI {}: {}", subject, e.getMessage());
      String fallback = extractPid(subject, subjectData, namespaceMapper, schema);
      for (Column col : pkeyColumns) {
        if (!col.isReference()) {
          row.set(col.getName(), fallback);
        }
      }
    }
  }

  private static String extractUriLocalName(String uri) {
    int lastSlash = uri.lastIndexOf('/');
    int lastHash = uri.lastIndexOf('#');
    int pos = Math.max(lastSlash, lastHash);
    return pos >= 0 ? uri.substring(pos + 1) : uri;
  }

  private static void resolveCompositePkeys(
      Map<String, List<Row>> rowsByTable,
      Map<Resource, Row> rowBySubject,
      Map<Resource, String> tableBySubject,
      Map<Resource, Map<ColumnMapping, List<Value>>> matchedData,
      Schema schema,
      NamespaceMapper namespaceMapper) {

    Map<Resource, List<ParentRef>> reverseIndex = buildReverseIndex(matchedData);

    for (Map.Entry<String, List<Row>> tableEntry : new HashMap<>(rowsByTable).entrySet()) {
      String tableName = tableEntry.getKey();
      Table table = schema.getTable(tableName);
      if (table == null) {
        continue;
      }

      List<Column> unannotatedRefPkeys = findUnannotatedRefPkeyColumns(table);
      if (unannotatedRefPkeys.isEmpty()) {
        continue;
      }

      List<Row> resolvedRows = new ArrayList<>();
      for (Map.Entry<Resource, Row> subjectEntry : rowBySubject.entrySet()) {
        Resource subject = subjectEntry.getKey();
        if (!tableName.equals(tableBySubject.get(subject))) {
          continue;
        }
        Row originalRow = subjectEntry.getValue();
        List<ParentRef> parentRefs = reverseIndex.getOrDefault(subject, List.of());

        for (Column refCol : unannotatedRefPkeys) {
          if (originalRow.getString(refCol.getName()) != null) {
            resolvedRows.add(originalRow);
            continue;
          }
          String refTableName = refCol.getRefTableName();
          List<ParentRef> matchingParents =
              parentRefs.stream()
                  .filter(pr -> refTableName.equals(tableBySubject.get(pr.parentSubject())))
                  .toList();

          if (matchingParents.isEmpty()) {
            resolvedRows.add(originalRow);
          } else {
            for (ParentRef parent : matchingParents) {
              Row copy = copyRow(originalRow);
              String parentPid =
                  extractPid(
                      parent.parentSubject(),
                      matchedData.get(parent.parentSubject()),
                      namespaceMapper,
                      schema);
              copy.set(refCol.getName(), parentPid);
              resolvedRows.add(copy);
            }
          }
        }
      }

      if (!resolvedRows.isEmpty()) {
        rowsByTable.put(tableName, resolvedRows);
      }
    }
  }

  private static Map<Resource, List<ParentRef>> buildReverseIndex(
      Map<Resource, Map<ColumnMapping, List<Value>>> matchedData) {
    Map<Resource, List<ParentRef>> index = new HashMap<>();

    for (Map.Entry<Resource, Map<ColumnMapping, List<Value>>> entry : matchedData.entrySet()) {
      Resource parentSubject = entry.getKey();
      for (Map.Entry<ColumnMapping, List<Value>> mappingEntry : entry.getValue().entrySet()) {
        Column col = mappingEntry.getKey().column();
        if (!col.isReference()) {
          continue;
        }
        for (Value val : mappingEntry.getValue()) {
          if (val.isIRI() && matchedData.containsKey((Resource) val)) {
            Resource childSubject = (Resource) val;
            index
                .computeIfAbsent(childSubject, k -> new ArrayList<>())
                .add(new ParentRef(parentSubject, col));
          }
        }
      }
    }

    return index;
  }

  private static List<Column> findUnannotatedRefPkeyColumns(Table table) {
    List<Column> result = new ArrayList<>();
    for (Column col : table.getMetadata().getColumns()) {
      if (col.getKey() == 1
          && col.isReference()
          && (col.getSemantics() == null || col.getSemantics().length == 0)) {
        result.add(col);
      }
    }
    return result;
  }

  private static Row copyRow(Row original) {
    Row copy = new Row();
    original.getValueMap().forEach(copy::set);
    return copy;
  }

  private static void setColumnValue(
      Row row,
      Column column,
      List<Value> values,
      Map<Resource, Map<ColumnMapping, List<Value>>> matchedData,
      Map<String, OntologyMapper> ontologyMappers,
      Schema schema,
      NamespaceMapper namespaceMapper) {

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
                    v ->
                        extractPid(
                            (Resource) v,
                            matchedData.getOrDefault((Resource) v, Map.of()),
                            namespaceMapper,
                            schema))
                .toArray(String[]::new);
        row.setRefArray(colName, (Object[]) pids);
      } else {
        Value refValue = values.get(0);
        if (refValue.isIRI()) {
          Resource refSubject = (Resource) refValue;
          String refPid =
              extractPid(
                  refSubject,
                  matchedData.getOrDefault(refSubject, Map.of()),
                  namespaceMapper,
                  schema);
          row.set(colName, refPid);
        }
      }
    } else if (colType == ColumnType.EMAIL) {
      String email = stripMailtoPrefix(values.get(0).stringValue());
      row.set(colName, TypeUtils.getTypedValue(email, colType));
    } else if (colType == ColumnType.EMAIL_ARRAY) {
      String[] arr =
          values.stream().map(v -> stripMailtoPrefix(v.stringValue())).toArray(String[]::new);
      row.set(colName, arr);
    } else if (colType.name().endsWith("_ARRAY")) {
      String[] arr = values.stream().map(Value::stringValue).toArray(String[]::new);
      row.set(colName, arr);
    } else {
      row.set(colName, TypeUtils.getTypedValue(values.get(0).stringValue(), colType));
    }
  }

  private static String extractPid(
      Resource subject,
      Map<ColumnMapping, List<Value>> data,
      NamespaceMapper namespaceMapper,
      Schema schema) {
    for (Map.Entry<ColumnMapping, List<Value>> entry : data.entrySet()) {
      Column column = entry.getKey().column();
      String[] semantics = column.getSemantics();
      if (semantics == null) {
        continue;
      }
      for (String semantic : semantics) {
        if (!semantic.contains(":")) {
          continue;
        }
        IRI resolved = namespaceMapper.map(schema, semantic);
        if (DCTERMS_IDENTIFIER_IRI.equals(resolved) && !entry.getValue().isEmpty()) {
          return entry.getValue().get(0).stringValue();
        }
      }
    }
    return extractUriLocalName(subject.stringValue());
  }

  private static String stripMailtoPrefix(String value) {
    return value.startsWith("mailto:") ? value.substring(7) : value;
  }
}
