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
      <h1 class="text-center">{{ settings.title }}</h1>
      <h3 class="text-center text-secondary" v-show="settings.subTitle">
        {{ settings.subTitle }}
      </h3>
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
        class="btn btn-outline-secondary"
        v-on:click="editMode = !editMode"
      >
        Close
      </button>
      <button class="btn btn-outline-secondary" v-on:click="saveSetting">
        Save
      </button>
      <p v-show="saving">Saving...</p>
      <form action="">
        <div class="form-group">
          <label for="title">Title</label>
          <input
            id="title"
            type="text"
            class="form-control"
            v-model="settings.title"
          />
        </div>

        <div class="form-group">
          <label for="title">Sub title</label>
          <input
            id="subTitle"
            type="text"
            class="form-control"
            v-model="settings.subTitle"
          />
        </div>

        <div
          class="form-check"
          v-for="wiget in settings.wigets"
          :key="wiget.name"
        >
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

export default {
  name: "HomeView",
  data() {
    return {
      loaded: false,
      saving: false,
      editMode: false,
      componentStore: {},
      settings: {
        title: "My title",
        subTitle: "this is your home page",
        wigets: [],
      },
    };
  },
  computed: {
    activeWigets() {
      return this.settings.wigets.filter((w) => w.active);
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
      const wiget = this.settings.wigets.find((w) => w.name === name);
      if (wiget.active && !this.componentStore[name]) {
        const res = await loadWidget(wiget.name);
        this.componentStore[wiget.name] = res.default;
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
          value: JSON.stringify(this.settings),
        },
      }).catch((e) => {
        console.error(e);
      });

      console.log(resp.change.message);
      this.saving = false;
    },
    parseSettings(settingsString) {
      if (settingsString) {
        this.settings = JSON.parse(settingsString);
      }

      this.activeWigets.forEach((aw) => {
        this.toggleWiget(aw.name);
      });
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
