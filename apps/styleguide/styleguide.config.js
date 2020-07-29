module.exports = {
  // set your styleguidist configuration here
  title: "MOLGENIS EMX2 Style Guide",
  ribbon: {
    // Link to open on the ribbon click (required)
    url: "https://github.com/mswertz/molgenis-emx2/",
    // Text to show on the ribbon (optional)
    text: "Fork me on GitHub"
  },
  theme: {
    color: {
      ribbonBackground: "black"
    }
  },
  assetsDir: "public",
  webpackConfig: {
    devServer: {
      proxy: {
        "/graphql": "http://localhost:8080/TestCohortCatalogue"
      }
    }
  },
  exampleMode: "collapse",
  template: {
    head: {
      links: [
        {
          rel: "stylesheet",
          href:
            "https://fonts.googleapis.com/css?family=Oswald:500|Roboto|Roboto+Mono&display=swap"
        },
        {
          rel: "stylesheet",
          href:
            // 'https://stackpath.bootstrapcdn.com/bootstrap/3.4.1/css/bootstrap.min.css'
            "assets/css/bootstrap-molgenis-blue.css"
        },
        {
          rel: "stylesheet",
          href:
            "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css"
        }
      ]
    }
  },
  styleguideDir: "../../docs",
  sections: [
    {
      name: "Introduction",
      content: "src/styleguide/introduction.md"
    },
    {
      name: "Components",
      components: "src/components/[A-Z]*.vue"
    },
    {
      name: "Molecules",
      components: "src/molecules/[A-Z]*.vue"
    },
    {
      name: "Mixins",
      components: "src/mixins/[A-Z]*.vue"
    }
    // {
    //   name: 'Organisms',
    //   content: 'src/styleguide/organisms.md',
    //   components: 'src/components/organisms/[A-Z]*.vue'
    // },
    // {
    //   name: 'Pages',
    //   components: 'src/components/pages/[A-Z]*.vue'
    // }
  ]
};
