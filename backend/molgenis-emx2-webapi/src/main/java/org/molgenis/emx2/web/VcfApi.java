package org.molgenis.emx2.web;

import static org.molgenis.emx2.io.FileUtils.getTempFile;
import static org.molgenis.emx2.web.Constants.ACCEPT_VCF;
import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;

import io.javalin.Javalin;
import io.javalin.http.Context;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.beaconv2.vcfimport.VcfImport;

/**
 * VCF will be imported into a fixed data structure and therefore operate on a schema level. If none
 * of the required tables exist, create them in the designated schema. If some tables exist or all
 * tables with missing attributes, throw an error to prevent half-baked imports that mess up the
 * user's database.
 */
public class VcfApi {
  private VcfApi() {
    // hide constructor
  }

  public static void create(Javalin app) {
    final String schemaPath = "/{schema}/api/vcf";
    app.post(schemaPath, VcfApi::importVCF);
  }

  private static void importVCF(Context ctx) throws ServletException, IOException {
    // no half-baked imports: use either complete structure or empty structure
    boolean nonePresent = noTablesPresent(ctx);
    boolean allPresent = false;
    if (!nonePresent) {
      allPresent = allTablesAndAttributesPresent(ctx);
    }
    if (!(nonePresent || allPresent)) {
      throw new MolgenisException(
          "Partial table structure detected, not going to import. Start with either a clean database, or a compliant structure.");
    }
    Schema schema = getSchema(ctx);
    assert schema != null;
    Table genomicVariations = schema.getTableByNameOrIdCaseInsensitive("GenomicVariations");
    Table individuals = schema.getTableByNameOrIdCaseInsensitive("Individuals");
    File vcfTmpFile = requestBodyToTmpFile(ctx);
    VcfImport vcfImport = new VcfImport(vcfTmpFile, genomicVariations, individuals);
    int count = vcfImport.start();
    ctx.status(200);
    ctx.contentType(ACCEPT_VCF);
    ctx.result(String.valueOf(count));
  }

  /** From the request, create a temporary VCF file to enable import */
  private static File requestBodyToTmpFile(Context ctx) throws IOException, ServletException {
    // get uploaded file
    File tempFile = getTempFile(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT, ".tmp");
    tempFile.deleteOnExit();
    InputStream input;
    // match upload handling to request content type
    if (ctx.contentType() == null) {
      throw new MolgenisException("No Content-Type header provided.");
    } else if (Objects.requireNonNull(ctx.contentType())
        .startsWith("application/x-www-form-urlencoded")) {
      input = new ByteArrayInputStream(ctx.body().getBytes());
    } else if (Objects.requireNonNull(ctx.contentType()).startsWith("multipart/form-data")) {
      ctx.attribute(
          "org.eclipse.jetty.multipartConfig",
          new MultipartConfigElement(tempFile.getAbsolutePath()));
      input = ctx.req().getPart("file").getInputStream();
    } else {
      throw new MolgenisException("Request has a different Content-Type: " + ctx.contentType());
    }
    Files.copy(input, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    if (tempFile.exists() && Files.size(tempFile.toPath()) > 0) {
      return tempFile;
    } else {
      throw new MolgenisException("Tmp file creation failed");
    }
  }

  /** Check if none of the required tables are present */
  private static boolean noTablesPresent(Context ctx) {
    Schema schema = getSchema(ctx);
    assert schema != null;
    Table genomicVariations = schema.getTableByNameOrIdCaseInsensitive("GenomicVariations");
    Table individuals = schema.getTableByNameOrIdCaseInsensitive("Individuals");
    return genomicVariations == null && individuals == null;
  }

  /** Check if all required tables and attributes for import are present */
  private static boolean allTablesAndAttributesPresent(Context ctx) {
    Schema schema = getSchema(ctx);

    // Check GenomicVariations table and its required fields for import
    assert schema != null;
    Table genomicVariations = schema.getTableByNameOrIdCaseInsensitive("GenomicVariations");
    if (genomicVariations == null) {
      return false;
    }
    for (String columnName :
        new String[] {
          "position_assemblyId",
          "position_refseqId",
          "position_start",
          "referenceBases",
          "alternateBases",
          //  "individualId", TODO
          //  "zygosity"
        }) {
      if (genomicVariations.getMetadata().getColumn(columnName) == null) {
        return false;
      }
    }
    // etc!

    // Check Individuals table and its required fields for import
    Table individuals = schema.getTableByNameOrIdCaseInsensitive("Individuals");
    if (individuals == null) {
      return false;
    }
    for (String columnName : new String[] {"id"}) {
      if (individuals.getMetadata().getColumn(columnName) == null) {
        return false;
      }
    }
    // etc!

    return true;
  }
}
