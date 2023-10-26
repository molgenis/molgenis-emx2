package org.molgenis.emx2.rdf;

public class StringsForRDFTest {
  static final String TTL_PREFIX_1 =
      "@prefix PetStoreNr1: <http://localhost:8080/petStoreNr1/api/rdf/> .";
  static final String TTL_PREFIX_2 =
      "@prefix PetStoreNr2: <http://localhost:8080/petStoreNr2/api/rdf/> .";
  static final String TTL_ROOT = "<http://localhost:8080> a sio:SIO_000750;";
  static final String TTL_SCHEMA_1 =
      "<http://localhost:8080/petStoreNr1/api/rdf> a rdfs:Container;";
  static final String TTL_SCHEMA_2 =
      "<http://localhost:8080/petStoreNr2/api/rdf> a rdfs:Container;";
  static final String TTL_TABLE_CATEGORY_1 = "emx0:Category a owl:Class";
  static final String TTL_TABLE_CATEGORY_2 = "emx1:Category a owl:Class";
  static final String TTL_TABLE_PET_1 = "emx0:Pet a owl:Class";
  static final String TTL_TABLE_PET_2 = "emx1:Pet a owl:Class";
  static final String TTL_COL_CATEGORY_NAME_1 =
      "<http://localhost:8080/petStoreNr1/api/rdf/Category/column/name> a sio:SIO_000757";
  static final String TTL_COL_CATEGORY_NAME_2 =
      "<http://localhost:8080/petStoreNr2/api/rdf/Category/column/name> a sio:SIO_000757";
  static final String TTL_COL_PET_NAME_1 =
      "<http://localhost:8080/petStoreNr1/api/rdf/Pet/column/name> a sio:SIO_000757";
  static final String TTL_COL_PET_NAME_2 =
      "<http://localhost:8080/petStoreNr2/api/rdf/Pet/column/name> a sio:SIO_000757";
  static final String TTL_COL_PET_DETAILS_1 =
      "<http://localhost:8080/petStoreNr1/api/rdf/Pet/column/details> a sio:SIO_000757";
  static final String TTL_COL_PET_DETAILS_2 =
      "<http://localhost:8080/petStoreNr2/api/rdf/Pet/column/details> a sio:SIO_000757";
  static final String TTL_ROW_POOKY_1 =
      "<http://localhost:8080/petStoreNr1/api/rdf/Pet/pooky> a emx0:Pet";
  static final String TTL_ROW_SPIKE_1 =
      "<http://localhost:8080/petStoreNr1/api/rdf/Pet/spike> a emx0:Pet";
  static final String TTL_ROW_POOKY_2 =
      "<http://localhost:8080/petStoreNr2/api/rdf/Pet/pooky> a emx1:Pet";
  static final String TTL_ROW_SPIKE_2 =
      "<http://localhost:8080/petStoreNr2/api/rdf/Pet/spike> a emx1:Pet";
  static final String TTL_ROW_CAT_1 =
      "<http://localhost:8080/petStoreNr1/api/rdf/Category/cat> a emx0:Category";
  static final String TTL_ROW_DOG_1 =
      "<http://localhost:8080/petStoreNr1/api/rdf/Category/dog> a emx0:Category";
  static final String TTL_ROW_CAT_2 =
      "<http://localhost:8080/petStoreNr2/api/rdf/Category/cat> a emx1:Category";
  static final String TTL_ROW_DOG_2 =
      "<http://localhost:8080/petStoreNr2/api/rdf/Category/dog> a emx1:Category";
}
