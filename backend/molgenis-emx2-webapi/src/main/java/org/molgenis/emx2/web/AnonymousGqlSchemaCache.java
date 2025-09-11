package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.util.EnvHelpers.getEnvInt;
import static org.molgenis.emx2.web.util.EnvHelpers.getEnvLong;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import graphql.GraphQL;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.graphql.GraphqlApiFactory;

public class AnonymousGqlSchemaCache {
  private final Cache<String, GraphQL> anonymousGqlObjectCache = init();
  private final GraphqlApiFactory graphqlApiFactory = new GraphqlApiFactory();

  private AnonymousGqlSchemaCache() {}

  private static class Holder {
    private static final AnonymousGqlSchemaCache INSTANCE = new AnonymousGqlSchemaCache();
  }

  public static AnonymousGqlSchemaCache getInstance() {
    return Holder.INSTANCE;
  }

  @NotNull
  @Contract(" -> new")
  private Cache<String, GraphQL> init() {
    return Caffeine.newBuilder()
        .maximumSize(getEnvInt("ANONYMOUS_GQL_CACHE_MAX_SIZE", 100))
        .expireAfterAccess(
            getEnvLong("ANONYMOUS_GQL_CACHE_EXPIRE_ACCESS_MIN", 1L), TimeUnit.MINUTES)
        .expireAfterWrite(getEnvLong("ANONYMOUS_GQL_CACHE_EXPIRE_WRITE_MIN", 30L), TimeUnit.MINUTES)
        .build();
  }

  public void invalidate() {
    anonymousGqlObjectCache.invalidateAll();
  }

  public GraphQL get(Schema schema) {
    return anonymousGqlObjectCache.get(
        schema.getName(),
        key -> graphqlApiFactory.createGraphqlForSchema(schema, TaskApi.taskService).getGraphQL());
  }
}
