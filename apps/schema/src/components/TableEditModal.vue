<template>
  <IconAction
    v-if="!show"
    class="btn-sm hoverIcon"
    icon="pencil-alt"
    @click="show = true"
  />
  <LayoutModal
    v-else
    :title="'Edit ' + table.name"
    @close="close"
    :isCloseButtonShown="!isDisabled"
  >
    <template v-slot:body>
      <MessageWarning v-if="table.drop">Marked for deletion</MessageWarning>
      <InputString
        id="table_name"
        v-model="table.name"
        label="Table name"
        :errorMessage="nameInvalid"
      />
      <InputText
        id="table_description"
        v-model="table.description"
        label="Table Description"
      />
      <InputSelect
        v-if="extendsOptions"
        id="table_extends"
        v-model="table.inherit"
        :required="true"
        :options="inheritOptions"
        :readonly="table.oldName !== undefined"
        :errorMessage="subclassInvalid"
        label="Extends table (can not be edited after creation)"
      />
      <InputString
        id="table_semantics"
        :list="true"
        v-model="table.semantics"
        label="semantics (comma separated list of IRI defining type, and/or keyword 'id')"
      />
    </template>
    <template v-slot:footer>
      <ButtonAction @click="close" :disabled="isDisabled">Done</ButtonAction>
      <ButtonDanger @click="toggleDelete" v-if="table.drop != true">
        Mark as deleted
      </ButtonDanger>
      <ButtonDanger @click="toggleDelete" v-else>
        Undo mark as deleted
      </ButtonDanger>
    </template>
  </LayoutModal>
</template>

<script>
import {
  InputString,
  InputText,
  LayoutModal,
  IconAction,
  ButtonDanger,
  ButtonAction,
  MessageWarning,
  InputSelect,
} from "molgenis-components";

export default {
  components: {
    LayoutModal,
    InputString,
    InputText,
    IconAction,
    ButtonDanger,
    ButtonAction,
    MessageWarning,
    InputSelect,
  },
  props: {
    /** Table metadata object entered as v-model */
    value: Object,
    /** schema */
    schema: Object,
    /** in case of '+' subclass we show 'extends' option */
    extendsOptions: null,
  },
  data: function () {
    return {
      /** copy of table metadata being edited now */
      table: {},
      /** whether modal is shown */
      show: false,
    };
  },
  computed: {
    inheritOptions() {
      if (this.extendsOptions) {
        return this.extendsOptions.filter((value) => value != this.name);
      }
      return null;
    },
    nameInvalid() {
      return !this.table.name || this.table.name.search(/^[a-zA-Z0-9 _]*$/)
        ? "Name is required and can only contain 'azAZ_ '"
        : null;
    },
    subclassInvalid() {
      return this.extendOptions && this.table.inherit == undefined
        ? "Extends is required in case of subclass"
        : null;
    },
    isDisabled() {
      return this.nameInvalid || this.subclassInvalid;
    },
  },
  methods: {
    close() {
      this.show = false;
      this.$emit("input", this.table);
    },
    toggleDelete() {
      //need to do deep set otherwise vue doesn't see it
      if (!this.table.drop) {
        this.$set(this.table, "drop", true);
      } else {
        this.$set(this.table, "drop", false);
      }
    },
  },
  created() {
    this.table = this.value;
    //force showing of the new table editor
    if (this.table.name === undefined) {
      this.show = true;
    }
  },
};
</script>
