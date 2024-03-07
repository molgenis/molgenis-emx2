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
      state.data = response;
      state.success = true;
    } catch (error: Error) {
      state.error = error; 
    } finally {
      state.loading = false;
    }
  }

  await fetchData();
  return {
    ...toRefs(state)
  }
}
