interface ICohort {
  name: string;
  acronym?: string;
  description?: string;
  website?: string;
  logo?: IUrlObject;
  contactEmail?: string;
  institution?: {
    acronym: string;
  };
  type: INameObject[];
  collectionType: INameObject[];
  populationAgeGroups?: IOntologyNode[];
  startYear: number;
  endYear: number;
  countries: {
    name: string;
  }[];
  regions: {
    name: string;
  }[];
  numberOfParticipants: number;
  numberOfParticipantsWithSamples?: number;
  designDescription: string;
  designSchematic: IFile;
  design: {
    definition: string;
    name: string;
  };
  designPaper?: {
    title: string;
    doi: string;
  }[];
  inclusionCriteria?: string;
  collectionEvents: ICollectionEvent[];
  partners: IPartner[];
  contributors: IContributor[];
  networks: INetwork[];
  releaseDescription?: string;
  dataAccessConditionsDescription?: string;
  dataAccessConditions?: { name: string }[];
  fundingStatement?: string;
  acknowledgements?: string;
  documentation?: IDocumentation[];
}

interface IFile {
  id?: string;
  size?: number;
  extension?: string;
  url?: string;
}

interface IDocumentation {
  name: string;
  description: string;
  url: string;
  file: IFile;
}

interface IPartner {
  institution: {
    pid: string;
    acronym: string;
    website: string;
    name: string;
    description: string;
    logo: IUrlObject;
  };
}

interface IContributor {
  contributionDescription: string;
  contact: IContact;
}

interface IContact {
  firstName: string;
  surname: string;
  initials: string;
  department: string;
  email: string;
  title: INameObject;
  institution: INameObject;
}

interface INameObject {
  name: string;
}

interface IUrlObject {
  url: string;
}

interface ICollectionEvent {
  name: string;
  description: string;
  startYear: INameObject;
  endYear: number;
  numberOfParticipants: number;
  ageGroups: INameObject[];
  definition: string;
  dataCategories: ICollectionEventCategory[];
  sampleCategories: ICollectionEventCategory[];
  areasOfInformation: ICollectionEventCategory[];
  standardizedTools: ICollectionEventCategory[];
  standardizedToolsOther: string;
  subcohorts: INameObject[];
  coreVariables: INameObject[];
}

interface ICollectionEventCategory {
  name: string;
  parent?: INameObject;
  definition?: string;
}

interface ICollectionEventCategorySet {
  name: string;
  children?: ICollectionEventCategorySet[];
  definition?: string;
}

interface INetwork {
  name: string;
  description?: string;
  logo?: IUrlObject;
  website?: string;
}

interface ITreeNode {
  name: string;
  children?: ITreeNode[];
  parent?: string;
}

interface IOntologyNode extends ITreeNode {
  code?: string;
  definition?: string;
  ontologyTermURI?: string;
}

interface ISetting {
  key: string;
  value: string;
}
