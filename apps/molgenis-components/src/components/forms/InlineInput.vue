<template>
  <span @focusin="onfocusin" @focusout="onfocusout">
    test
    <slot v-if="isEditing"></slot>
    <span v-else>{{ value }}</span>
    <IconAction
      v-if="!isEditing"
      icon="pencil-alt"
      @click="isEditing = !isEditing"
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
export default {
  components: { IconAction },
  name: "InlineInput",
  props: {
    value: { type: [String, Number, Object, Array, Boolean], default: null },
  },
  data() {
    return {
      isEditing: false,
    };
  },
  methods: {
    onfocusin() {
      // this.isEditing = true;
      console.log("in");
    },
    onfocusout() {
      // this.isEditing = false;
      console.log("out");
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
        value: ["test"],
        ontology: null
      };
    },
  };
</script>
</docs>
