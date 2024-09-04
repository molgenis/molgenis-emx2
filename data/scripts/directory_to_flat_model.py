"""
Script to convert the BBMRI-ERIC directory data to the flat model
"""

import argparse
import molgenis_emx2_pyclient

def main():
    """Main function doing the conversion"""
    # Read password from command line
    parser = argparse.ArgumentParser(description="Command line arguments")
    parser.add_argument("-url", type = str, required = True, dest="server", help="URL of server to access")
    parser.add_argument("-pw", type=str, dest="password", required=True, help="Password for server access")
    args = parser.parse_args()
    # Connect to server
    with molgenis_emx2_pyclient.Client(args.server) as client:
        client.signin('admin', args.password)
        # Test stuff
        print(client.schema_names)

if __name__ == "__main__":
    main()
