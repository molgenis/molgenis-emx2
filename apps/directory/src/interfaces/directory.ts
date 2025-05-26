// Generated (on: 2024-11-20T11:45:09.401243) from Generator.java for schema: directory

export interface IFile {
  id?: string;
  size?: number;
  extension?: string;
  url?: string;
}

export interface ITreeNode {
  name: string;
  children?: ITreeNode[];
  parent?: {
    name: string;
  };
}

export interface IOntologyNode extends ITreeNode {
  label: string;
  code?: string;
  definition?: string;
  ontologyTermURI?: string;
  order?: number;
}

export interface IAccessTypes {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IAccessTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IAccessTypes[];
}

export interface IAgeRanges {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IAgeRanges;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IAgeRanges[];
}

export interface IAgeUnits {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IAgeUnits;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IAgeUnits[];
}

export interface IAlsoKnownIn {
  id: string;
  name_system: string;
  pid?: string;
  url: string;
  national_node: INationalNodes;
  label?: string;
}

export interface IAssessmentLevels {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IAssessmentLevels;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IAssessmentLevels[];
}

export interface IBiobank {
  id: string;
  pid: string;
  name: string;
  acronym?: string;
  description: string;
  url?: string;
  location?: string;
  country: IOntologyNode;
  latitude?: string;
  longitude?: string;
  head?: IPersons;
  contact: IPersons;
  juridical_person: string;
  network?: INetworks[];
  also_known?: IAlsoKnownIn[];
  collections?: ICollections[];
  services?: IServices[];
  quality?: IQualityInfoBiobanks[];
  collaboration_commercial?: boolean;
  collaboration_non_for_profit?: boolean;
  national_node: INationalNodes;
  withdrawn: boolean;
  viewmodel?: Record<string, any>;
}

export interface IBodyParts {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IBodyParts;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IBodyParts[];
}

export interface ICategories {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICategories;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICategories[];
}

export interface ICollectionFacts {
  id: string;
  collection: ICollections;
  sex?: IOntologyNode;
  age_range?: IOntologyNode;
  sample_type?: IOntologyNode;
  disease?: IDiseaseTypes;
  number_of_samples?: number;
  number_of_donors?: number;
  last_update: string;
  national_node: INationalNodes;
}

export interface ICollectionTypes {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICollectionTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICollectionTypes[];
}

export interface ICollections {
  id: string;
  name: string;
  acronym?: string;
  description: string;
  url?: string;
  location?: string;
  country: IOntologyNode;
  latitude?: string;
  longitude?: string;
  head?: IPersons;
  contact: IPersons;
  national_node: INationalNodes;
  withdrawn: boolean;
  parent_collection?: ICollections;
  sub_collections?: ICollections[];
  biobank: IBiobank;
  biobank_label: string;
  network?: INetworks[];
  combined_network?: INetworks[];
  also_known?: IAlsoKnownIn[];
  studies?: IStudies[];
  type: IOntologyNode[];
  data_categories: IOntologyNode[];
  order_of_magnitude: IOntologyNode;
  size?: number;
  categories?: IOntologyNode[];
  timestamp?: string;
  quality?: IQualityInfoCollections[];
  combined_quality?: IOntologyNode[];
  number_of_donors?: number;
  order_of_magnitude_donors?: IOntologyNode;
  sex?: IOntologyNode[];
  diagnosis_available?: IDiseaseTypes[];
  age_low?: number;
  age_high?: number;
  age_unit?: IOntologyNode;
  facts?: ICollectionFacts[];
  materials?: IOntologyNode[];
  storage_temperatures?: IOntologyNode[];
  body_part_examined?: IOntologyNode[];
  imaging_modality?: IOntologyNode[];
  image_dataset_type?: IOntologyNode[];
  collaboration_commercial?: boolean;
  collaboration_non_for_profit?: boolean;
  data_use?: IOntologyNode[];
  duc_profile?: string;
  commercial_use?: boolean;
  access_fee?: IOntologyNode[];
  access_joint_project?: IOntologyNode[];
  access_description?: string;
  access_uri?: string;
  sop?: IOntologyNode[];
}

export interface ICommonNetworkElements {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICommonNetworkElements;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICommonNetworkElements[];
}

export interface IContactPersonsNationalNodes {
  id: string;
  first_name?: string;
  last_name: string;
  email: string;
  role: string;
}

export interface ICountries {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICountries;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICountries[];
}

export interface IDataCategories {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IDataCategories;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IDataCategories[];
}

export interface IDataRefreshTypes {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IDataRefreshTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IDataRefreshTypes[];
}

export interface IDataUseOntologies {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IDataUseOntologies;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IDataUseOntologies[];
}

export interface IDiseaseTypes {
  order?: number;
  name: string;
  label?: string;
  parent?: IDiseaseTypes[];
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IDiseaseTypes[];
  exact_mapping?: IDiseaseTypes[];
  btnt_mapping?: IDiseaseTypes[];
  ntbt_mapping?: IDiseaseTypes[];
}

export interface IImageDatasetTypes {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IImageDatasetTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IImageDatasetTypes[];
}

export interface IImagingModalities {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IImagingModalities;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IImagingModalities[];
}

export interface IMaterialTypes {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IMaterialTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IMaterialTypes[];
}

export interface INationalNodes {
  id: string;
  description: string;
  dns?: string;
  contact_persons?: IContactPersonsNationalNodes[];
  data_refresh: IOntologyNode;
  date_start?: string;
  date_end?: string;
}

export interface INetworks {
  id: string;
  name: string;
  acronym?: string;
  description: string;
  location?: string;
  latitude?: string;
  longitude?: string;
  also_known?: IAlsoKnownIn[];
  url?: string;
  juridical_person?: string;
  contact: IPersons;
  parent_network?: INetworks[];
  common_network_elements?: IOntologyNode[];
  national_node: INationalNodes;
  withdrawn: boolean;
}

export interface IPersons {
  id: string;
  title_before_name?: string;
  first_name?: string;
  last_name?: string;
  title_after_name?: string;
  email: string;
  phone?: string;
  address?: string;
  zip?: string;
  city?: string;
  country: IOntologyNode;
  role?: string;
  biobanks?: IBiobank[];
  collections?: ICollections[];
  networks?: INetworks[];
  national_node: INationalNodes;
}

export interface IQualityInfoBiobanks {
  id: string;
  biobank: IBiobank;
  quality_standard: IOntologyNode;
  assess_level_bio: IOntologyNode;
  certification_number?: string;
  certification_report?: string;
  certification_image_link?: string;
}

export interface IQualityInfoCollections {
  id: string;
  collection: ICollections;
  quality_standard: IOntologyNode;
  assess_level_col: IOntologyNode;
  certification_number?: string;
  certification_report?: string;
  certification_image_link?: string;
}

export interface IQualityInfoServices {
  id: string;
  service: IServices;
  qualityStandard: IOntologyNode;
  assessmentLevel: IOntologyNode;
  certificationNumber?: string;
  certificationReport?: string;
  certificationImageLink?: string;
}

export interface IQualityStandards {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IQualityStandards;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IQualityStandards[];
}

export interface ISOPs {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ISOPs;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ISOPs[];
}

export interface ISampleSizes {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ISampleSizes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ISampleSizes[];
}

export interface IServiceCategories {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IServiceCategories;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IServiceCategories[];
}

export interface IServiceTypes {
  order?: number;
  name: string;
  label?: string;
  parent?: IServiceTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IServiceTypes[];
  serviceCategory: IOntologyNode;
}

export interface IServices {
  id: string;
  name: string;
  serviceTypes: IServiceTypes[];
  acronym?: string;
  description: string;
  descriptionUrl?: string;
  device?: string;
  deviceSystem?: string;
  tRL?: IOntologyNode;
  accessDescriptionUrl?: string;
  unitOfAccess?: string;
  accessDescription: string;
  unitCost?: string;
  qualityStandards?: IQualityInfoServices[];
  contactInformation?: IPersons;
  national_node: INationalNodes;
  biobank: IBiobank;
}

export interface ISexTypes {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ISexTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ISexTypes[];
}

export interface IStorageTemperatureTypes {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IStorageTemperatureTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IStorageTemperatureTypes[];
}

export interface IStudies {
  id: string;
  title: string;
  description?: string;
  type?: string;
  sex?: IOntologyNode[];
  age_low?: number;
  age_high?: number;
  age_unit?: IOntologyNode;
  number_of_subjects?: number;
  also_known?: IAlsoKnownIn[];
  collections?: ICollections[];
  national_node: INationalNodes;
}

export interface ITRLs {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ITRLs;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ITRLs[];
}
