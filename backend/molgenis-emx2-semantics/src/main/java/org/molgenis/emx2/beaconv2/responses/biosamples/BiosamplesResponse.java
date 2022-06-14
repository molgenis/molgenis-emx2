package org.molgenis.emx2.beaconv2.responses.biosamples;

import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.SelectColumn.s;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.molgenis.emx2.*;
import org.molgenis.emx2.beaconv2.common.Ontology;
import spark.Request;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BiosamplesResponse {

  // annotation to print empty array as "[ ]" as required per Beacon spec
  @JsonInclude(JsonInclude.Include.ALWAYS)
  BiosamplesResultSets[] resultSets;

  // query parameters, ignore from output
  @JsonIgnore String qId;

  public BiosamplesResponse(Request request, List<Table> tables) throws Exception {

    List<BiosamplesResultSets> rList = new ArrayList<>();
    qId = request.queryParams("id");

    for (Table t : tables) {
      System.out.println("## table: " + t.getName());
      Query q = t.query();

      // for every column that is an ontology, add the name, code and codesystem to select
      for (Column c : t.getMetadata().getColumns()) {
        if (c.isOntology()) {
          List<Column> ontoRefCols =
              c.getRefTable().getColumns().stream()
                  .filter(
                      colDef ->
                          colDef.getName().equals("name")
                              || colDef.getName().equals("code")
                              || colDef.getName().equals("codesystem"))
                  .collect(Collectors.toList());
          ArrayList<String> colNames = new ArrayList<>();
          for (Column cc : ontoRefCols) {
            colNames.add(cc.getName());
          }
          q.select(new SelectColumn(c.getName(), colNames));
        } else if (c.isReference()) {
          throw new Exception(
              "Reference datatypes (except ontology) not yet supported in Biosamples");
        } else {
          q.select(s(c.getName()));
        }
      }

      if (qId != null) {
        q.where(f("id", EQUALS, qId));
      }

      HashMap<String, BiosamplesResultSetsItem> aList = new HashMap<>();

      // todo better way to deal with duplicated rows when 1 row has multiple values in a column...
      // tmp store for 0..n variables
      HashMap<String, List<Ontology>> sampleOriginType = new HashMap<>();

      for (Row r : q.retrieveRows()) {
        String id = r.getString("id");
        BiosamplesResultSetsItem a;
        if (!aList.containsKey(id)) {
          a = new BiosamplesResultSetsItem();
          // 0..1 variables, ok to put once
          a.id = id;
          a.collectionMoment = r.getString("collectionMoment");
          a.collectionDate = r.getString("collectionDate");
          a.biosampleStatus =
              new Ontology(
                  r.getString("biosampleStatus-codesystem") + r.getString("biosampleStatus-code"),
                  r.getString("biosampleStatus-name"));
          a.obtentionProcedure =
              new ObtentionProcedure(
                  r.getString("obtentionProcedure-codesystem")
                      + r.getString("obtentionProcedure-code"),
                  r.getString("obtentionProcedure-name"));

          // tmp store for 0..n variables
          List<Ontology> sampleOriginTypeList = new ArrayList<>();
          sampleOriginTypeList.add(
              new Ontology(
                  r.getString("sampleOriginType-codesystem") + r.getString("sampleOriginType-code"),
                  r.getString("sampleOriginType-name")));
          sampleOriginType.put(id, sampleOriginTypeList);
          aList.put(id, a);
        } else {
          // tmp store for 0..n variables
          sampleOriginType
              .get(id)
              .add(
                  new Ontology(
                      r.getString("sampleOriginType-codesystem")
                          + r.getString("sampleOriginType-code"),
                      r.getString("sampleOriginType-name")));
        }
      }

      for (String id : aList.keySet()) {
        BiosamplesResultSetsItem a = aList.get(id);
        a.sampleOriginType =
            sampleOriginType.get(id).toArray(new Ontology[sampleOriginType.get(id).size()]);
      }

      BiosamplesResultSets aSet =
          new BiosamplesResultSets(
              t.getSchema().getName(),
              aList.size(),
              aList.values().toArray(new BiosamplesResultSetsItem[aList.size()]));
      rList.add(aSet);
    }

    this.resultSets = rList.toArray(new BiosamplesResultSets[rList.size()]);
  }
}
