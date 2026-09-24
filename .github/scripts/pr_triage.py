"""Decides what to do with a pull request on open/reopen, and does the
writes: main(), the event handlers, the apply/orchestration functions that
mix a decision with I/O, and the step summary.

The pure verdicts live in pr_triage_decide (no network, no GitHub API, no
environment reads, no clock); every HTTP call lives in pr_triage_github. This
module wires the two together and is the one the workflow runs directly, so
the script directory is put on sys.path before either sibling import.
"""

import datetime
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import pr_triage_decide
import pr_triage_github


def current_date():
    return datetime.date.today()


def load_teams_mapping(path):
    with open(path, encoding="utf-8") as handle:
        return pr_triage_decide.parse_teams_mapping(handle.read())


def mapping_file_path(pr_triage_file):
    return os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(pr_triage_file))), "pr-triage-teams.yml")


def find_board_item_for_pr(content_id, token):
    """Resolves a content node's (PullRequest or Issue) item on board 15, plus
    -- for a PullRequest only -- the issues it closes by a BODY KEYWORD
    (close/closes/fixes/resolves/... ). Both content kinds share the
    identical projectItems shape, so the fragment is factored into
    ProjectItemFields; closingIssuesReferences exists only on PullRequest, and
    riding both the unfiltered and userLinkedOnly variants on this same query
    costs no extra round trip. keyword_closing_issues does the split -- see
    notes/github-facts.md §8. Returns {"item": item-or-None, "closing_issues":
    [{"id", "number"}, ...]} -- closing_issues is always [] for an Issue node.

    Lives here rather than in pr_triage_github because it needs
    keyword_closing_issues, and pr_triage_github imports nothing of ours."""
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
    result = pr_triage_github.graphql_request(query, {"contentId": content_id}, token)
    node = pr_triage_github.require_node(result, content_id)
    item = None
    for candidate in node["projectItems"]["nodes"]:
        if candidate["project"]["id"] == pr_triage_github.BOARD_PROJECT_ID:
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
    closing_issues = pr_triage_decide.keyword_closing_issues(all_closing_issues, user_linked_issues)
    return {"item": item, "closing_issues": closing_issues}


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
SINGLE_SELECT_FIELD_SPECS = (
    ("status", pr_triage_github.STATUS_FIELD_ID, True, "Status"),
    ("team", pr_triage_github.TEAM_FIELD_ID, False, "Team"),
)
FIELD_LABELS = {"status": "Status", "team": "Team", "sprint": "Sprint"}


def resolve_and_apply_board_fields(pr_node_id, existing_item, fields, current_values, current_iteration, board_token, summary_rows):
    resolved_single_select = {}
    for key, field_id, strip_emoji, _label in SINGLE_SELECT_FIELD_SPECS:
        target_value = fields[key]
        if target_value is not None:
            options = pr_triage_github.fetch_project_field_options(field_id, board_token)
            resolved_single_select[key] = (field_id, pr_triage_decide.find_option_id_by_name(options, target_value, strip_emoji=strip_emoji))

    sprint_iteration_id = current_iteration["id"] if (fields["sprint"] is not None and current_iteration) else None

    if existing_item:
        item_id = existing_item["id"]
        summary_rows.append(("Board item", f"found existing item {item_id}"))
    else:
        item_id = pr_triage_github.add_item_to_project(pr_node_id, board_token)
        summary_rows.append(("Board item", f"no existing item, added {item_id}"))

    for key, label in FIELD_LABELS.items():
        target_value = fields[key]
        if target_value is None:
            summary_rows.append((label, f"left as-is ({current_values[key]!r})"))
        elif key == "sprint":
            pr_triage_github.set_project_field_iteration(item_id, pr_triage_github.SPRINT_FIELD_ID, sprint_iteration_id, board_token)
            summary_rows.append((f"{label} set", target_value))
        else:
            field_id, option_id = resolved_single_select[key]
            pr_triage_github.set_project_field_option(item_id, field_id, option_id, board_token)
            summary_rows.append((f"{label} set", target_value))

    return item_id


def resolve_current_iteration(board_token):
    iterations = pr_triage_github.fetch_project_iterations(pr_triage_github.SPRINT_FIELD_ID, board_token)
    return pr_triage_decide.find_current_iteration(iterations, current_date())


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
    reaches this function; see apply_board_write.

    Order matters, owner ruling: every linked issue's card is written FIRST,
    and the PR's own card is removed LAST. If an issue write fails partway
    (a GraphQL error, a timeout), the PR's card survives -- a transient
    duplicate, which the spec explicitly prefers over the alternative this
    ordering rules out: the PR's card gone and no issue card written, i.e.
    zero cards for the same piece of work."""
    current_iteration = resolve_current_iteration(board_token)
    current_sprint_title = current_iteration["title"] if current_iteration else None

    for issue_ref in closing_issues:
        issue_lookup = find_board_item_for_pr(issue_ref["id"], board_token)
        existing_issue_item = issue_lookup["item"]
        fields = pr_triage_decide.decide_issue_card_update(action, is_draft, existing_issue_item, mapped_team, current_sprint_title)
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

    if pr_triage_decide.decide_remove_pr_card(closing_issues, pr_item):
        was = f"Status {pr_item['status']!r}, Team {pr_item['team']!r}, Sprint {pr_item['sprint']!r}"
        # Recorded BEFORE the mutation runs -- this row is the one place a
        # person can recover the values from, so a timeout after GitHub has
        # already committed the removal must not lose it.
        summary_rows.append(("PR card removed", f"{pr_item['id']} (was: {was})"))
        pr_triage_github.remove_item_from_board(pr_item["id"], board_token)
    else:
        summary_rows.append(("PR card", f"none to add or remove (closes {len(closing_issues)} linked issue(s))"))


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

    if pr_triage_decide.decide_redirect_to_issues(closing_issues):
        if action not in pr_triage_decide.PR_CARD_REMOVAL_ACTIONS:
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
    if pr_triage_decide.is_blank(current_values["sprint"]):
        current_iteration = resolve_current_iteration(board_token)
    current_sprint_title = current_iteration["title"] if current_iteration else None

    fields = pr_triage_decide.decide_board_update(
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

    print(f"event=pull_request action={action} head_branch={head_branch} author={author_login}")

    summary_rows = [
        ("Event/action", f"pull_request / {action}"),
        ("Head branch", head_branch),
        ("Author", author_login),
    ]

    try:
        mapping_path = mapping_file_path(__file__)
        mapping = load_teams_mapping(mapping_path)
        mapped_team = pr_triage_decide.team_for(author_login, mapping) or pr_triage_decide.UNKNOWN_AUTHOR_TEAM

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

        decision = pr_triage_decide.decide(author_login=author_login, is_draft=is_draft, mapping=mapping)

        board_token = os.environ["PROJECT_BOARD_TOKEN"]

        assignment_succeeded = True
        if decision["known"]:
            try:
                github_token = os.environ["GITHUB_TOKEN"]
                assign_result = pr_triage_github.assign_author(repo, pr_number, author_login, github_token)
                pr_triage_decide.check_assignment_succeeded(author_login, assign_result)
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
                draft_result = pr_triage_github.convert_pr_to_draft(pr_node_id, board_token)
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

    if action in pr_triage_decide.BOARD_UPDATE_ACTIONS:
        handle_board_update(action, pull_request)
        return

    if action != "opened":
        handle_unrecognized_action(action, pull_request)
        return

    repo = event["repository"]["full_name"]
    handle_open_triage(action, pull_request, repo)


if __name__ == "__main__":
    main()
