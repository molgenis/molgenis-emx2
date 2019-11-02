// package org.molgenis.emx2.graphql;
//
// import graphql.schema.*;
// <dependency>
//            <groupId>com.graphql-java</groupId>
//            <artifactId>graphql-java</artifactId>
//            <version>2019-05-27T04-13-44-5373f96</version>
//        </dependency>
//
//
//
// import org.molgenis.emx2.Column;
// import org.molgenis.emx2.SchemaMetadata;
// import org.molgenis.emx2.TableMetadata;
//
// import static graphql.Scalars.GraphQLInt;
// import static graphql.Scalars.GraphQLString;
// import static graphql.schema.GraphQLArgument.newArgument;
// import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
// import static graphql.schema.GraphQLObjectType.newObject;
// import static graphql.schema.GraphQLSchema.newSchema;
//
// public class GrahpqlEndpoint {
//
//  GraphQLSchema getSchema(SchemaMetadata model) {
//    GraphQLSchema.Builder schema = GraphQLSchema.newSchema();
//
//    GraphQLObjectType.Builder query = GraphQLObjectType.newObject().name("QueryOld");
//    for (String tableName : model.getTableNames()) {
//      TableMetadata table = model.getTableMetadata(tableName);
//      GraphQLObjectType.Builder type = GraphQLObjectType.newObject().name(table.getTableName());
//      for (Column col : table.getColumns()) {
//
// type.field(GraphQLFieldDefinition.newFieldDefinition().name(col.getColumnName()).type(Scalars.GraphQLString));
//      }
//      query.field(
//          GraphQLFieldDefinition.newFieldDefinition()
//              .name(table.getTableName())
//              .type(GraphQLList.list(type.build()))
//
// .argument(GraphQLArgument.newArgument().name("where").type(Scalars.GraphQLString).build())
//
// .argument(GraphQLArgument.newArgument().name("limit").type(Scalars.GraphQLInt).build())
//
// .argument(GraphQLArgument.newArgument().name("offset").type(Scalars.GraphQLInt).build())
//              .build());
//    }
//
//    schema.query(query.build());
//    return schema.build();
//  }
// }
