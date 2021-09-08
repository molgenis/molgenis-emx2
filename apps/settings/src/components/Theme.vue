<template>
  <div>
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <Spinner v-if="loading" />
    <div v-else>
      <p>Use settings below to change look and feel:</p>
      <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
      <label>Choose primary color</label><br />
      <v-swatches class="mb-2" v-model="primary" show-fallback />
      <InputString label="Set logo url" v-model="logoURL" />
      <!--      <div class="form-group form-inline">-->
      <!--        <label>secondary color</label>-->
      <!--        <v-swatches-->
      <!--          class="ml-2"-->
      <!--          v-model="secondary"-->
      <!--          shapes="circles"-->
      <!--          popover-x="left"-->
      <!--          swatches="text-advanced"-->
      <!--          show-fallback-->
      <!--        />-->
      <!--    </div>-->
      <ButtonAction @click="saveSettings">Save theme</ButtonAction>
      <br /><br />
      <a :href="this.session.settings.cssURL">view theme css</a>
    </div>
  </div>
</template>

<script>
import {
  ButtonAction,
  InputString,
  MessageError,
  MessageSuccess,
} from "@mswertz/emx2-styleguide";
import VSwatches from "vue-swatches";
import { request } from "graphql-request";
import "vue-swatches/dist/vue-swatches.css";

export default {
  components: {
    InputString,
    ButtonAction,
    MessageError,
    MessageSuccess,
    VSwatches,
  },
  props: {
    session: Object,
  },
  data() {
    return {
      primary: null,
      secondary: null,
      logoURL: null,
      loading: false,
      graphqlError: null,
      success: null,
    };
  },
  created() {
    this.loadSettings();
  },
  watch: {
    session() {
      this.loadSettings();
    },
  },
  methods: {
    loadSettings() {
      if (this.session.settings.cssURL) {
        this.logoURL = this.session.settings.logoURL;
        const urlParams = new URL(
          this.session.settings.cssURL,
          document.baseURI
        ).searchParams;
        this.primary = urlParams.get("primary")
          ? "#" + urlParams.get("primary")
          : null;
        this.secondary = urlParams.get("secondary")
          ? "#" + urlParams.get("secondary")
          : null;
      }
    },
    saveSettings() {
      let settingsAlter = [];
      let settingsDrop = [];
      let cssUrl = "theme.css?";
      if (this.primary || this.secondary) {
        if (this.primary) cssUrl += "primary=" + this.primary.substr(1) + "&";
        if (this.secondary)
          cssUrl += "secondary=" + this.secondary.substr(1) + "&";
        cssUrl = cssUrl.substr(0, cssUrl.length - 1);
        settingsAlter.push({ key: "cssURL", value: cssUrl });
      } else {
        settingsDrop.push({ key: "cssURL" });
      }
      if (this.logoURL) {
        settingsAlter.push({ key: "logoURL", value: this.logoURL });
      } else {
        settingsDrop.push({ key: "logoURL" });
      }
      this.$emit("reload");
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
