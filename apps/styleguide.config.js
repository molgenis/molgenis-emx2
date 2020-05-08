const path = require("path");

module.exports = {
  // set your styleguidist configuration here
  title: "MOLGENIS EMX2 Style Guide",
  ribbon: {
    // Link to open on the ribbon click (required)
    url: "https://github.com/mswertz/molgenis-emx2-ui/",
    // Text to show on the ribbon (optional)
    text: "Fork me on GitHub"
  },
  theme: {
    color: {
      ribbonBackground: "black"
    }
  },
  assetsDir: "styleguide/public",
  // here proxy
  webpackConfig: {
    devServer: {
      proxy: {
        "/graphql": "http://localhost:8080/pet%20store"
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
  styleguideDir: "../docs/styleguide",
  sections: [
    {
      name: "Introduction",
      content: "styleguide/src/styleguide/introduction.md"
    },
    {
      name: "Components",
      components: "styleguide/src/components/[A-Z]*.vue",
      content: "styleguide/src/styleguide/components.md"
    },
    {
      name: "Molecules",
      components: "styleguide/src/molecules/[A-Z]*.vue"
    },
    {
      name: "Organisms",
      components: "styleguide/src/organisms/[A-Z]*.vue"
    }
    // {
    //   name: 'Pages',
    //   components: 'src/components/pages/[A-Z]*.vue'
    // }
  ]
};
