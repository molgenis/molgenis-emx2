package org.molgenis.emx2.fairdatapoint;

import static org.molgenis.emx2.semantics.QueryHelper.finalizeFilter;

import graphql.ExecutionResult;
import graphql.GraphQL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.graphql.GraphqlApiFactory;

public class Queries {

  public static List<Map<String, Object>> queryDataset(Schema schema, String idField, String id) {
    GraphQL grapql = new GraphqlApiFactory().createGraphqlForSchema(schema);
    ExecutionResult executionResult =
        grapql.execute(
            "{Dataset("
                + finalizeFilter("filter:{" + idField + ": {equals:\"" + id + "\"")
                + "){"
                + "id,"
                + "distribution{name,description, type{name,codesystem,code,ontologyTermURI,definition}, files{identifier,md5checksum,name,server,path, format{name,codesystem,code,ontologyTermURI,definition}}},"
                + "accrualPeriodicity,"
                + "spatial{ontologyTermURI},"
                + "spatialResolutionInMeters,"
                + "temporal,"
                + "temporalResolution,"
                + "wasGeneratedBy,"
                + "accessRights,"
                + "contactPoint{identifier},"
                + "creator{identifier},"
                + "description,"
                + "hasPolicy,"
                + "identifier,"
                + "isReferencedBy,"
                + "keyword,"
                + "landingPage,"
                + "license,"
                + "language{ontologyTermURI},"
                + "relation,"
                + "rights,"
                + "qualifiedRelation,"
                + "publisher{name},"
                + "theme,"
                + "title,"
                + "type,"
                + "qualifiedAttribution,"
                + "mg_insertedOn,"
                + "mg_updatedOn"
                + "}}");
    Map<String, Object> result = executionResult.toSpecification();
    if (result.get("data") == null
        || ((HashMap<String, Object>) result.get("data")).get("Dataset") == null) {
      return new ArrayList<>();
    }
    return (List<Map<String, Object>>)
        ((HashMap<String, Object>) result.get("data")).get("Dataset");
  }

  public static List<Map<String, Object>> queryDistribution(
      Schema schema, String idField, String id) {
    GraphQL grapql = new GraphqlApiFactory().createGraphqlForSchema(schema);
    ExecutionResult executionResult =
        grapql.execute(
            "{Distribution("
                + finalizeFilter("filter:{" + idField + ": {equals:\"" + id + "\"")
                + "){"
                + "name,"
                + "description,"
                + "type{name,codesystem,code,ontologyTermURI,definition},"
                + "files{identifier,md5checksum,name,server,path,format{name,codesystem,code,ontologyTermURI,definition}},"
                + "belongsToDataset{"
                + "id,"
                + "spatialResolutionInMeters,"
                + "temporalResolution,"
                + "accessRights,"
                + "description," // note: there is also one in Distribution itself
                + "hasPolicy,"
                + "license,"
                + "rights,"
                + "title,"
                + "mg_insertedOn,"
                + "mg_updatedOn"
                + "}"
                + "mg_insertedOn,"
                + "mg_updatedOn"
                + "}}");
    Map<String, Object> result = executionResult.toSpecification();
    if (result.get("data") == null
        || ((HashMap<String, Object>) result.get("data")).get("Distribution") == null) {
      return new ArrayList<>();
    }
    return (List<Map<String, Object>>)
        ((HashMap<String, Object>) result.get("data")).get("Distribution");
  }
}
