<template>
  <tr v-if="attribute && attribute.value && attribute.value.length">
    <th scope="row" class="pr-1 align-top text-nowrap">
      {{ displayName(attribute) }}
    </th>
    <td>
      <span>
        {{ attribute.value.join(', ') }}
      </span>
    </td>
  </tr>
</template>

<script>
export default {
  props: {
    attribute: {
      type: Object
    }
  },
  methods: {
    displayName (item) {
      return item.label || item.name || item.id
    }
  }
}
</script>
