<template>
  <IconAction
    v-if="!show"
    icon="cog"
    @click="show = true"
    class="float-right"
  />
  <div v-else style="background-color: #eee">
    <IconAction icon="times" @click="show = false" class="float-right" />
    <div class="container">
      <h2>Table settings</h2>
      <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
      <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
      <Spinner v-if="loading" />
      <form v-else>
        <p v-pre>
          The data is in variable 'a'. For example,
          <tt>{{ a }}</tt>
          will show all values available.
          <tt>{{ a.name }}</tt>
          will get the 'name' column
        </p>
        <InputText
          name="cardTemplate"
          label="cardTemplate"
          :value="cardTemplate"
          @input="emitCardTemplate"
        />
        <InputText
          name="recordTemplate"
          label="recordTemplate"
          :value="recordTemplate"
          @input="emitRecordTemplate"
        />
        <ButtonAction @click="saveSettings">Save settings</ButtonAction>
      </form>
    </div>
  </div>
</template>

<script>
import IconAction from "../forms/IconAction";
import InputText from "../forms/InputText";
import ButtonAction from "../forms/ButtonAction";
import MessageSuccess from "../forms/MessageSuccess";
import MessageError from "../forms/MessageError";
import { request } from "graphql-request";
import Spinner from "../layout/Spinner";

export default {
  components: {
    Spinner,
    MessageError,
    MessageSuccess,
    IconAction,
    InputText,
    ButtonAction,
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
      console.log("emit");
      this.$emit("update:cardTemplate", value);
    },
    emitRecordTemplate(value) {
      console.log("emit");
      this.$emit("update:recordTemplate", value);
    },
    saveSettings() {
      this.loading = true;
      this.graphqlError = null;
      this.success = null;
      request(
        this.graphqlURL,
        `mutation change($settings:[MolgenisSettingsInput]){change(settings:$settings){message}}`,
        {
          settings: [
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
          ],
        }
      )
        .then((data) => {
          this.success = data.change.message;
          this.loading = false;
        })
        .catch((error) => {
          this.graphqlError = error.response.errors[0].message;
          this.loading = false;
        });
    },
  },
};
</script>
