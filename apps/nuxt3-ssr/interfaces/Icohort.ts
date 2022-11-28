interface ICohort {
  name: string
  description: string
  website: string
  logo: IUrlObject
  contactEmail: string
  institution: {
    acronym: string
  }
  type: INameObject
  collectionType: INameObject
  populationAgeGroups: INameObject[]
  startYear: string
  endYear: string
  countries: INameObject[]
  numberOfParticipants: string
  designDescription: string
  design: {
    definition: string
    name: string
  }
  partners: IPartner[]
  contributors: IContributor[]
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
