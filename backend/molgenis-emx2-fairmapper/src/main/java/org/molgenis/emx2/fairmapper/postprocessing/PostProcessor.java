package org.molgenis.emx2.fairmapper.postprocessing;

import org.molgenis.emx2.io.tablestore.InMemoryTableStore;

/**
 * Post-processing step in the DCAT harvesting pipeline. Applied to the transform step results to
 * make final adjustments to the InMemoryTableStore to make it suitable for uploading in the desired
 * schema. Implementations mutate the provided table store.
 */
public interface PostProcessor {

  void process(InMemoryTableStore tableStore);
}
