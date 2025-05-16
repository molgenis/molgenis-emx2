package org.molgenis.emx2.web.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

public class CatalogueSiteMapTest {

  @Test
  public void buildSiteMap() {
    Schema schema = mock(Schema.class);
    Table table = mock(Table.class);
    Query query = mock(Query.class);
    List<Row> row =
        List.of(
            new Row("id", "my-id", "type", "Data source"),
            new Row("id", "my-second-id", "type", "Network"));

    when(schema.getTable("Resources")).thenReturn(table);
    when(table.select(any(), any())).thenReturn(query);
    when(query.retrieveRows()).thenReturn(row);
    CatalogueSiteMap catalogueSiteMap = new CatalogueSiteMap(schema, "https://my/base/url");

    String expected =
        """
    <?xml version="1.0" encoding="UTF-8"?>
    <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" >
      <url>
        <loc>https://my/base/url/catalogue/all/collections/my-id</loc>
      </url>
      <url>
        <loc>https://my/base/url/catalogue/all/networks/my-second-id</loc>
      </url>
    </urlset>""";
    assertEquals(expected, catalogueSiteMap.buildSiteMap());
  }

  @Test
  void buildSiteMapForSchemaWithVariables() {
    Schema schema = mock(Schema.class);
    Table resourceTable = mock(Table.class);
    Table variableTable = mock(Table.class);
    Query resourceQuery = mock(Query.class);
    Query variableQuery = mock(Query.class);
    List<Row> resourceRows =
        List.of(
            new Row("id", "my-id", "type", "Data source"),
            new Row("id", "my-second-id", "type", "Network"));
    List<Row> variableRows =
        List.of(new Row("name", "Var name", "resource", "lifetime", "dataset", "core"));

    when(schema.getTable("Resources")).thenReturn(resourceTable);
    when(schema.getTable("Variables")).thenReturn(variableTable);
    when(resourceTable.select(any(), any())).thenReturn(resourceQuery);
    when(resourceQuery.retrieveRows()).thenReturn(resourceRows);

    when(schema.query("Variables")).thenReturn(variableQuery);
    when(variableQuery.select(any(), any(), any())).thenReturn(variableQuery);
    when(variableQuery.where(any())).thenReturn(variableQuery);
    when(variableQuery.retrieveRows()).thenReturn(variableRows);
    CatalogueSiteMap catalogueSiteMap = new CatalogueSiteMap(schema, "https://my/base/url");

    String expected =
        """
    <?xml version="1.0" encoding="UTF-8"?>
    <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" >
      <url>
        <loc>https://my/base/url/catalogue/all/collections/my-id</loc>
      </url>
      <url>
        <loc>https://my/base/url/catalogue/all/networks/my-second-id</loc>
      </url>
      <url>
        <loc>https://my/base/url/catalogue/all/variables/Var+name-lifetime-core-lifetime?keys={&quot;name&quot;:&quot;Var+name&quot;,&quot;resource&quot;:{&quot;id&quot;:&quot;lifetime&quot;},&quot;dataset&quot;:{&quot;name&quot;:&quot;core&quot;,&quot;resource&quot;:{&quot;id&quot;:&quot;lifetime&quot;}}}</loc>
      </url>
    </urlset>""";
    assertEquals(expected, catalogueSiteMap.buildSiteMap());
  }

  @Test
  void buildSiteMapWhenExpectedTableIsMissing() {
    Schema schema = mock(Schema.class);
    Table table = mock(Table.class);
    Query query = mock(Query.class);

    when(table.select(any(), any())).thenReturn(query);

    CatalogueSiteMap catalogueSiteMap = new CatalogueSiteMap(schema, "https://my/base/url");
    assertThrows(MolgenisException.class, catalogueSiteMap::buildSiteMap);
  }
}
