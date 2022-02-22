<template>
  <div>
    <h1>Database interactions</h1>
    <button @click="getPetNames">Query the database!</button>
    <br/>
    <input v-model="table" placeholder="enter table name"/>
    <br/>

    <h2>Select</h2>
    <select v-model="selectedTable">
      <option disabled value="">Select table to query:</option>
      <option>Category</option>
      <option>Order</option>
      <option>Pet</option>
      <option>User</option>
      <option>Tag</option>
    </select>

    <br/>
    <div v-if="loading">loading...</div>
    <div v-else-if="graphqlError">Error: {{ graphqlError }}</div>
    <div v-else>result: {{ rows }}</div>

  </div>
</template>

<script>
import { request } from "graphql-request";

export default {
  data() {
    return {
      rows: Array,
      loading: false,
      graphqlError: null,
      table: "enter table name",
      selectedTable: "Pet"
    };
  },
  created() {
    let query = "mutation { addCategory( name:'bella', category:dog, status:available, weight:10" +
        "}";
    this.loading = true;
    //do query
    request("graphql", query)
      .then((data) => {
        this.rows = data["Tag"];
        this.loading = false;
      })
      .catch((error) => {
        if (Array.isArray(error.response.errors)) {
          this.graphqlError = error.response.errors[0].message;
        } else {
          this.graphqlError = error;
        }
        this.loading = false;
      });
  },
  methods: {
    getPetNames() {
      let query = "{ " + this.table + "{name}}";
      console.log(this.table);
      // let query = "{Tag{name}}";
      this.loading = true;
      //do query
      request("graphql", query)
          .then((data) => {
            this.rows = data[this.table];
            this.loading = false;
          })
          .catch((error) => {
            if (Array.isArray(error.response.errors)) {
              this.graphqlError = error.response.errors[0].message;
            } else {
              this.graphqlError = error;
            }
            this.loading = false;
          });
    }

  }

};
</script>
