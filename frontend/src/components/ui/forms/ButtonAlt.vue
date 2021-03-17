<template>
    <button
        class="btn btn-outline-primary border-0"
        type="button"
        @click="onClick"
    >
        <i v-if="icon" class="mr-2 ml-0" :class="icon" />
        <slot />
    </button>
</template>

<script>
/** Cancel button */
import ButtonAction from "./ButtonAction";

export default {
  extends: ButtonAction,
};
</script>
