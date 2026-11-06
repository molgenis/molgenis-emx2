# Migration 8.0: split Resources

Model change: commit 87cef8725 (#5937). The table `Resources` is split by inheritance:

```
Resources
├── Collections   (extends Resources)
└── Networks      (extends Resources)
    └── Catalogues (extends Networks)
```

What the migration must do:

- Move each row of `Resources.csv` to one new table, by the value of `type`:
  - `type` contains `Catalogue`: `Catalogues.csv` (also when it contains `Network`, because a catalogue is a network);
  - else `type` contains `Network`: `Networks.csv`;
  - else: `Collections.csv`.
- Each new file has only the columns of its own table and of its parent tables.
  For example, `Catalogues.csv` has no `type` column (only Collections has it), and `Collections.csv` has no `network type` column.
- The column `rdf type` is removed.
- Values do not change.
- Other tables that refer to a resource (for example `Subpopulations.resource`) do not change.
  They still refer to `Resources`, and a row of Collections, Networks or Catalogues is also a row of Resources.

These cases contain only rows without a decision of meaning.
For example, no catalogue row has values in a Collections-only column, and `data resources` lists only collections.
What to do with such rows is a decision for the data managers. It is not tested here.
