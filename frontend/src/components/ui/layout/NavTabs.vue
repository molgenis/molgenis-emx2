<template>
    <ul class="nav nav-tabs">
        <li v-if="label" class="nav-item">
            <a class="nav-link disabled" href="#">{{ label }}</a>
        </li>
        <li v-for="(item, index) in options" :key="index" class="nav-item">
            <a
                class="nav-link"
                :class="{ active: item === value }"
                href="#"
                @click.prevent="select(item)"
            >
                {{ item }}
            </a>
        </li>
    </ul>
</template>

<script>
import InputSelect from "../forms/InputSelect.vue";

export default {
  extends: InputSelect,
  emits: ['update:modelValue'],
  methods: {
    select(item) {
      this.value = item;
      this.$emit('update:modelValue', this.value);
    },
  },
};
</script>
