interface ICohort {
  name: string
  acronym?: string
  description?: string
  website?: string
  logo?: IUrlObject
  contactEmail?: string
  institution?: {
    acronym: string
  }
  type: INameObject[]
  collectionType: INameObject[]
  populationAgeGroups: INameObject[]
  startYear: number
  endYear: number
  countries: {
    name: string
    order: number
  }[]
  numberOfParticipants: number
  designDescription: string
  design: {
    definition: string
    name: string
  }
  collectionEvents: ICollectionEvent[]
  partners: IPartner[]
  contributors: IContributor[]
  networks: INetwork []
}

interface IPartner {
  institution: {
    pid: string
    name: string
    description: string
    logo: IUrlObject
  }
}

interface IContributor {
  contributionDescription: string
  contact: IContact
}

interface IContact {
  firstName: string
  surname: string
  initials: string
  department: string
  email: string
  title: INameObject
  institution: INameObject
}

interface INameObject {
  name: string
}

interface IUrlObject {
  url: string
}

interface ICollectionEvent {
  name: string
  description: string
  startYear: INameObject
  endYear: number
  numberOfParticipants: number
  ageGroups: INameObject[]
  definition: string
  dataCategories: ICollectionEventCategory[]
  sampleCategories: ICollectionEventCategory[]
  areasOfInformation: ICollectionEventCategory[]
  subcohorts: INameObject[]
  coreVariables: INameObject[]
}

interface ICollectionEventCategory {
  name: string
  parent?: INameObject
  definition?: string
}

interface ICollectionEventCategorySet {
  name: string
  children?: ICollectionEventCategorySet[]
  definition?: string
}

interface INetwork {
  name: string
  description?: string
  logo?: IUrlObject
}