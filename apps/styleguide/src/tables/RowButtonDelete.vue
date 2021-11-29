<template>
  <span>
    <LayoutModal v-if="open" :title="title" @close="closeForm">
      <template v-slot:body>
        <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
        <MessageError v-else-if="error">{{ error }}</MessageError>
        <div v-else>
          Delete
          <strong>{{ table }}({{ pkeyAsString }})</strong>
          <br />Are you sure?
          <br />
        </div>
      </template>
      <template v-slot:footer>
        <ButtonAlt @click="closeForm">Close</ButtonAlt>
        <ButtonAction v-if="!success && !success" @click="executeDelete">
          Delete
        </ButtonAction>
      </template>
    </LayoutModal>
    <IconDanger icon="trash" @click="openForm" />
  </span>
</template>

<script>
import RowButtonAdd from "./RowButtonAdd";
import LayoutModal from "../layout/LayoutModal";
import IconDanger from "../forms/IconDanger";
import ButtonAlt from "../forms/ButtonAlt";
import ButtonAction from "../forms/ButtonAction";
import MessageError from "../forms/MessageError";
import MessageSuccess from "../forms/MessageSuccess";
import { request } from "graphql-request";

export default {
  extends: RowButtonAdd,
  data: function () {
    return {
      success: null,
      error: null,
    };
  },
  components: {
    LayoutModal,
    IconDanger,
    ButtonAction,
    ButtonAlt,
    MessageSuccess,
    MessageError,
  },
  props: {
    pkey: Object,
    graphqlURL: { type: String, default: "graphql" },
  },
  computed: {
    title() {
      return `Delete from ${this.table}`;
    },
    pkeyAsString() {
      return this.flattenObject(this.pkey);
    },
    tableId() {
      return this.table.replaceAll(" ", "_");
    },
  },
  methods: {
    executeDelete() {
      let query = `mutation delete($pkey:[${this.tableId}Input]){delete(${this.tableId}:$pkey){message}}`;
      let variables = { pkey: [this.pkey] };
      request(this.graphqlURL, query, variables)
        .then((data) => {
          this.success = data.delete.message;
          this.$emit("close");
        })
        .catch((error) => {
          this.error = error.response.errors[0].message;
        });
    }, //duplicated code from MolgenisTable, think of util lib
    flattenObject(object) {
      let result = "";
      Object.keys(object).forEach((key) => {
        if (object[key] === null) {
          //nothing
        } else if (typeof object[key] === "object") {
          result += this.flattenObject(object[key]);
        } else {
          result += " " + object[key];
        }
      });
      return result;
    },
  },
};
</script>

<docs>
Example
```
<!-- normally you don't need graphqlURL, default url = 'graphql' just works -->
<RowButtonDelete table="Pet" :pkey="{name:'MyPet4'}" graphqlURL="/pet store/graphql"/>
```
</docs>
