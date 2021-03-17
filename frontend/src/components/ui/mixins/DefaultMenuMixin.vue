<script>
export default {
  computed: {
    defaultMenu() {
      return [
        { label: "Tables", href: "../tables/" },
        {
          label: "Schema",
          href: "../schema/",
        },
        {
          label: "Up/Download",
          href: "../updownload/",
        },
        {
          label: "Graphql",
          href: "../graphql-playground/",
        },
        {
          label: "Settings",
          href: "../settings/",
        },
      ];
    },
  },
};
</script>
