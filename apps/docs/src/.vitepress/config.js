module.exports = {
  head: [["link", { rel: "stylesheet", href: "/apps/docs/temp.css" }]],
  lang: "en-US",
  title: "MOLGENIS-EMX2 DOCUMENTATION",
  description: "MOLGENIS EMX2 DOCUMENTATION",
  base: "/apps/docs/",
  dist: "dist",
  themeConfig: {
    repo: "mswertz/molgenis-emx2",
    docsDir: "docs/src",
    editLinks: true,
    editLinkText: "Edit this page on GitHub",
    nav: [
      { text: "Introduction", link: "/" },
      {
        text: "Release Notes",
        link: "https://github.com/mswertz/molgenis-emx2/releases",
      },
    ],

    sidebar: {
      "/": getGuideSidebar(),
      "/guide/": getGuideSidebar(),
      "/config/": getConfigSidebar(),
    },
  },
};

function getGuideSidebar() {
  return [
    {
      text: "Introduction",
      children: [
        { text: "What is EMX2?", link: "/" },
        { text: "Configuration", link: "configuration" },
        { text: "Architecture", link: "architecture" },
        { text: "How to run", link: "how_to_run" },
        { text: "EMX2 file format reference", link: "emx2" },
      ],
    },
  ];
}

function getConfigSidebar() {
  return [{ text: "Config Reference", link: "/config/" }];
}
