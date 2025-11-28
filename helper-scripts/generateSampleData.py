"""
This script generates Kotlin Sample Data for the AlQuran Android App.

Sample Data is used for testing and development purposes.
Sample Data is generated from the following sources:
- https://mp3quran.net/api/v3/suwar?language=ar
- https://mp3quran.net/api/v3/reciters?language=ar

Sample Data includes:
- suppressed SpellCheckingInspection annotation
- necessary imports
- sampleReciters kotlin list
- sampleSurahs kotlin list

Usage:
    python generateSampleData.py
"""

import os
import sys
from dataclasses import dataclass, fields, is_dataclass
from typing import TypeVar

import requests
from requests import Response
from requests.adapters import HTTPAdapter
from requests.exceptions import RequestException
from typing_extensions import Annotated, get_args, get_origin
from urllib3.util.retry import Retry

sys.stdout.reconfigure(encoding="utf-8")
sys.stderr.reconfigure(encoding="utf-8")

EDITOR_INDENT_SIZE = 4
"""
The size of the editor's indentation.

This variable is used to configure the indentation used in the generated Kotlin code.
"""

EDITOR_INDENT = " " * EDITOR_INDENT_SIZE
"""
The editor's indentation.

This is the string representation of the indentation used in the generated Kotlin code.
"""

T = TypeVar("T")
"""
Generic type variable.

This type variable is used to indicate that a type is a generic type.
"""


@dataclass
class Surah:
    """
    A class representing a Surah (chapter) of the Quran.

    This class is a dataclass that holds information about a Surah. It has the following attributes:
    Throws a :class:`Moshaf`.

    Attributes:
       id: (int) The unique identifier of the Surah.
       name: (str) The name of the Surah.
       startPage: (int) The starting page number of the Surah in the Quran.
       endPage: (int) The ending page number of the Surah in the Quran.
       makkia: (int) A flag indicating whether the Surah is considered Makkia or not.
       type: (int) The type of the Surah.
    """

    id: int
    name: str
    startPage: Annotated[int, "start_page"]
    endPage: Annotated[int, "end_page"]
    makkia: int
    type: int

    def __toString__(self) -> str:
        """
        Returns a string representation of the Surah object.

        :return: (str) A string representation of the Surah object.
        """
        return (
            "Surah("
            + f"id = {self.id}, "
            + f'name = "{self.name}", '
            + f"startPage = {self.startPage}, "
            + f"endPage = {self.endPage}, "
            + f"makkia = {self.makkia}, "
            + f"type = {self.type}"
            + ")"
        )

    def __str__(self) -> str:
        """
        Returns a string representation of the Surah object.

        :return: (str) A string representation of the Surah object.
        """
        return self.__toString__()

    def __repr__(self) -> str:
        """
        Returns a string representation of the Surah object.

        :return: (str) A string representation of the Surah object.
        """
        return self.__toString__()


@dataclass
class Moshaf:
    """
    A class representing a Moshaf (Quran recitation).

    This class is a dataclass that holds information about a Moshaf, which is a recitation of the Quran. It has the following attributes:

    Attributes:
       id: (int) The unique identifier of the Moshaf.
       name: (str) The name of the Moshaf.
       server: (str) The server URL hosting the Moshaf.
       surahsCount: (int) The total number of Surahs in the Moshaf.
       moshafType: (int) The type of the Moshaf.
       surahIdsStr: (str) The list of Surah IDs in the Moshaf.
    """

    id: int
    name: str
    server: str
    surahsCount: Annotated[int, "surah_total"]
    moshafType: Annotated[int, "moshaf_type"]
    surahIdsStr: Annotated[str, "surah_list"]

    def __toString__(self, withIndent: bool = False) -> str:
        """
        Returns a string representation of the Moshaf object.

        :param withIndent: (bool) Whether to include indentation in the string representation. Defaults to False.
        :return: (str) A string representation of the Moshaf object.
        """
        return (
            "Moshaf(\n"
            + f"{EDITOR_INDENT * 8 if withIndent else ''}id = {self.id},\n"
            + f'{EDITOR_INDENT * 8 if withIndent else ""}name = "{self.name}",\n'
            + f'{EDITOR_INDENT * 8 if withIndent else ""}server = "{self.server}",\n'
            + f"{EDITOR_INDENT * 8 if withIndent else ''}surahsCount = {self.surahsCount},\n"
            + f"{EDITOR_INDENT * 8 if withIndent else ''}moshafType = {self.moshafType},\n"
            + f'{EDITOR_INDENT * 8 if withIndent else ""}surahIdsStr = "{self.surahIdsStr}"\n'
            + f"{EDITOR_INDENT * 6 if withIndent else ''})"
        )

    def __str__(self) -> str:
        """
        Returns a string representation of the Moshaf object.

        :return: (str) A string representation of the Moshaf object.
        """
        return self.__toString__(withIndent=True)

    def __repr__(self) -> str:
        """
        Returns a string representation of the Moshaf object.

        :return: (str) A string representation of the Moshaf object.
        """
        return self.__toString__()


@dataclass
class Reciter:
    """
    A class representing a Reciter (Quran reciter).

    This class is a dataclass that holds information about a Reciter, which is a person who recites the Quran. It has the following attributes:

    Attributes:
       id: (int) The unique identifier of the Reciter.
       name: (str) The name of the Reciter.
       letter: (str) The letter representing the Reciter.
       date: (str) The date of the Reciter's recitation.
       moshafList: (list[Moshaf]) The list of Moshafs (recitations) by the Reciter.
    """

    id: int
    name: str
    letter: str
    date: str
    moshafList: Annotated[list[Moshaf], "moshaf"]

    def __toString__(self, withIndent: bool = False) -> str:
        """
        Returns a string representation of the Reciter object.

        :param withIndent: (bool) Whether to include indentation in the string representation. Defaults to False.
        :return: (str) A string representation of the Reciter object.
        """
        moshafList = f",\n{EDITOR_INDENT * 6 if withIndent else ''}".join(
            f"{str(moshaf) if withIndent else repr(moshaf)}"
            for moshaf in self.moshafList
        )

        return (
            f"{EDITOR_INDENT * 2 if withIndent else ''}Reciter(\n"
            + f"{EDITOR_INDENT * 4 if withIndent else ''}id = {self.id}.asReciterId,\n"
            + f'{EDITOR_INDENT * 4 if withIndent else ""}name = "{self.name}",\n'
            + f'{EDITOR_INDENT * 4 if withIndent else ""}letter = "{self.letter}",\n'
            + f'{EDITOR_INDENT * 4 if withIndent else ""}date = "{self.date}",\n'
            + f"{EDITOR_INDENT * 4 if withIndent else ''}moshafList = listOf(\n"
            + f"{EDITOR_INDENT * 6 if withIndent else ''}{moshafList}\n"
            + f"{EDITOR_INDENT * 4 if withIndent else ''})\n"
            f"{EDITOR_INDENT * 2 if withIndent else ''})"
        )

    def __str__(self) -> str:
        """
        Returns a string representation of the Reciter object.

        :return: (str) A string representation of the Reciter object.
        """
        return self.__toString__(withIndent=True)

    def __repr__(self) -> str:
        """
        Returns a string representation of the Reciter object.

        :return: (str) A string representation of the Reciter object.
        """
        return self.__toString__()


def toDataClass(data: dict, cls: type[T]) -> T:
    """
    Converts a dictionary to a dataclass object.

    :param data: (dict) The dictionary to convert.
    :param cls: (type[T]) The dataclass type to convert to.
    :return: (T) The converted dataclass object.
    """
    initKwargs = {}

    for field in fields(cls):
        fieldType = field.type
        fieldName = field.name

        # If using Annotated, extract the Annotated type
        if get_origin(fieldType) is Annotated:
            realType, annotatedType = get_args(fieldType)
            fieldType = realType
            fieldName = annotatedType

        if fieldName not in data:
            continue

        fieldValue = data[fieldName]

        fieldName = field.name

        # Nested dataclass
        if is_dataclass(fieldType):
            initKwargs[fieldName] = toDataClass(fieldValue, fieldType)

        # List of dataclasses
        elif (
            hasattr(fieldType, "__origin__")
            and get_origin(fieldType) is list
            and is_dataclass(get_args(fieldType)[0])
        ):
            inner = get_args(fieldType)[0]
            initKwargs[fieldName] = [toDataClass(item, inner) for item in fieldValue]

        else:
            initKwargs[fieldName] = fieldValue

    return cls(**initKwargs)


def sendRequest(url: str) -> Response | None:
    """
    Sends a GET request to the specified URL.

    With a retry strategy defined as follows:
        1. request timeout is 3s
        2. retry count is 3 times spaced by an
           exponential delay as 250ms, 500ms, 1s

    so the max delay of this function if there's
    a connection error is:
    3s + 250ms + 3s + 500ms + 3s + 1s = 10s 750ms

    :param url: (str) The URL to send the request to.
    :return: (Response) The response from the server.
    """

    """
    delay between retries is calculated as:
    backoff factor * (2 ^ number of previous retries)
    so 250ms, 500ms, 1s
    """
    retryStrategy = Retry(total=3, backoff_factor=0.125)
    adapter = HTTPAdapter(max_retries=retryStrategy)
    session = requests.session()
    session.mount(prefix="http://", adapter=adapter)
    session.mount(prefix="https://", adapter=adapter)

    try:
        response = session.get(url=url, timeout=3) # timeout in seconds
    except RequestException as ex:
        print(f"ERROR: {ex}", file=sys.stderr)
        return None

    return response


def getReciters() -> list[Reciter] | None:
    """
    Fetches reciters data from mp3quran.net API.

    :return: (list[Reciter]) A list of Reciter objects.
    """
    response = sendRequest(url="https://mp3quran.net/api/v3/reciters?language=ar")

    if response is None:
        return None
    else:
        data = response.json()["reciters"]
        return [toDataClass(item, Reciter) for item in data]


def getSurahs() -> list[Surah] | None:
    """
    Fetches surahs data from mp3quran.net API.

    :return: (list[Surah]) A list of Surah objects.
    """
    response = sendRequest(url="https://mp3quran.net/api/v3/suwar?language=ar")

    if response is None:
        return None
    else:
        data = response.json()["suwar"]
        return [toDataClass(item, Surah) for item in data]


def main() -> None:
    """
    Fetches reciters and surahs data from mp3quran.net API and generates Kotlin SampleData.kt file.

    This function fetches reciters and surahs data from mp3quran.net API and generates Kotlin SampleData.kt file
    containing the fetched data. The generated file is saved in the `mobile/src/main/java/com/hifnawy/alquran/utils/`
    directory with the name `SampleData.kt`.

    This function does not take any parameters and does not return anything.
    """
    projectRootDir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    outputPath = os.path.join(
        f"{projectRootDir}/mobile/src/main/java/com/hifnawy/alquran/utils/SampleData.kt"
    )

    print("[*] Fetching data...")

    print("[*] Fetching reciters...")
    reciters = getReciters()

    if reciters:
        print("[✓] Reciters data fetched successfully.")
    else:
        print("[✗] Reciters data fetching failed.", file=sys.stderr)
        exit(1)

    print("[*] Fetching surahs...")
    surahs = getSurahs()
    if surahs:
        print("[✓] Surahs data fetched successfully.")
    else:
        print("[✗] Surahs data fetching failed.", file=sys.stderr)
        exit(2)

    print("[✓] Data fetched successfully.")

    print("[*] Converting to Reciters Kotlin...")
    kotlinReciters = ",\n\n".join(str(reciter) for reciter in reciters)
    print("[✓] Reciters Conversion to Kotlin completed.")

    print("[*] Creating Reciters Kotlin list...")
    kotlinRecitersList = f"val sampleReciters = listOf(\n{kotlinReciters}\n)\n"
    print("[✓] Reciters Kotlin list created.")

    print("[*] Converting to Surahs Kotlin...")
    kotlinSurahs = f",\n{EDITOR_INDENT * 2}".join(str(surah) for surah in surahs)
    print("[✓] Surahs Conversion to Kotlin completed.")

    print("[*] Creating Surahs Kotlin list...")
    kotlinSurahsList = (
        f"val sampleSurahs = listOf(\n{EDITOR_INDENT * 2}{kotlinSurahs}\n)\n"
    )
    print("[✓] Surahs Kotlin list created.")

    fileHeaders = [
        '@file:Suppress("SpellCheckingInspection")\n',
        "package com.hifnawy.alquran.utils\n",
        "import com.hifnawy.alquran.shared.model.Moshaf",
        "import com.hifnawy.alquran.shared.model.Reciter",
        "import com.hifnawy.alquran.shared.model.Surah",
        "import com.hifnawy.alquran.shared.model.asReciterId\n",
    ]

    print(f"[*] Generating {outputPath}...", end="\n\n")
    with open(outputPath, "w", encoding="utf-8") as file:
        for header in fileHeaders:
            file.write(f"{header}\n")
            print(header)

        for line in kotlinRecitersList.splitlines():
            file.write(f"{line}\n")
            print(line)

        file.write("\n")
        print()

        for line in kotlinSurahsList.splitlines():
            file.write(f"{line}\n")
            print(line)
        print()
    print(f"[✓] {outputPath} generated successfully.")


if __name__ == "__main__":
   main()
