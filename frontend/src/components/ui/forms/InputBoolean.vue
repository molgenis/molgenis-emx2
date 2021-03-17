<template>
    <div v-if="inplace">
        <div
            class="form-check form-check-inline"
            :class="{ 'is-invalid': errorMessage }"
        >
            <input
                :id="id"
                v-model="valueArray[0]"
                :aria-describedby="id + 'Help'"
                class="form-check-input"
                type="checkbox"
                @change="$emit('input', $event.target.checked)"
            >
        </div>
    </div>
    <InputRadio
        v-else
        v-model="valueArray[0]"
        v-bind="$props"
        :options="[true, false]"
        @input="emitValue"
    />
</template>

<script>
import BaseInput from "./_baseInput.vue";
import InputRadio from "./InputRadio.vue";

export default {
  components: { InputRadio },
  extends: BaseInput,
};
</script>
