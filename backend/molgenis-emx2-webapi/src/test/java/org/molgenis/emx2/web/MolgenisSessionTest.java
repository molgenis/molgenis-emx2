package org.molgenis.emx2.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import graphql.GraphQL;
import java.util.ConcurrentModificationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.graphql.GraphqlApiFactory;

class MolgenisSessionTest {

  @Test
  void getGraphqlForSchema() {
    Database database = mock(Database.class);

    // Mock the database to return a schema when requested
    Schema schema = mock(Schema.class);
    SchemaMetadata metaData = mock(SchemaMetadata.class);
    when(metaData.getName()).thenReturn("testSchema");
    when(schema.getMetadata()).thenReturn(metaData);
    when(database.getSchema("testSchema")).thenReturn(schema);
    GraphqlApiFactory graphQlApiFactory = mock(GraphqlApiFactory.class);
    // Mock the GraphqlApiFactory to return a GraphQL instance
    GraphQL mockGraphQL = mock(GraphQL.class);
    when(graphQlApiFactory.createGraphqlForSchema(any(), any())).thenReturn(mockGraphQL);
    MolgenisSession molgenisSession = new MolgenisSession(database, graphQlApiFactory);
    GraphQL graphqlForSchema = molgenisSession.getGraphqlForSchema("testSchema");
    assertNotNull(graphqlForSchema, "GraphQL schema should not be null");

    assertSame(
        mockGraphQL, graphqlForSchema, "Returned GraphQL instance should match the mock instance");

    when(database.getActiveUser()).thenReturn("anonymous");
    GraphQL anonymousGraphql = molgenisSession.getGraphqlForSchema("testSchema");
    assertNotNull(anonymousGraphql, "Anonymous GraphQL schema should not be null");

    molgenisSession.clearCache();
    GraphQL anonymousGraphqlAfterClear = molgenisSession.getGraphqlForSchema("testSchema");
    assertNotNull(anonymousGraphqlAfterClear, "Anonymous GraphQL schema should not be null");
    assertEquals(anonymousGraphql, anonymousGraphqlAfterClear);
  }

  @Test
  void concurrentGetGraphQL() throws InterruptedException {
    Database database = mock(Database.class);

    // Mock the database to return a schema when requested
    Schema schema = mock(Schema.class);
    SchemaMetadata metaData = mock(SchemaMetadata.class);
    when(metaData.getName()).thenReturn("testSchema");
    when(schema.getMetadata()).thenReturn(metaData);
    when(database.getSchema("testSchema")).thenReturn(schema);
    GraphqlApiFactory graphQlApiFactory = mock(GraphqlApiFactory.class);
    // Mock the GraphqlApiFactory to return a GraphQL instance
    GraphQL mockGraphQL = mock(GraphQL.class);
    when(graphQlApiFactory.createGraphqlForSchema(any(), any())).thenReturn(mockGraphQL);
    MolgenisSession molgenisSession = new MolgenisSession(database, graphQlApiFactory);
    when(database.isAnonymous()).thenReturn(false);

    // Create two threads for getting the GraphQL for the schema
    int threadCount = 2;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch readyLatch = new CountDownLatch(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(threadCount);

    ConcurrentLinkedQueue<Throwable> failures = new ConcurrentLinkedQueue<>();

    for (int i = 0; i < threadCount; i++) {
      executor.submit(
          () -> {
            try {
              readyLatch.countDown();
              startLatch.await();
              molgenisSession.getGraphqlForSchema("testSchema");
            } catch (ConcurrentModificationException cme) {
              failures.add(cme);
            } catch (InterruptedException ignore) {

            } finally {
              doneLatch.countDown();
            }
          });
    }

    readyLatch.await();
    startLatch.countDown();
    doneLatch.await();
    executor.shutdown();

    assertEquals(0, failures.size());
  }
}
