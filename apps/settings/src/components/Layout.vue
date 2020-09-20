<template>
  <div>
    <MessageError v-if="error">{{ error }}</MessageError>
    <Spinner v-if="loading" />
    <div v-else>
      <h5 class="card-title">Manage layout settings</h5>
      <p>Use settings below to change look and feel of your group:</p>
      <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
      <InputString
        label="cssURL"
        v-model="settings['cssURL']"
        :defaultValue="session.settings['cssURL']"
        help="url of the bootstrap css you want to use"
      />
      <ButtonAction @click="saveSettings">Save settings</ButtonAction>
    </div>
    <ShowMore title="debug">
      <pre>
session: {{ session }}

settings: {{ settings }}
        </pre
      >
    </ShowMore>
  </div>
</template>

<script>
import {
  InputString,
  ShowMore,
  ButtonAction,
  MessageError,
  MessageSuccess
} from "@mswertz/emx2-styleguide";
import { request } from "graphql-request";

export default {
  components: {
    InputString,
    ButtonAction,
    MessageError,
    MessageSuccess,
    ShowMore
  },
  props: {
    session: Object
  },
  data() {
    return {
      settings: {},
      loading: false,
      error: null,
      success: null
    };
  },
  methods: {
    saveSettings() {
      let settingsMap = [];
      Object.keys(this.settings).forEach(key => {
        if (this.settings[key] != null) {
          settingsMap.push({ key: key, value: this.settings[key] });
        }
      });
      alert("result: " + JSON.stringify(settingsMap));
      this.loading = true;
      this.loading = true;
      this.error = null;
      this.success = null;
      request(
        "graphql",
        `mutation alter($settings:[AlterSettingInput]){alter(settings:$settings){message}}`,
        { settings: settingsMap }
      )
        .then(data => {
          this.success = data.alter.message;
        })
        .catch(error => {
          this.error = error.response.errors[0].message;
        })
        .finally((this.loading = false));
    }
  }
};
</script>
