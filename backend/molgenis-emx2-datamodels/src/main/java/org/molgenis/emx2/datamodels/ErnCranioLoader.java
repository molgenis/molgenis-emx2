package org.molgenis.emx2.datamodels;

import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.MolgenisIO;
import org.molgenis.emx2.sql.SqlDatabase;

public class ErnCranioLoader extends AbstractDataLoader {
  
  @Override
  void loadInternalImplementation(schema schema, boolean includeDemoData) {
    createSchema(schema, 'ern-cranio/model/molgenis.csv');
    
    // load ontologies
    MolgenisIO.fromClasspathDirectory("ern-cranio/ontologies", schema, false);
    
    // load demo data
    // if (includeDemoData) {
    //   MolgenisIO.fromClasspathDirectory('ern-cranio/demoddata/...', schema, false);
    // }
    
  }
}
