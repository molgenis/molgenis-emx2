<template>
  <span @focusout="onfocusout">
    <slot v-if="isEditing"></slot>
    <span v-else @click="openAndFocusInput()"
      ><slot name="display">{{ value }}</slot></span
    >
    <IconAction
      v-if="!isEditing"
      icon="pencil-alt"
      @click="openAndFocusInput()"
    />
  </span>
</template>

<style scoped>
.inline >>> .form-group {
  margin-bottom: 0px;
}
</style>

<script>
import IconAction from "./IconAction.vue";
import Vue from "vue";

export default {
  components: { IconAction },
  name: "InlineInput",
  props: {
    value: { type: [String, Number, Object, Array, Boolean], default: null },
  },
  data() {
    return {
      isEditing: false,
      isOpen: false,
    };
  },
  methods: {
    onfocusout() {
      if (this.isOpen) {
        this.isEditing = false;
        this.isOpen = false;
      }
    },
    openAndFocusInput() {
      this.isEditing = true;
      Vue.nextTick(() => {
        const input = this.$slots.default[0].elm.querySelector("input");
        input.focus(); // FIXME: focus only seems to work the first time its triggered on an element
        this.isOpen = true;
      });
    },
  },
};
</script>

<docs>
<template>
  <div>
    <demo-item>
      <InlineInput v-model="value">
        <InputString
          id="string-input"
          v-model="value"/>
      </InlineInput>
    </demo-item>
    <demo-item>
      <InlineInput v-model="value">
        <template v-slot:display><b>HTML Override</b></template>
        <InputString
          id="string-input"
          v-model="value"/>
      </InlineInput>
    </demo-item>
    <demo-item>
      <InlineInput v-model="ontology">
        <InputOntology
          id="input-ontology-1"
          v-model="ontology"
          label="My ontology select"
          description="please choose your options in tree below"
          :options="[
            { name: 'pet' },
            { name: 'cat', parent: { name: 'pet' } },
            { name: 'dog', parent: { name: 'pet' } },
            { name: 'cattle' },
            { name: 'cow', parent: { name: 'cattle' } },
          ]"
          :isMultiSelect="true"
        />
      </InlineInput>
    </demo-item>
  </div>
</template>
<script>
  export default {
    data() {
      return {
        value: "test",
        ontology: null
      };
    },
  };
</script>
</docs>
