<template>
    <button
        class="btn"
        :class="{
            'btn-outline-primary': !error,
            'btn-outline-danger': error,
        }"
        data-clear
        :disabled="disabled"
        title="Add"
        type="button"
        @click="$emit('add')"
    >
        <i class="fa fa-plus">
            <span aria-hidden="true" class="sr-only">Plus</span>
        </i>
    </button>
</template>

<script>
export default {
  props: {
    disabled: Boolean,
    error: String,
  },
};
</script>
