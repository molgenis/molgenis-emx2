<template>
  <div>
    hello
    <InputFile label="Test the file upload" v-model="file" />
    <ButtonAction @click="submit">Submit</ButtonAction>
    Upload: {{ file }}
    <br />
    Upload: {{ error }}
    <br />
    Upload: {{ data }}
  </div>
</template>

<script>
import InputFile from "./InputFile";
import ButtonAction from "./ButtonAction";
import RecursiveIterator from "recursive-iterator";
import objectPath from "object-path";

export default {
  components: {
    ButtonAction,
    InputFile
  },
  data() {
    return {
      file: null,
      loading: false,
      error: null,
      data: null
    };
  },
  methods: {
    submit() {
      alert("start submit");
      let query = `mutation insert($value:[CollectionDocumentsInput]){insert(CollectionDocuments:$value){message}}`;
      let variables = { value: { name: "test", file: this.file } };
      this.request("graphql", query, variables)
        .then(data => {
          this.data = data;
          this.loading = false;
        })
        .catch(error => {
          this.error = "internal server error" + error;
          this.loading = false;
        });
    },
    request(url, query, variables) {
      //thanks to https://medium.com/@danielbuechele/file-uploads-with-graphql-and-apollo-5502bbf3941e
      const formData = new FormData();
      // search for File objects on the request and set it as formData
      for (let { node, path } of new RecursiveIterator(variables)) {
        if (node instanceof File) {
          const id = Math.random().toString(36);
          formData.append(id, node);
          objectPath.set(variables, path.join("."), id);
        }
      }
      formData.append("query", query);
      formData.append("variables", JSON.stringify(variables || {}));
      return fetch(url, {
        headers: { Accept: "*/*" },
        body: formData,
        method: "POST"
      });
    }
  }
};
</script>

<docs>
    ```
    <TestFile/>
    ```
</docs>
