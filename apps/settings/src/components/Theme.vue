<template>
  <div>
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <Spinner v-if="loading" />
    <div v-else>
      <h5 class="card-title">Manage layout settings</h5>
      <p>Use settings below to change look and feel of your group:</p>
      <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
      <InputString name="primary color" v-model="theme_primary" />
      <InputString name="secondary color" v-model="theme_secondary" />
      <ButtonAction @click="saveSettings">Save settings</ButtonAction>
    </div>
    <ShowMore title="debug">
      <pre>
session: {{ session }}

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
      theme_primary: null,
      theme_secondary: null,
      loading: false,
      graphqlError: null,
      success: null,
    };
  },
  methods: {
    saveSettings() {
      let settingsAlter = [];
      let settingsDrop = [];
      if (this.theme_primary || this.theme_secondary) {
        let cssUrl = "theme.css?";
        if (this.theme_primary) cssUrl += "primary=" + this.theme_primary + "&";
        if (this.theme_secondary)
          cssUrl += "secondary=" + this.theme_secondary + "&";
        cssUrl = cssUrl.substr(0, cssUrl.length - 1);
        settingsAlter.push({ key: "cssURL", value: cssUrl });
      } else {
        settingsDrop.push({ key: "cssURL" });
      }
      console.log(JSON.stringify(settingsAlter));
      this.loading = true;
      this.loading = true;
      this.graphqlError = null;
      this.success = null;
      //alter
      if (settingsAlter.length > 0) {
        request(
          "graphql",
          `mutation change($alter:[MolgenisSettingsInput]){change(settings:$alter){message}}`,
          { alter: settingsAlter }
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
          { drop: settingsDrop }
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
