<script>
export default {
  computed: {
    defaultMenu() {
      return [
        { label: "Tables", href: "tables", role: "Viewer" },
        {
          label: "Schema",
          href: "schema",
          role: "Manager",
        },
        {
          label: "Up/Download",
          href: "updownload",
          role: "Editor",
        },
        {
          label: "Graphql",
          href: "graphql-playground",
          role: "Viewer",
        },
        {
          label: "Settings",
          href: "settings",
          role: "Manager",
        },
        {
          label: "Help",
          href: "docs",
          role: "Viewer",
        },
      ];
    },
  },
};
</script>
