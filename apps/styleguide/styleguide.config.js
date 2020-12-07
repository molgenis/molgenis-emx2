module.exports = {
  // set your styleguidist configuration here
  title: "MOLGENIS EMX2 Style Guide",
  ribbon: {
    // Link to open on the ribbon click (required)
    url: "https://github.com/mswertz/molgenis-emx2/",
    // Text to show on the ribbon (optional)
    text: "Fork me on GitHub",
  },
  theme: {
    color: {
      ribbonBackground: "black",
    },
  },
  assetsDir: "public",
  webpackConfig: {
    devServer: {
      proxy: {
        "/graphql": "http://localhost:8080/CohortsCentral/graphql",
        "**/graphql": "http://localhost:8080/",
        apps: "http://localhost:8080/",
      },
    },
  },
  tocMode: "collapse",
  exampleMode: "collapse",
  template: {
    head: {
      links: [
        {
          rel: "stylesheet",
          href:
            "https://fonts.googleapis.com/css?family=Oswald:500|Roboto|Roboto+Mono&display=swap",
        },
        {
          rel: "stylesheet",
          href:
            // 'https://stackpath.bootstrapcdn.com/bootstrap/3.4.1/css/bootstrap.min.css'
            "assets/css/bootstrap-molgenis-blue.css",
        },
        {
          rel: "stylesheet",
          href:
            "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css",
        },
      ],
    },
  },
  styleguideDir: "dist",
  sections: [
    {
      name: "Introduction",
      content: "src/styleguide/introduction.md",
    },
    {
      name: "Sign in",
      components: "src/signin/[A-Z]*.vue",
    },
    {
      name: "Form",
      components: "src/forms/[A-Z]*.vue",
    },
    {
      name: "Table",
      components: "src/tables/[A-Z]*.vue",
    },
    {
      name: "Layout",
      components: "src/layout/[A-Z]*.vue",
    },
    {
      name: "Mixins",
      components: "src/mixins/[A-Z]*.vue",
    },
  ],
};
