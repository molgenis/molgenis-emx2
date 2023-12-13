<template>
  <div>
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <Spinner v-if="loading" />
    <div v-else>
      <p>Use settings below to change look and feel:</p>
      <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
      <label><b>Choose primary color</b></label
      ><br />
      <ColorPicker
        class="mb-2"
        v-model:pureColor="primaryColor"
        format="hex6"
        shape="circle"
      />
      {{ primaryColor }}
      <br /><br /><label><b>Choose the menu bar color</b></label
      ><br />
      <ColorPicker
        class="mb-2"
        v-model:pureColor="menubarColor"
        format="hex6"
        shape="circle"
      />
      {{ menubarColor }}
      <br />
      <InputString
        id="theme-url-input"
        label="Set logo url"
        v-model="logoURL"
      />
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
  Spinner,
} from "molgenis-components";
import { ColorPicker } from "vue3-colorpicker";
import "vue3-colorpicker/style.css";
import { request } from "graphql-request";

export default {
  components: {
    InputString,
    ButtonAction,
    MessageError,
    MessageSuccess,
    ColorPicker,
    Spinner,
  },
  props: {
    session: Object,
  },
  data() {
    return {
      primaryColor: null,
      menubarColor: null,
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
      if (this.session?.settings?.cssURL) {
        this.logoURL = this.session.settings.logoURL;
        const urlParams = new URL(
          this.session.settings.cssURL,
          document.baseURI
        ).searchParams;
        this.primaryColor = urlParams.get("primaryColor")
          ? "#" + urlParams.get("primaryColor")
          : null;
        this.menubarColor = urlParams.get("menubarColor")
          ? "#" + urlParams.get("menubarColor")
          : null;
      }
    },
    saveSettings() {
      let settingsAlter = [];
      let settingsDrop = [];
      let cssUrl = "theme.css?";
      if (this.primaryColor || this.menubarColor) {
        if (this.primaryColor)
          cssUrl += "primaryColor=" + this.primaryColor.substr(1) + "&";
        if (this.menubarColor)
          cssUrl += "menubarColor=" + this.menubarColor.substr(1) + "&";
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
  emits: ["reload"],
};
</script>
