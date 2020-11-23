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
          label: "Upload",
          href: "../import/",
        },
        {
          label: "GraphQL",
          href: "../graphql-playground/",
        },
        {
          label: "Settings",
          href: "../settings/",
        },
        {
          label: "Docs",
          href: "/apps/docs/",
          newWindow: true,
        },
      ];
    },
  },
};
</script>
