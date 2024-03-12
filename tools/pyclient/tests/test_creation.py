import os

import pytest
from dotenv import load_dotenv

from tools.pyclient.src.molgenis_emx2_pyclient import Client
from tools.pyclient.src.molgenis_emx2_pyclient.exceptions import SigninError


class TestClientCreation:
    """Class to test the initialization of a client object."""
    url = "https://emx2.dev.molgenis.org"

    def test_init(self):
        """Tests whether the url supplied in instantiation of the client equals the url of the object."""
        with Client(self.url) as client:
            assert client.url == self.url, f"The given url does not match the current url of the client: \n" \
                                           f"{self.url} vs. {client.url}"

    def test_incorrect_pw(self):
        """Tests how the client responds to incorrect sign-in credentials."""
        with Client(self.url) as client:
            with pytest.raises(SigninError):
                client.signin('username', 'password')

    def test_signin(self):
        """Tests whether signing in to the client works."""
        load_dotenv()
        username = os.environ.get('MG_USERNAME')
        password = os.environ.get('MG_PASSWORD')

        assert username is not None, f"No value for MG_USERNAME found, check environment."
        assert password is not None, f"No value for MG_PASSWORD found, check environment."

        with Client(self.url) as client:
            pre_schemas = client.schema_names
            client.signin(username, password)

            post_schemas = client.schema_names

            assert client.signin_status == 'success', f"Sign in succeeded, but sign in status is not updated."
            assert len(post_schemas) > len(pre_schemas), "The list of schemas should be longer after signing in."
