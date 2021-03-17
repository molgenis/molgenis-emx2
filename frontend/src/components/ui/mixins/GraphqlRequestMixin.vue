<script>
import RecursiveIterator from "recursive-iterator";
import objectPath from "object-path";

export default {
  methods: {
    requestMultipart(url, query, variables) {
      return new Promise(function (resolve, reject) {
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
        fetch(url, {
          body: formData,
          method: "POST",
        })
          .then((response) => {
            if (response.ok) {
              response.json().then((result) => {
                if (!result.errors && result.data) {
                  resolve({
                    data: result.data,
                  });
                } else {
                  {
                    reject({
                      errors: result.errors,
                    });
                  }
                }
              });
            } else {
              response.json().then((result) => {
                reject({ errors: result.errors });
              });
            }
          })
          .catch((error) => {
            alert("catch: " + error.json());
            reject({ status: error, query: query });
          });
      });
    },
  },
};
</script>
