<template>
  <div class="row">
    <div v-show="loaded" :class="editMode ? 'col-6' : 'col-12'">
      <button class="btn float-right" v-on:click="editMode = !editMode">
        Edit
      </button>
      <h1 class="text-center">{{ title }}</h1>
      <h3 class="text-center text-secondary" v-show="blurb">{{ blurb }}</h3>
      <div v-for="wiget in activeWigets" :key="wiget.name">
        <keep-alive>
          <component v-bind:is="wiget.component"></component>
        </keep-alive>
      </div>
    </div>
    <div v-if="loaded && editMode" class="col-6 mg-editor">
      <form action="">
        <div class="form-group">
          <label for="title">Title</label>
          <input id="title" type="text" class="form-control" v-model="title" />
        </div>

        <div class="form-group">
          <label for="title">Blurb</label>
          <input id="blurb" type="text" class="form-control" v-model="blurb" />
        </div>

        <div class="form-check" v-for="wiget in wigets" :key="wiget.name">
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
      editMode: false,
      title: "My title",
      blurb: "this is your home page",
      wigets: [],
    };
  },
  computed: {
    activeWigets() {
      return this.wigets.filter((w) => w.active);
    },
  },
  methods: {
    async toggleWiget(name) {
      const wiget = this.wigets.find((w) => w.name === name);
      if (wiget.active && !wiget.component) {
        const res = await loadWidget(wiget.name);
        wiget.component = res.default;
      }
    },
    async querySettings() {
      const fetchHomeSettings = gql`
        query HomePage {
          HomePage(limit: 1) {
            name
            title
            subTitle
            component
          }
        }
      `;
      const resp = await request("graphql", fetchHomeSettings).catch((e) =>
        console.error(e)
      );
      return resp.HomePage[0];
    },
    parseSettings(settings) {
      this.title = settings.title;
      this.blurb = settings.subTitle;
      this.wigets = JSON.parse(settings.component);

      this.activeWigets.forEach((aw) => {
        this.toggleWiget(aw.name);
      });
    },
  },
  async created() {
    const settings = await this.querySettings();
    this.parseSettings(settings);
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
