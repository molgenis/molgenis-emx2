"""Every HTTP call PR triage makes, and the board and API addresses they
need. No decision logic and no import of another pr_triage_* module.
"""

import json
import urllib.error
import urllib.request

BOARD_PROJECT_ID = "PVT_kwDOABnCXs4AgIEx"
STATUS_FIELD_ID = "PVTSSF_lADOABnCXs4AgIExzgVUF6A"
TEAM_FIELD_ID = "PVTSSF_lADOABnCXs4AgIExzgbkzoM"
SPRINT_FIELD_ID = "PVTIF_lADOABnCXs4AgIExzgavkek"

GITHUB_API_URL = "https://api.github.com"
GITHUB_GRAPHQL_URL = "https://api.github.com/graphql"
HTTP_TIMEOUT_SECONDS = 30


class GraphqlError(Exception):
    pass


def http_request(url, token, method="GET", body=None):
    data = json.dumps(body).encode("utf-8") if body is not None else None
    request = urllib.request.Request(url, data=data, method=method)
    request.add_header("Authorization", f"Bearer {token}")
    request.add_header("Accept", "application/vnd.github+json")
    if data is not None:
        request.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(request, timeout=HTTP_TIMEOUT_SECONDS) as response:
            return json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as error:
        print(f"GitHub API error {error.code} for {method} {url}: {error.read().decode('utf-8')}")
        raise


def graphql_request(query, variables, token):
    result = http_request(
        GITHUB_GRAPHQL_URL,
        token,
        method="POST",
        body={"query": query, "variables": variables},
    )
    errors = result.get("errors")
    if errors:
        raise GraphqlError(f"GraphQL request returned errors: {errors}")
    return result


def require_node(result, resolved_id):
    node = result["data"]["node"]
    if node is None:
        raise GraphqlError(
            f"GraphQL id '{resolved_id}' resolved to no node (data.node is null). "
            f"The id is probably stale, e.g. a board field that was deleted and recreated."
        )
    return node


def assign_author(repo, pr_number, author_login, token):
    return http_request(
        f"{GITHUB_API_URL}/repos/{repo}/issues/{pr_number}/assignees",
        token,
        method="POST",
        body={"assignees": [author_login]},
    )


def convert_pr_to_draft(pr_node_id, token):
    query = """
    mutation($pullRequestId: ID!) {
      convertPullRequestToDraft(input: { pullRequestId: $pullRequestId }) {
        pullRequest { id isDraft }
      }
    }
    """
    return graphql_request(query, {"pullRequestId": pr_node_id}, token)


def add_item_to_project(pr_node_id, token):
    query = """
    mutation($projectId: ID!, $contentId: ID!) {
      addProjectV2ItemById(input: { projectId: $projectId, contentId: $contentId }) {
        item { id }
      }
    }
    """
    result = graphql_request(
        query, {"projectId": BOARD_PROJECT_ID, "contentId": pr_node_id}, token
    )
    return result["data"]["addProjectV2ItemById"]["item"]["id"]


def remove_item_from_board(item_id, token):
    """The story's one removal (aim 6): a PR's own CARD on board 15, removed
    unconditionally the moment the PR closes an issue and already has one --
    no guard on its Status or fields. This removes the board item only,
    nothing about the pull request itself; the GraphQL mutation is still
    named deleteProjectV2Item (GitHub's name, not ours). Never call this with
    an issue's item id; an issue's card is never removed by anything in this
    workflow."""
    query = """
    mutation($projectId: ID!, $itemId: ID!) {
      deleteProjectV2Item(input: { projectId: $projectId, itemId: $itemId }) {
        deletedItemId
      }
    }
    """
    return graphql_request(query, {"projectId": BOARD_PROJECT_ID, "itemId": item_id}, token)


def set_project_field_option(item_id, field_id, option_id, token):
    query = """
    mutation($projectId: ID!, $itemId: ID!, $fieldId: ID!, $optionId: String!) {
      updateProjectV2ItemFieldValue(
        input: {
          projectId: $projectId
          itemId: $itemId
          fieldId: $fieldId
          value: { singleSelectOptionId: $optionId }
        }
      ) {
        projectV2Item { id }
      }
    }
    """
    return graphql_request(
        query,
        {
            "projectId": BOARD_PROJECT_ID,
            "itemId": item_id,
            "fieldId": field_id,
            "optionId": option_id,
        },
        token,
    )


def fetch_project_field_options(field_id, token):
    query = """
    query($fieldId: ID!) {
      node(id: $fieldId) {
        ... on ProjectV2SingleSelectField {
          options { id name }
        }
      }
    }
    """
    result = graphql_request(query, {"fieldId": field_id}, token)
    return require_node(result, field_id)["options"]


def set_project_field_iteration(item_id, field_id, iteration_id, token):
    query = """
    mutation($projectId: ID!, $itemId: ID!, $fieldId: ID!, $iterationId: String!) {
      updateProjectV2ItemFieldValue(
        input: {
          projectId: $projectId
          itemId: $itemId
          fieldId: $fieldId
          value: { iterationId: $iterationId }
        }
      ) {
        projectV2Item { id }
      }
    }
    """
    return graphql_request(
        query,
        {
            "projectId": BOARD_PROJECT_ID,
            "itemId": item_id,
            "fieldId": field_id,
            "iterationId": iteration_id,
        },
        token,
    )


def fetch_project_iterations(field_id, token):
    query = """
    query($fieldId: ID!) {
      node(id: $fieldId) {
        ... on ProjectV2IterationField {
          configuration {
            iterations { id title startDate duration }
          }
        }
      }
    }
    """
    result = graphql_request(query, {"fieldId": field_id}, token)
    return require_node(result, field_id)["configuration"]["iterations"]
