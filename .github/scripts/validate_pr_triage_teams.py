"""Validation gates for .github/pr-triage-teams.yml, run as a workflow step.

Pure checks (find_bot_or_machine_logins, find_unknown_team_values) take an
already-parsed mapping and, for the team check, an already-resolved set of
live board option names, so they are testable without a network. main() does
the file read and the live GraphQL resolution, then calls the pure checks.
"""

import os
import sys
import urllib.error

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import pr_triage

MACHINE_ACCOUNTS = {"JV-CI-CD", "molgenis-jenkins"}
BANNED_WORKFLOW_TERMS = (
    "author_association",
    "user.type",
    "/collaborators",
    '["user"]["type"]',
    "['user']['type']",
)
FILES_HOLDING_BANNED_TERMS_AS_LITERAL_FIXTURES = {os.path.basename(__file__), "test_pr_triage.py"}


def is_bot_login(login):
    return login.endswith("[bot]")


def find_bot_or_machine_logins(mapping):
    return [login for login in mapping if is_bot_login(login) or login in MACHINE_ACCOUNTS]


def find_unknown_team_values(mapping, valid_team_names):
    return [(login, team) for login, team in mapping.items() if team not in valid_team_names]


def find_blank_team_values(mapping):
    return [login for login, team in mapping.items() if not team.strip()]


def find_duplicate_logins(text):
    logins = [login for login, _ in pr_triage.parse_teams_entries(text)]
    duplicates = []
    for login in logins:
        if logins.count(login) > 1 and login not in duplicates:
            duplicates.append(login)
    return duplicates


def find_missing_status_option(status_options, status_name):
    for option in status_options:
        if pr_triage.strip_emoji_prefix(option["name"]) == status_name:
            return None
    available = ", ".join(sorted(option["name"] for option in status_options))
    return f"'{status_name}' is not a live Status option. Live Status options are: {available}"


def find_missing_team_option(team_options, team_name):
    valid_team_names = {option["name"] for option in team_options}
    if team_name in valid_team_names:
        return None
    available = ", ".join(sorted(valid_team_names))
    return f"'{team_name}' is not a live Team option. Live Team options are: {available}"


def is_credential_error(http_error):
    return getattr(http_error, "code", None) in (401, 403)


def find_banned_terms_in_source(source_text, banned_terms=BANNED_WORKFLOW_TERMS):
    return [term for term in banned_terms if term in source_text]


def find_banned_terms_across_triage_source(repo_root):
    findings = []

    workflow_path = os.path.join(repo_root, ".github", "workflows", "pr-triage.yml")
    with open(workflow_path, encoding="utf-8") as handle:
        workflow_terms = find_banned_terms_in_source(handle.read())
    if workflow_terms:
        findings.append(("pr-triage.yml", workflow_terms))

    scripts_dir = os.path.join(repo_root, ".github", "scripts")
    for filename in sorted(os.listdir(scripts_dir)):
        if not filename.endswith(".py") or filename in FILES_HOLDING_BANNED_TERMS_AS_LITERAL_FIXTURES:
            continue
        with open(os.path.join(scripts_dir, filename), encoding="utf-8") as handle:
            terms = find_banned_terms_in_source(handle.read())
        if terms:
            findings.append((filename, terms))

    return findings


def main(repo_root=None):
    if repo_root is None:
        repo_root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    mapping_path = os.path.join(repo_root, ".github", "pr-triage-teams.yml")
    with open(mapping_path, encoding="utf-8") as handle:
        mapping_text = handle.read()
    mapping = pr_triage.parse_teams_mapping(mapping_text)

    errors = []

    duplicate_logins = find_duplicate_logins(mapping_text)
    if duplicate_logins:
        errors.append(f"login(s) listed more than once in pr-triage-teams.yml: {duplicate_logins}")

    bad_logins = find_bot_or_machine_logins(mapping)
    if bad_logins:
        errors.append(f"bot or machine account login(s) present in pr-triage-teams.yml: {bad_logins}")

    blank_team_logins = find_blank_team_values(mapping)
    if blank_team_logins:
        errors.append(f"login(s) with a blank Team value in pr-triage-teams.yml: {blank_team_logins}")

    banned_terms_found = find_banned_terms_across_triage_source(repo_root)
    if banned_terms_found:
        errors.append(
            f"reader-dependent decision term(s) found, never allowed in this workflow's logic: {banned_terms_found}"
        )

    # Live board-option resolution (Team values, and Working/Review/Dev existing)
    # needs PROJECT_BOARD_TOKEN, and this validator must not hold that secret:
    # it runs against the PR's own edit to this very file, before review. So
    # this step is offline-only here; the same check still happens, just later
    # and loud: find_option_id_by_name raises with the full live option list
    # when a value doesn't resolve, at write time in the triage job. Do not
    # reinstate a token here to bring it back to validate time.
    board_token = os.environ.get("PROJECT_BOARD_TOKEN")
    if board_token:
        try:
            team_options = pr_triage.fetch_project_field_options(pr_triage.TEAM_FIELD_ID, board_token)
            status_options = pr_triage.fetch_project_field_options(pr_triage.STATUS_FIELD_ID, board_token)
        except urllib.error.HTTPError as error:
            if is_credential_error(error):
                print(f"ERROR: PROJECT_BOARD_TOKEN was rejected by GitHub (HTTP {error.code}). "
                      "This is a credential problem: rotate the PROJECT_BOARD_TOKEN secret. "
                      "It is not a problem with pr-triage-teams.yml.")
            else:
                print(f"ERROR: unexpected HTTP {error.code} resolving live board options: {error}")
            sys.exit(1)

        valid_team_names = {option["name"] for option in team_options}

        unknown_team_values = find_unknown_team_values(mapping, valid_team_names)
        if unknown_team_values:
            available = ", ".join(sorted(valid_team_names))
            errors.append(
                f"unknown Team value(s) in pr-triage-teams.yml: {unknown_team_values}. "
                f"Live Team options are: {available}"
            )

        missing_working_status = find_missing_status_option(status_options, pr_triage.STATUS_WORKING)
        if missing_working_status:
            errors.append(missing_working_status)

        missing_review_status = find_missing_status_option(status_options, pr_triage.STATUS_REVIEW)
        if missing_review_status:
            errors.append(missing_review_status)

        missing_dev_team = find_missing_team_option(team_options, pr_triage.UNKNOWN_AUTHOR_TEAM)
        if missing_dev_team:
            errors.append(missing_dev_team)
    else:
        print(
            "PROJECT_BOARD_TOKEN not present: skipping live board-option checks "
            "(Team values, and Working/Review/Dev existing on the board). These "
            "run at write time in the triage job instead."
        )

    if errors:
        for error in errors:
            print(f"ERROR: {error}")
        sys.exit(1)

    if board_token:
        print(f"pr-triage-teams.yml is valid: {len(mapping)} authors, all Team values resolve to live board options")
    else:
        print(f"pr-triage-teams.yml passes all offline checks: {len(mapping)} authors")


if __name__ == "__main__":
    main()
