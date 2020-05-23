<template>
  <div id="app">
    <Molgenis :menuItems="menuItems" :title="title" id="__top">
      <Schema />
    </Molgenis>
  </div>
</template>

<script>
import { Molgenis } from '@mswertz/emx2-styleguide'
import Schema from './components/Schema'

export default {
  components: {
    Schema,
    Molgenis
  },
  computed: {
    title() {
      return 'Schema' //todo get schema name from metadata
    },
    schema() {
      return 'TEMP'
    },
    menuItems() {
      return [
        { label: 'Home', href: '../home' },
        {
          label: 'Design',
          href: '../design'
        },
        {
          label: 'Members',
          href: '../members'
        },
        {
          label: 'GraphQL',
          href: '/api/playground.html?schema=/api/graphql/' + this.schema
        }
      ]
    }
  }
}
</script>
