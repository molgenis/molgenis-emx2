<template>
  <div>
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
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
  ButtonAction,
  InputString,
  MessageError,
  MessageSuccess,
  ShowMore,
} from "@mswertz/emx2-styleguide";
import { request } from "graphql-request";

export default {
  components: {
    InputString,
    ButtonAction,
    MessageError,
    MessageSuccess,
    ShowMore,
  },
  props: {
    session: Object,
  },
  data() {
    return {
      settings: {},
      loading: false,
      graphqlError: null,
      success: null,
    };
  },
  methods: {
    saveSettings() {
      let settingsAlter = [];
      let settingsDrop = [];
      Object.keys(this.settings).forEach((key) => {
        if (this.settings[key] != undefined) {
          settingsAlter.push({ key: key, value: this.settings[key] });
        } else {
          settingsDrop.push({ key: key });
        }
      });
      this.loading = true;
      this.loading = true;
      this.graphqlError = null;
      this.success = null;
      //alter
      if (settingsAlter.length > 0) {
        request(
          "graphql",
          `mutation change($alter:[MolgenisSettingsInput]){change(settings:$alter){message}}`,
          { settings: settingsAlter }
        )
          .then((data) => {
            this.success = data.change.message;
          })
          .catch((error) => {
            this.graphqlError = error.response.errors[0].message;
          })
          .finally((this.loading = false));
      }
      // drop, dunno how to do this in one call!
      if (settingsDrop.length > 0) {
        request(
          "graphql",
          `mutation drop($drop:[DropSettingsInput]){drop(settings:$drop){message}}`,
          { settings: settingsDrop }
        )
          .then((data) => {
            this.success = data.drop.message;
          })
          .catch((error) => {
            this.graphqlError = error.response.errors[0].message;
          })
          .finally((this.loading = false));
      }
    },
  },
};
</script>
