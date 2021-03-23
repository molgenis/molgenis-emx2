<script>
export default {
  computed: {
    defaultMenu() {
      return [
        {
          href: '../tables/',
          label: 'Tables',
        },
        {
          href: '../schema/',
          label: 'Schema',
        },
        {
          href: '../updownload/',
          label: 'Up/Download',
        },
        {
          href: '../graphql-playground/',
          label: 'Graphql',
        },
        {
          href: '../settings/',
          label: 'Settings',
        },
      ]
    },
  },
}
</script>
