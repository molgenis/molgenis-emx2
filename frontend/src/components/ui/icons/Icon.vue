<template>
    <svg
        class="icon" height="40"
        viewBox="0 0 24 24"
        width="40"
    >
        <component :is="name" />
    </svg>
</template>

<script>
import home from './IconHome.vue'

export default {
  components: {
      home,
  },
  props: {
    name: {
        required: true,
        type: String
    }
  }
}
</script>
<style lang="postcss">
.icon {
    fill: currentColor;
    height: 40px;
    width: 40px;

    &.icon-tiny {
        height: calc(var(--space-1) / 2);
        width: calc(var(--space-1) / 2);
    }

    &.icon-mini {
        height: var(--space-1);
        width: var(--space-1);
    }

    &.icon-small {
        height: var(--space-2);
        width: var(--space-2);
    }

    &.icon-large {
        height: 50px;
        width: 50px;
    }
}
</style>