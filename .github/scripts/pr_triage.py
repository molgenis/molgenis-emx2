"""Decides what to do with a pull request on open/reopen, and does the writes.

The decision (decide) is pure: author login + current draft state + the loaded
teams mapping in, an assign/draft/board verdict out. No network, no GitHub
API, no environment reads. Everything below main() is I/O built on top of it.
"""

import json
import os
import re
import sys
import urllib.error
import urllib.request

BOARD_PROJECT_ID = "PVT_kwDOABnCXs4AgIEx"
STATUS_FIELD_ID = "PVTSSF_lADOABnCXs4AgIExzgVUF6A"
TEAM_FIELD_ID = "PVTSSF_lADOABnCXs4AgIExzgbkzoM"
STATUS_WORKING = "Working"
STATUS_REVIEW = "Review"
KNOWN_AUTHOR_STATUS = STATUS_WORKING
UNKNOWN_AUTHOR_STATUS = STATUS_REVIEW
UNKNOWN_AUTHOR_TEAM = "Dev"

GITHUB_API_URL = "https://api.github.com"
GITHUB_GRAPHQL_URL = "https://api.github.com/graphql"
HTTP_TIMEOUT_SECONDS = 30


class GraphqlError(Exception):
    pass


class AssignmentDroppedError(Exception):
    pass


def check_assignment_succeeded(author_login, assign_result):
    assignee_logins = [assignee["login"] for assignee in assign_result.get("assignees", [])]
    if author_login not in assignee_logins:
        raise AssignmentDroppedError(
            f"GitHub did not assign '{author_login}' (assignees after the call: {assignee_logins}). "
            f"The login is probably no longer valid for assignment; .github/pr-triage-teams.yml is probably stale."
        )


def decide(author_login, is_draft, mapping):
    team = mapping.get(author_login)
    known = bool(team) and bool(team.strip())
    if not known:
        return {
            "known": False,
            "assign": False,
            "force_draft": False,
            "status": UNKNOWN_AUTHOR_STATUS,
            "team": UNKNOWN_AUTHOR_TEAM,
        }
    return {
        "known": True,
        "assign": True,
        "force_draft": not is_draft,
        "status": KNOWN_AUTHOR_STATUS,
        "team": team,
    }


def decide_transition(action, is_draft=None):
    if action == "ready_for_review":
        return {"status": STATUS_REVIEW}
    if action == "converted_to_draft":
        return {"status": STATUS_WORKING}
    if action == "reopened":
        return {"status": STATUS_WORKING if is_draft else STATUS_REVIEW}
    return None


def strip_emoji_prefix(name):
    return re.sub(r"^[^\w]+", "", name).strip()


def find_option_id_by_name(options, target_name, strip_emoji):
    for option in options:
        candidate = strip_emoji_prefix(option["name"]) if strip_emoji else option["name"]
        if candidate == target_name:
            return option["id"]
    available = ", ".join(f'{o["name"]!r} ({o["id"]})' for o in options)
    raise ValueError(
        f"'{target_name}' does not match any live board option. Available options: {available}"
    )


def parse_teams_entries(text):
    entries = []
    in_teams_block = False
    for raw_line in text.splitlines():
        line = raw_line.split("#", 1)[0].rstrip()
        if not line.strip():
            continue
        if line.strip() == "teams:":
            in_teams_block = True
            continue
        if not in_teams_block:
            continue
        if not line.startswith((" ", "\t")):
            in_teams_block = False
            continue
        key, _, value = line.strip().partition(":")
        entries.append((key.strip(), value.strip()))
    return entries


def parse_teams_mapping(text):
    return dict(parse_teams_entries(text))


def load_teams_mapping(path):
    with open(path, encoding="utf-8") as handle:
        return parse_teams_mapping(handle.read())


def mapping_file_path(pr_triage_file):
    return os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(pr_triage_file))), "pr-triage-teams.yml")


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


def find_board_item_for_pr(pr_node_id, token):
    query = """
    query($contentId: ID!) {
      node(id: $contentId) {
        ... on PullRequest {
          projectItems(first: 100) {
            nodes {
              id
              project { id }
              fieldValueByName(name: "Status") {
                ... on ProjectV2ItemFieldSingleSelectValue { name }
              }
            }
          }
        }
      }
    }
    """
    result = graphql_request(query, {"contentId": pr_node_id}, token)
    nodes = result["data"]["node"]["projectItems"]["nodes"]
    for node in nodes:
        if node["project"]["id"] == BOARD_PROJECT_ID:
            field_value = node["fieldValueByName"]
            old_status = field_value["name"] if field_value else None
            return {"id": node["id"], "old_status": old_status}
    return None


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
    return result["data"]["node"]["options"]


def write_step_summary(rows):
    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if not summary_path:
        return
    with open(summary_path, "a", encoding="utf-8") as handle:
        handle.write("### PR triage\n\n| Fact | Value |\n|---|---|\n")
        for name, value in rows:
            handle.write(f"| {name} | {value} |\n")


def handle_transition(action, transition, pull_request):
    author_login = pull_request["user"]["login"]
    head_branch = pull_request["head"]["ref"]
    pr_node_id = pull_request["node_id"]
    target_status = transition["status"]

    verdict = f"transition -> Status {target_status} only (Team and assignee left untouched)"

    print(f"event=pull_request action={action} head_branch={head_branch} author={author_login}")
    print(f"verdict: {verdict}")

    summary_rows = [
        ("Event/action", f"pull_request / {action}"),
        ("Head branch", head_branch),
        ("Author", author_login),
        ("Verdict", verdict),
    ]

    try:
        board_token = os.environ["PROJECT_BOARD_TOKEN"]

        status_options = fetch_project_field_options(STATUS_FIELD_ID, board_token)
        status_option_id = find_option_id_by_name(status_options, target_status, strip_emoji=True)

        existing_item = find_board_item_for_pr(pr_node_id, board_token)
        if existing_item:
            item_id = existing_item["id"]
            summary_rows.append(("Board item", f"found existing item {item_id}"))
            summary_rows.append(("Status before", existing_item["old_status"]))
        else:
            item_id = add_item_to_project(pr_node_id, board_token)
            summary_rows.append(("Board item", f"no existing item, added {item_id}"))
            summary_rows.append(("Status before", "none, item just added"))

        set_project_field_option(item_id, STATUS_FIELD_ID, status_option_id, board_token)
        summary_rows.append(("Status after", target_status))
        summary_rows.append(("Team", "not touched, deliberate"))
        summary_rows.append(("Assignee", "not touched, deliberate, including when empty"))
    except Exception as error:
        summary_rows.append(("Failure", str(error)))
        raise
    finally:
        write_step_summary(summary_rows)


OPEN_TRIAGE_ACTIONS = ("opened",)


def handle_unrecognized_action(action, pull_request):
    author_login = pull_request["user"]["login"]
    head_branch = pull_request["head"]["ref"]

    verdict = f"unrecognized action '{action}' -> no action taken"

    print(f"event=pull_request action={action} head_branch={head_branch} author={author_login}")
    print(f"verdict: {verdict}")

    write_step_summary(
        [
            ("Event/action", f"pull_request / {action}"),
            ("Head branch", head_branch),
            ("Author", author_login),
            ("Verdict", verdict),
        ]
    )


def main():
    event_path = os.environ["GITHUB_EVENT_PATH"]
    with open(event_path, encoding="utf-8") as handle:
        event = json.load(handle)

    action = event["action"]
    pull_request = event["pull_request"]

    transition = decide_transition(action, is_draft=pull_request.get("draft"))
    if transition is not None:
        handle_transition(action, transition, pull_request)
        return

    if action not in OPEN_TRIAGE_ACTIONS:
        handle_unrecognized_action(action, pull_request)
        return

    author_login = pull_request["user"]["login"]
    head_branch = pull_request["head"]["ref"]
    pr_number = pull_request["number"]
    pr_node_id = pull_request["node_id"]
    is_draft = pull_request["draft"]
    repo = event["repository"]["full_name"]

    mapping_path = mapping_file_path(__file__)
    mapping = load_teams_mapping(mapping_path)

    decision = decide(author_login=author_login, is_draft=is_draft, mapping=mapping)

    if decision["known"]:
        verdict = f"known -> assign+draft+{decision['status']}/{decision['team']}"
    else:
        verdict = (
            f"unknown (absent from .github/pr-triage-teams.yml) -> "
            f"no assignee, draft untouched, boarded {decision['status']}/{decision['team']}"
        )

    print(f"event=pull_request action={action} head_branch={head_branch} author={author_login}")
    print(f"verdict: {verdict}")

    summary_rows = [
        ("Event/action", f"pull_request / {action}"),
        ("Head branch", head_branch),
        ("Author", author_login),
        ("Verdict", verdict),
    ]

    try:
        board_token = os.environ["PROJECT_BOARD_TOKEN"]

        if decision["assign"]:
            github_token = os.environ["GITHUB_TOKEN"]
            assign_result = assign_author(repo, pr_number, author_login, github_token)
            check_assignment_succeeded(author_login, assign_result)
            summary_rows.append(("Assignee set", True))
        elif decision["known"]:
            summary_rows.append(("Assignee set", "skipped, not required by decision"))
        else:
            summary_rows.append(("Assignee set", "skipped, unknown author is never assigned"))

        if decision["force_draft"]:
            github_token = os.environ["GITHUB_TOKEN"]
            draft_result = convert_pr_to_draft(pr_node_id, github_token)
            summary_rows.append(
                ("Converted to draft", draft_result["data"]["convertPullRequestToDraft"]["pullRequest"]["isDraft"])
            )
        elif decision["known"]:
            summary_rows.append(("Converted to draft", "skipped, already draft"))
        else:
            summary_rows.append(("Converted to draft", "skipped, draft state left untouched for unknown author"))

        status_options = fetch_project_field_options(STATUS_FIELD_ID, board_token)
        status_option_id = find_option_id_by_name(status_options, decision["status"], strip_emoji=True)

        team_options = fetch_project_field_options(TEAM_FIELD_ID, board_token)
        team_option_id = find_option_id_by_name(team_options, decision["team"], strip_emoji=False)

        item_id = add_item_to_project(pr_node_id, board_token)
        summary_rows.append(("Board item id", item_id))

        set_project_field_option(item_id, STATUS_FIELD_ID, status_option_id, board_token)
        summary_rows.append(("Status option id written", status_option_id))

        set_project_field_option(item_id, TEAM_FIELD_ID, team_option_id, board_token)
        summary_rows.append(("Team option id written", team_option_id))
    except Exception as error:
        summary_rows.append(("Failure", str(error)))
        raise
    finally:
        write_step_summary(summary_rows)


if __name__ == "__main__":
    main()
