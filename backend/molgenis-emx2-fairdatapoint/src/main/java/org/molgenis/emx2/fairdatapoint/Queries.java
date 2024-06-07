package org.molgenis.emx2.fairdatapoint;

import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.beaconv2.QueryHelper.finalizeFilter;

import graphql.ExecutionResult;
import graphql.GraphQL;
import java.awt.*;
import java.util.*;
import java.util.List;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
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
                + "propertyValue,"
                + "mg_insertedOn,"
                + "mg_updatedOn"
                + "}}");
    Map<String, Object> result = executionResult.toSpecification();
    if (result.get("data") != null) {
      return (List<Map<String, Object>>)
          ((HashMap<String, Object>) result.get("data")).get("Dataset");
    } else {
      Table cohortsTable = schema.getTable("Cohorts");
      if (cohortsTable != null) {
        List<Row> cohorts =
            cohortsTable
                .select(s("id"), s("name"), s("description"), s("mg_insertedOn"), s("mg_updatedOn"))
                .where(f("id", EQUALS, id))
                .retrieveRows();
        for (Row cohort : cohorts) {
          Map<String, Object> dataset = new LinkedHashMap<>();
          dataset.put("identifier", cohort.getString("id"));
          dataset.put("title", cohort.getString("name"));
          dataset.put("description", cohort.getString("description"));
          dataset.put("distribution", List.of()); // empty for now
          // creator
          // contact point
          // publisher
          // theme
          // type
          // license
          dataset.put("mg_insertedOn", cohort.getDateTime("mg_insertedOn"));
          dataset.put("mg_updatedOn", cohort.getDateTime("mg_updatedOn"));
          return List.of(dataset);
        }
      }
    }
    // else
    return null;
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
                + "},"
                + "propertyValue,"
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
