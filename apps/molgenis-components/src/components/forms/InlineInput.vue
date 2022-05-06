<template>
  <span @focusout="onfocusout" ref="inlineInput">
    <span v-if="isEditing"><slot></slot></span>
    <span v-else @click="openAndFocusInput()" class="editable">
      <slot name="display">{{ value }}</slot>
    </span>
    <IconAction
      v-if="!isEditing"
      icon="pencil-alt"
      @click="openAndFocusInput()"
    />
  </span>
</template>

<style scoped>
.editable {
  border-bottom: 1px dashed lightblue;
}
</style>

<script>
import IconAction from "./IconAction.vue";

export default {
  components: { IconAction },
  name: "InlineInput",
  props: {
    value: { type: [String, Number, Object, Array, Boolean], default: null },
  },
  data() {
    return {
      isEditing: false,
      isInputFieldVisible: false,
    };
  },
  methods: {
    onfocusout() {
      if (this.isInputFieldVisible) {
        this.isEditing = false;
        this.isInputFieldVisible = false;
      }
    },
    async openAndFocusInput() {
      this.isEditing = true;
      this.isEditing = true;
      await this.$nextTick();
      this.isInputFieldVisible = true;
      const input = this.$refs.inlineInput.querySelector("input");
      input?.focus();
    },
  },
};
</script>

<docs>
<template>
  <div>
    <p>Inline input wil make simple input fields work inline. Note: this will need an input tag to work.</p>
    <demo-item>
      <div>Inline string input</div>
      <InlineInput v-model="value">
        <InputString
          id="string-input"
          v-model="value"/>
      </InlineInput>
    </demo-item>
    <demo-item>
      <div>Inline string input with custom display slot</div>
      <InlineInput v-model="value">
        <template v-slot:display><b>HTML Override</b></template>
        <InputString
          id="string-input-override"
          v-model="value"/>
      </InlineInput>
    </demo-item>
    <demo-item>
      <div>Inline InputDecimal</div>
      <InlineInput v-model="decimal">
        <InputDecimal
          id="decimal-input"
          v-model="decimal"/>
      </InlineInput>
    </demo-item>
  </div>
</template>
<script>
  export default {
    data() {
      return {
        value: "test",
        decimal: 3.14159265
      };
    },
  };
</script>
</docs>
