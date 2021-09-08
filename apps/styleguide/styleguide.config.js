const HOST = process.env.MOLGENIS_APPS_HOST || "http://localhost:8080";
const SCHEMA = process.env.MOLGENIS_APPS_SCHEMA || "pet%20store";

module.exports = {
  // set your styleguidist configuration here
  title: "MOLGENIS EMX2 Style Guide",
  theme: {
    color: {
      ribbonBackground: "black",
    },
    maxWidth: "auto",
  },
  assetsDir: "public",
  webpackConfig: {
    devServer: {
      port: "9090",
      proxy: {
        "/graphql": {
          target: `${HOST}/${SCHEMA}`,
          changeOrigin: true,
          secure: false,
        },
        "**/graphql": { target: `${HOST}`, changeOrigin: true, secure: false },
        "/api": { target: `${HOST}`, changeOrigin: true, secure: false },
        "/apps": { target: `${HOST}`, changeOrigin: true, secure: false },
        "^/theme.css": {
          target: `${HOST}/${SCHEMA}`,
          changeOrigin: true,
          secure: false,
        },
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
          href: "https://fonts.googleapis.com/css?family=Oswald:500|Roboto|Roboto+Mono&display=swap",
        },
        {
          rel: "stylesheet",
          href: "/apps/central/theme.css",
        },
      ],
    },
  },
  styleguideDir: "build",
  sections: [
    {
      name: "Introduction",
      content: "src/styleguide/introduction.md",
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
