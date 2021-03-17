<template>
    <button class="btn btn-danger" type="button" @click="onClick">
        <slot />
    </button>
</template>

<script>
import ButtonAction from "./ButtonAction";

/** Button that is shown as a primary action */
export default {
  extends: ButtonAction,
};
</script>
