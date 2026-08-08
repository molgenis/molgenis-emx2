"""Decides what to do with a pull request on open/reopen, and does the writes.

The decision (decide) is pure: author login + current draft state + the loaded
teams mapping in, an assign/draft/board verdict out. No network, no GitHub
API, no environment reads. Everything below main() is I/O built on top of it.
"""

import datetime
import json
import os
import re
import sys
import urllib.error
import urllib.request

BOARD_PROJECT_ID = "PVT_kwDOABnCXs4AgIEx"
STATUS_FIELD_ID = "PVTSSF_lADOABnCXs4AgIExzgVUF6A"
TEAM_FIELD_ID = "PVTSSF_lADOABnCXs4AgIExzgbkzoM"
SPRINT_FIELD_ID = "PVTIF_lADOABnCXs4AgIExzgavkek"
STATUS_WORKING = "Working"
STATUS_REVIEW = "Review"
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
            "team": UNKNOWN_AUTHOR_TEAM,
        }
    return {
        "known": True,
        "assign": True,
        "force_draft": not is_draft,
        "team": team,
    }


TRANSITION_ACTIONS = ("ready_for_review", "converted_to_draft", "reopened")


def is_blank(value):
    return not value or not value.strip()


def target_status_from_draft_state(is_draft):
    return STATUS_WORKING if is_draft else STATUS_REVIEW


def current_date():
    return datetime.date.today()


def find_current_iteration(iterations, today):
    for iteration in iterations:
        start = datetime.date.fromisoformat(iteration["startDate"])
        end = start + datetime.timedelta(days=iteration["duration"])
        if start <= today < end:
            return iteration
    return None


STATUS_MANAGED_VALUES = (STATUS_WORKING, STATUS_REVIEW)


def decide_status(is_draft, current_status):
    if is_blank(current_status) or strip_emoji_prefix(current_status) in STATUS_MANAGED_VALUES:
        return target_status_from_draft_state(is_draft)
    return None


def decide_board_update(
    action, is_draft, current_status, current_team, mapped_team, current_sprint=None, current_sprint_title=None
):
    if action not in TRANSITION_ACTIONS and action != "synchronize":
        return None
    return {
        "status": decide_status(is_draft, current_status),
        "team": mapped_team if is_blank(current_team) else None,
        "sprint": current_sprint_title if (current_sprint_title and is_blank(current_sprint)) else None,
    }


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
              status: fieldValueByName(name: "Status") {
                ... on ProjectV2ItemFieldSingleSelectValue { name }
              }
              team: fieldValueByName(name: "Team") {
                ... on ProjectV2ItemFieldSingleSelectValue { name }
              }
              sprint: fieldValueByName(name: "Sprint") {
                ... on ProjectV2ItemFieldIterationValue { title }
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
            status_value = node["status"]
            team_value = node["team"]
            sprint_value = node["sprint"]
            return {
                "id": node["id"],
                "status": status_value["name"] if status_value else None,
                "team": team_value["name"] if team_value else None,
                "sprint": sprint_value["title"] if sprint_value else None,
            }
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
    return result["data"]["node"]["configuration"]["iterations"]


def write_step_summary(rows):
    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if not summary_path:
        return
    with open(summary_path, "a", encoding="utf-8") as handle:
        handle.write("### PR triage\n\n| Fact | Value |\n|---|---|\n")
        for name, value in rows:
            handle.write(f"| {name} | {value} |\n")


# One handler for every event whose only job is to move board fields, never the
# assignee or the draft state: the three transitions (which set Status outright
# and fill Team/Sprint only if empty) and synchronize (which fills all three
# only if empty). decide_board_update carries the difference; this function is
# purely the I/O around it, so a future field slots into SINGLE_SELECT_FIELD_SPECS
# without touching the fill-vs-overwrite logic.
BOARD_UPDATE_ACTIONS = TRANSITION_ACTIONS + ("synchronize",)

SINGLE_SELECT_FIELD_SPECS = (
    ("status", STATUS_FIELD_ID, True, "Status"),
    ("team", TEAM_FIELD_ID, False, "Team"),
)


def handle_board_update(action, pull_request):
    author_login = pull_request["user"]["login"]
    head_branch = pull_request["head"]["ref"]
    pr_node_id = pull_request["node_id"]
    is_draft = pull_request["draft"]

    mapping_path = mapping_file_path(__file__)
    mapping = load_teams_mapping(mapping_path)
    mapped_team = decide(author_login=author_login, is_draft=is_draft, mapping=mapping)["team"]

    print(f"event=pull_request action={action} head_branch={head_branch} author={author_login}")

    summary_rows = [
        ("Event/action", f"pull_request / {action}"),
        ("Head branch", head_branch),
        ("Author", author_login),
    ]

    try:
        board_token = os.environ["PROJECT_BOARD_TOKEN"]

        existing_item = find_board_item_for_pr(pr_node_id, board_token)
        current_status = existing_item["status"] if existing_item else None
        current_team = existing_item["team"] if existing_item else None
        current_sprint = existing_item["sprint"] if existing_item else None

        iterations = fetch_project_iterations(SPRINT_FIELD_ID, board_token)
        current_iteration = find_current_iteration(iterations, current_date())
        current_sprint_title = current_iteration["title"] if current_iteration else None

        fields = decide_board_update(
            action,
            is_draft,
            current_status,
            current_team,
            mapped_team,
            current_sprint=current_sprint,
            current_sprint_title=current_sprint_title,
        )

        verdict = (
            f"{action} -> Status "
            f"{fields['status'] if fields['status'] else f'left as {current_status!r}'}, Team "
            f"{fields['team'] if fields['team'] else f'left as {current_team!r}'}, Sprint "
            f"{fields['sprint'] if fields['sprint'] else f'left as {current_sprint!r}'}"
        )
        print(f"verdict: {verdict}")
        summary_rows.append(("Verdict", verdict))

        resolved_single_select = []
        for key, field_id, strip_emoji, label in SINGLE_SELECT_FIELD_SPECS:
            target_value = fields[key]
            if target_value is None:
                continue
            options = fetch_project_field_options(field_id, board_token)
            option_id = find_option_id_by_name(options, target_value, strip_emoji=strip_emoji)
            resolved_single_select.append((field_id, option_id, label, target_value))

        sprint_target = fields["sprint"]
        if sprint_target is not None:
            sprint_iteration_id = current_iteration["id"]

        if existing_item:
            item_id = existing_item["id"]
            summary_rows.append(("Board item", f"found existing item {item_id}"))
        else:
            item_id = add_item_to_project(pr_node_id, board_token)
            summary_rows.append(("Board item", f"no existing item, added {item_id}"))

        for field_id, option_id, label, target_value in resolved_single_select:
            set_project_field_option(item_id, field_id, option_id, board_token)
            summary_rows.append((f"{label} set", target_value))

        if sprint_target is not None:
            set_project_field_iteration(item_id, SPRINT_FIELD_ID, sprint_iteration_id, board_token)
            summary_rows.append(("Sprint set", sprint_target))

        for key, current_value, label in (
            ("status", current_status, "Status"),
            ("team", current_team, "Team"),
            ("sprint", current_sprint, "Sprint"),
        ):
            if fields[key] is None:
                summary_rows.append((label, f"left as-is ({current_value!r})"))

        summary_rows.append(("Assignee", "not touched, deliberate, including when empty"))
        summary_rows.append(("Draft state", "not touched, deliberate"))
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


def handle_open_triage(action, pull_request, repo):
    author_login = pull_request["user"]["login"]
    head_branch = pull_request["head"]["ref"]
    pr_number = pull_request["number"]
    pr_node_id = pull_request["node_id"]
    is_draft = pull_request["draft"]

    mapping_path = mapping_file_path(__file__)
    mapping = load_teams_mapping(mapping_path)

    decision = decide(author_login=author_login, is_draft=is_draft, mapping=mapping)

    print(f"event=pull_request action={action} head_branch={head_branch} author={author_login}")

    summary_rows = [
        ("Event/action", f"pull_request / {action}"),
        ("Head branch", head_branch),
        ("Author", author_login),
    ]
    failures = []

    board_token = os.environ["PROJECT_BOARD_TOKEN"]

    assignment_succeeded = True
    if decision["assign"]:
        try:
            github_token = os.environ["GITHUB_TOKEN"]
            assign_result = assign_author(repo, pr_number, author_login, github_token)
            check_assignment_succeeded(author_login, assign_result)
            summary_rows.append(("Assignee set", True))
        except Exception as error:
            print(f"ERROR: assignment failed: {error}")
            summary_rows.append(("Assignee set", f"FAILED: {error}"))
            failures.append(str(error))
            assignment_succeeded = False
    elif decision["known"]:
        summary_rows.append(("Assignee set", "skipped, not required by decision"))
    else:
        summary_rows.append(("Assignee set", "skipped, unknown author is never assigned"))

    # Assign-then-draft is a real dependency, not the chaining we removed elsewhere:
    # claiming ownership is what force-to-draft means, so a failed assignment must
    # leave the PR exactly as its author left it, boarded Review, not force-drafted
    # and unowned. The board write below stays independent of both outcomes.
    actual_is_draft = is_draft
    if decision["force_draft"] and assignment_succeeded:
        try:
            draft_result = convert_pr_to_draft(pr_node_id, board_token)
            actual_is_draft = draft_result["data"]["convertPullRequestToDraft"]["pullRequest"]["isDraft"]
            summary_rows.append(("Converted to draft", actual_is_draft))
        except Exception as error:
            print(f"ERROR: convert-to-draft failed: {error}")
            summary_rows.append(("Converted to draft", f"FAILED: {error}"))
            failures.append(str(error))
    elif decision["force_draft"] and not assignment_succeeded:
        summary_rows.append(("Converted to draft", "skipped, assignment failed"))
    elif decision["known"]:
        summary_rows.append(("Converted to draft", "skipped, already draft"))
    else:
        summary_rows.append(("Converted to draft", "skipped, draft state left untouched for unknown author"))

    target_status = decide_status(actual_is_draft, current_status=None)

    if decision["known"]:
        verdict = f"known -> assign+draft+{target_status}/{decision['team']}"
    else:
        verdict = (
            f"unknown (absent from .github/pr-triage-teams.yml) -> "
            f"no assignee, draft untouched, boarded {target_status}/{decision['team']}"
        )
    print(f"verdict: {verdict}")
    summary_rows.insert(3, ("Verdict", verdict))

    try:
        status_options = fetch_project_field_options(STATUS_FIELD_ID, board_token)
        status_option_id = find_option_id_by_name(status_options, target_status, strip_emoji=True)

        team_options = fetch_project_field_options(TEAM_FIELD_ID, board_token)
        team_option_id = find_option_id_by_name(team_options, decision["team"], strip_emoji=False)

        item_id = add_item_to_project(pr_node_id, board_token)
        summary_rows.append(("Board item id", item_id))

        set_project_field_option(item_id, STATUS_FIELD_ID, status_option_id, board_token)
        summary_rows.append(("Status option id written", status_option_id))

        set_project_field_option(item_id, TEAM_FIELD_ID, team_option_id, board_token)
        summary_rows.append(("Team option id written", team_option_id))
    except Exception as error:
        print(f"ERROR: board write failed: {error}")
        summary_rows.append(("Board write", f"FAILED: {error}"))
        failures.append(str(error))

    write_step_summary(summary_rows)

    if failures:
        print(f"ERROR: {len(failures)} outcome(s) failed: {'; '.join(failures)}")
        sys.exit(1)


def main():
    event_path = os.environ["GITHUB_EVENT_PATH"]
    with open(event_path, encoding="utf-8") as handle:
        event = json.load(handle)

    action = event["action"]
    pull_request = event["pull_request"]

    if action in BOARD_UPDATE_ACTIONS:
        handle_board_update(action, pull_request)
        return

    if action not in OPEN_TRIAGE_ACTIONS:
        handle_unrecognized_action(action, pull_request)
        return

    repo = event["repository"]["full_name"]
    handle_open_triage(action, pull_request, repo)


if __name__ == "__main__":
    main()
