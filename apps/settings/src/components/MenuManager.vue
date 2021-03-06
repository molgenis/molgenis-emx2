<template>
  <div>
    <h5 class="card-title">Manage menu</h5>
    <p>Customize menu structure and labels below:</p>
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
    <Spinner v-if="loading" />
    <div v-else>
      <div class="row float-right">
        <IconAction
          icon="plus"
          @click="
            draft.unshift({ label: 'new', href: '' });
            updateKey();
          "
        />
      </div>
      <MenuDesign :items="draft" :key="key" @change="sanitizeDraft" />
      <br />
      <div class="row float-right">
        <ButtonAlt @click="reset">Reset</ButtonAlt>
        <ButtonAction @click="saveSettings">Save</ButtonAction>
      </div>
    </div>
    <br />
    <br />
    <ShowMore title="debug">
      <pre>
draft = {{ draft }}

menu = {{ menu }}

session = {{ session }}
      </pre>
    </ShowMore>
  </div>
</template>

<script>
import MenuDesign from "./MenuDesign";
import {
  ButtonAction,
  ButtonAlt,
  IconAction,
  MessageError,
  MessageSuccess,
  ShowMore,
  DefaultMenuMixin,
} from "@mswertz/emx2-styleguide";
import { request } from "graphql-request";

export default {
  mixins: [DefaultMenuMixin],
  components: {
    MenuDesign,
    ShowMore,
    IconAction,
    ButtonAlt,
    ButtonAction,
    MessageSuccess,
    MessageError,
  },
  props: {
    session: {},
  },
  data() {
    return {
      draft: [],
      key: 0,
      graphqlError: null,
      success: null,
      loading: false,
    };
  },
  methods: {
    updateKey() {
      this.key = Math.random().toString(36).substring(7);
    },
    reset() {
      if (this.session && this.session.settings && this.session.settings.menu) {
        this.draft = JSON.parse(JSON.stringify(this.session.settings.menu));
      } else {
        //deep clone
        this.draft = JSON.parse(JSON.stringify(this.defaultMenu));
      }
      this.updateKey();
    },
    saveSettings() {
      this.loading = true;
      this.graphqlError = null;
      this.success = null;
      request(
        "graphql",
        `mutation change($settings:[MolgenisSettingsInput]){change(settings:$settings){message}}`,
        { settings: { key: "menu", value: JSON.stringify(this.draft) } }
      )
        .then((data) => {
          this.success = data.change.message;
        })
        .catch((error) => {
          this.graphqlError = error.response.errors[0].message;
        })
        .finally((this.loading = false));
    },
    sanitizeDraft() {
      if (this.draft) {
        this.draft.forEach((item) => {
          //give random keys so we can monitor moves
          item.key = Math.random().toString(36).substring(7);
          //give empty submenu so we can drag-nest
          if (item.submenu == undefined) {
            item.submenu = [];
          } else {
            item.submenu.forEach((sub) => delete sub.submenu);
          }
        });
      }
    },
  },
  created() {
    this.reset();
    this.sanitizeDraft();
  },
};
</script>
