package org.molgenis.emx2.rdf;

import static java.util.Map.entry;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.molgenis.emx2.Constants.API_FILE;
import static org.molgenis.emx2.rdf.RdfUtils.getSchemaNamespace;

import com.google.common.net.UrlEscapers;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.util.Values;
import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.URIUtils;

/**
 * Used for functionalities that are {@link ColumnType} specific. This includes:
 *
 * <ul>
 *   <li>Retrieving the appropiate {@link CoreDatatype.XSD}
 *   <li>The method for extracting the {@link Value}'s for that specific {@link ColumnType} usable
 *       for generating the RDF
 * </ul>
 */
public abstract class ColumnTypeRdfMapper {
  // All ColumnType mappings.
  // mapping.keySet() should be equal to ColumnType.values()
  private static final Map<ColumnType, RdfColumnType> mapping =
      Map.ofEntries(
          // SIMPLE
          entry(ColumnType.BOOL, RdfColumnType.BOOLEAN),
          entry(ColumnType.BOOL_ARRAY, RdfColumnType.BOOLEAN),
          entry(ColumnType.UUID, RdfColumnType.UUID),
          entry(ColumnType.UUID_ARRAY, RdfColumnType.UUID),
          entry(ColumnType.FILE, RdfColumnType.FILE),

          // STRING
          entry(ColumnType.STRING, RdfColumnType.STRING),
          entry(ColumnType.STRING_ARRAY, RdfColumnType.STRING),
          entry(ColumnType.TEXT, RdfColumnType.STRING),
          entry(ColumnType.TEXT_ARRAY, RdfColumnType.STRING),
          entry(ColumnType.JSON, RdfColumnType.STRING),

          // NUMERIC
          entry(ColumnType.INT, RdfColumnType.INT),
          entry(ColumnType.INT_ARRAY, RdfColumnType.INT),
          entry(ColumnType.LONG, RdfColumnType.LONG),
          entry(ColumnType.LONG_ARRAY, RdfColumnType.LONG),
          entry(ColumnType.DECIMAL, RdfColumnType.DECIMAL),
          entry(ColumnType.DECIMAL_ARRAY, RdfColumnType.DECIMAL),
          entry(ColumnType.DATE, RdfColumnType.DATE),
          entry(ColumnType.DATE_ARRAY, RdfColumnType.DATE),
          entry(ColumnType.DATETIME, RdfColumnType.DATETIME),
          entry(ColumnType.DATETIME_ARRAY, RdfColumnType.DATETIME),
          entry(ColumnType.PERIOD, RdfColumnType.DURATION),
          entry(ColumnType.PERIOD_ARRAY, RdfColumnType.DURATION),

          // RELATIONSHIP
          entry(ColumnType.REF, RdfColumnType.REFERENCE),
          entry(ColumnType.REF_ARRAY, RdfColumnType.REFERENCE),
          entry(ColumnType.REFBACK, RdfColumnType.REFERENCE),

          // LAYOUT and other constants
          entry(ColumnType.HEADING, RdfColumnType.SKIP), // Should not be in RDF output.

          // format flavors that extend a baseType
          entry(ColumnType.AUTO_ID, RdfColumnType.STRING),
          entry(ColumnType.ONTOLOGY, RdfColumnType.ONTOLOGY),
          entry(ColumnType.ONTOLOGY_ARRAY, RdfColumnType.ONTOLOGY),
          entry(ColumnType.EMAIL, RdfColumnType.EMAIL),
          entry(ColumnType.EMAIL_ARRAY, RdfColumnType.EMAIL),
          entry(ColumnType.HYPERLINK, RdfColumnType.URI),
          entry(ColumnType.HYPERLINK_ARRAY, RdfColumnType.URI));

  /** Retrieve all {@link ColumnType}{@code 's} which have a mapping available. */
  static Set<ColumnType> getMapperKeys() {
    return mapping.keySet();
  }

  public static CoreDatatype.XSD getCoreDataType(Column column) {
    return getCoreDataType(column.getColumnType());
  }

  public static CoreDatatype.XSD getCoreDataType(ColumnType columnType) {
    return mapping.get(columnType).getCoreDatatype();
  }

  /**
   * Returns the output for the defined cell:
   *
   * <ul>
   *   <li>If {@link ColumnType} should not be represented in RDF, returns an empty {@link Set}
   *   <li>If field is empty, returns an empty {@link Set}
   *   <li>If field has value(s), returns a filled {@link Set}
   * </ul>
   */
  public static Set<Value> retrieveValues(
      RdfMapData rdfMapData, final Row row, final Column column) {
    return retrieveValues(rdfMapData, row, column, mapping.get(column.getColumnType()));
  }

  /**
   * Same as {@link #retrieveValues(RdfMapData, Row, Column)}, except manually defining which {@link
   * RdfColumnType} should be used. <br>
   * <br>
   * It is suggested to only use this method if really needed (for example if needing an email as a
   * string literal in RDF instead of default behavior which creates a {@code mailto:} IRI).
   * <strong>Using this incorrectly will cause issues!</strong>
   *
   * @see #retrieveValues(RdfMapData, Row, Column)
   */
  public static Set<Value> retrieveValues(
      RdfMapData rdfMapData,
      final Row row,
      final Column column,
      final RdfColumnType rdfColumnType) {
    return (rdfColumnType.isEmpty(row, column)
        ? Set.of()
        : rdfColumnType.retrieveValues(rdfMapData, row, column));
  }

  public enum RdfColumnType {
    BOOLEAN(CoreDatatype.XSD.BOOLEAN) {
      @Override
      Set<Value> retrieveValues(RdfMapData rdfMapData, Row row, Column column) {
        return basicRetrieval(row.getBooleanArray(column.getName()), Values::literal);
      }
    },
    UUID(CoreDatatype.XSD.ANYURI) {
      @Override
      Set<Value> retrieveValues(RdfMapData rdfMapData, Row row, Column column) {
        return basicRetrievalString(
            row.getStringArray(column.getName()), (i) -> URIUtils.encodedIRI("urn:uuid:" + i));
      }
    },
    STRING(CoreDatatype.XSD.STRING) {
      @Override
      Set<Value> retrieveValues(RdfMapData rdfMapData, Row row, Column column) {
        return basicRetrievalString(row.getStringArrayPreserveDelimiter(column), Values::literal);
      }
    },
    INT(CoreDatatype.XSD.INT) {
      @Override
      Set<Value> retrieveValues(RdfMapData rdfMapData, Row row, Column column) {
        return basicRetrieval(row.getIntegerArray(column.getName()), Values::literal);
      }
    },
    LONG(CoreDatatype.XSD.LONG) {
      @Override
      Set<Value> retrieveValues(RdfMapData rdfMapData, Row row, Column column) {
        return basicRetrieval(row.getLongArray(column.getName()), Values::literal);
      }
    },
    DECIMAL(CoreDatatype.XSD.DECIMAL) {
      @Override
      Set<Value> retrieveValues(RdfMapData rdfMapData, Row row, Column column) {
        return basicRetrieval(row.getDecimalArray(column.getName()), Values::literal);
      }
    },
    DATE(CoreDatatype.XSD.DATE) {
      @Override
      Set<Value> retrieveValues(RdfMapData rdfMapData, Row row, Column column) {
        return basicRetrieval(
            row.getDateArray(column.getName()), (i) -> literal(i.toString(), getCoreDatatype()));
      }
    },
    DATETIME(CoreDatatype.XSD.DATETIME) {
      @Override
      Set<Value> retrieveValues(RdfMapData rdfMapData, Row row, Column column) {
        return basicRetrieval(
            row.getDateTimeArray(column.getName()),
            (i) -> literal(dateTimeFormatter.format((LocalDateTime) i), getCoreDatatype()));
      }
    },
    DURATION(CoreDatatype.XSD.DURATION) {
      @Override
      Set<Value> retrieveValues(RdfMapData rdfMapData, Row row, Column column) {
        return basicRetrieval(row.getPeriodArray(column.getName()), Values::literal);
      }
    },

    URI(CoreDatatype.XSD.ANYURI) {
      @Override
      Set<Value> retrieveValues(RdfMapData rdfMapData, Row row, Column column) {
        return basicRetrievalString(row.getStringArray(column.getName()), URIUtils::encodedIRI);
      }
    },
    EMAIL(CoreDatatype.XSD.ANYURI) {
      @Override
      Set<Value> retrieveValues(RdfMapData rdfMapData, Row row, Column column) {
        return basicRetrievalString(
            row.getStringArray(column.getName()), (i) -> URIUtils.encodedIRI("mailto:" + i));
      }
    },
    FILE(CoreDatatype.XSD.ANYURI) {
      @Override
      Set<Value> retrieveValues(RdfMapData rdfMapData, Row row, Column column) {
        final String schemaPath =
            UrlEscapers.urlPathSegmentEscaper().escape(column.getSchemaName());
        final String tablePath = UrlEscapers.urlPathSegmentEscaper().escape(column.getTableName());
        final String columnPath = UrlEscapers.urlPathSegmentEscaper().escape(column.getName());
        final String fileName =
            UrlEscapers.urlPathSegmentEscaper().escape(row.getString(column.getName()));
        return Set.of(
            Values.iri(
                rdfMapData.getBaseURL()
                    + "/"
                    + schemaPath
                    + API_FILE
                    + "/"
                    + tablePath
                    + "/"
                    + columnPath
                    + "/"
                    + fileName));
      }
    },
    REFERENCE(CoreDatatype.XSD.ANYURI) {
      @Override
      Set<Value> retrieveValues(RdfMapData rdfMapData, Row row, Column column) {
        Map<String, String> colNameToRefTableColName =
            column.getReferences().stream()
                .collect(Collectors.toMap(Reference::getName, Reference::getRefTo));
        return RdfColumnType.retrieveReferenceValues(
            rdfMapData, row, column, colNameToRefTableColName);
      }

      @Override
      boolean isEmpty(Row row, Column column) {
        // Composite key requires all fields to be filled. Using refLink from a non-required field
        // could cause a part of the composite key to be defined.
        // Therefore, if a composite key is partly defined, assume it is not defined.
        return column.getReferences().stream().anyMatch(i -> row.getString(i.getName()) == null);
      }
    },
    ONTOLOGY(CoreDatatype.XSD.ANYURI) {
      @Override
      Set<Value> retrieveValues(RdfMapData rdfMapData, Row row, Column column) {
        final TableMetadata target = column.getRefTable();
        final String rootTableName =
            UrlEscapers.urlPathSegmentEscaper().escape(target.getRootTable().getIdentifier());
        final Namespace ns =
            getSchemaNamespace(rdfMapData.getBaseURL(), target.getRootTable().getSchema());

        String[] names =
            (column.isArray()
                ? row.getStringArray(column.getName())
                : new String[] {row.getString(column.getName())});

        Map<String, IRI> mappedNames =
            rdfMapData
                .getOntologyIriMapper()
                .map(column.getRefTable().getSchemaName(), column.getRefTableName(), names);

        return mappedNames.keySet().stream()
            .map(
                i ->
                    (mappedNames.get(i) != null
                        ? mappedNames.get(i)
                        : Values.iri(
                            ns,
                            rootTableName
                                + "?"
                                + new PrimaryKey(Map.of("name", i)).getEncodedValue())))
            .collect(Collectors.toUnmodifiableSet());
      }
    },
    SKIP(CoreDatatype.XSD.STRING) {
      @Override
      Set<Value> retrieveValues(RdfMapData rdfMapData, Row row, Column column) {
        return Set.of();
      }
    };

    private static final DateTimeFormatter dateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final CoreDatatype.XSD coreDatatype;

    public CoreDatatype.XSD getCoreDatatype() {
      return coreDatatype;
    }

    RdfColumnType(CoreDatatype.XSD coreDatatype) {
      this.coreDatatype = coreDatatype;
    }

    /**
     * Generic retrieval function. Can be used for {@link Values#literal(Object)} or any custom
     * function which outputs a {@link Value}.
     */
    private static Set<Value> basicRetrieval(Object[] object, Function<Object, Value> function) {
      return Arrays.stream(object)
          .map(value -> (Value) function.apply(value))
          .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Similar to {@link #basicRetrieval(Object[], Function)}, but with some changes:
     *
     * <ul>
     *   <li>Enforces {@link Values#literal(String)} to be called when using it as {@code function}
     *       parameter
     *   <li>Removes the need for casting in custom functions that require a {@link String} as input
     * </ul>
     */
    private static Set<Value> basicRetrievalString(
        String[] object, Function<String, Value> function) {
      return Arrays.stream(object)
          .map(value -> (Value) function.apply(value))
          .collect(Collectors.toUnmodifiableSet());
    }

    abstract Set<Value> retrieveValues(
        final RdfMapData rdfMapData, final Row row, final Column column);

    boolean isEmpty(final Row row, final Column column) {
      return row.getString(column.getName()) == null;
    }

    private static Set<Value> retrieveReferenceValues(
        final RdfMapData rdfMapData,
        final Row row,
        final Column tableColumn,
        final Map<String, String> colNameToRefTableColName) {
      final TableMetadata target = tableColumn.getRefTable();
      final String rootTableName =
          UrlEscapers.urlPathSegmentEscaper().escape(target.getRootTable().getIdentifier());
      final Namespace ns =
          getSchemaNamespace(rdfMapData.getBaseURL(), target.getRootTable().getSchema());

      final Map<Integer, Map<String, String>> items = new HashMap<>();
      for (final String colName : colNameToRefTableColName.keySet()) {
        final String[] values =
            (tableColumn.isArray()
                ? row.getStringArray(colName)
                : new String[] {row.getString(colName)});

        if (values == null) continue;

        for (int i = 0; i < values.length; i++) {
          Map<String, String> keyValuePairs = items.getOrDefault(i, new LinkedHashMap<>());
          keyValuePairs.put(colNameToRefTableColName.get(colName), values[i]);
          items.put(i, keyValuePairs);
        }
      }

      final Set<Value> values = new HashSet<>();
      for (final Map<String, String> item : items.values()) {
        PrimaryKey key = new PrimaryKey(item);
        values.add(Values.iri(ns, rootTableName + "?" + key.getEncodedValue()));
      }
      return Set.copyOf(values);
    }
  }
}
