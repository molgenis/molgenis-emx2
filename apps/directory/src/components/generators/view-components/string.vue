<template>
  <tr v-if="attribute && attribute.value && attribute.value.length">
    <th scope="row" class="pr-1 align-top text-nowrap">
      {{ displayName(attribute) }}
    </th>
    <td>
      <span>
        {{ attribute.value }}
      </span>

      <template v-if="attribute.linkValue">
        <span
          id="copy-icon"
          @click.prevent="copyToClipboard(attribute.linkValue)"
          v-b-tooltip.hover="'Copy to clipboard'"
          class="fa fa-clipboard ml-1">
        </span>
      </template>
    </td>
  </tr>
</template>

<script>
import { mapMutations } from 'vuex'

export default {
  props: {
    attribute: {
      type: Object
    }
  },
  methods: {
    ...mapMutations(['SetNotification']),
    displayName (item) {
      return item.label || item.name || item.id
    },
    copyToClipboard (link) {
      navigator.clipboard.writeText(link)
      this.SetNotification(`Copied ${link}`)
    }
  }
}
</script>

<style scoped>
.fa-clipboard {
  position: relative;
  font-size: large;
}

.fa-clipboard:hover {
  cursor: pointer;
}

.fa-external-link {
  top: 1px;
  position: relative;
}

.fa-external-link:hover {
  cursor: pointer;
}
</style>
