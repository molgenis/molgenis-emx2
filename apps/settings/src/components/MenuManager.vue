<template>
  <div>
    <h5 class="card-title">Manage menu</h5>
    <p>Customize menu structure and labels below.</p>
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
    <Spinner v-if="loading" />
    <div v-else class="container">
      <div>
        <IconAction @click="addItem" icon="plus" />
        <ButtonAlt @click="reset">Reset</ButtonAlt>
        <ButtonAction @click="saveSettings">Save</ButtonAction>
      </div>
      <br />
      <MenuDesign
        :items="draft"
        :key="key"
        @change="sanitizeDraft"
        @add="addItem"
      />
    </div>
    <div>
      Help:
      <ul>
        <li>
          use 'label' to set the name how your menu item should be displayed
        </li>
        <li>
          use 'href' to define where your menu item links to. Simply use name of
          an apps to link to it within this schema (e.g. 'tables' links to
          tables app). You can also make cross-links to other schema using a
          fully qualified path like '/otherschema/tables'. And you can link to
          other servers using http://otherserver.com/.
        </li>
        <li>
          use 'role' to indicate if menu item should be shown only in case user
          has particular role (optional).
        </li>
      </ul>
      You can drag to change order using the three dots (and also create submenu
      items by dragging slightly to the right).
    </div>
  </div>
</template>

<script>
import MenuDesign from "./MenuDesign.vue";
import {
  ButtonAction,
  ButtonAlt,
  IconAction,
  MessageError,
  MessageSuccess,
} from "molgenis-components";
import { request } from "graphql-request";

const defaultMenu = [
  { label: "Tables", href: "tables", role: "Viewer" },
  {
    label: "Schema",
    href: "schema",
    role: "Manager",
  },
  {
    label: "Up/Download",
    href: "updownload",
    role: "Editor",
  },
  {
    label: "Graphql",
    href: "graphql-playground",
    role: "Viewer",
  },
  {
    label: "Settings",
    href: "settings",
    role: "Manager",
  },
  {
    label: "Help",
    href: "docs",
    role: "Viewer",
  },
];

export default {
  components: {
    MenuDesign,
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
    addItem() {
      this.draft.unshift({ label: "new", href: "" });
      this.updateKey();
    },
    updateKey() {
      this.key = Math.random().toString(36).substring(7);
    },
    reset() {
      if (this.session?.settings?.menu) {
        this.draft = JSON.parse(JSON.stringify(this.session.settings.menu));
      } else {
        //deep clone
        this.draft = JSON.parse(JSON.stringify(defaultMenu));
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
