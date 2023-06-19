# __main__.py

import argparse
import logging
import sys
from getpass import getpass

from ontomanager import OntologyManager

actions = ['add', 'delete', 'update']


def main():
    """Perform an action using the Molgenis Ontology Manager"""

    parser = argparse.ArgumentParser()
    parser.add_argument("action")
    parser.add_argument("table")
    parser.add_argument("-n", "--name", type=str,
                        help="Fill in the name of the term. Mandatory for add and delete actions.")
    parser.add_argument("-o", "--order", type=int,
                        help="Fill in the order of this term, optional.")
    parser.add_argument("-p", "--parent", type=str,
                        help="Fill in the parent of this term, optional.")
    parser.add_argument("-l", "--label", type=str,
                        help="Fill in the label for this term, optional.")
    parser.add_argument("-d", "--definition", type=str,
                        help="Fill in the definition for this term, optional.")
    parser.add_argument("-u", "--URI", type=str,
                        help="Fill in the ontology term URI for this term, optional.")

    args = parser.parse_args()

    # Read the action that is to be performed from the command line
    if len(sys.argv) > 1:
        action = args.action
        assert action in actions, f"Action {action} is not supported. Select one from {','.join(actions)}."

        url = input("Server url: ")
        username = input("Username: ")
        password = getpass()

        kwargs = {'name': args.name,
                  'label': args.label,
                  'parent': args.parent,
                  'ontologyTermURI': args.URI,
                  'definition': args.definition,
                  'order': args.order}

        ontoman = OntologyManager(url, username, password)
        ontoman.perform(action=action, table=args.table, **kwargs)


if __name__ == '__main__':
    logging.basicConfig(level='INFO')
    logging.getLogger("requests").setLevel(logging.WARNING)
    logging.getLogger("urllib3").setLevel(logging.WARNING)
    main()
