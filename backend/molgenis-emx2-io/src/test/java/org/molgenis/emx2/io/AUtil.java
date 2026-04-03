// package org.molgenis.emx2.io;
//
// import org.molgenis.emx2.Row;
// import org.molgenis.emx2.io.readers.CsvTableReader;
// import org.molgenis.emx2.io.readers.CsvTableWriter;
//
// import java.io.File;
// import java.io.FileWriter;
// import java.io.IOException;
// import java.util.Iterator;
// import java.util.Map;
//
// public class AUtil {
//
//  public static void main(String[] args) throws IOException {
//
//    // convert a file
//
//    CsvTableWriter.write(
//        new Iterable<Row>() {
//          final Iterator<Row> it =
//              CsvTableReader.read(new File("/Users/umcg-mswertz/Downloads/test/fake_podb.csv"),
// ',')
//                  .iterator();
//
//          int count = 1;
//
//          @Override
//          public Iterator<Row> iterator() {
//            return new Iterator<Row>() {
//              @Override
//              public boolean hasNext() {
//                return it.hasNext();
//              }
//
//              @Deprecated
//              /** @deprecated */
//              public Row next() {
//                Row copy = new Row(it.next());
//
//                if (copy.getInteger("leeftijd") < 18) {
//                  copy.set("leeftijd", "<18");
//                } else if (copy.getInteger("leeftijd") > 50) {
//                  copy.set("leeftijd", ">50");
//                } else {
//                  copy.set("leeftijd", "18-50");
//                }
//
//                // also remove trailing ,
//                String diagnose = copy.getString("diagnose");
//                copy.set(
//                    "diagnose", diagnose.substring(0, diagnose.length() - 1).replace('*', ','));
//                copy.set("id", count);
//                copy.set("PALGAexcerptnr", count);
//                copy.set("Regelnummer", count++);
//
//                return copy;
//              }
//
//              @Override
//              public void remove() {
//                throw new UnsupportedOperationException();
//              }
//            };
//          }
//        },
//        new FileWriter(new File("/Users/umcg-mswertz/Downloads/test/fake_podb_copy.csv")),
//        ',');
//  }
// }
