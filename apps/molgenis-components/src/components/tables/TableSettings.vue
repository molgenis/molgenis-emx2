<template>
  <IconAction v-if="!show" icon="cog" @click="show = true" />
  <LayoutModal v-else title="Table Settings" @close="show = false" :show="show">
    <template v-slot:body>
      <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
      <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
      <Spinner v-if="loading" />
      <form v-else>
        <p v-pre>
          The data is in variable 'row'. For example,
          <tt>{{ row }}</tt>
          will show all values available.
          <tt>{{ row.name }}</tt>
          will get the 'name' column
        </p>
        <InputText
          id="table-settings-card-template"
          name="cardTemplate"
          label="cardTemplate"
          :value="cardTemplate"
          @update:modelValue="emitCardTemplate"
        />
        <InputText
          id="table-settings-record-template"
          name="recordTemplate"
          label="recordTemplate"
          :value="recordTemplate"
          @update:modelValue="emitRecordTemplate"
        />
        <ButtonAlt @click="show = false">Close</ButtonAlt>
        <ButtonAction @click="saveSettings">Save settings</ButtonAction>
      </form>
    </template>
  </LayoutModal>
</template>

<script>
import Client from "../../client/client.js";
import IconAction from "../forms/IconAction.vue";
import InputText from "../forms/InputText.vue";
import ButtonAction from "../forms/ButtonAction.vue";
import MessageSuccess from "../forms/MessageSuccess.vue";
import MessageError from "../forms/MessageError.vue";
import Spinner from "../layout/Spinner.vue";
import LayoutModal from "../layout/LayoutModal.vue";
import ButtonAlt from "../forms/ButtonAlt.vue";

export default {
  name: "TableSettings",
  components: {
    Spinner,
    MessageError,
    MessageSuccess,
    IconAction,
    InputText,
    ButtonAction,
    LayoutModal,
    ButtonAlt,
  },
  props: {
    graphqlURL: {
      type: String,
      default: "graphql",
    },
    tableName: String,
    cardTemplate: String,
    recordTemplate: String,
  },
  data() {
    return {
      show: false,
      graphqlError: null,
      success: null,
      loading: false,
    };
  },
  methods: {
    emitCardTemplate(value) {
      this.$emit("update:cardTemplate", value);
    },
    emitRecordTemplate(value) {
      this.$emit("update:recordTemplate", value);
    },
    async saveSettings() {
      this.loading = true;
      this.graphqlError = null;
      this.success = null;
      const client = Client.newClient(this.graphqlURL);
      const resp = await client
        .saveTableSettings([
          {
            table: this.tableName,
            key: "cardTemplate",
            value: this.cardTemplate,
          },
          {
            table: this.tableName,
            key: "recordTemplate",
            value: this.recordTemplate,
          },
        ])
        .catch((error) => {
          this.graphqlError = error.response.errors[0].message;
        });
      this.success = resp.data.data.change.message;
      this.loading = false;
    },
  },
};
</script>
