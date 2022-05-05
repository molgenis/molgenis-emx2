import axios from "axios";

const request = async (url, graqphl) => {
  const result = await axios.post(url, { query: graqphl }).catch((error) => {
    console.log(JSON.stringify(error));
    return error;
  });
  return result.data.data;
};

export { request };
