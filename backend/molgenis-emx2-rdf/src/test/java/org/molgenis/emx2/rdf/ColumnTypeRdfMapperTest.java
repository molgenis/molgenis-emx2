package org.molgenis.emx2.rdf;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.TestDatabaseFactory;

/**
 * When a new {@link ColumnType} is added and/or {@link #validateAllColumnTypesCovered()} fails, be
 * sure to add thew new {@link ColumnType} to all tests!!!
 */
class ColumnTypeRdfMapperTest {
  static final String TEST_SCHEMA = "AllColumnTypes";
  static final String TEST_TABLE = "TestTable";
  static final String REF_TABLE = "TestRefTable";
  static final String REFBACK_TABLE = "TestRefBackTable";
  static final String ONT_TABLE = "TestOntology";
  static final ClassLoader classLoader = ColumnTypeRdfMapperTest.class.getClassLoader();
  static final SimpleValueFactory factory = SimpleValueFactory.getInstance();
  static final String baseUrl = "http://localhost:8080/";
  static final String rdfApiUrlPrefix = baseUrl + TEST_SCHEMA + "/api/rdf/";
  static final String fileApiUrlPrefix = baseUrl + TEST_SCHEMA + "/api/file/";
  static final ColumnTypeRdfMapper mapper = new ColumnTypeRdfMapper(baseUrl);
  static final File TEST_FILE =
      new File(classLoader.getResource("testfiles/molgenis.png").getFile());

  static Database database;
  static Schema allColumnTypes;
  static Row firstRow;

  @BeforeAll
  public static void setup() {
    // Initialize schema.
    database = TestDatabaseFactory.getTestDatabase();
    allColumnTypes = database.dropCreateSchema(TEST_SCHEMA);

    // Generates a column for each ColumnType.
    // Filters out REFBACK so that it can be added as last step when all REFs are generated.
    List<Column> columns =
        new ArrayList<>(
            Arrays.stream(ColumnType.values())
                .map((value) -> column(value.name(), value))
                .filter((column -> !column.getColumnType().equals(ColumnType.REFBACK)))
                .toList());

    // Defines column-specific settings.
    for (Column column : columns) {
      switch (column.getColumnType()) {
        case STRING -> column.setPkey();
        case REF, REF_ARRAY -> column.setRefTable(REF_TABLE);
        case ONTOLOGY, ONTOLOGY_ARRAY -> column.setRefTable(ONT_TABLE);
      }
    }
    // refback is possible in one go since 26 nov 2024 :-)
    columns.add(
        column(ColumnType.REFBACK.name(), ColumnType.REFBACK)
            .setRefTable(REFBACK_TABLE)
            .setRefBack("ref"));

    // Creates tables.
    allColumnTypes.create(
        // Ontology table
        table(ONT_TABLE).setTableType(TableType.ONTOLOGIES),
        // Table to ref towards
        table(REF_TABLE, column("id", ColumnType.STRING).setPkey()),
        // Table to test on
        table(TEST_TABLE, columns.toArray(new Column[0])),
        // Table to get refbacks from
        table(
            REFBACK_TABLE,
            column("id", ColumnType.STRING).setPkey(),
            column("ref", ColumnType.REF).setRefTable(TEST_TABLE)));

    // Inserts table data
    allColumnTypes
        .getTable(ONT_TABLE)
        .insert(
            row("name", "aa", "ontologyTermURI", "http://example.com/aa"),
            row("name", "bb", "ontologyTermURI", "http://example.com/bb"),
            row("name", "cc"));

    allColumnTypes.getTable(REF_TABLE).insert(row("id", "1"), row("id", "2"), row("id", "3"));

    allColumnTypes
        .getTable(TEST_TABLE)
        .insert(
            row(
                // SIMPLE
                ColumnType.BOOL.name(),
                "true",
                ColumnType.BOOL_ARRAY.name(),
                "true,false",
                ColumnType.UUID.name(),
                "e8af409e-86f7-11ef-85b2-6b76fd707d70",
                ColumnType.UUID_ARRAY.name(),
                "e8af409e-86f7-11ef-85b2-6b76fd707d70,14bfb4ca-86f8-11ef-8cc0-378b59fe72e8",
                ColumnType.FILE.name(),
                TEST_FILE,
                // STRING
                ColumnType.STRING.name(),
                "lonelyString",
                ColumnType.STRING_ARRAY.name(),
                "string1,string2",
                ColumnType.TEXT.name(),
                "lonelyText",
                ColumnType.TEXT_ARRAY.name(),
                "text1,text2",
                ColumnType.JSON.name(),
                "{\"a\":1,\"b\":2}",
                // NUMERIC
                ColumnType.INT.name(),
                "0",
                ColumnType.INT_ARRAY.name(),
                "1,2",
                ColumnType.LONG.name(),
                "3",
                ColumnType.LONG_ARRAY.name(),
                "4,5",
                ColumnType.DECIMAL.name(),
                "0.5",
                ColumnType.DECIMAL_ARRAY.name(),
                "1.5,2.5",
                ColumnType.DATE.name(),
                "2000-01-01",
                ColumnType.DATE_ARRAY.name(),
                "2001-01-01,2002-01-01",
                ColumnType.DATETIME.name(),
                "3000-01-01T12:30:00",
                ColumnType.DATETIME_ARRAY.name(),
                "3001-01-01T12:30:00,3002-01-01T12:30:00",
                ColumnType.PERIOD.name(),
                "P1D",
                ColumnType.PERIOD_ARRAY.name(),
                "P1M,P1Y",
                // RELATIONSHIP
                ColumnType.REF.name(),
                "1",
                ColumnType.REF_ARRAY.name(),
                "2,3",
                // -- no manual entry: ColumnType.REFBACK
                // LAYOUT and other constants
                ColumnType.HEADING.name(),
                "heading",
                // format flavors that extend a baseType
                // -- no manual entry: ColumnType.AUTO_ID
                ColumnType.ONTOLOGY.name(),
                "aa",
                ColumnType.ONTOLOGY_ARRAY.name(),
                "bb,cc",
                ColumnType.EMAIL.name(),
                "aap@example.com",
                ColumnType.EMAIL_ARRAY.name(),
                "noot@example.com,mies@example.com",
                ColumnType.HYPERLINK.name(),
                "https://molgenis.org",
                ColumnType.HYPERLINK_ARRAY.name(),
                "https://molgenis.org, https://github.com/molgenis"));

    allColumnTypes.getTable(REFBACK_TABLE).insert(row("id", "1", "ref", "lonelyString"));

    // Use query to explicitly retrieve all rows as the following would exclude REFBACK values:
    // allColumnTypes.getTable(TEST_TABLE).retrieveRows()
    SelectColumn[] selectColumns =
        Arrays.stream(ColumnType.values())
            .map(i -> new SelectColumn(i.name()))
            .toArray(SelectColumn[]::new);

    // Describe first row for easy access.
    firstRow =
        allColumnTypes.getTable(TEST_TABLE).query().select(selectColumns).retrieveRows().get(0);
  }

  @AfterAll
  public static void tearDown() {
    database = TestDatabaseFactory.getTestDatabase();
    database.dropSchema(allColumnTypes.getName());
  }

  private Set<Value> retrieveValues(String columnName) {
    return mapper.retrieveValues(
        firstRow, allColumnTypes.getTable(TEST_TABLE).getMetadata().getColumn(columnName));
  }

  private Set<Value> retrieveValuesCustom(
      String columnName, ColumnTypeRdfMapper.RdfColumnType rdfColumnType) {
    return mapper.retrieveValues(
        firstRow,
        allColumnTypes.getTable(TEST_TABLE).getMetadata().getColumn(columnName),
        rdfColumnType);
  }

  private Value retrieveFirstValue(String columnName) {
    return retrieveValues(columnName).stream().findFirst().get();
  }

  /**
   * If {@link ColumnTypeRdfMapper} misses any mappings for {@link ColumnType}, this test will fail.
   * Should prevent new types being added without implementing the API support as well.
   */
  @Test
  void validateAllColumnTypesCovered() {
    Set<ColumnType> columnTypes = Arrays.stream(ColumnType.values()).collect(Collectors.toSet());
    Set<ColumnType> columnMappings = ColumnTypeRdfMapper.getMapperKeys();

    Assertions.assertEquals(columnTypes, columnMappings);
  }

  /**
   * Validates if {@link org.eclipse.rdf4j.model.Value} is of expected type. Only validates the
   * non-array {@link ColumnType}{@code s} (as array-versions should be of identical type).
   */
  @Test
  void validateValueTypes() {
    Row row = allColumnTypes.getTable(TEST_TABLE).retrieveRows().get(0);

    Assertions.assertAll(
        // SIMPLE
        () -> Assertions.assertTrue(retrieveFirstValue(ColumnType.BOOL.name()).isLiteral()),
        () -> Assertions.assertTrue(retrieveFirstValue(ColumnType.UUID.name()).isIRI()),
        () -> Assertions.assertTrue(retrieveFirstValue(ColumnType.FILE.name()).isIRI()),
        // STRING
        () -> Assertions.assertTrue(retrieveFirstValue(ColumnType.STRING.name()).isLiteral()),
        () -> Assertions.assertTrue(retrieveFirstValue(ColumnType.TEXT.name()).isLiteral()),
        () -> Assertions.assertTrue(retrieveFirstValue(ColumnType.JSON.name()).isLiteral()),

        // NUMERIC
        () -> Assertions.assertTrue(retrieveFirstValue(ColumnType.INT.name()).isLiteral()),
        () -> Assertions.assertTrue(retrieveFirstValue(ColumnType.LONG.name()).isLiteral()),
        () -> Assertions.assertTrue(retrieveFirstValue(ColumnType.DECIMAL.name()).isLiteral()),
        () -> Assertions.assertTrue(retrieveFirstValue(ColumnType.DATE.name()).isLiteral()),
        () -> Assertions.assertTrue(retrieveFirstValue(ColumnType.DATETIME.name()).isLiteral()),
        () -> Assertions.assertTrue(retrieveFirstValue(ColumnType.PERIOD.name()).isLiteral()),

        // RELATIONSHIP
        () -> Assertions.assertTrue(retrieveFirstValue(ColumnType.REF.name()).isIRI()),
        () -> Assertions.assertTrue(retrieveFirstValue(ColumnType.REFBACK.name()).isIRI()),

        // LAYOUT and other constants
        // ColumnType.HEADING.name() -> no Value should be present to validate on

        // format flavors that extend a baseType
        () -> Assertions.assertTrue(retrieveFirstValue(ColumnType.AUTO_ID.name()).isLiteral()),
        () -> Assertions.assertTrue(retrieveFirstValue(ColumnType.ONTOLOGY.name()).isIRI()),
        () -> Assertions.assertTrue(retrieveFirstValue(ColumnType.EMAIL.name()).isIRI()),
        () -> Assertions.assertTrue(retrieveFirstValue(ColumnType.HYPERLINK.name()).isIRI()));
  }

  @Test
  void validateValuesRetrieval() {
    Assertions.assertAll(
        // SIMPLE
        () ->
            Assertions.assertEquals(
                Set.of(Values.literal(true)), retrieveValues(ColumnType.BOOL.name())),
        () ->
            Assertions.assertEquals(
                Set.of(Values.literal(true), Values.literal(false)),
                retrieveValues(ColumnType.BOOL_ARRAY.name())),
        () ->
            Assertions.assertEquals(
                Set.of(Values.iri("urn:uuid:e8af409e-86f7-11ef-85b2-6b76fd707d70")),
                retrieveValues(ColumnType.UUID.name())),
        () ->
            Assertions.assertEquals(
                Set.of(
                    Values.iri("urn:uuid:e8af409e-86f7-11ef-85b2-6b76fd707d70"),
                    Values.iri("urn:uuid:14bfb4ca-86f8-11ef-8cc0-378b59fe72e8")),
                retrieveValues(ColumnType.UUID_ARRAY.name())),
        () ->
            Assertions.assertEquals(
                Set.of(
                    Values.iri(
                        fileApiUrlPrefix
                            + TEST_TABLE
                            + "/"
                            + ColumnType.FILE.name()
                            + "/"
                            // Not sure how to retrieve more directly as changes everytime
                            + firstRow.getString(ColumnType.FILE.name()))),
                retrieveValues(ColumnType.FILE.name())),

        // STRING
        () ->
            Assertions.assertEquals(
                Set.of(Values.literal("lonelyString", CoreDatatype.XSD.STRING)),
                retrieveValues(ColumnType.STRING.name())),
        () ->
            Assertions.assertEquals(
                Set.of(
                    Values.literal("string1", CoreDatatype.XSD.STRING),
                    Values.literal("string2", CoreDatatype.XSD.STRING)),
                retrieveValues(ColumnType.STRING_ARRAY.name())),
        () ->
            Assertions.assertEquals(
                Set.of(Values.literal("lonelyText", CoreDatatype.XSD.STRING)),
                retrieveValues(ColumnType.TEXT.name())),
        () ->
            Assertions.assertEquals(
                Set.of(
                    Values.literal("text1", CoreDatatype.XSD.STRING),
                    Values.literal("text2", CoreDatatype.XSD.STRING)),
                retrieveValues(ColumnType.TEXT_ARRAY.name())),
        () ->
            Assertions.assertEquals(
                Set.of(Values.literal("{\"a\":1,\"b\":2}", CoreDatatype.XSD.STRING)),
                retrieveValues(ColumnType.JSON.name())),

        // NUMERIC
        () ->
            Assertions.assertEquals(
                Set.of(Values.literal(0)), retrieveValues(ColumnType.INT.name())),
        () ->
            Assertions.assertEquals(
                Set.of(Values.literal(1), Values.literal(2)),
                retrieveValues(ColumnType.INT_ARRAY.name())),
        () ->
            Assertions.assertEquals(
                Set.of(Values.literal(3L)), retrieveValues(ColumnType.LONG.name())),
        () ->
            Assertions.assertEquals(
                Set.of(Values.literal(4L), Values.literal(5L)),
                retrieveValues(ColumnType.LONG_ARRAY.name())),
        () ->
            Assertions.assertEquals(
                Set.of(Values.literal(0.5D)), retrieveValues(ColumnType.DECIMAL.name())),
        () ->
            Assertions.assertEquals(
                Set.of(Values.literal(1.5D), Values.literal(2.5D)),
                retrieveValues(ColumnType.DECIMAL_ARRAY.name())),
        () ->
            Assertions.assertEquals(
                Set.of(Values.literal("2000-01-01", CoreDatatype.XSD.DATE)),
                retrieveValues(ColumnType.DATE.name())),
        () ->
            Assertions.assertEquals(
                Set.of(
                    Values.literal("2001-01-01", CoreDatatype.XSD.DATE),
                    Values.literal("2002-01-01", CoreDatatype.XSD.DATE)),
                retrieveValues(ColumnType.DATE_ARRAY.name())),
        () ->
            Assertions.assertEquals(
                Set.of(Values.literal("3000-01-01T12:30:00", CoreDatatype.XSD.DATETIME)),
                retrieveValues(ColumnType.DATETIME.name())),
        () ->
            Assertions.assertEquals(
                Set.of(
                    Values.literal("3001-01-01T12:30:00", CoreDatatype.XSD.DATETIME),
                    Values.literal("3002-01-01T12:30:00", CoreDatatype.XSD.DATETIME)),
                retrieveValues(ColumnType.DATETIME_ARRAY.name())),
        () ->
            Assertions.assertEquals(
                Set.of(Values.literal("P1D", CoreDatatype.XSD.DURATION)),
                retrieveValues(ColumnType.PERIOD.name())),
        () ->
            Assertions.assertEquals(
                Set.of(
                    Values.literal("P1M", CoreDatatype.XSD.DURATION),
                    Values.literal("P1Y", CoreDatatype.XSD.DURATION)),
                retrieveValues(ColumnType.PERIOD_ARRAY.name())),

        // RELATIONSHIP
        () ->
            Assertions.assertEquals(
                Set.of(Values.iri(rdfApiUrlPrefix + REF_TABLE + "?id=1")),
                retrieveValues(ColumnType.REF.name())),
        () ->
            Assertions.assertEquals(
                Set.of(
                    Values.iri(rdfApiUrlPrefix + REF_TABLE + "?id=2"),
                    Values.iri(rdfApiUrlPrefix + REF_TABLE + "?id=3")),
                retrieveValues(ColumnType.REF_ARRAY.name())),
        () ->
            Assertions.assertEquals(
                Set.of(Values.iri(rdfApiUrlPrefix + REFBACK_TABLE + "?id=1")),
                retrieveValues(ColumnType.REFBACK.name())),

        // LAYOUT and other constants -> should return empty sets as they should be excluded
        () -> Assertions.assertEquals(Set.of(), retrieveValues(ColumnType.HEADING.name())),
        // format flavors that extend a baseType
        () -> // AUTO_ID is unique so full equality check not possible
        Assertions.assertTrue(
                retrieveValues(ColumnType.AUTO_ID.name()).stream()
                    .findFirst()
                    .get()
                    .stringValue()
                    .matches("[0-9a-zA-Z]+")),
        () ->
            Assertions.assertEquals(
                Set.of(Values.iri("http://example.com/aa")),
                retrieveValues(ColumnType.ONTOLOGY.name())),
        () ->
            Assertions.assertEquals(
                Set.of(
                    Values.iri("http://example.com/bb"),
                    Values.iri(rdfApiUrlPrefix + ONT_TABLE + "?name=cc")),
                retrieveValues(ColumnType.ONTOLOGY_ARRAY.name())),
        () ->
            Assertions.assertEquals(
                Set.of(Values.iri("mailto:aap@example.com")),
                retrieveValues(ColumnType.EMAIL.name())),
        () ->
            Assertions.assertEquals(
                Set.of(
                    Values.iri("mailto:noot@example.com"), Values.iri("mailto:mies@example.com")),
                retrieveValues(ColumnType.EMAIL_ARRAY.name())),
        () ->
            Assertions.assertEquals(
                Set.of(Values.iri("https://molgenis.org")),
                retrieveValues(ColumnType.HYPERLINK.name())),
        () ->
            Assertions.assertEquals(
                Set.of(
                    Values.iri("https://molgenis.org"), Values.iri("https://github.com/molgenis")),
                retrieveValues(ColumnType.HYPERLINK_ARRAY.name())),

        // Overriding default behaviour
        // ontology as reference (key=1 instead of ontologyTermURI)
        () ->
            Assertions.assertEquals(
                Set.of(Values.iri(rdfApiUrlPrefix + ONT_TABLE + "?name=aa")),
                retrieveValuesCustom(
                    ColumnType.ONTOLOGY.name(), ColumnTypeRdfMapper.RdfColumnType.REFERENCE)),
        () ->
            Assertions.assertEquals(
                Set.of(
                    Values.iri(rdfApiUrlPrefix + ONT_TABLE + "?name=bb"),
                    Values.iri(rdfApiUrlPrefix + ONT_TABLE + "?name=cc")),
                retrieveValuesCustom(
                    ColumnType.ONTOLOGY_ARRAY.name(), ColumnTypeRdfMapper.RdfColumnType.REFERENCE)),
        // email as regular string
        () ->
            Assertions.assertEquals(
                Set.of(Values.literal("aap@example.com")),
                retrieveValuesCustom(
                    ColumnType.EMAIL.name(), ColumnTypeRdfMapper.RdfColumnType.STRING)));
  }
}
