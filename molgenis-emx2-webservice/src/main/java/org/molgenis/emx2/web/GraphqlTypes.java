package org.molgenis.emx2.web;

import graphql.Scalars;
import graphql.schema.*;
import org.molgenis.emx2.ColumnType;

import java.util.LinkedHashMap;
import java.util.Map;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

public class GraphqlTypes {
  static final String NAME = "name";
  static final String INPUT = "input";
  static final String FILTER = "filter";
  static final String TABLES = "tables";
  static final String MEMBERS = "members";
  static final String FILTER1 = "Filter";
  static final String DETAIL = "detail";
  static final String ITEMS = "items";
  static final String COUNT = "count";
  static final String LIMIT = "limit";
  static final String OFFSET = "offset";
  static final String SEARCH = "search";
  static final String ORDERBY = "orderby";

  // mutation result
  static GraphQLObjectType typeForMutationResult =
      newObject()
          .name("MolgenisMessage")
          .field(newFieldDefinition().name("type").type(Scalars.GraphQLString).build())
          .field(newFieldDefinition().name("title").type(Scalars.GraphQLString).build())
          .field(newFieldDefinition().name(DETAIL).type(Scalars.GraphQLString).build())
          .build();

  static Map<ColumnType, GraphQLInputObjectType> filterInputTypes = new LinkedHashMap<>();
}
