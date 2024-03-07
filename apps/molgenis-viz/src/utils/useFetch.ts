import { reactive, toRefs } from "vue";
import { gql, request } from "graphql-request";

interface useFetchProps {
  url: string;
  query: string;
}

interface useFetchState {
  loading: Boolean;
  success: Boolean;
  data: Array[];
  error?: String;
}

export async function useFetch<useFetchProps>(url, query) {
  const state = reactive<useFetchState>({
    loading: false,
    success: false,
    error: null,
    data: [],
  });
  
  async function fetchData() {
    state.loading = true;

    try { 
      const response = await request(url, query);
      
      if (!response) {
        const err = response.json().errors[0].message;
        throw new Error(err)
      }
      
      state.data = response;
      state.success = true;
    } catch (error: Error) {
      console.log(error.message)
      // state.error = error.message;
    } finally {
      state.loading = false;
    }
  }

  await fetchData();
  return {
    ...toRefs(state)
  }
}
