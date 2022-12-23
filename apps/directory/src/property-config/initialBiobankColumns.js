export const initialBiobankColumns = [
  { label: 'Id:', column: 'id', type: 'string', showCopyIcon: true },
  { label: 'PID:', column: 'PID', type: 'string', showCopyIcon: true, copyValuePrefix: 'http://hdl.handle.net/' },
  { label: 'Description:', column: 'description', type: 'longtext' },
  { label: 'Quality labels:', column: { qualityInfo: ['label', 'certificationReport', 'certificationImage', 'certificationNumber'] }, type: 'quality' },
  { label: 'Collection types:', column: { collections: [{ collectionType: ['name', 'label'] }] }, type: 'array' },
  { label: 'Juridical person:', column: 'juridicalPerson', type: 'string' },
  { label: 'Biobank capabilities:', column: { capabilities: ['name', 'label'] }, type: 'mref' }
]

export default initialBiobankColumns