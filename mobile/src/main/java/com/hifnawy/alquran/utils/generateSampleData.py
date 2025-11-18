import json
import requests


def ktString(value):
    """Escape quotes inside strings for safe Kotlin output."""
    if not isinstance(value, str):
        return value
    return value.replace('"', '\\"')


def surahToKotlin(surah):
    return (
        "Surah("
        f"id = {surah['id']}, "
        f'name = "{ktString(surah["name"])}", '
        f'start_page = {surah["start_page"]}, '
        f'end_page = {surah["end_page"]}, '
        f'makkia = {surah["makkia"]}, '
        f'type = {surah["type"]}'
        ")"
    )


def moshafToKotlin(moshaf):
    return (
        "Moshaf(\n"
        f"\t\t\t\tid = {moshaf['id']},\n"
        f'\t\t\t\tname = "{ktString(moshaf["name"])}",\n'
        f'\t\t\t\tserver = "{ktString(moshaf["server"])}",\n'
        f"\t\t\t\tsurah_total = {moshaf['surah_total']},\n"
        f"\t\t\t\tmoshaf_type = {moshaf['moshaf_type']},\n"
        f'\t\t\t\tsurah_list = "{ktString(moshaf["surah_list"])}"\n'
        "\t\t\t)"
    )


def reciterToKotlin(reciter):
    moshafs = ",\n\t\t\t".join(moshafToKotlin(moshaf) for moshaf in reciter["moshaf"])
    return (
        "\tReciter(\n"
        f"\t\tid = {reciter['id']},\n"
        f'\t\tname = "{ktString(reciter["name"])}",\n'
        f'\t\tletter = "{ktString(reciter["letter"])}",\n'
        f'\t\tdate = "{ktString(reciter["date"])}",\n'
        f"\t\tmoshaf = listOf(\n"
        f"\t\t\t{moshafs}\n"
        "\t\t)\n"
        "\t)"
    )


def getReciters():
    url = "https://mp3quran.net/api/v3/reciters?language=ar"
    payload = {}
    headers = {}
    response = requests.request("GET", url, headers=headers, data=payload)
    responseJson = json.loads(response.text)

    return responseJson["reciters"]


def getSurahs():
    url = "https://mp3quran.net/api/v3/suwar?language=ar"
    payload = {}
    headers = {}
    response = requests.request("GET", url, headers=headers, data=payload)
    responseJson = json.loads(response.text)

    return responseJson["suwar"]


if __name__ == "__main__":
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
    kotlinSurahs = ",\n\t".join(surahToKotlin(surah) for surah in surahs)
    print("✔ Surahs Conversion to Kotlin completed.")

    print("⏳ Creating Surahs Kotlin list...")
    kotlinSurahsList = f"val sampleSurahs = listOf(\n\t{kotlinSurahs}\n)\n"
    print("✔ Surahs Kotlin list created.")

    print("⏳ Writing to SampleData.kt...")
    with open("SampleData.kt", "w", encoding="utf-8") as file:
        print("⏳ Writing Reciters Kotlin list to file...")
        file.write(kotlinRecitersList)
        print("✔ Reciters Kotlin list written to file.")
        
        file.write("\n")
        
        print("⏳ Writing Surahs Kotlin list to file...")
        file.write(kotlinSurahsList)
        print("✔ Surahs Kotlin list written to file.")
    print("✔ SampleData.kt written successfully.")
