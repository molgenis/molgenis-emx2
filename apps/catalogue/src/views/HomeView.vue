<template>
  <div class="row">
    <div :class="editMode ? 'col-6' : 'col-12'">
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
    <div v-if="editMode" class="col-6 mg-editor">
      <form action="">
        <div class="form-group">
          <label for="title">Title</label>
          <input id="title" type="text" class="form-control" v-model="title" />
        </div>

        <div class="form-group">
          <label for="title">Blurb</label>
          <input id="blurb" type="text" class="form-control" v-model="blurb" />
        </div>

        <div class="form-check">
          <input
            class="form-check-input"
            type="checkbox"
            value=""
            id="defaultCheck1"
            v-model="wigets[0].active"
            @change="toggleWiget('Comp1')"
          />
          <label class="form-check-label" for="defaultCheck1"> Comp 1 </label>
        </div>
        <div class="form-check">
          <input
            class="form-check-input"
            type="checkbox"
            value=""
            id="defaultCheck2"
            v-model="wigets[1].active"
            @change="toggleWiget('Comp2')"
          />
          <label class="form-check-label" for="defaultCheck2"> Comp 2 </label>
        </div>
      </form>
    </div>
  </div>
</template>

<script>
const loadWidget = (widget) => {
  widget += ".vue";
  console.log("Load " + widget);
  return import("../components/home-page/" + widget);
};
export default {
  name: "HomeView",
  data() {
    return {
      editMode: false,
      title: "Cohorts, biobanks and dataset of the UMCG",
      blurb: "Universitair Medisch Centrum Groningen, the Netherlands",
      wigets: [
        { name: "Comp1", active: false, component: null },
        { name: "Comp2", active: false, component: null },
      ],
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
  }
};
</script>

<style scoped>
.mg-editor {
  border-left: solid 1px;
}
</style>
>
