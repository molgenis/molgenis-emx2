<template>
    <form-group
        :id="id" :help="help"
        :label="label"
        :placeholder="placeholder"
    >
        <input
            :id="id"
            v-model="valueArray[0]"
            :aria-describedby="id + 'Help'"
            class="form-control"
            :placeholder="placeholder"
            type="password"
            @input="emitValue"
        >
    </form-group>
</template>

<script>
import _baseInput from "./_baseInput.vue";

/** Input for passwords */
export default {
  extends: _baseInput,
};
</script>
