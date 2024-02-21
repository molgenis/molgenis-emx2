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
      <InputString
        id="theme-url-input"
        label="Set logo url"
        v-model="logoURL"
      />
      <label><b>Additional Css</b></label>
      <InputText
        id="additional-css-input"
        label="CSS"
        v-model="additionalCss"
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
  InputText,
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
    InputText,
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
      secondaryColor: null,
      logoURL: null,
      loading: false,
      graphqlError: null,
      success: null,
      additionalCss: null,
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
      console.log(this.session);
      this.additionalCss = this.session?.settings?.additionalCss;
      if (this.session?.settings?.cssURL) {
        this.logoURL = this.session.settings.logoURL;
        const urlParams = new URL(
          this.session.settings.cssURL,
          document.baseURI
        ).searchParams;
        this.primaryColor = urlParams.get("primaryColor")
          ? "#" + urlParams.get("primaryColor")
          : null;
        this.secondaryColor = urlParams.get("secondaryColor")
          ? "#" + urlParams.get("secondaryColor")
          : null;
      }
    },
    saveSettings() {
      let settingsAlter = [];
      let settingsDrop = [];
      let cssUrl = "theme.css?";
      if (this.primaryColor || this.secondaryColor) {
        if (this.primaryColor)
          cssUrl += "primaryColor=" + this.primaryColor.substr(1) + "&";
        if (this.secondaryColor)
          cssUrl += "secondaryColor=" + this.secondaryColor.substr(1) + "&";
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
      if (this.additionalCss) {
        settingsAlter.push({ key: "additionalCss", value: this.additionalCss });
      } else {
        settingsDrop.push({ key: "additionalCss" });
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
