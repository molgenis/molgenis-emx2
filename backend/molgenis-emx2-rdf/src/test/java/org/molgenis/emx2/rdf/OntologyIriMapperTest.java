package org.molgenis.emx2.rdf;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.TableType;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class OntologyIriMapperTest {
  static final String SCHEMA_NAME = "OntologyIriMapper";
  static final String DATA_TABLE_FIRST = "DataTableFirst";
  static final String DATA_TABLE_SECOND = "DataTableSecond";
  static final String ONT_TABLE_FIRST = "OntologyTableFirst";
  static final String ONT_TABLE_SECOND = "OntologyTableSecond";

  static Database database;
  static Schema ontologyIriMapper;

  @BeforeAll
  static void beforeAll() {
    // Initialize schema.
    database = TestDatabaseFactory.getTestDatabase();
    ontologyIriMapper = database.dropCreateSchema(SCHEMA_NAME);

    ontologyIriMapper.create(
        // Ontology tables
        table(ONT_TABLE_FIRST).setTableType(TableType.ONTOLOGIES),
        table(ONT_TABLE_SECOND).setTableType(TableType.ONTOLOGIES),
        // Data tables
        table(
            DATA_TABLE_FIRST,
            column("id", ColumnType.STRING).setPkey(),
            column("ontology1", ColumnType.ONTOLOGY).setRefTable(ONT_TABLE_FIRST)),
        table(
            DATA_TABLE_SECOND,
            column("id", ColumnType.STRING).setPkey(),
            column("ontology1", ColumnType.ONTOLOGY_ARRAY).setRefTable(ONT_TABLE_FIRST),
            column("ontology2", ColumnType.ONTOLOGY).setRefTable(ONT_TABLE_SECOND)));

    ontologyIriMapper
        .getTable(ONT_TABLE_FIRST)
        .insert(
            row(
                "name",
                OntologyIRI.ONT1_A.getName(),
                "ontologyTermURI",
                OntologyIRI.ONT1_A.getIri()),
            row(
                "name", OntologyIRI.ONT1_B.getName(),
                "ontologyTermURI", OntologyIRI.ONT1_B.getIri()),
            row("name", OntologyIRI.ONT1_C.getName()));

    ontologyIriMapper
        .getTable(ONT_TABLE_SECOND)
        .insert(
            row("name", OntologyIRI.ONT2_AA.getName()),
            row(
                "name",
                OntologyIRI.ONT2_BB.getName(),
                "ontologyTermURI",
                OntologyIRI.ONT2_BB.getIri()));
  }

  @AfterAll
  static void afterAll() {
    database.dropSchema(ontologyIriMapper.getName());
  }

  @Test
  void testDataTableFirst() {
    OntologyIriMapper mapper = new OntologyIriMapper(ontologyIriMapper.getTable(DATA_TABLE_FIRST));

    assertAll(
        () -> assertEquals(OntologyIRI.ONT1_A.getIri(), mapperGet(mapper, OntologyIRI.ONT1_A)),
        () -> assertEquals(OntologyIRI.ONT1_B.getIri(), mapperGet(mapper, OntologyIRI.ONT1_B)),
        () -> assertEquals(OntologyIRI.ONT1_C.getIri(), mapperGet(mapper, OntologyIRI.ONT1_C)),
        // ONT_TABLE_SECOND is not findable through DATA_TABLE_FIRST
        () -> assertNull(mapperGet(mapper, OntologyIRI.ONT2_AA)),
        () -> assertNull(mapperGet(mapper, OntologyIRI.ONT2_BB)));
  }

  @Test
  void testDataTableSecond() {
    OntologyIriMapper mapper = new OntologyIriMapper(ontologyIriMapper.getTable(DATA_TABLE_SECOND));

    assertAll( // all should be present
        () -> assertEquals(OntologyIRI.ONT1_A.getIri(), mapperGet(mapper, OntologyIRI.ONT1_A)),
        () -> assertEquals(OntologyIRI.ONT1_B.getIri(), mapperGet(mapper, OntologyIRI.ONT1_B)),
        () -> assertEquals(OntologyIRI.ONT1_C.getIri(), mapperGet(mapper, OntologyIRI.ONT1_C)),
        () -> assertEquals(OntologyIRI.ONT2_AA.getIri(), mapperGet(mapper, OntologyIRI.ONT2_AA)),
        () -> assertEquals(OntologyIRI.ONT2_BB.getIri(), mapperGet(mapper, OntologyIRI.ONT2_BB)));
  }

  @Test
  void testOntTableFirst() {
    OntologyIriMapper mapper = new OntologyIriMapper(ontologyIriMapper.getTable(ONT_TABLE_FIRST));

    assertAll(
        () -> assertEquals(OntologyIRI.ONT1_A.getIri(), mapperGet(mapper, OntologyIRI.ONT1_A)),
        () -> assertEquals(OntologyIRI.ONT1_B.getIri(), mapperGet(mapper, OntologyIRI.ONT1_B)),
        () -> assertEquals(OntologyIRI.ONT1_C.getIri(), mapperGet(mapper, OntologyIRI.ONT1_C)),
        () -> assertNull(mapperGet(mapper, OntologyIRI.ONT2_AA)),
        () -> assertNull(mapperGet(mapper, OntologyIRI.ONT2_BB)));
  }

  @Test
  void testOntTableSecond() {
    OntologyIriMapper mapper = new OntologyIriMapper(ontologyIriMapper.getTable(ONT_TABLE_SECOND));

    assertAll(
        () -> assertNull(mapperGet(mapper, OntologyIRI.ONT1_A)),
        () -> assertNull(mapperGet(mapper, OntologyIRI.ONT1_B)),
        () -> assertNull(mapperGet(mapper, OntologyIRI.ONT1_C)),
        () -> assertEquals(OntologyIRI.ONT2_AA.getIri(), mapperGet(mapper, OntologyIRI.ONT2_AA)),
        () -> assertEquals(OntologyIRI.ONT2_BB.getIri(), mapperGet(mapper, OntologyIRI.ONT2_BB)));
  }

  private IRI mapperGet(OntologyIriMapper mapper, OntologyIRI ontologyIRI) {
    return mapper.get(SCHEMA_NAME, ontologyIRI.getTableName(), ontologyIRI.getName());
  }

  private enum OntologyIRI {
    ONT1_A(ONT_TABLE_FIRST, "a", "http://example.com/a"),
    ONT1_B(ONT_TABLE_FIRST, "b", "http://example.com/b"),
    ONT1_C(ONT_TABLE_FIRST, "c", null),
    ONT2_AA(ONT_TABLE_SECOND, "aa", null),
    ONT2_BB(ONT_TABLE_SECOND, "bb", "http://example.com/bb");

    private final String tableName;
    private final String name;
    private final IRI iri;

    public String getTableName() {
      return tableName;
    }

    public String getName() {
      return name;
    }

    public IRI getIri() {
      return iri;
    }

    OntologyIRI(String tableName, String name, String iri) {
      this.tableName = tableName;
      this.name = name;
      this.iri = (iri == null ? null : Values.iri(iri));
    }
  }
}
