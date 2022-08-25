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
        v-if="rootTable !== undefined"
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
    </template>
  </LayoutModal>
</template>

<script>
import {
  InputString,
  InputText,
  LayoutModal,
  IconAction,
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
    ButtonAction,
    MessageWarning,
    InputSelect,
  },
  props: {
    /** Table metadata object entered as v-model */
    value: Object,
    /** root table, used in case of subclasses */
    rootTable: Object,
    /** schema, used for uniques check */
    schema: Object,
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
      if (this.rootTable && this.rootTable.subclasses !== undefined) {
        let result = [this.rootTable.name];
        result.push(
          ...this.rootTable.subclasses
            .map((subclass) => subclass.name)
            .filter((name) => name !== this.table.name)
        );
        return result;
      }
    },
    nameInvalid() {
      if (
        this.table.name === undefined ||
        this.table.name.trim() === "" ||
        this.table.name.search(/^[a-zA-Z0-9 _]*$/)
      ) {
        return "Name is required and can only contain 'azAZ_ '";
      }
      if (
        this.schema.tables.filter(
          (table) =>
            table.name === this.table.name ||
            (table.subclasses !== undefined &&
              table.subclasses
                .map((subclass) => subclass.name)
                .includes(this.table.name))
        ).length > 0
      ) {
        return "Name should be unique";
      }
      return null;
    },
    subclassInvalid() {
      return this.inheritOptions && this.table.inherit === undefined
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
  },
  created() {
    //deep copy
    this.table = JSON.parse(JSON.stringify(this.value));
    //force showing of the new table editor
    if (this.table.name === undefined) {
      this.show = true;
    }
  },
};
</script>
