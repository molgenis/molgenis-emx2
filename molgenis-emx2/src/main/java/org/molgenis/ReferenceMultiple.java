package org.molgenis;

public interface ReferenceMultiple {

  /** composite foreign key, multiple columns are created so instead return table */
  Table to(String toTable, String... toColumn) throws MolgenisException;

  /** Uses the default primary key to map to */
  Table to(String toTable) throws MolgenisException;
}
