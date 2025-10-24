"""
Main file executing tests.
To be deleted upon finishing tests.
"""
import asyncio

from tests.test_client import test_truncate


def main():
    asyncio.run(test_truncate())


if __name__ == '__main__':
    main()
