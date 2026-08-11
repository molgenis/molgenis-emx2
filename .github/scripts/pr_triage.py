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


def is_blank(value):
    return not value or not value.strip()


def team_for(author_login, mapping):
    team = mapping.get(author_login)
    return None if is_blank(team) else team


def decide(author_login, is_draft, mapping):
    team = team_for(author_login, mapping)
    if team is None:
        return {"known": False, "force_draft": False, "team": UNKNOWN_AUTHOR_TEAM}
    return {"known": True, "force_draft": not is_draft, "team": team}


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
    target = target_status_from_draft_state(is_draft)
    if is_blank(current_status):
        return target
    current = strip_emoji_prefix(current_status)
    if current == target:
        return None
    if current in STATUS_MANAGED_VALUES:
        return target
    return None


def decide_board_update(is_draft, current_status, current_team, mapped_team, current_sprint=None, current_sprint_title=None):
    return {
        "status": decide_status(is_draft, current_status),
        "team": mapped_team if is_blank(current_team) else None,
        "sprint": current_sprint_title if (current_sprint_title and is_blank(current_sprint)) else None,
    }


def keyword_closing_issues(all_closing_issues, user_linked_issues):
    """Aim 6 redirects only on a KEYWORD-derived closing reference
    (close/closes/closed/fix/fixes/fixed/resolve/resolves/resolved in the PR
    body) [owner ruling, narrowing the original "closingIssuesReferences
    reports it, so it counts" reading]. An issue linked only by hand through
    GitHub's Development sidebar, with no keyword in the body, does NOT
    redirect -- `Addresses: <url>` is a real habit in this repo, used
    deliberately to link without auto-closing.

    GitHub's closingIssuesReferences field reports both kinds together;
    userLinkedOnly=true reports only the hand-linked ones. Keyword-derived is
    therefore `all_closing_issues` minus `user_linked_issues`, matched by
    node id -- verified against two real PRs of each shape, see
    notes/github-facts.md §8."""
    user_linked_ids = {issue["id"] for issue in user_linked_issues}
    return [issue for issue in all_closing_issues if issue["id"] not in user_linked_ids]


def decide_redirect_to_issues(closing_issues):
    """Aim 6: a PR that closes at least one issue gets no card of its own --
    every linked issue's card is written instead. An empty list changes
    nothing; the PR is boarded exactly as it always was."""
    return bool(closing_issues)


def decide_remove_pr_card(closing_issues, existing_pr_item):
    """The story's one removal: unconditional, and narrow. True exactly when
    the PR closes at least one issue AND the PR already has an item on board
    15 -- never guarded by that item's Status or fields, and never true for
    an issue's item, which this is never called with. "Removal" is
    board-only: the card comes off board 15, nothing about the PR itself
    changes.

    Deliberately action-blind: this function does not know synchronize is
    excluded from removal. That exclusion lives in the one call site,
    apply_board_write's `action not in PR_CARD_REMOVAL_ACTIONS` guard --
    apply_redirected_board_write (and this function) are never reached for
    synchronize at all. If a second caller is ever added, that guard has to
    move or be duplicated; it does not enforce itself here."""
    return decide_redirect_to_issues(closing_issues) and existing_pr_item is not None


def decide_issue_card_update(action, is_draft, existing_issue_item, mapped_team, current_sprint_title):
    """Pure decision for ONE linked issue's card. existing_issue_item is None
    when the issue has no item yet on board 15.

    - `synchronize` never touches a linked issue's card at all -- existing or
      missing, this returns None.
    - `edited` never touches an EXISTING issue card (Status is not moved by a
      body edit), but still adds and fills a missing one, because a PR with
      no card and an issue with no card is the one outcome no aim tolerates.
      Status is left unwritten on the card it adds -- only opened and the
      three transitions ever move a linked issue's Status.
    - `opened` and the three transitions set Status only on an existing card
      (Team and Sprint stay exactly as a person left them), and fill all
      three -- Status, Team, Sprint -- on a card they add.

    Returns None for "write nothing", otherwise a fields dict shaped like
    resolve_and_apply_board_fields expects: {"status", "team", "sprint"},
    each either a target value or None meaning "leave as-is"."""
    if existing_issue_item is not None:
        if action not in ISSUE_STATUS_ACTIONS:
            return None
        return {"status": decide_status(is_draft, existing_issue_item["status"]), "team": None, "sprint": None}

    if action not in ISSUE_STATUS_ACTIONS and action not in ISSUE_ADD_ONLY_ACTIONS:
        return None
    status = target_status_from_draft_state(is_draft) if action in ISSUE_STATUS_ACTIONS else None
    return {"status": status, "team": mapped_team, "sprint": current_sprint_title}


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


def find_board_item_for_pr(content_id, token):
    """Resolves a content node's (PullRequest or Issue) item on board 15, plus
    -- for a PullRequest only -- the issues it closes by a BODY KEYWORD
    (close/closes/fixes/resolves/... ). Both content kinds share the
    identical projectItems shape, so the fragment is factored into
    ProjectItemFields; closingIssuesReferences exists only on PullRequest, and
    riding both the unfiltered and userLinkedOnly variants on this same query
    costs no extra round trip. keyword_closing_issues does the split -- see
    notes/github-facts.md §8. Returns {"item": item-or-None, "closing_issues":
    [{"id", "number"}, ...]} -- closing_issues is always [] for an Issue node."""
    query = """
    query($contentId: ID!) {
      node(id: $contentId) {
        ... on PullRequest {
          projectItems(first: 100) { ...ProjectItemFields }
          closingIssuesReferences(first: 20) {
            nodes { id number }
          }
          userLinkedClosingIssues: closingIssuesReferences(first: 20, userLinkedOnly: true) {
            nodes { id number }
          }
        }
        ... on Issue {
          projectItems(first: 100) { ...ProjectItemFields }
        }
      }
    }
    fragment ProjectItemFields on ProjectV2ItemConnection {
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
    """
    result = graphql_request(query, {"contentId": content_id}, token)
    node = require_node(result, content_id)
    item = None
    for candidate in node["projectItems"]["nodes"]:
        if candidate["project"]["id"] == BOARD_PROJECT_ID:
            status_value = candidate["status"]
            team_value = candidate["team"]
            sprint_value = candidate["sprint"]
            item = {
                "id": candidate["id"],
                "status": status_value["name"] if status_value else None,
                "team": team_value["name"] if team_value else None,
                "sprint": sprint_value["title"] if sprint_value else None,
            }
            break
    closing_issues_field = node.get("closingIssuesReferences")
    all_closing_issues = closing_issues_field["nodes"] if closing_issues_field else []
    user_linked_field = node.get("userLinkedClosingIssues")
    user_linked_issues = user_linked_field["nodes"] if user_linked_field else []
    closing_issues = keyword_closing_issues(all_closing_issues, user_linked_issues)
    return {"item": item, "closing_issues": closing_issues}


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


def write_step_summary(rows):
    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if not summary_path:
        return
    with open(summary_path, "a", encoding="utf-8") as handle:
        handle.write("### PR triage\n\n| Fact | Value |\n|---|---|\n")
        for name, value in rows:
            handle.write(f"| {name} | {value} |\n")


# Every event that touches the board (the three transitions, synchronize,
# edited, and opened) applies the one rule in decide_board_update -- to the
# PR's OWN card when it closes no issue: Status per decide_status, Team and
# Sprint fill-only. Sprint's mutation shape differs from a single-select
# (iterationId, not singleSelectOptionId), which is why it is written separately
# below rather than through SINGLE_SELECT_FIELD_SPECS; a real fourth field would
# still touch this table, current_values, FIELD_LABELS, and the write loop below.
TRANSITION_ACTIONS = ("ready_for_review", "converted_to_draft", "reopened")
BOARD_UPDATE_ACTIONS = TRANSITION_ACTIONS + ("synchronize", "edited")

# Aim 6, redirected to a linked issue's card instead of the PR's own: which
# actions may set that card's Status, and which may only add+fill a missing
# one without ever moving Status. See decide_issue_card_update.
ISSUE_STATUS_ACTIONS = TRANSITION_ACTIONS + ("opened",)
ISSUE_ADD_ONLY_ACTIONS = ("edited",)

# The actions that remove the PR's own card when it closes an issue: opened,
# edited, and the three transitions. NOT synchronize -- a push that removed
# the PR's card while never adding the issue's would leave zero cards for
# the same piece of work (owner ruling, review finding F1). For a closing
# PR, synchronize therefore writes nothing anywhere: not the PR's card
# (removal or otherwise), not any issue's.
PR_CARD_REMOVAL_ACTIONS = ISSUE_STATUS_ACTIONS + ISSUE_ADD_ONLY_ACTIONS

SINGLE_SELECT_FIELD_SPECS = (
    ("status", STATUS_FIELD_ID, True, "Status"),
    ("team", TEAM_FIELD_ID, False, "Team"),
)
FIELD_LABELS = {"status": "Status", "team": "Team", "sprint": "Sprint"}


def resolve_and_apply_board_fields(pr_node_id, existing_item, fields, current_values, current_iteration, board_token, summary_rows):
    resolved_single_select = {}
    for key, field_id, strip_emoji, _label in SINGLE_SELECT_FIELD_SPECS:
        target_value = fields[key]
        if target_value is not None:
            options = fetch_project_field_options(field_id, board_token)
            resolved_single_select[key] = (field_id, find_option_id_by_name(options, target_value, strip_emoji=strip_emoji))

    sprint_iteration_id = current_iteration["id"] if (fields["sprint"] is not None and current_iteration) else None

    if existing_item:
        item_id = existing_item["id"]
        summary_rows.append(("Board item", f"found existing item {item_id}"))
    else:
        item_id = add_item_to_project(pr_node_id, board_token)
        summary_rows.append(("Board item", f"no existing item, added {item_id}"))

    for key, label in FIELD_LABELS.items():
        target_value = fields[key]
        if target_value is None:
            summary_rows.append((label, f"left as-is ({current_values[key]!r})"))
        elif key == "sprint":
            set_project_field_iteration(item_id, SPRINT_FIELD_ID, sprint_iteration_id, board_token)
            summary_rows.append((f"{label} set", target_value))
        else:
            field_id, option_id = resolved_single_select[key]
            set_project_field_option(item_id, field_id, option_id, board_token)
            summary_rows.append((f"{label} set", target_value))

    return item_id


def resolve_current_iteration(board_token):
    iterations = fetch_project_iterations(SPRINT_FIELD_ID, board_token)
    return find_current_iteration(iterations, current_date())


def board_verdict(prefix, fields, current_values):
    parts = [
        f"{label} {fields[key] if fields[key] else f'left as {current_values[key]!r}'}"
        for key, label in FIELD_LABELS.items()
    ]
    return f"{prefix} -> " + ", ".join(parts)


def _record_verdict(summary_rows, insert_at, verdict):
    print(f"verdict: {verdict}")
    row = ("Verdict", verdict)
    if insert_at is None:
        summary_rows.append(row)
    else:
        summary_rows.insert(insert_at, row)


def apply_redirected_board_write(action, is_draft, mapped_team, pr_item, closing_issues, board_token, summary_rows):
    """Aim 6, for an action in PR_CARD_REMOVAL_ACTIONS: the PR closes at
    least one issue, so it gets no card of its own -- an existing one is
    removed from board 15, unconditionally -- and each linked issue's card
    is written instead, per decide_issue_card_update. synchronize never
    reaches this function; see apply_board_write."""
    if decide_remove_pr_card(closing_issues, pr_item):
        was = f"Status {pr_item['status']!r}, Team {pr_item['team']!r}, Sprint {pr_item['sprint']!r}"
        # Recorded BEFORE the mutation runs -- this row is the one place a
        # person can recover the values from, so a timeout after GitHub has
        # already committed the removal must not lose it.
        summary_rows.append(("PR card removed", f"{pr_item['id']} (was: {was})"))
        remove_item_from_board(pr_item["id"], board_token)
    else:
        summary_rows.append(("PR card", f"none to add or remove (closes {len(closing_issues)} linked issue(s))"))

    current_iteration = resolve_current_iteration(board_token)
    current_sprint_title = current_iteration["title"] if current_iteration else None

    for issue_ref in closing_issues:
        issue_lookup = find_board_item_for_pr(issue_ref["id"], board_token)
        existing_issue_item = issue_lookup["item"]
        fields = decide_issue_card_update(action, is_draft, existing_issue_item, mapped_team, current_sprint_title)
        if fields is None:
            summary_rows.append((f"Issue #{issue_ref['number']}", "left untouched"))
            continue
        current_values = {
            "status": existing_issue_item["status"] if existing_issue_item else None,
            "team": existing_issue_item["team"] if existing_issue_item else None,
            "sprint": existing_issue_item["sprint"] if existing_issue_item else None,
        }
        item_id = resolve_and_apply_board_fields(
            issue_ref["id"], existing_issue_item, fields, current_values, current_iteration, board_token, summary_rows
        )
        summary_rows.append((f"Issue #{issue_ref['number']}", f"item {item_id}"))


def apply_board_write(action, pr_node_id, is_draft, mapped_team, board_token, summary_rows, verdict_prefix, insert_verdict_at=None):
    """The redirect-aware board write shared by every board-writing action --
    opened, the three transitions, synchronize, and edited. Reads the PR's
    own item and the issues it closes in one round trip (find_board_item_for_pr),
    then either redirects to those issues' cards (aim 6, decide_redirect_to_issues)
    or writes the PR's own card exactly as before this ticket. For a closing
    PR, synchronize is a special case: it is not in PR_CARD_REMOVAL_ACTIONS,
    so it writes nothing anywhere -- not the PR's card, not any issue's."""
    lookup = find_board_item_for_pr(pr_node_id, board_token)
    pr_item = lookup["item"]
    closing_issues = lookup["closing_issues"]

    if decide_redirect_to_issues(closing_issues):
        if action not in PR_CARD_REMOVAL_ACTIONS:
            _record_verdict(
                summary_rows, insert_verdict_at, f"{verdict_prefix} -> closes {len(closing_issues)} issue(s), {action} writes nothing"
            )
            return
        _record_verdict(
            summary_rows, insert_verdict_at, f"{verdict_prefix} -> closes {len(closing_issues)} issue(s), no PR card"
        )
        apply_redirected_board_write(action, is_draft, mapped_team, pr_item, closing_issues, board_token, summary_rows)
        return

    current_values = {
        "status": pr_item["status"] if pr_item else None,
        "team": pr_item["team"] if pr_item else None,
        "sprint": pr_item["sprint"] if pr_item else None,
    }
    current_iteration = None
    if is_blank(current_values["sprint"]):
        current_iteration = resolve_current_iteration(board_token)
    current_sprint_title = current_iteration["title"] if current_iteration else None

    fields = decide_board_update(
        is_draft,
        current_values["status"],
        current_values["team"],
        mapped_team,
        current_sprint=current_values["sprint"],
        current_sprint_title=current_sprint_title,
    )

    _record_verdict(summary_rows, insert_verdict_at, board_verdict(verdict_prefix, fields, current_values))

    resolve_and_apply_board_fields(pr_node_id, pr_item, fields, current_values, current_iteration, board_token, summary_rows)


def handle_board_update(action, pull_request):
    author_login = pull_request["user"]["login"]
    head_branch = pull_request["head"]["ref"]
    pr_node_id = pull_request["node_id"]
    is_draft = pull_request["draft"]

    mapping_path = mapping_file_path(__file__)
    mapping = load_teams_mapping(mapping_path)
    mapped_team = team_for(author_login, mapping) or UNKNOWN_AUTHOR_TEAM

    print(f"event=pull_request action={action} head_branch={head_branch} author={author_login}")

    summary_rows = [
        ("Event/action", f"pull_request / {action}"),
        ("Head branch", head_branch),
        ("Author", author_login),
    ]

    try:
        board_token = os.environ["PROJECT_BOARD_TOKEN"]

        apply_board_write(action, pr_node_id, is_draft, mapped_team, board_token, summary_rows, verdict_prefix=action)

        summary_rows.append(("Assignee", "not touched, deliberate, including when empty"))
        summary_rows.append(("Draft state", "not touched, deliberate"))
    except Exception as error:
        summary_rows.append(("Failure", str(error)))
        raise
    finally:
        write_step_summary(summary_rows)


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

    print(f"event=pull_request action={action} head_branch={head_branch} author={author_login}")

    summary_rows = [
        ("Event/action", f"pull_request / {action}"),
        ("Head branch", head_branch),
        ("Author", author_login),
    ]
    failures = []

    try:
        mapping_path = mapping_file_path(__file__)
        mapping = load_teams_mapping(mapping_path)

        decision = decide(author_login=author_login, is_draft=is_draft, mapping=mapping)

        board_token = os.environ["PROJECT_BOARD_TOKEN"]

        assignment_succeeded = True
        if decision["known"]:
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

        try:
            verdict_prefix = "known, assign+draft" if decision["known"] else "unknown (absent from .github/pr-triage-teams.yml), no assignee, draft untouched"
            apply_board_write(
                "opened",
                pr_node_id,
                actual_is_draft,
                decision["team"],
                board_token,
                summary_rows,
                verdict_prefix=verdict_prefix,
                insert_verdict_at=3,
            )
        except Exception as error:
            print(f"ERROR: board write failed: {error}")
            summary_rows.append(("Board write", f"FAILED: {error}"))
            failures.append(str(error))
    except Exception as error:
        print(f"ERROR: {error}")
        summary_rows.append(("Failure", str(error)))
        failures.append(str(error))
    finally:
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

    if action != "opened":
        handle_unrecognized_action(action, pull_request)
        return

    repo = event["repository"]["full_name"]
    handle_open_triage(action, pull_request, repo)


if __name__ == "__main__":
    main()
