import { reactive, toRefs } from "vue";
import { gql, request } from "graphql-request";

interface useFetchProps {
  url: String;
  query: String;
}

interface useFetchState {
  loading: Boolean;
  success: Boolean;
  data: Array[];
  error?: String;
}

export async function useFetch<useFetchProps>(url, query) {
  const state = reactive<useFetchState>({
    loading: true,
    success: false,
    error: null,
    data: [],
  });
  
  async function fetchData() {
    state.loading = true;

    try { 
      const response = await request(url, query);
  
      if (!response.ok) {
        throw new Error(response.errors[0].message);
      }
      state.data = response;
      state.success = true;
    } catch (error: Error) {
      state.error = error.message;
    } finally {
      state.loading = false;
    }
  }

  await fetchData();
  return {
    ...toRefs(state)
  }
}
