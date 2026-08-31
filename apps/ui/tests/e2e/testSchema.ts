import type { APIRequestContext } from "@playwright/test";

/* A spec that mutates schema-wide state gets its own copy of a template schema,
   so it can run beside the specs that read the seeded ones. Not named *.spec.ts,
   so playwright does not collect it. */

/* A retry runs in a fresh worker process, so this suffix differs per attempt and
   two attempts of one file can never touch the same schema. Schema names are
   capped at 32 characters, so keep the caller's base short. */
export const RUN_ID = Math.random().toString(36).slice(2, 8);

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

/* teardown must not throw: a beforeAll that failed leaves no schema, and an
   afterAll error would replace the real cause in the report */
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
  ).catch((error) => {
    console.error(`could not drop ${name}:`, error);
  });
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
