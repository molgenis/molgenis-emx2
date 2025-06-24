export interface RouterViewIF {
  ontologySchemaName?: string;
}

export interface AdvancedSettingsEntryIF {
  key: string;
  value?: string;
}

export interface OrganisationsRecordIF {
  name: string;
  code?: string;
  city?: string;
  country?: string;
  latitude: string;
  longitude: string;
}
