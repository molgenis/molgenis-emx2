<template>
  <div>
    <h5 class="card-title">Manage pages</h5>
    <ul>
      <li v-for="(page, index) in pages" :key="index">
        <a :href="'../pages/#/' + page">{{ page }}</a>
        <IconAction icon="edit" @click="openPageEdit(page)" />
      </li>
    </ul>
    <form class="form-inline">
      <InputString
        id="page-title"
        label="Add new page: "
        v-model="newPage"
        :errorMessage="nameError" />
      <ButtonAction v-if="newPage && !nameError" @click="openPageEdit(newPage)"
        >Create new
      </ButtonAction>
    </form>
  </div>
</template>

<script>
import { ButtonAction, IconAction, InputString } from "molgenis-components";

export default {
  components: {
    IconAction,
    ButtonAction,
    InputString,
  },
  props: {
    session: Object,
  },
  data() {
    return {
      newPage: null,
    };
  },
  methods: {
    openPageEdit(page) {
      window.open("../pages/#/" + page + "/edit", "_self");
    },
  },
  computed: {
    nameError() {
      if (this.pages.includes(this.newPage)) {
        return "Page name already exists";
      } else {
        return undefined;
      }
    },
    pages() {
      if (this.session && this.session.settings) {
        return Object.keys(this.session.settings)
          .filter(key => key.startsWith("page."))
          .map(key => key.substring(5));
      }
      return [];
    },
  },
};
</script>
