const initialBiobankColumns = [
  { label: 'Id:', column: 'id', type: 'string', showCopyIcon: true },
  { label: 'PID:', column: 'pid', type: 'string', showCopyIcon: true, copyValuePrefix: 'http://hdl.handle.net/' },
  { label: 'Description:', column: 'description', type: 'longtext' },
  { label: 'Quality labels:', column: 'quality', type: 'quality', showOnBiobankCard: true },
  { label: 'Collection types:', column: 'collection_types', type: 'array', showOnBiobankCard: true },
  { label: 'Juridical person:', column: 'juridical_person', type: 'string', showOnBiobankCard: true },
  { label: 'Biobank capabilities:', column: 'capabilities', type: 'mref', showOnBiobankCard: true }
]

module.exports = initialBiobankColumns
