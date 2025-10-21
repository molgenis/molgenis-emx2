"""
Main file executing tests.
To be deleted upon finishing tests.
"""
import asyncio

from tests.test_client import test_upload_file


def main():
    asyncio.run(test_upload_file())


if __name__ == '__main__':
    main()
