<template>
  <div>
    <span v-if="text === 'undefined'" />
    <span v-else-if="text.length <= length">{{ text }}</span>
    <span v-else-if="expand">
      {{ text }}
      <a href="#" @click.prevent="expand = false"> ... </a>
    </span>
    <span v-else>
      {{ text.slice(0, length) }}
      <a href="#" @click.prevent="expand = true"> ... </a>
    </span>
  </div>
</template>

<script>
export default {
  props: {
    length: {
      type: Number,
      default: () => 20,
    },
    text: String,
  },
  data() {
    return {
      expand: false,
    }
  },
}
</script>
