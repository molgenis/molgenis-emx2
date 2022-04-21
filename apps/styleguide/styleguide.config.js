module.exports = {
  // set your styleguidist configuration here
  title: "MOLGENIS EMX2 Style Guide",
  theme: {
    color: {
      ribbonBackground: "black",
    },
    maxWidth: "auto",
  },
  require: ["./router-mock.js"],
  assetsDir: "public",
  webpackConfig: {
    devServer: {
      port: "9090",
      proxy: require("../dev-proxy.config"),
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
    {
      name: "Display",
      components: "src/display/[A-Z]*.vue",
    },
    {
      name: "Task",
      components: "src/task/[A-Z]*.vue",
    },
  ],
};
