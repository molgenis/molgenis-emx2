<template>
    <form-group v-bind="$props">
        <div class="input-group">
            <span class="input-group-prepend">
                <button
                    class="btn border-right-0 border btn-outline-primary"
                    type="button"
                >
                    <i class="fa fa-search" />
                </button>
            </span>
            <input
                :id="id"
                v-model="valueArray[0]"
                :aria-describedby="id + 'Help'"
                :class="{ 'form-control': true, 'is-invalid': errorMessage }"
                :placeholder="placeholder"
                :readonly="readonly"
                @input="emitValue"
                @keypress="keyhandler"
            >
            <AppendClear @clear="clearValue(0)" />
        </div>
    </form-group>
</template>

<script>
import InputString from "./InputString.vue";
import AppendClear from "./_appendClear";

export default {
  components: { AppendClear },
  extends: InputString,
};
</script>
