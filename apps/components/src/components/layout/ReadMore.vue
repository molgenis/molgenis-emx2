<template>
  <div>
    <span v-if="text === 'undefined'"></span>
    <span v-else-if="text.length <= length">{{ text }}</span>
    <span v-else-if="expand">
      {{ text }}
      <a @click.stop="expand = false" href="#">...show less </a>
    </span>
    <span v-else>
      {{ text.slice(0, length) }}
      <a @click.stop="expand = true" href="#" title="Expand text">
        ...show more
      </a>
    </span>
  </div>
</template>

<script>
export default {
  props: {
    text: String,
    length: {
      type: Number,
      default: () => 20,
    },
  },
  data() {
    return {
      expand: false,
    };
  },
};
</script>