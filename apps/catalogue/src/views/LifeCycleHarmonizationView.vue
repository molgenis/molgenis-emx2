<template>
<div>
  <h3>Harmonizarion</h3>
  <table class="table ">
    <thead>
      <tr>
        <th scope="col" :key=index >#</th>
        <th class="rotated-text text-nowrap" scope="col" v-for="(harmonization, index) in harmonizations" :key=index >
          <div>
            <span>{{harmonization.acronym}}</span>
          </div>
        </th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <th class="text-nowrap" scope="row">variable 1</th>
        <td v-for="(harmonization, index) in harmonizations" :key=index >#</td>
      </tr>
      <tr>
        <th class="text-nowrap" scope="row">variable 2</th>
        <td v-for="(harmonization, index) in harmonizations" :key=index >#</td>
      </tr>
      <tr>
        <th class="text-nowrap" scope="row">variable 3</th>
        <td v-for="(harmonization, index) in harmonizations" :key=index >#</td>
      </tr>
    </tbody>
  </table>
</div>
</template>

<script>
import { mapGetters, mapActions } from 'vuex'

export default {
  name: "LifeCycleHarmonizationView",
  computed: {
    ...mapGetters(['harmonizations']),
  },
  methods: {
    ...mapActions(['fetchHarmonizations'])
  },
  mounted () {
    this.fetchHarmonizations()
  }
}
</script>

<style scoped>
th.rotated-text {
    height: 140px;
    padding: 0 !important;
}

th.rotated-text > div {
    transform:
        /* translate(13px, 0px) */
        rotate(300deg);
    width: 1rem;
}

th.rotated-text > div > span {
    padding: 5px 10px;
}
</style>
