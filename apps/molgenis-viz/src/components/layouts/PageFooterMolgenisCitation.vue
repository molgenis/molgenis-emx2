<template>
  <div class="footer-meta molgenis-citation">
    <p>
      This database was created using
      <a href="https://www.molgenis.org/">MOLGENIS open source software</a> v{{
        molgenisVersion
      }}
      released on {{ molgenisBuildDate }}.
    </p>
  </div>
</template>

<script>
export default {
  data() {
    return {
      molgenisVersion: null,
      molgenisBuildDate: null,
    };
  },
  methods: {
    async fetchData(url) {
      const response = await fetch(url);
      return response.json();
    },
    getAppContext() {
      Promise.all([this.fetchData("/app-ui-context")]).then((response) => {
        const data = response[0];
        this.molgenisVersion = data.version;

        const buildDate = new Date(data.buildDate.split(" ")[0]);
        const month = buildDate.toLocaleString("default", { month: "long" });
        const day = buildDate.getDay();
        const year = buildDate.getFullYear();
        this.molgenisBuildDate = `${day} ${month} ${year}`;
      });
    },
  },
  mounted() {
    this.getAppContext();
  },
};
</script>

<style lang="scss">
.molgenis-citation {
  background-color: $gray-000;
  text-align: center;
  font-size: 10pt;
  max-width: $max-width;
  margin: 0 auto;
  padding: 0.8em;
}
</style>
