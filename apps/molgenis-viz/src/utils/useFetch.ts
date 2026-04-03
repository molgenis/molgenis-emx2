import { reactive, toRefs } from "vue";
import { gql, request } from "graphql-request";

interface useFetchProps {
  url: string;
  query: string;
}

interface useFetchIF {
  loading: boolean;
  success: boolean;
  data: object[];
  error?: string;
}

export async function useFetch<useFetchProps>(url, query): Promise<useFetchIF> {
  const state = reactive<useFetchIF>({
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
    ...toRefs(state),
  };
}
