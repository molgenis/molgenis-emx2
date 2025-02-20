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

  private static final String GENOMIC_VARIATIONS_TABLE_NAME = "GenomicVariations";
  private static final String GENOMIC_VARIATIONS_POSITION_ASSEMBLYID = "position_assemblyId";
  private static final String GENOMIC_VARIATIONS_POSITION_REFSEQID = "position_refseqId";
  private static final String GENOMIC_VARIATIONS_POSITION_START = "position_start";
  private static final String GENOMIC_VARIATIONS_REFERENCE_BASES = "referenceBases";
  private static final String GENOMIC_VARIATIONS_ALTERNATE_BASES = "alternateBases";
  private static final String INDIVIDUALS_TABLE_NAME = "Individuals";
  private static final String INDIVIDUALS_ID = "id";
  private static final String CASE_LEVEL_DATA_TABLE_NAME = "GenomicVariationsCaseLevel";
  private static final String CASE_LEVEL_DATA_ID = "id";
  private static final String CASE_LEVEL_DATA_INDIVIDUAL_ID = "individualId";
  private static final String CASE_LEVEL_DATA_ZYGOSITY = "zygosity";

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
    Table genomicVariations =
        schema.getTableByNameOrIdCaseInsensitive(GENOMIC_VARIATIONS_TABLE_NAME);
    Table individuals = schema.getTableByNameOrIdCaseInsensitive(INDIVIDUALS_TABLE_NAME);
    File vcfFile = requestBodyToTmpFile(ctx);
    VcfImport vcfImport = new VcfImport(vcfFile, genomicVariations, individuals);
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
    Table genomicVariations =
        schema.getTableByNameOrIdCaseInsensitive(GENOMIC_VARIATIONS_TABLE_NAME);
    Table caseLevelData = schema.getTableByNameOrIdCaseInsensitive(CASE_LEVEL_DATA_TABLE_NAME);
    Table individuals = schema.getTableByNameOrIdCaseInsensitive(INDIVIDUALS_TABLE_NAME);
    return genomicVariations == null && individuals == null && caseLevelData == null;
  }

  /** Check if all required tables and attributes for import are present */
  private static boolean allTablesAndAttributesPresent(Context ctx) {
    Schema schema = getSchema(ctx);
    assert schema != null;
    if (!tableExistsAndHasColumnNames(
        schema,
        GENOMIC_VARIATIONS_TABLE_NAME,
        GENOMIC_VARIATIONS_POSITION_ASSEMBLYID,
        GENOMIC_VARIATIONS_POSITION_REFSEQID,
        GENOMIC_VARIATIONS_POSITION_START,
        GENOMIC_VARIATIONS_REFERENCE_BASES,
        GENOMIC_VARIATIONS_ALTERNATE_BASES)) {
      return false;
    }

    if (!tableExistsAndHasColumnNames(schema, INDIVIDUALS_TABLE_NAME, INDIVIDUALS_ID)) {
      return false;
    }

    return tableExistsAndHasColumnNames(
        schema,
        CASE_LEVEL_DATA_TABLE_NAME,
        CASE_LEVEL_DATA_ID,
        CASE_LEVEL_DATA_INDIVIDUAL_ID,
        CASE_LEVEL_DATA_ZYGOSITY);
  }

  public static boolean tableExistsAndHasColumnNames(
      Schema schema, String tableName, String... columnNames) {
    Table table = schema.getTableByNameOrIdCaseInsensitive(tableName);
    if (table == null) {
      return false;
    }
    for (String columnName : columnNames) {
      if (table.getMetadata().getColumn(columnName) == null) {
        return false;
      }
    }
    return true;
  }
}
