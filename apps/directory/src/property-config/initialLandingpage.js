const initialLandingpage = {
  enabled: false,
  page_header: 'BBMRI-ERIC Directory',
  goto_catalogue_link: 'Browse the catalogue',
  page_search: {
    buttonText: 'Search',
    searchPlaceholder: 'Find a biobank or collection',
    ariaLabel: 'Searchbox for finding a biobank or collection'
  },
  page_call_to_actions: [
    {
      ctaText: 'Learn how',
      ctaUrl: '#',
      bodyHtml: `<h2>BBMRI Directory</h2>
        <p>Make your biobank visible</p>
        <p>Make your collections accessible</p>`
    },
    {
      ctaText: 'References',
      ctaUrl: '#',
      bodyHtml: `<h2>Services we offer</h2>
        <p>Manuals &amp; templates</p>`
    },
    {
      ctaText: 'Support',
      ctaUrl: '#',
      bodyHtml: `<h2>Support</h2>
        <p>Contact the servicedesk</p>`
    }
  ],
  page_biobank_spotlight: {
    header: 'Biobank of interest',
    biobankName: 'A BioBank',
    biobankId: '',
    bodyHtml: '<p>Lorum ipsum dolor amet</p>',
    buttonText: 'Go to biobank'
  },
  page_collection_spotlight: {
    header: 'New collections',
    collections: [
      {
        id: '',
        name: 'Collection 1',
        linkText: 'See more details'
      },
      {
        id: '',
        name: 'Collection 2',
        linkText: 'See more details'
      },
      {
        id: '',
        name: 'Collection 3',
        linkText: 'See more details'
      },
      {
        id: '',
        name: 'Collection 4',
        linkText: 'See more details'
      }
    ]
  },
  css: {
    pageHeader: {
      backgroundStyle:
        'background: url("/plugin/app/molgenis-app-biobank-explorer/img/bacteria.jpg");background-size: cover;height: 30rem;width: 75%;border-radius: 1rem; color: #fff;',
      linkStyle: 'color: #fff;'
    },
    searchBar: {
      inputStyle: '',
      inputClasses: 'border border-dark border-right-0',
      buttonStyle: '',
      buttonClasses: 'btn-primary search-button border border-dark border-left-0'
    },
    cta: {
      backgroundStyle: 'background-color: var(--info);',
      buttonClasses: 'btn-secondary',
      buttonStyle: ''
    },
    biobankSpotlight: {
      backgroundStyle:
        "background: url('/plugin/app/molgenis-app-biobank-explorer/img/microscope.jpg');background-size: cover;height: 30rem;width: 50%;border-radius: 1rem; color: #fff;",
      buttonClasses: 'btn-primary',
      buttonStyle: ''
    },
    collectionSpotlight: {
      backgroundStyle: 'background-color: var(--info);',
      linkClasses: 'text-info',
      linkStyle: ''
    }
  }
}

export default initialLandingpage
