<template>
  <div>
    <h1>Page list</h1>
    <table class="table">
      <thead>
        <tr>
          <th>Page</th>
          <th>View</th>
          <th>Edit</th>
        </tr>
      </thead>
      <tr v-for="page in pages">
        <td>{{ page }}</td>
        <td>
          <router-link :to="'/' + page">view</router-link>
        </td>
        <td>
          <router-link :to="'/' + page + '/edit'">edit</router-link>
        </td>
      </tr>
    </table>
  </div>
</template>

<script>
import {ShowMore} from "molgenis-components";

export default {
  components: {
    ShowMore
  },
  props: {
    session: Object
  },
  computed: {
    pages() {
      if (this.session && this.session.settings) {
        return Object.keys(this.session.settings)
          .filter(key => key.startsWith("page."))
          .map(key => key.substring(5));
      }
    }
  }
};
</script>
