<template>
  <div class="hello">
    <h1>{{ msg }}</h1>
    <p>
      A demo app to take emx-2 for a spin
    </p>
    <hr>
    <div>{{myData}}</div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from 'vue';

export default defineComponent({
  name: 'CohortDemo',
  props: {
    msg: String,
  },
  data () {
    return {
      myData: {}
    }
  },
  async created () {
    this.myData = await (await fetch('/CohortsCentral/catalogue/graphql',
     {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({
        query: `{
          Variables(limit: 10) {
            name
            description
            label
            unit{
              name
            }
          }
        }`
      })
    })).json()
  }
});
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
h3 {
  margin: 40px 0 0;
}
ul {
  list-style-type: none;
  padding: 0;
}
li {
  display: inline-block;
  margin: 0 10px;
}
a {
  color: #42b983;
}
</style>
