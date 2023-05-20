<template>
  <div>
    <div v-for="name in names" :key="name" class="p-0">
      <div class="mt-0">
        <a
          :href="'#'"
          v-scroll-to="{
            el: '#' + name,
          }"
          :class="'text-' + color"
        >
          {{ name }}
        </a>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "SectionIndex",
  props: {
    names: {
      type: Array,
    },
    color: {
      type: String,
      default: () => "primary",
    },
  },
};
</script>
