# __main__.py

import argparse
import sys

from .manager import Manager

actions = ['add', 'remove', 'rename']


def main():
    """Perform an action using the Molgenis Ontology Manager"""

    parser = argparse.ArgumentParser()
    parser.add_argument("action")
    parser.add_argument("table")

    args = parser.parse_args()

    # Read the action that is to be performed from the command line
    if len(sys.argv) > 1:
        action = args.action
        assert action in actions, f"Action {action} is not supported. Select one from {','.join(actions)}."

        ontoman = Manager()
        ontoman.perform(action=action, table=args.table)


if __name__ == '__main__':
    main()
