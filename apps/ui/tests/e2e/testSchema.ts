import type { APIRequestContext } from "@playwright/test";

/* A spec that mutates schema-wide state gets its own copy of a template schema,
   so it can run beside the specs that read the seeded ones. Not named *.spec.ts,
   so playwright does not collect it. */

export async function gql(
  api: APIRequestContext,
  url: string,
  query: string,
  variables?: Record<string, unknown>
) {
  const response = await api.post(url, {
    headers: { "Content-Type": "application/json" },
    data: variables ? { query, variables } : { query },
  });
  const body = await response.json();
  if (body.errors) {
    throw new Error(`GraphQL error on ${url}: ${JSON.stringify(body.errors)}`);
  }
  return body.data;
}

export async function signinAdmin(api: APIRequestContext, route: string) {
  return gql(
    api,
    `${route}graphql`,
    `mutation {
      signin(email: "admin", password: "admin") {
        status
        message
      }
    }`
  );
}

export async function createSchemaFromTemplate(
  api: APIRequestContext,
  route: string,
  name: string,
  template: string
) {
  await deleteSchema(api, route, name).catch(() => {}); // a crashed earlier run leaves one behind
  const data = await gql(
    api,
    `${route}graphql`,
    `mutation create($name: String, $template: String) {
      createSchema(name: $name, template: $template, includeDemoData: true) {
        message
        taskId
      }
    }`,
    { name, template }
  );
  await waitForTask(api, route, name, data.createSchema.taskId);
}

export async function deleteSchema(
  api: APIRequestContext,
  route: string,
  name: string
) {
  return gql(
    api,
    `${route}graphql`,
    `mutation deleteSchema($id: String) {
      deleteSchema(id: $id) {
        message
      }
    }`,
    { id: name }
  );
}

// the import runs as a background task, so the schema is not there when createSchema returns
async function waitForTask(
  api: APIRequestContext,
  route: string,
  name: string,
  taskId: string
) {
  for (let attempt = 0; attempt < 240; attempt++) {
    const data = await gql(
      api,
      `${route}graphql`,
      `query task($id: String) {
        _tasks(id: $id) {
          status
        }
      }`,
      { id: taskId }
    );
    const status = data._tasks[0]?.status;
    if (status === "COMPLETED") return;
    if (status === "ERROR") {
      throw new Error(`import of ${name} failed`);
    }
    await new Promise((resolve) => setTimeout(resolve, 500));
  }
  throw new Error(`import of ${name} did not finish in time`);
}
