package org.molgenis.emx2.web;

import org.molgenis.emx2.*;
import org.molgenis.emx2.io.readers.CsvTableWriter;
import org.molgenis.emx2.io.readers.vcf.VcfReader;
import org.molgenis.emx2.io.readers.vcf.VcfRow;
import spark.Request;
import spark.Response;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.List;

import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.web.Constants.ACCEPT_CSV;
import static org.molgenis.emx2.web.Constants.ACCEPT_FORMDATA;
import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;
import static spark.Spark.*;

/**
 * USAGE: curl -v -F upload=@vcf_data_gendecs/vcftest1.vcf http://localhost:8080/gendecs/api/vcf
 * todo: use HTSJDK "VCFFileReader" for reading, parsing, validating etc todo: add to
 * molgenis-emx2-webapi build.grade "implementation 'com.github.samtools:htsjdk:2.24.1'"
 */
public class VcfApi {

  // todo: This is not correct. Variant definitions, observations and annotations should be in
  // separate tables according to the 'unified model'. For now however, this is a practical
  // and simple solution to store VCF data
  public static final String VCF_VARIANTS_TABLE = "vcf_variants";

  private VcfApi() {
    // hide constructor
  }

  public static void create() {
    final String tablePath = "/:schema/api/vcf";
    get(tablePath, VcfApi::tableRetrieve);
    post(tablePath, ACCEPT_FORMDATA, VcfApi::tableUpdate);
    delete(tablePath, VcfApi::tableDelete);
  }

  /**
   * todo: implement this function to retrieve variants, based on File name metadata? goal is
   * roundtrip to VCF
   *
   * @param request
   * @param response
   * @return
   * @throws IOException
   */
  private static String tableRetrieve(Request request, Response response) throws IOException {
    Table table = MolgenisWebservice.getTable(request);
    List<Row> rows = table.retrieveRows();
    StringWriter writer = new StringWriter();
    CsvTableWriter.write(rows, writer, '\t');
    response.type(ACCEPT_CSV);
    response.header("Content-Disposition", "attachment; filename=\"" + table.getName() + ".csv\"");
    response.status(200);
    return writer.toString();
  }

  /**
   * Add variants to vcf-variants in simple and crude way. We add a UUID per variant and the
   * filename (for now also a UUID) where the variants originated from
   *
   * @param request
   * @param response
   * @return
   */
  private static String tableUpdate(Request request, Response response)
      throws ServletException, IOException {
    // needed for multipart request so we can retrieve the filename etc.
    MultipartConfigElement multipartConfigElement = new MultipartConfigElement("/tmp");
    request.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);
    Schema schema = getSchema(request);

    // todo: create separate File table for file metadata/header
    // for now, just store a file name for each variant (so no file table yet)

    if (schema.getTable(VCF_VARIANTS_TABLE) == null) {

      // todo: import as a transaction
      schema.tx(database -> {});

      TableMetadata tm = table(VCF_VARIANTS_TABLE);
      // todo: .setType(ColumnType.REF).setRefTable("ref") with Chromosome lookup
      // todo: harmonize with Unified Model !!
      tm.add(Column.column(VcfRow.DBID).setType(ColumnType.STRING).setKey(1));
      tm.add(Column.column(VcfRow.SRCFILE).setType(ColumnType.STRING));
      tm.add(Column.column(VcfRow.CHROM).setType(ColumnType.STRING));
      tm.add(Column.column(VcfRow.POS).setType(ColumnType.INT));
      tm.add(Column.column(VcfRow.RSID).setType(ColumnType.STRING));
      tm.add(Column.column(VcfRow.REF).setType(ColumnType.STRING));
      tm.add(Column.column(VcfRow.ALT).setType(ColumnType.STRING));
      tm.add(Column.column(VcfRow.QUAL).setType(ColumnType.STRING));
      tm.add(Column.column(VcfRow.FILTER).setType(ColumnType.STRING));
      tm.add(Column.column(VcfRow.INFO).setType(ColumnType.TEXT));
      tm.add(Column.column(VcfRow.FORMAT).setType(ColumnType.STRING));
      tm.add(Column.column(VcfRow.SAMPLES).setType(ColumnType.TEXT));
      schema.create(tm);

      // attempting to solve https://github.com/molgenis/molgenis-emx2/issues/625, no joy...
      // schema.getDatabase().clearCache();
      // schema.getDatabase().getListener().schemaChanged(schema.getName());

    } else {

      // todo: table exists, should check if columns are what we need to import the VCF variants

    }

    int count = schema.getTable(VCF_VARIANTS_TABLE).save(getRowList(request));

    response.status(200);
    response.type(ACCEPT_CSV);
    return "" + count;
  }

  private static Iterable<Row> getRowList(Request request) throws ServletException, IOException {
    String fileName = getFileName(request.raw().getPart("upload"));
    InputStream fileContent = request.raw().getPart("upload").getInputStream();
    return VcfReader.read(new InputStreamReader(fileContent), fileName); // old: request.body()
  }

  private static String getFileName(Part part) {
    for (String cd : part.getHeader("content-disposition").split(";")) {
      if (cd.trim().startsWith("filename")) {
        return cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
      }
    }
    return null;
  }

  private static String tableDelete(Request request, Response response)
      throws ServletException, IOException {

    // todo: delete by file name metadata? remove those variants from list

    int count = MolgenisWebservice.getTable(request).delete(getRowList(request));
    response.type(ACCEPT_CSV);
    response.status(200);
    return "" + count;
  }
}
