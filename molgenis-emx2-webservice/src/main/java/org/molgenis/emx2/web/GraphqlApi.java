package org.molgenis.emx2.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.Scalars;
import graphql.schema.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.Filter;
import org.molgenis.emx2.sql.SqlGraphJsonQuery;
import org.molgenis.emx2.sql.SqlTypeUtils;
import org.molgenis.emx2.utils.MolgenisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLInputObjectType.newInputObject;
import static graphql.schema.GraphQLObjectType.newObject;
import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.sql.Filter.f;
import static org.molgenis.emx2.web.JsonApi.jsonToSchema;
import static org.molgenis.emx2.web.JsonApi.schemaToJson;
import static spark.Spark.*;

/**
 * Benchmarks show the api part adds about 10-30ms overhead on top of the underlying database call
 */
public class GraphqlApi {
  private static Logger logger = LoggerFactory.getLogger(GraphqlApi.class);

  private GraphqlApi() {
    // hide constructor
  }

  public static void createGraphQLSchema() {

    // schema level operations
    final String schemaPath = "/api/graphql/:schema"; // NOSONAR
    get(schemaPath, GraphqlApi::getQuery);
    post(schemaPath, GraphqlApi::getQuery);
  }

  private static String getQuery(Request request, Response response) throws IOException {
    long start = System.currentTimeMillis();

    // todo, invalidate on data changes
    // Schema

    Schema schema =
        MolgenisWebservice.getAuthenticatedDatabase(request)
            .getSchema(request.params(MolgenisWebservice.SCHEMA));

    GraphQLSchema gl = createGraphQLSchema(schema);
    GraphQL g = GraphQL.newGraphQL(gl).build();
    logger.info(
        "todo: create cache schema loading, it takes "
            + (System.currentTimeMillis() - start)
            + "ms");

    // tests show overhead of this step is about 1ms (jooq takes the rest)
    String query = null;
    if ("POST".equals(request.requestMethod())) {
      ObjectNode node = new ObjectMapper().readValue(request.body(), ObjectNode.class);
      query = node.get("query").asText();
    } else {
      query =
          request.queryParamOrDefault(
              "query",
              "{\n"
                  + "  __schema {\n"
                  + "    types {\n"
                  + "      name\n"
                  + "    }\n"
                  + "  }\n"
                  + "}");
    }
    logger.info("query: " + query.replaceAll("[\n|\r|\t]", "").replaceAll(" +", " "));

    // tests show overhead of this step is about 20ms (jooq takes the rest)
    ExecutionResult executionResult = g.execute(query);
    for (GraphQLError err : executionResult.getErrors()) {
      System.err.println(err);
    }

    // tests show conversions below is under 3ms
    Map<String, Object> toSpecificationResult = executionResult.toSpecification();
    String result = JsonApi.getWriter().writeValueAsString(toSpecificationResult);

    logger.info("graphql request completed in " + (System.currentTimeMillis() - start) + "ms");

    return result;
  }

  public static GraphQLSchema createGraphQLSchema(Schema model) {

    GraphQLObjectType.Builder queryBuilder = newObject().name("Query");
    GraphQLObjectType.Builder mutationBuilder = newObject().name("Save");

    GraphQLObjectType mutationResultType = typeForMutationResult();

    // add meta query and mutation
    queryBuilder.field(
        newFieldDefinition().name("_meta").type(metaType()).dataFetcher(metaQueryFetcher(model)));

    mutationBuilder.field(
        newFieldDefinition()
            .name("alterMetadata")
            .type(mutationResultType)
            .dataFetcher(metaMutationFetcher(model))
            .argument(GraphQLArgument.newArgument().name("input").type(metaInput())));

    // add query and mutation for each table
    for (String tableName : model.getTableNames()) {
      Table table = model.getTable(tableName);
      TableMetadata tableMetadata = table.getMetadata();

      // add each table as a table type
      GraphQLObjectType.Builder tableTypeBuilder = newObject().name(tableName);
      for (Column col : tableMetadata.getColumns()) {
        tableTypeBuilder.field(
            newFieldDefinition().name(col.getColumnName()).type(typeForColumn(col)));
      }
      GraphQLObjectType tableType = tableTypeBuilder.build();

      // add each table as input type
      GraphQLInputObjectType.Builder inputBuilder = newInputObject().name(tableName + "Input");
      for (Column col : tableMetadata.getColumns()) {
        inputBuilder.field(
            newInputObjectField().name(col.getColumnName()).type(typeForColumnInput(col)));
      }
      GraphQLInputObjectType inputType = inputBuilder.build();

      // create connection type
      GraphQLObjectType.Builder connectionBuilder = newObject().name(tableName + "Connection");
      connectionBuilder.field(newFieldDefinition().name("count").type(Scalars.GraphQLInt));
      // connectionBuilder.field(newFieldDefinition().name("meta").type(metadataType));
      connectionBuilder.field(newFieldDefinition().name("items").type(GraphQLList.list(tableType)));
      GraphQLObjectType connection = connectionBuilder.build();

      // add as field to query
      queryBuilder.field(
          newFieldDefinition()
              .name(tableMetadata.getTableName())
              .type(connection)
              .dataFetcher(tableQueryFetcher(table))
              .argument(
                  GraphQLArgument.newArgument()
                      .name("filter")
                      .type(tableQueryFilterType(tableMetadata))
                      .build())
              .argument(
                  GraphQLArgument.newArgument()
                      .name("search")
                      .type(Scalars.GraphQLString)
                      .build()));

      // add 'save' and 'delecte' fields to mutation
      mutationBuilder.field(
          newFieldDefinition()
              .name("save" + tableMetadata.getTableName())
              .type(mutationResultType)
              .dataFetcher(fetcherForSave(table))
              .argument(
                  GraphQLArgument.newArgument().name("input").type(GraphQLList.list(inputType))));

      //      newFieldDefinition()
      //          .name("delete" + tableMetadata.getTableName())
      //          .type(GraphQLTypeReference.typeRef(MUTATION_RESULT))
      //          .dataFetcher(createDeleteFetcher(table))
      //
      // .argument(GraphQLArgument.newArgument().name("input").type(GraphQLList.list(inputType)));
    }

    // assemble and return
    return graphql.schema.GraphQLSchema.newSchema()
        .query(queryBuilder)
        .mutation(mutationBuilder)
        .build();
  }

  private static DataFetcher<?> metaMutationFetcher(Schema model) {
    return dataFetchingEnvironment -> {
      try {
        Map<String, Object> metaInput = dataFetchingEnvironment.getArgument("input");
        model.tx(
            db -> {
              try {
                if (metaInput.containsKey("tables")) {
                  String json = JsonApi.getWriter().writeValueAsString(metaInput);
                  SchemaMetadata otherSchema = jsonToSchema(json);
                  model.merge(otherSchema);
                }
                if (metaInput.containsKey("members")) {
                  List<Map<String, String>> members = (List) metaInput.get("members");
                  for (Map<String, String> m : members) {
                    model.addMember(m.get("user"), m.get("role"));
                  }
                }
              } catch (IOException e) {
                throw new MolgenisException(e);
              }
            });
        Map result = new LinkedHashMap<>();
        result.put("detail", "success");
        return result;
      } catch (MolgenisException e) {
        return transform(e);
      }
    };
  }

  private static Object transform(MolgenisException e) {
    Map result = new LinkedHashMap<>();
    result.put("title", e.getTitle());
    result.put("type", e.getType());
    result.put("detail", e.getDetail());
    return result;
  }

  private static GraphQLInputType typeForColumnInput(Column col) {
    ColumnType type = col.getColumnType();
    if (REF.equals(type)) type = SqlTypeUtils.getRefColumnType(col);
    switch (type) {
      case DECIMAL:
        return Scalars.GraphQLBigDecimal;
      case INT:
        return Scalars.GraphQLInt;
      default:
        return Scalars.GraphQLString;
    }
  }

  private static GraphQLObjectType metaType() {
    GraphQLObjectType.Builder metaBuilder = new GraphQLObjectType.Builder();
    metaBuilder.name("MolgenisMetaType");
    metaBuilder.field(newFieldDefinition().name("tables").type(GraphQLList.list(metaTablesType())));
    metaBuilder.field(
        newFieldDefinition().name("members").type(GraphQLList.list(metaMembersType())));
    metaBuilder.field(newFieldDefinition().name("roles").type(GraphQLList.list(metaRolesType())));
    return metaBuilder.build();
  }

  private static GraphQLType metaRolesType() {
    GraphQLObjectType.Builder rolesTypeBuilder = new GraphQLObjectType.Builder();
    rolesTypeBuilder.name("MolgenisRolesType");
    rolesTypeBuilder.field(newFieldDefinition().name("name").type(Scalars.GraphQLString));
    return rolesTypeBuilder.build();
  }

  private static GraphQLType metaMembersType() {
    GraphQLObjectType.Builder membersTypeBuilder = new GraphQLObjectType.Builder();
    membersTypeBuilder.name("MolgenisMembersType");
    membersTypeBuilder.field(newFieldDefinition().name("user").type(Scalars.GraphQLString));
    membersTypeBuilder.field(newFieldDefinition().name("role").type(Scalars.GraphQLString));
    return membersTypeBuilder.build();
  }

  private static GraphQLObjectType metaTablesType() {

    GraphQLObjectType.Builder columnTypeBuilder = new GraphQLObjectType.Builder();
    columnTypeBuilder.name("MolgenisColumnType");
    columnTypeBuilder.field(newFieldDefinition().name("name").type(Scalars.GraphQLString));
    columnTypeBuilder.field(newFieldDefinition().name("columnType").type(Scalars.GraphQLString));
    columnTypeBuilder.field(newFieldDefinition().name("pkey").type(Scalars.GraphQLBoolean));
    columnTypeBuilder.field(newFieldDefinition().name("nullable").type(Scalars.GraphQLBoolean));
    columnTypeBuilder.field(newFieldDefinition().name("refTableName").type(Scalars.GraphQLString));
    columnTypeBuilder.field(newFieldDefinition().name("refColumnName").type(Scalars.GraphQLString));

    GraphQLObjectType.Builder tableTypeBuilder = new GraphQLObjectType.Builder();
    tableTypeBuilder.name("MolgenisTableType");
    tableTypeBuilder.field(newFieldDefinition().name("name").type(Scalars.GraphQLString));
    tableTypeBuilder.field(
        newFieldDefinition().name("pkey").type(GraphQLList.list(Scalars.GraphQLString)));
    tableTypeBuilder.field(
        newFieldDefinition()
            .name("unique")
            .type(GraphQLList.list(GraphQLList.list(Scalars.GraphQLString))));

    tableTypeBuilder.field(
        newFieldDefinition().name("columns").type(GraphQLList.list(columnTypeBuilder.build())));

    return tableTypeBuilder.build();
  }

  private static GraphQLInputObjectType metaInput() {
    GraphQLInputObjectType.Builder metaBuilder = new GraphQLInputObjectType.Builder();
    metaBuilder.name("MolgenisMetaInput");
    metaBuilder.field(
        newInputObjectField().name("tables").type(GraphQLList.list(metaTablesInput())));
    metaBuilder.field(
        newInputObjectField().name("members").type(GraphQLList.list(metaMembersInput())));
    return metaBuilder.build();
  }

  private static GraphQLInputObjectType metaMembersInput() {
    GraphQLInputObjectType.Builder membersTypeBuilder = new GraphQLInputObjectType.Builder();
    membersTypeBuilder.name("MolgenisMembersInput");
    membersTypeBuilder.field(newInputObjectField().name("user").type(Scalars.GraphQLString));
    membersTypeBuilder.field(newInputObjectField().name("role").type(Scalars.GraphQLString));
    return membersTypeBuilder.build();
  }

  private static GraphQLInputObjectType metaTablesInput() {
    // todo: is there a way to use same type between input and query?
    GraphQLInputObjectType.Builder columnTypeBuilder = new GraphQLInputObjectType.Builder();
    columnTypeBuilder.name("MolgenisColumnInput");
    columnTypeBuilder.field(newInputObjectField().name("name").type(Scalars.GraphQLString));
    columnTypeBuilder.field(newInputObjectField().name("columnType").type(Scalars.GraphQLString));
    columnTypeBuilder.field(newInputObjectField().name("pkey").type(Scalars.GraphQLBoolean));
    columnTypeBuilder.field(newInputObjectField().name("nullable").type(Scalars.GraphQLBoolean));
    columnTypeBuilder.field(newInputObjectField().name("refTableName").type(Scalars.GraphQLString));
    columnTypeBuilder.field(
        newInputObjectField().name("refColumnName").type(Scalars.GraphQLString));

    GraphQLInputObjectType.Builder tableTypeBuilder = new GraphQLInputObjectType.Builder();
    tableTypeBuilder.name("MolgenisTableInput");
    tableTypeBuilder.field(newInputObjectField().name("name").type(Scalars.GraphQLString));
    tableTypeBuilder.field(
        newInputObjectField().name("pkey").type(GraphQLList.list(Scalars.GraphQLString)));
    tableTypeBuilder.field(
        newInputObjectField()
            .name("unique")
            .type(GraphQLList.list(GraphQLList.list(Scalars.GraphQLString))));

    tableTypeBuilder.field(
        newInputObjectField().name("columns").type(GraphQLList.list(columnTypeBuilder.build())));

    return tableTypeBuilder.build();
  }

  private static GraphQLOutputType typeForColumn(Column col) {
    switch (col.getColumnType()) {
      case DECIMAL:
        return Scalars.GraphQLBigDecimal;
      case INT:
        return Scalars.GraphQLInt;
      case REF:
        return GraphQLTypeReference.typeRef(col.getRefTableName());
      case REF_ARRAY:
        return GraphQLTypeReference.typeRef(col.getRefTableName() + "Connection");
      default:
        return Scalars.GraphQLString;
    }
  }

  private static GraphQLObjectType typeForMutationResult() {
    return newObject()
        .name("MolgenisMessage")
        .field(newFieldDefinition().name("type").type(Scalars.GraphQLString).build())
        .field(newFieldDefinition().name("title").type(Scalars.GraphQLString).build())
        .field(newFieldDefinition().name("detail").type(Scalars.GraphQLString).build())
        .build();
  }

  private static GraphQLInputObjectType tableQueryFilterType(TableMetadata table) {
    GraphQLInputObjectType.Builder filterBuilder =
        newInputObject().name(table.getTableName() + "Filter");
    for (Column col : table.getColumns()) {
      filterBuilder.field(tableQueryFilterType(col));
    }
    return filterBuilder.build();
  }

  private static GraphQLInputObjectField tableQueryFilterType(Column col) {
    // colname : { "eq":[string], "range": [num,num,...,...], "contains":[string]}
    switch (col.getColumnType()) {
      case REF:
        return newInputObjectField()
            .name(col.getColumnName())
            .type(GraphQLTypeReference.typeRef(col.getRefTableName() + "Filter"))
            .build();
      case REF_ARRAY:
        return newInputObjectField()
            .name(col.getColumnName())
            .type(GraphQLList.list(GraphQLTypeReference.typeRef(col.getRefTableName() + "Filter")))
            .build();
      default:
        return newInputObjectField().name(col.getColumnName()).type(typeForStringFilter()).build();
    }
  }

  private static GraphQLInputObjectType stringFilter;

  private static GraphQLInputObjectType typeForStringFilter() {
    if (stringFilter == null) {
      stringFilter =
          newInputObject()
              .name("MolgenisStringFilter")
              .field(newInputObjectField().name("eq").type(GraphQLList.list(Scalars.GraphQLString)))
              .build();
    }
    return stringFilter;
  }

  private static DataFetcher fetcherForSave(Table aTable) {
    return dataFetchingEnvironment -> {
      try {
        Table table = aTable;
        List<Map<String, Object>> map = dataFetchingEnvironment.getArgument("input");
        int count = table.update(convertToRows(map));
        return resultMessage("success. saved " + count + " records");
      } catch (MolgenisException me) {
        return transform(me);
      }
    };
  }

  private static Map<String, String> resultMessage(String detail) {
    Map<String, String> message = new LinkedHashMap<>();
    message.put("detail", detail);
    return message;
  }

  private static Iterable<Row> convertToRows(List<Map<String, Object>> map) {
    List<Row> rows = new ArrayList<>();
    for (Map<String, Object> row : map) {
      rows.add(new Row(row));
    }
    return rows;
  }

  private static DataFetcher<?> fetcherForDelete(Table table) {
    return dataFetchingEnvironment -> {
      throw new UnsupportedOperationException();
    };
  }

  private static DataFetcher<?> metaQueryFetcher(Schema schema) {
    return dataFetchingEnvironment -> {

      // silly conversions, look into if we can bypass

      // add schema
      String json = schemaToJson(schema.getMetadata());
      Map<String, Object> result = new ObjectMapper().readValue(json, Map.class);

      // add members
      List<Map<String, String>> members = new ArrayList<>();
      for (Member m : schema.getMembers()) {
        members.add(Map.of("user", m.getUser(), "role", m.getRole()));
      }
      result.put("members", members);

      // add roles
      List<Map<String, String>> roles = new ArrayList<>();
      for (String role : schema.getRoles()) {
        roles.add(Map.of("name", role));
      }
      result.put("roles", roles);

      return result;
    };
  }

  private static DataFetcher tableQueryFetcher(Table aTable) {
    return dataFetchingEnvironment -> {
      Table table = aTable;
      Object o = dataFetchingEnvironment.getArgument("filter");
      SqlGraphJsonQuery q = new SqlGraphJsonQuery(table);
      q.select(createSelect(dataFetchingEnvironment.getSelectionSet()));
      if (dataFetchingEnvironment.getArgument("filter") != null) {
        q.filter(createFilters(table, dataFetchingEnvironment.getArgument("filter")));
      }
      String search = dataFetchingEnvironment.getArgument("search");
      if (search != null) {
        // todo proper tokenizer
        q.search(search.split(" "));
      }
      return transform(q.retrieve());
    };
  }

  private static Filter[] createFilters(Table table, Map<String, Object> filter) {
    List<Filter> subFilters = new ArrayList<>();
    for (Map.Entry<String, Object> entry : filter.entrySet()) {
      Column c = table.getMetadata().getColumn(entry.getKey());
      if (c == null)
        throw new RuntimeException(
            "Column " + entry.getKey() + " unknown in table " + table.getName());
      switch (c.getColumnType()) {
        case REF:
        case REF_ARRAY:
          subFilters.add(
              f(
                  c.getColumnName(),
                  createFilters(
                      table.getSchema().getTable(c.getRefTableName()), (Map) entry.getValue())));
          break;
        default:
          // expect scalar comparison, todo for all types
          if (entry.getValue() instanceof Map && ((Map) entry.getValue()).containsKey("eq")) {
            ArrayList values = (ArrayList) ((Map) entry.getValue()).get("eq");
            subFilters.add(f(entry.getKey()).eq(values.toArray()));
          } else {
            throw new RuntimeException(
                "unknown filter expression " + entry.getValue() + " for column " + entry.getKey());
          }
      }
    }
    return subFilters.toArray(new Filter[subFilters.size()]);
  }

  /** bit unfortunate that we have to convert from json to map and back */
  private static Object transform(String json) throws IOException {
    // benchmark shows this only takes a few ms so not a performance issue
    if (json != null) {
      return new ObjectMapper().readValue(json, Map.class);
    } else {
      return new LinkedHashMap<>();
    }
  }

  /**
   * creates a list like List.of(field1,field2, path1, List.of(pathsubfield1), ...)
   *
   * @return
   * @param f
   * @param selection
   */
  private static List createSelect(DataFetchingFieldSelectionSet selection) {
    ArrayList result = new ArrayList();
    for (SelectedField s : selection.getFields()) {
      if (!s.getQualifiedName().contains("/")) {
        result.add(s.getName());
        if (s.getSelectionSet().getFields().size() > 0) {
          result.add(createSelect(s.getSelectionSet()));
        }
      }
    }
    return result;
  }
}
