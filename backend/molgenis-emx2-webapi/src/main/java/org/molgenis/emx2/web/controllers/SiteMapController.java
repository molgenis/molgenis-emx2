package org.molgenis.emx2.web.controllers;

import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;

import com.redfin.sitemapgenerator.ChangeFreq;
import com.redfin.sitemapgenerator.WebSitemapGenerator;
import com.redfin.sitemapgenerator.WebSitemapUrl;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

public class SiteMapController {
    private static final Logger logger = LoggerFactory.getLogger(SiteMapController.class);
    private static final List<String> ResourceList = Arrays.asList("Cohorts", "Studies", "Networks", "Institutions", "Databanks", "Datasources");

    private SiteMapController() {
    }

    public static String getSiteMapForSchema(Request request, Response response) {
        response.type("text/xml, application/xml");
        Schema schema = getSchema(request);

        final String baseUrl = request.scheme() + "://" + request.host() + "/" + schema.getName();
        final String appName = "ssr-catalogue";
        final String resourceBasePath = baseUrl + "/" + appName;

        try {
            WebSitemapGenerator wsg = new WebSitemapGenerator(baseUrl);

            ResourceList.forEach(
                    resource -> {
                        List<String> pids = SiteMapController.getResourcePids(schema, resource);
                        pids.forEach(
                                pid -> {
                                    try {
                                        wsg.addUrl(SiteMapController.urlForResource(resourceBasePath, resource, pid));
                                    } catch (MalformedURLException e) {
                                        logger.error(
                                                "Failed to generate sitemap url (schema: ("
                                                        + schema.getName()
                                                        + " , resource: "
                                                        + resource
                                                        + " , pid: "
                                                        + pid,
                                                e);
                                    }
                                });
                    });

            return String.join(System.lineSeparator(), wsg.writeAsStrings());
        } catch (MalformedURLException e) {
            String errorDescription = "Error initializing WebSitemapGenerator";
            logger.error(errorDescription, e);
            throw new MolgenisException(errorDescription);
        }
    }

    private static List<String> getResourcePids(Schema schema, String resourceName) {
        Table table = schema.getTable(resourceName);
        List<Row> rows = table.select(s("pid")).retrieveRows();
        return rows.stream().map(row -> row.getString("pid")).toList();
    }

    private static WebSitemapUrl urlForResource(
            String resourceBasePath, String resourceName, String resourceId)
            throws MalformedURLException {
        return new WebSitemapUrl.Options(
                resourceBasePath + "/" + resourceName.toLowerCase() + "/" + resourceId)
                .lastMod(new Date())
                .priority(1.0)
                .changeFreq(ChangeFreq.DAILY)
                .build();
    }
}
