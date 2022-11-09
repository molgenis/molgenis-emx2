<template>
  <IconAction v-if="!show" icon="cog" @click="show = true" />
  <LayoutModal v-else title="Table Settings" @close="show = false" :show="show">
    <template v-slot:body>
      {{tableMetadata}}
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
          @input="emitCardTemplate"
        />
        <InputText
          id="table-settings-record-template"
          name="recordTemplate"
          label="recordTemplate"
          :value="recordTemplate"
          @input="emitRecordTemplate"
        />
        <ButtonAlt @click="show = false">Close</ButtonAlt>
        <ButtonAction @click="saveSettings">Save settings</ButtonAction>
      </form>
    </template>
  </LayoutModal>
</template>

<script>
import {request} from "../../client/client.js";
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
    tableMetadata: {type: Object, required: true}
  },
  data() {
    return {
      show: false,
      graphqlError: null,
      success: null,
      loading: false,
    };
  },
  computed: {
    cardTemplate() {
      return this.tableMetadata.settings?.filter(setting => setting.key === "cardTemplate").map(setting => setting.value)[0];
    },
    recordTemplate() {
      return this.tableMetadata.settings?.filter(setting => setting.key === "recordTemplate").map(setting => setting.value)[0];
    }
  },
  methods: {
    emitCardTemplate(value) {
      if(!this.tableMetadata.settings) {
        this.tableMetadata.settings = [];
      }
      //remove old
      this.tableMetadata.settings = this.tableMetadata.settings.filter(setting => setting.key !== "cardTemplate");
      //set new
      this.tableMetadata.settings.push({key:"cardTemplate", value: value});
    },
    emitRecordTemplate(value) {
      console.log(value)
      if(!this.tableMetadata.settings) {
        this.tableMetadata.settings = [];
      }
      //remove old
      this.tableMetadata.settings = this.tableMetadata.settings.filter(setting => setting.key !== "recordTemplate");
      //set new
      this.tableMetadata.settings.push({key:"recordTemplate", value: value});
    },
    async saveSettings() {
      this.loading = true;
      this.graphqlError = null;
      this.success = null;
      const resp = await request(this.graphqlURL, `mutation change($tables:[MolgenisTableInput]){change(tables:$tables){message}}`, {tables:[this.tableMetadata]})
        .catch((error) => {
          this.graphqlError = error.response.data.errors[0].message;
        });
      this.success = resp.change.message;
      this.loading = false;
      this.$emit("update:settings")
    },
  },
  created() {
    if(!this.tableMetadata.settings) {
      this.tableMetadata.settings = []
    }
  }
};
</script>
