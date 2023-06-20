/**
 * Return the whole base url comprising protocol, host, and base path of the application.
 */
export const getBaseUrl = () => {
  const baseUrl =  ''
  return `${window.location.protocol}//${window.location.host}${baseUrl}/#`
}

// Should be better to put in the data model
const getOntologyTerm = (property, term) => {
  const mappings = {
    properties: {
      diagnosis_available: 'http://purl.obolibrary.org/obo/OGMS_0000073',
      storage_temperatures: 'http://purl.obolibrary.org/obo/OMIABIS_0001013',
      materials: 'http://purl.obolibrary.org/obo/NCIT_C93863', // NCIT: Material Identifier Type Code
      sex: 'http://purl.obolibrary.org/obo/PATO_0000047' // OMIABIS: biological sex
    },
    materials: {
      WHOLE_BLOOD: {
        code: 'OBI_0000655',
        uri: 'http://purl.obolibrary.org/obo/OBI_0000655',
        name: 'blood specimen'
      },
      FAECES: {
        code: 'OBI_0002503',
        uri: 'http://purl.obolibrary.org/obo/OBI_0002503',
        name: 'feces specimen'
      },
      PLASMA: {
        code: 'OBI_0100016',
        uri: 'http://purl.obolibrary.org/obo/OBI_0100016',
        name: 'blood plasma specimen'
      },
      SALIVA: {
        code: 'OBI_0002507',
        uri: 'http://purl.obolibrary.org/obo/OBI_0002507',
        name: 'saliva specimen'
      },
      SERUM: {
        code: 'OBI_0100017',
        uri: 'http://purl.obolibrary.org/obo/OBI_0100017',
        name: 'blood serum specimen'
      },
      TISSUE_FROZEN: {
        code: 'OBI_0000922',
        uri: 'http://purl.obolibrary.org/obo/OBI_0000922',
        name: 'frozen specimen'
      }
    },
    data_categories: {
      SURVEY_DATA: {
        code: 'OMIABIS_0000060',
        uri: 'http://purl.obolibrary.org/obo/OMIABIS_0000060',
        name: 'survey data'
      },
      MEDICAL_RECORDS: {
        code: 'OMIABIS_0001027',
        uri: 'http://purl.obolibrary.org/obo/OMIABIS_0001027',
        name: 'sample medical record'
      }
    },
    sex: {
      MALE: {
        code: 'PATO_0000384',
        uri: 'http://purl.obolibrary.org/obo/PATO_0000384',
        name: 'male'
      },
      FEMALE: {
        code: 'PATO_0000383',
        uri: 'http://purl.obolibrary.org/obo/PATO_0000383',
        name: 'female'
      },
      UNDIFFERENTIAL: {
        code: 'PATO_0001340',
        uri: 'http://purl.obolibrary.org/obo/PATO_0001340',
        name: 'hermaphrodite'
      },
      NAV: {
        label: 'Not Available'
      },
      NASK: {
        label: 'Not Asked'
      },
      UNKNOWN: {
        label: 'unknown'
      }
    }
  }
  return property in mappings ? mappings[property][term] : undefined
}

const getCollectionAdditionalProperty = (data, propertyName) => {
  let value

  if ('ontologyTermURI' in data) {
    console.log("1")
    // it means the data contains the uri of the code in the model and must be used
    value = {
      '@type': 'CategoryCode',
      '@id': data.ontologyTermURI,
      codeValue: data.ontologyTermURI.split('/').slice(-1).pop() || undefined
    }
  } else {
    // Split values such as /api/v2/eu_bbmri_eric_material_types/TISSUE_FROZEN
    const codeValue = 'code' in data ? data.code : 'ontologyTermURI' in data ? data.ontologyTermURI.split('/').slice(-1).pop() : ""
    const ontologyTerm = getOntologyTerm(propertyName, codeValue)
    // it gets mappings from the static object. It should be better to include the uri in the model
    if (ontologyTerm) {
      if ('ontologyTermURI' in ontologyTerm) {
        value = {
          '@type': 'CategoryCode',
          '@id': ontologyTerm.ontologyTermURI,
          codeValue: ontologyTerm.code
        }
      } else {
        value = ontologyTerm.label
      }
    } else { // if no ontology term is found, it adds the label of the value
      value = data.label || data.size
    }
  }
  return {
    '@type': 'PropertyValue',
    propertyId: getOntologyTerm('properties', propertyName) || undefined,
    name: propertyName,
    value: value
  }
}

export const mapCollectionToBioschemas = (collection) => {
  const jsonld = {
    '@context': 'https://schema.org',
    '@type': 'Dataset',
    '@id': `${getBaseUrl()}/collection/${collection.id}`,
    description: collection.description || collection.name,
    identifier: collection.id,
    keywords: 'sample, collection',
    name: collection.name,
    url: `${getBaseUrl()}/collection/${collection.id}`,
    includedInDataCatalog: {
      '@type': 'DataCatalog',
      '@id': `${getBaseUrl()}/biobank/${collection.biobank.id}`,
      name: collection.biobank.name,
      url: `${getBaseUrl()}/biobank/${collection.biobank.id}`
    },
    additionalProperty: []
  }
  const properties = ['diagnosis_available', 'materials', 'data_categories', 'storage_temperatures', 'sex', 'order_of_magnitude']
  properties.forEach(prop => {
    if (prop in collection) {
      if (collection[prop] instanceof Array) {
        collection[prop].forEach(item =>
          jsonld.additionalProperty.push(getCollectionAdditionalProperty(item, prop))
        )
      } else {
        jsonld.additionalProperty.push(getCollectionAdditionalProperty(collection[prop], prop))
      }
    }
  })
  return jsonld
}

export const mapBiobankToBioschemas = (biobank) => {

  console.log(biobank)
  return {
    '@context': 'https://schema.org',
    '@type': 'DataCatalog',
    '@id': `http://hdl.handle.net/${biobank.pid}`,
    description: biobank.description || biobank.name, // some collections doesn't have a description
    keywords: 'biobank',
    name: biobank.name,
    provider: {
      '@type': 'Organization',
      description: biobank.description || biobank.name, // some collections doesn't have a description
      legalName: biobank.juridical_person,
      name: biobank.name,
      sameAs: `${getBaseUrl()}/biobank/${biobank.id}`,
      topic: 'http://edamontology.org/topic_3337', // biobank in EDAM
      contactPoint: biobank.contact ? {
        '@type': 'ContactPoint',
        email: biobank.contact.email
      } : undefined,
      location: biobank.contact ? {
        '@type': 'PostalAddress',
        contactType: 'juridical person',
        addressLocality: `${biobank.contact.city ? biobank.contact.city + ', ' : ''}${biobank.contact.country.name || biobank.contact.country.label || ''}`,
        streetAddress: biobank.contact.address || undefined
      } : undefined
    },
    url: `${getBaseUrl()}/biobank/${biobank.id}`,
    alternateName: biobank.acronym,
    dataset: Array.from(biobank.collections, collection => {
      return {
        '@type': 'Dataset',
        '@id': `${getBaseUrl()}/collection/${collection.id}`,
        url: `${getBaseUrl()}/collection/${collection.id}`,
        identifier: collection.id,
        name: collection.name,
        description: collection.description || collection.name
      }
    }),
    identifier: biobank.id
  }
}
