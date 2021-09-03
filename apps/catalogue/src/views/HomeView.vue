<template>
  <div class="row">
    <div v-if="loaded" :class="editMode ? 'col-6' : 'col-12'">
      <button
        v-show="canEdit && !editMode"
        class="btn btn-outline-secondary float-right"
        v-on:click="editMode = !editMode"
      >
        Edit
      </button>
      <div v-for="wiget in activeWigets" :key="wiget.name">
        <keep-alive>
          <component
            v-bind:is="componentStore[wiget.name]"
            v-bind="wiget.props"
          ></component>
        </keep-alive>
      </div>
    </div>
    <div v-if="loaded && editMode" class="col-6 mg-editor">
      <button
        class="btn btn-outline-secondary mr-1"
        v-on:click="editMode = !editMode"
      >
        Close
      </button>
      <button class="btn btn-outline-secondary" v-on:click="saveSetting">
        Save
      </button>
      <p v-show="saving">Saving...</p>
      <h2 class="mt-1">Wigets settings</h2>
      <form class="mt-1" action="">
        <div class="form-check" v-for="wiget in wigets" :key="wiget.name">
          <hr />
          <input
            class="form-check-input"
            type="checkbox"
            :id="wiget.name"
            v-model="wiget.active"
            @change="toggleWiget(wiget.name)"
          />
          <label class="form-check-label" :for="wiget.name">
            {{ wiget.name }}
          </label>
          <ul class="un">
            <li v-for="(value, name) in wiget.props" :key="name">
              <div class="form-group">
                <label for="name">{{ name }}</label>
                <input
                  :id="name"
                  type="text"
                  class="form-control"
                  :value="value"
                  @keyup="updateProp(wiget.name, name, $event)"
                />
              </div>
            </li>
          </ul>
        </div>
      </form>
    </div>
  </div>
</template>

<script>
import { request, gql } from "graphql-request";

const loadWidget = (widget) => {
  widget += ".vue";
  console.log("Load " + widget);
  return import("../components/home-page/" + widget);
};

const WIGETS_DEFAULTS = [
  {
    name: "HeadingWiget",
    active: false,
    props: { title: "Your Title", subTitle: "Your sub title can go here " },
  },
  { name: "SearchComp", active: true, props: { resourceType: "cohorts" } },
];

export default {
  name: "HomeView",
  data() {
    return {
      loaded: false,
      saving: false,
      editMode: false,
      componentStore: {},
      wigets: [],
    };
  },
  computed: {
    activeWigets() {
      return this.wigets.filter((w) => w.active);
    },
    canEdit() {
      return !!(
        // todo: wireup session via prop or store
        (this.$parent.session && this.$parent.session.roles.includes("Editor"))
      );
    },
  },
  methods: {
    async toggleWiget(name) {
      const wiget = this.wigets.find((w) => w.name === name);
      if (wiget.active && !this.componentStore[name]) {
        const res = await loadWidget(wiget.name);
        this.$set(this.componentStore, wiget.name, res.default);
      }
    },
    async loadSettings() {
      const fetchHomeSettings = gql`
        query HomePage {
          _settings {
            key
            value
          }
        }
      `;
      const resp = await request("graphql", fetchHomeSettings).catch((e) =>
        console.error(e)
      );
      return resp._settings && resp._settings.find((s) => s.key === "home-page")
        ? resp._settings.find((s) => s.key === "home-page").value
        : null;
    },
    async saveSetting() {
      if (this.saving) {
        return;
      }
      this.saving = true;
      const settingsMutation = gql`
        mutation change($settings: [MolgenisSettingsInput]) {
          change(settings: $settings) {
            message
          }
        }
      `;

      const resp = await request("graphql", settingsMutation, {
        settings: {
          key: "home-page",
          value: JSON.stringify(this.wigets),
        },
      }).catch((e) => {
        console.error(e);
      });

      console.log(resp.change.message);
      this.saving = false;
    },
    parseSettings(settingsString) {
      if (settingsString) {
        const loadedWigets = JSON.parse(settingsString);
        // Merge loaded setting with the defaults
        this.wigets = WIGETS_DEFAULTS.reduce((accum, defaultWiget) => {
          // If the wiget was not loaded from the backend
          if (!accum.find((w) => w.name === defaultWiget.name)) {
            // Use the default add add it at the default position
            const position = WIGETS_DEFAULTS.findIndex(
              (wd) => wd.name === defaultWiget.name
            );
            accum.splice(position, 0, defaultWiget);
          }
          return accum;
        }, loadedWigets);
      }

      this.activeWigets.forEach((aw) => {
        this.toggleWiget(aw.name);
      });
    },
    updateProp(wigetName, propName, event) {
      this.wigets.find((w) => w.name === wigetName).props[propName] =
        event.target.value;
    },
  },
  async created() {
    const settingsString = await this.loadSettings();
    this.parseSettings(settingsString);
    this.loaded = true;
  },
};
</script>

<style scoped>
.mg-editor {
  border-left: solid 1px;
}
</style>
>
