import os
import json
import requests

EDITOR_INDENT_SIZE = 4
EDITOR_INDENT = " " * EDITOR_INDENT_SIZE

def ktString(value: str)-> str:
    """Escape quotes inside strings for safe Kotlin output."""
    if not isinstance(value, str):
        return value
    return value.replace('"', '\\"')


def surahToKotlin(surah: dict) -> str:
    return (
        "Surah("
        f"id = {surah['id']}, "
        f'name = "{ktString(surah["name"])}", '
        f'startPage = {surah["start_page"]}, '
        f'endPage = {surah["end_page"]}, '
        f'makkia = {surah["makkia"]}, '
        f'type = {surah["type"]}'
        ")"
    )


def moshafToKotlin(moshaf: dict) -> str:
    return (
        "Moshaf(\n"
        f"{EDITOR_INDENT * 8}id = {moshaf['id']},\n"
        f'{EDITOR_INDENT * 8}name = "{ktString(moshaf["name"])}",\n'
        f'{EDITOR_INDENT * 8}server = "{ktString(moshaf["server"])}",\n'
        f"{EDITOR_INDENT * 8}surahsCount = {moshaf['surah_total']},\n"
        f"{EDITOR_INDENT * 8}moshafType = {moshaf['moshaf_type']},\n"
        f'{EDITOR_INDENT * 8}surahIdsStr = "{ktString(moshaf["surah_list"])}"\n'
        f"{EDITOR_INDENT * 6})"
    )


def reciterToKotlin(reciter: dict) -> str:
    moshafList = f",\n{EDITOR_INDENT * 6}".join(moshafToKotlin(moshaf) for moshaf in reciter["moshaf"])
    return (
        f"{EDITOR_INDENT * 2}Reciter(\n"
        f"{EDITOR_INDENT * 4}id = {reciter['id']}.asReciterId,\n"
        f'{EDITOR_INDENT * 4}name = "{ktString(reciter["name"])}",\n'
        f'{EDITOR_INDENT * 4}letter = "{ktString(reciter["letter"])}",\n'
        f'{EDITOR_INDENT * 4}date = "{ktString(reciter["date"])}",\n'
        f"{EDITOR_INDENT * 4}moshafList = listOf(\n"
        f"{EDITOR_INDENT * 6}{moshafList}\n"
        f"{EDITOR_INDENT * 4})\n"
        f"{EDITOR_INDENT * 2})"
    )


def getReciters() -> dict:
    url = "https://mp3quran.net/api/v3/reciters?language=ar"
    payload = {}
    headers = {}
    response = requests.request("GET", url, headers=headers, data=payload)
    responseJson = json.loads(response.text)

    return responseJson["reciters"]


def getSurahs() -> dict:
    url = "https://mp3quran.net/api/v3/suwar?language=ar"
    payload = {}
    headers = {}
    response = requests.request("GET", url, headers=headers, data=payload)
    responseJson = json.loads(response.text)

    return responseJson["suwar"]


if __name__ == "__main__":
    projectRootDir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    outputPath = os.path.join(f"{projectRootDir}/mobile/src/main/java/com/hifnawy/alquran/utils/SampleData.kt")

    print("⏳ Fetching data...")

    print("⏳ Fetching reciters...")
    reciters = getReciters()
    print("✔ Reciters data fetched successfully.")

    print("⏳ Fetching surahs...")
    surahs = getSurahs()
    print("✔ Surahs data fetched successfully.")

    print("✔ Data fetched successfully.")

    print("⏳ Converting to Reciters Kotlin...")
    kotlinReciters = ",\n\n".join(reciterToKotlin(reciter) for reciter in reciters)
    print("✔ Reciters Conversion to Kotlin completed.")

    print("⏳ Creating Reciters Kotlin list...")
    kotlinRecitersList = f"val sampleReciters = listOf(\n{kotlinReciters}\n)\n"
    print("✔ Reciters Kotlin list created.")

    print("⏳ Converting to Surahs Kotlin...")
    kotlinSurahs = f",\n{EDITOR_INDENT * 2}".join(surahToKotlin(surah) for surah in surahs)
    print("✔ Surahs Conversion to Kotlin completed.")

    print("⏳ Creating Surahs Kotlin list...")
    kotlinSurahsList = f"val sampleSurahs = listOf(\n{EDITOR_INDENT * 2}{kotlinSurahs}\n)\n"
    print("✔ Surahs Kotlin list created.")

    fileHeaders = [
        '@file:Suppress("SpellCheckingInspection")\n',
        "package com.hifnawy.alquran.utils\n",
        "import com.hifnawy.alquran.shared.model.Moshaf",
        "import com.hifnawy.alquran.shared.model.Reciter",
        "import com.hifnawy.alquran.shared.model.Surah",
        "import com.hifnawy.alquran.shared.model.asReciterId\n"
    ]

    print("⏳ Writing to SampleData.kt...")
    with open(outputPath, "w", encoding="utf-8") as file:
        for item in fileHeaders:
            file.write(item + "\n")

        print("⏳ Writing Reciters Kotlin list to file...")
        file.write(kotlinRecitersList)
        print("✔ Reciters Kotlin list written to file.")

        file.write("\n")

        print("⏳ Writing Surahs Kotlin list to file...")
        file.write(kotlinSurahsList)
        print("✔ Surahs Kotlin list written to file.")
    print("✔ SampleData.kt written successfully.")
