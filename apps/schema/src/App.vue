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
  data: function() {
    return {
      schema: null
    }
  },
  computed: {
    title() {
      return 'Schema' //todo get schema name from metadata
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
        }
      ]
    }
  }
}
</script>
