package org.molgenis.emx2.io.tablestore;

import static org.molgenis.emx2.io.tablestore.TableStoreForCsvFilesDirectory.CSV_EXTENSION;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.stream.StreamSupport;
import org.molgenis.emx2.BinaryFileWrapper;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.io.readers.CsvTableReader;

/** We test this in webapi */
public class TableStoreForURL implements TableAndFileStore {
  private URL baseURL;

  public TableStoreForURL(URL baseURL) {
    if (!baseURL.toString().endsWith("/")) {
      try {
        this.baseURL = new URL(baseURL + "/");
      } catch (Exception e) {
        throw new RuntimeException("baseURL reformatting failed");
      }
    } else {
      this.baseURL = baseURL;
    }
  }

  @Override
  public void writeTable(String name, List<String> columnNames, Iterable<Row> rows) {
    throw new UnsupportedOperationException("URL table store is readonly");
  }

  @Override
  public Iterable<Row> readTable(String name) {
    try {
      URL relativeUrl = new URL(this.baseURL, name + CSV_EXTENSION);
      BufferedReader reader = new BufferedReader(new InputStreamReader(relativeUrl.openStream()));
      return StreamSupport.stream(CsvTableReader.read(reader).spliterator(), false).toList();
    } catch (Exception e) {
      throw new MolgenisException(e.getMessage());
    }
  }

  @Override
  public void processTable(String name, RowProcessor processor) {
    processor.process(readTable(name).iterator(), this);
  }

  @Override
  public boolean containsTable(String name) {
    try {
      // we assume only csv at the moment
      URL relativeUrl = new URL(this.baseURL, name + CSV_EXTENSION);
      HttpURLConnection huc = (HttpURLConnection) relativeUrl.openConnection();
      int responseCode = huc.getResponseCode();
      return HttpURLConnection.HTTP_OK == responseCode;
    } catch (Exception e) {
      throw new MolgenisException(e.getMessage());
    }
  }

  @Override
  public Collection<String> tableNames() {
    throw new UnsupportedOperationException("URL table store cannot list files");
  }

  @Override
  public void writeFile(String fileName, byte[] binary) {
    throw new UnsupportedOperationException("URL table store cannot write files");
  }

  @Override
  public BinaryFileWrapper getBinaryFileWrapper(String name) {
    throw new UnsupportedOperationException(
        "Big todo: need to change how files are references to include file extension!!!");
  }
}
