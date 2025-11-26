# AlQuran Helper Scripts

This directory contains various helper scripts used in the development and maintenance of the AlQuran
application. These scripts automate tasks related to data generation, asset management, and other
development workflows.

- ## Prerequisites
    - [uv](https://docs.astral.sh/uv/) package manager (recommended)
    - [Python](https://www.python.org) 3.13 or higher
    - Git (for version control)

- ## Setup
    - ### Clone the repository:
      ```bash
       git clone https://github.com/abdalmoniem/AlQuran.git
       cd AlQuran/helper-scripts
       ```

    - ### Install Python (If not already installed):
      ```bash
       uv python install
       ```

    - ### Create and activate a virtual environment (Recommended):
       ```bash
       uv venv .venv
       .\venv\Scripts\activate  # On Windows
       # or
       source .venv/bin/activate  # On Unix/macOS
       ```

    - ### Install dependencies:
      ```bash
       uv sync
       ```

    - ## Available Scripts
        - ### [generateSampleData.py](generateSampleData.py)

          This script generates sample data for the AlQuran application.

            - ##### Usage:
                ```bash
                uv run generateSurahDrawables.py [--headless]
                ```

        - ### [generateSurahDrawables.py](generateSurahDrawables.py)

          This script generates drawable resources for surahs in the AlQuran application.

            - ##### Options:
                - `--headless`: Generate Surah images Headless (without canvas display)

            - ##### Usage:
                ```bash
                uv run generateSurahDrawables.py

                ```

- ## Development
    - ### Code Style
        - This project uses [Ruff](https://beta.ruff.rs/) for code formatting and linting.
          To check your code:
             ```bash
             uv run ruff check .
             ```

        - To automatically fix issues:
             ```bash
             uv run ruff check --fix .
             ```
