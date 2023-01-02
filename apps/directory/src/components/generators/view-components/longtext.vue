<template>
  <tr v-if="attribute && attribute.value && attribute.value.length">
    <th scope="row" class="pr-1 align-top text-nowrap">{{ displayName(attribute) }}</th>
    <td>
      {{ textToDisplay }}
       <span v-if="attribute.value.length > maxLength">
      <button @click="textClosed = !textClosed" class="btn btn-link p-0 pb-1">
        <span v-if="textClosed">... show more <i class="fa fa-angle-down" aria-hidden="true"></i></span>
        <span v-else>
          show less <i class="fa fa-angle-up" aria-hidden="true"></i></span>
      </button>
    </span>
    </td>
  </tr>
</template>

<script>
export default {
  name: 'longtext',
  props: {
    attribute: {
      type: Object
    }
  },
  methods: {
    displayName (item) {
      return item.label || item.name || item.id
    }
  },
  computed: {
    maxLength () {
      return this.attribute.maxLength | 500
    },
    textToDisplay () {
      if (this.textClosed && this.maxLength < this.attribute.value.length) {
        const shorttext = this.attribute.value.substr(0, this.maxLength)
        return shorttext.substr(
          0,
          Math.min(shorttext.length, shorttext.lastIndexOf(' '))
        )
      } else {
        return this.attribute.value
      }
    }
  },
  data () {
    return {
      textClosed: true
    }
  }
}
</script>
