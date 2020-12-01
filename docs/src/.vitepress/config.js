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
    },
  },
};

function getGuideSidebar() {
  return [
    {
      text: "Introduction",
      children: [
        { text: "Overview", link: "/" },
        { text: "Data model", link: "emx2format" },
        { text: "Settings", link: "settings" },
        { text: "For developers", link: "developers" },
        { text: "How to run", link: "how_to_run" },
      ],
    },
  ];
}

function getConfigSidebar() {
  return [{ text: "Config Reference", link: "/config/" }];
}
