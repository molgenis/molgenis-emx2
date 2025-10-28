"""
Main file executing tests.
To be deleted upon finishing tests.
"""
import asyncio

from tests.test_client import test_delete_records


def main():
    asyncio.run(test_delete_records())


if __name__ == '__main__':
    main()
