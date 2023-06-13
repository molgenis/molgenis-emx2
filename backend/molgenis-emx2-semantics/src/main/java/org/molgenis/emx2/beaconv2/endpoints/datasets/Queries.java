package org.molgenis.emx2.beaconv2.endpoints.datasets;

import static org.molgenis.emx2.semantics.RDFService.extractHost;
import static org.molgenis.emx2.semantics.rdf.IRIParsingEncoding.encodedIRI;
import static org.molgenis.emx2.semantics.rdf.IRIParsingEncoding.getURI;

import graphql.ExecutionResult;
import graphql.GraphQL;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.graphql.GraphqlApiFactory;
import org.molgenis.emx2.utils.TypeUtils;
import spark.Request;

public class Queries {

  public static List<DatasetsResultSets> queryDatasets(
      Request request, List<Table> tables, String... filters) throws URISyntaxException {
    List<DatasetsResultSets> resultSetsList = new ArrayList<>();

    StringBuffer concatFilters = new StringBuffer();
    for (String filter : filters) {
      concatFilters.append(filter + ",");
    }
    if (concatFilters.length() > 0) {
      concatFilters.deleteCharAt(concatFilters.length() - 1);
    }

    URI requestURI = getURI(request.url());
    String host = extractHost(requestURI);
    String apiFdp = host + "/api/fdp";
    String apiFdpDataset = apiFdp + "/dataset";
    for (Table table : tables) {
      List<DatasetsResultSetsItem> datasetsItemList = new ArrayList<>();

      GraphQL grapql = new GraphqlApiFactory().createGraphqlForSchema(table.getSchema());
      ExecutionResult executionResult =
          grapql.execute(
              "{Dataset"
                  + "(filter: { _and: [ "
                  + concatFilters
                  + " ] }  ){"
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

      List<Map<String, Object>> datasetsListFromJSON =
          (List<Map<String, Object>>) ((HashMap<String, Object>) result.get("data")).get("Dataset");

      if (datasetsListFromJSON != null) {
        for (Map map : datasetsListFromJSON) {
          String id = TypeUtils.toString(map.get("id"));
          String apiLink =
              encodedIRI(apiFdpDataset + "/" + table.getSchema().getName() + "/" + id).toString();
          DatasetsResultSetsItem datasetsItem = new DatasetsResultSetsItem();
          datasetsItem.setCreateDateTime(TypeUtils.toString(map.get("mg_insertedOn")));
          datasetsItem.setDataUseConditions(null); //todo add?
          datasetsItem.setDescription(TypeUtils.toString(map.get("description")));
          datasetsItem.setExternalUrl(apiLink);
          datasetsItem.setId(id);
          datasetsItem.setInfo(apiLink);
          datasetsItem.setName(TypeUtils.toString(map.get("title")));
          datasetsItem.setUpdateDateTime(TypeUtils.toString(map.get("mg_updatedOn")));
          datasetsItem.setVersion(TypeUtils.toString(map.get("mg_updatedOn")));
          datasetsItemList.add(datasetsItem);
        }
      }

      if (datasetsItemList.size() > 0) {
        DatasetsResultSets datasetsResultSets =
            new DatasetsResultSets(
                table.getSchema().getName(),
                datasetsItemList.toArray(new DatasetsResultSetsItem[datasetsItemList.size()]));
        resultSetsList.add(datasetsResultSets);
      }
    }
    return resultSetsList;
  }
}
