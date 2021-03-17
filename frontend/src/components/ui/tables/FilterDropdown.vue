<template>
    <ButtonDropdown v-if="visible" :label="title">
        <slot />
    </ButtonDropdown>
</template>

<script>
export default {
  props: {
    title: String,
    visible: Boolean,
  },
};
</script>
