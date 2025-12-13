"""
This script generates Surah images with optional canvas display.

Usage:
    python generateSurahDrawables.py [--headless]

Options:
    --headless: Generate Surah images Headless (without canvas display)
"""

import argparse
import os
import sys
import threading
from pathlib import Path
from time import sleep

import py5

sys.stdout.reconfigure(encoding="utf-8")
sys.stderr.reconfigure(encoding="utf-8")

# --- Command-line argument parsing ---

# Set up argparse to handle command-line options.
parser = argparse.ArgumentParser(
    description="Generate Surah images with optional canvas display."
)

# Define the --headless flag. If present, it sets isHeadless to True for batch processing without a window.
parser.add_argument(
    "--headless",
    action="store_true",
    help="Generate Surah images Headless (without canvas display)",
)

args = parser.parse_args()
isHeadless = args.headless

# print(f"{isHeadless = }")

# --- Global Configuration ---

# Project directory path calculation. Assumes the script is two directories deep from the project root.
projectRootDir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# Path to the Arabic font file required for rendering Surah names.
fontPath = os.path.join(
    f"{projectRootDir}/shared/src/main/res/font/decotype_thuluth_2.ttf"
)

# Path to the drawables directory where Surah images will be saved.
drawablesPath = os.path.join(f"{projectRootDir}/shared/src/main/res/drawable")

# --- State variables for the sketch ---

# Angle for the loading indicator rotation.
angle = 0

# Flag to indicate if Surah data is still loading (used in interactive mode).
loading = True

# Index of the current Surah being displayed/generated.
surahIndex = 0

# noinspection SpellCheckingInspection
# Hardcoded list of Surah names in Arabic. Index 0 is a placeholder.
surahs = [
    "اْسْمُ اٌلسُورَةِ",  # Surah Name (placeholder)
    "ٱلْفَاتِحَةِ",
    "ٱلْبَقَرَةِ",
    "آلِ عِمْرَانَ",
    "ٱلنِّسَاءِ",
    "ٱلْمَائِدَةِ",
    "ٱلْأَنْعَامِ",
    "ٱلْأَعْرَافِ",
    "ٱلْأَنْفَالِ",
    "ٱلتَّوْبَةِ",
    "يُونُسَ",
    "هُودَ",
    "يُوسُفَ",
    "ٱلرَّعْدِ",
    "إِبْرَاهِيمَ",
    "ٱلْحِجْرِ",
    "ٱلنَّحْلِ",
    "ٱلْإِسْرَاءِ",
    "ٱلْكَهْفِ",
    "مَرْيَمَ",
    "طَهَ",
    "ٱلْأَنْبِيَاءِ",
    "ٱلْحَجِّ",
    "ٱلْمُؤْمِنُونَ",
    "ٱلنُّورِ",
    "ٱلْفُرْقَانِ",
    "ٱلشُّعَرَاءِ",
    "ٱلنَّمْلِ",
    "ٱلْقَصَصِ",
    "ٱلْعَنْكَبُوتِ",
    "ٱلرُّومِ",
    "لُقْمَانَ",
    "ٱلسَّجْدَةِ",
    "ٱلْأَحْزَابِ",
    "سَبَأٍ",
    "فَاطِرٍ",
    "يَسۤ",
    "ٱلصَّافَّاتِ",
    "صۤ",
    "ٱلزُّمَرِ",
    "غَافِرٍ",
    "فُصِّلَتْ",
    "ٱلشُّورَىٰ",
    "ٱلزُّخْرُفِ",
    "ٱلدُّخَانِ",
    "ٱلْجَاثِيَةِ",
    "ٱلْأَحْقَافِ",
    "مُحَمَّدٍ",
    "ٱلْفَتْحِ",
    "ٱلْحُجُرَاتِ",
    "قۤ",
    "ٱلذَّارِيَاتِ",
    "ٱلطُّورِ",
    "ٱلنَّجْمِ",
    "ٱلْقَمَرِ",
    "ٱلرَّحْمَٰنِ",
    "ٱلْوَاقِعَةِ",
    "ٱلْحَدِيدِ",
    "ٱلْمُجَادِلَةِ",
    "ٱلْحَشْرِ",
    "ٱلْمُمْتَحَنَةِ",
    "ٱلصَّفِّ",
    "ٱلْجُمُعَةِ",
    "ٱلْمُنَافِقُونَ",
    "ٱلتَّغَابُنِ",
    "ٱلطَّلَاقِ",
    "ٱلْتَّحْرِيمِ",
    "ٱلْمَلِكِ",
    "ٱلْقَلَمِ",
    "ٱلْحَاقَّةِ",
    "ٱلْمَعَارِجِ",
    "نُوحٍ",
    "ٱلْجِنِّ",
    "ٱلْمُزَّمِّلِ",
    "ٱلْمُدَّثِّرِ",
    "ٱلْقِيَامَةِ",
    "ٱلْإِنسَانِ",
    "ٱلْمُرْسَلَاتِ",
    "ٱلنَّبَأِ",
    "ٱلنَّازِعَاتِ",
    "عَبَسَ",
    "ٱلتَّكْوِيرِ",
    "ٱلْإِنفِطَارِ",
    "ٱلْمُطَفِّفِينَ",
    "ٱلْاِنشِقَاقِ",
    "ٱلْبُرُوجِ",
    "ٱلطَّارِقِ",
    "ٱلْأَعْلَى",
    "ٱلْغَاشِيَةِ",
    "ٱلْفَجْرِ",
    "ٱلْبَلَدِ",
    "ٱلشَّمْسِ",
    "ٱلَّيْلِ",
    "ٱلضُّحَى",
    "ٱلشَّرْحِ",
    "ٱلتِّينِ",
    "ٱلْعَلَقِ",
    "ٱلْقَدْرِ",
    "ٱلْبَيْنَةِ",
    "ٱلزَّلْزَلَةِ",
    "ٱلْعَادِيَاتِ",
    "ٱلْقَارِعَةِ",
    "ٱلتَّكَاثُرِ",
    "ٱلْعَصْرِ",
    "ٱلْهُمْزَةِ",
    "ٱلْفِيلِ",
    "قُرَيْشٍ",
    "ٱلْمَاعُونِ",
    "ٱلْكَوْثَرِ",
    "ٱلْكَافِرُونَ",
    "ٱلنَّصْرِ",
    "ٱلْمَسَدِ",
    "ٱلْإِخْلَاصِ",
    "ٱلْفَلَقِ",
    "ٱلنَّاسِ",
]

# ---Timing constants for the animation cycle (in milliseconds) ---

# Text on screen time
onScreenDuration = 500

# Fade in duration
fadeInDuration = 500

# Fade out duration
fadeOutDuration = 500

# Total duration of one Surah display cycle (fade-in + full display + fade-out)
cycleDuration = onScreenDuration + fadeInDuration + fadeOutDuration

# Timestamp of the start of the current animation cycle.
previousTimestamp = 0

# --- Text rendering constants ---

# Text Height, since py5.text_ascent() + py5.text_descent() doesn't
# work for arabic fonts, I found the height for this specific font
# size manually by trial and error
fontHeight = 50

# Text font size
fontSize = 150

# Surah Al-Kafiron is too wide to fit in a 512x512 canvas with fontSize 150
fontSizeSpecial = 135

# Text is displayed in two lines, this is the spacing between the two lines
lineSpacing = 30

# Vertical offset to separate the two lines of text (e.g., "سُورَةُ" and the name).
textYOffset = (fontSize / 2) + lineSpacing

# Font used to display Surah name
surahFont: py5.Py5Font | None = None  # Py5 font object.

# Surah Al-Kafiron is too wide to fit in a 512x512 canvas with fontSize 150
surahFontSpecial: py5.Py5Font | None = None  # Py5 font object.

# --- Canvas and off-screen rendering ---

# Window width
sketchWidth = 512

# Window height
sketchHeight = 512

# Py5Graphics object used for off-screen rendering and image saving (exporter).
exporter: py5.Py5Graphics | None = None

# Type alias for clarity: a renderer can be the main py5 sketch or the Py5Graphics exporter.
RendererType = py5.Py5Graphics | type(py5)
"""Renderer Type, either `py5.Py5Graphics` or `py5`"""


def getSurahs() -> None:
    """
    Loads Surah data from an external API (asynchronously in interactive mode).

    The hardcoded list is replaced with data from mp3quran.net.
    Updates global 'loading' flag and resets 'previousTimestamp'.
    """
    global loading, surahs, previousTimestamp

    # Replace hardcoded list with API data
    # surahs = py5.load_json("https://mp3quran.net/api/v3/suwar?language=ar")
    sleep(3)  # Simulated delay for debugging or demonstrating loading
    loading = False

    previousTimestamp = py5.millis()


def saveFrame(surahName: str, filename: str, blur: bool = False) -> None:
    """
    Renders the current Surah content at full opacity (alpha 255) to the off-screen
    exporter and saves it as a PNG file in the 'drawables' directory.

    :param surahName - the name of the Surah
    :param filename: str
    :param blur: bool - blur the whole content
    """
    global exporter

    exporter.begin_draw()

    # Render content at full opacity (255) to the off-screen buffer
    drawSurahContent(surahName, exporter, 255, blur)

    exporter.end_draw()

    # Generate filename: surah_001.png, surah_002.png, or surah_name.png for index 0.
    exporter.save(filename)

    print(f"[✓] generated: {Path(filename).resolve()}")


def loadingIndicator() -> None:
    """
    Draws a rotating, fading bar indicator to show the sketch is busy loading data.
    Uses transformation matrices (push_matrix/pop_matrix) for rotation.
    """
    global angle, sketchWidth

    setBackgroundColor(py5)

    py5.push_matrix()
    # Move the origin to the center of the canvas
    py5.translate(py5.width / 2, py5.height / 2)
    py5.rotate(angle)

    barCount = 80
    barWidth = 20
    barHeight = 50
    barCornerRadius = 50
    # Radius calculated with padding
    radius = (sketchWidth / 4) - barHeight - 10

    for index in range(barCount):
        # Calculate the angular position of the bar
        theta = index * py5.TWO_PI / barCount

        py5.push_matrix()
        py5.rotate(theta)
        # Calculate alpha for fading effect (fades out from index 0)
        alpha = int(255 - index * (255 / barCount))
        alpha = max(0, alpha)

        # Draw the bar with calculated alpha
        py5.stroke(255, alpha)
        py5.fill(255, alpha)
        py5.rect(radius, -barWidth / 2, barHeight, barWidth, barCornerRadius)

        py5.pop_matrix()

    py5.pop_matrix()

    # Decrement angle for continuous rotation (0.5 degrees per frame at 60 fps)
    angle -= (py5.TWO_PI / 60) * 0.5


# --- Visual Functions ---
def setBackgroundColor(renderer: RendererType, alpha: int = 255) -> None:
    """
    Sets the background color with alternating themes based on the Surah index.
    The 'alpha' parameter controls the overall opacity of the theme color.

    :param renderer: RendererType
    :param alpha: int
    """
    global sketchWidth, sketchHeight

    red = py5.color(221, 95, 86)
    darkTeal = py5.color(51, 110, 106)

    # Set a mixed color as the base background
    mix = py5.lerp_color(red, darkTeal, 0.5)

    renderer.background(mix)

    # Choose theme color based on odd/even Surah index
    if surahIndex % 2 != 0:
        renderer.fill(red, alpha)
    else:
        renderer.fill(darkTeal, alpha)

    # Draw the solid theme color rectangle over the background
    renderer.stroke(0, 0)
    renderer.stroke_weight(0)
    renderer.rect(0, 0, sketchWidth, sketchHeight)


def drawSurahContent(
    surahName: str, renderer: RendererType, alpha: int, blur: bool = False
) -> None:
    """
    Renders the two-line Surah name text centered on the canvas.
    The text opacity is controlled by the 'alpha' parameter.

    :param surahName: str - the name of the Surah
    :param renderer: RendererType - the renderer type
    :param alpha: int the alpha of the background and the text
    :param blur: bool - blur the whole content
    """
    global surahFont, surahFontSpecial, textYOffset, fontHeight

    setBackgroundColor(renderer, alpha)

    renderer.text_align(py5.CENTER, py5.CENTER)

    centerX = renderer.width / 2
    # Offset Y position to center the two-line text block
    centerY = renderer.height / 2 - fontHeight

    # Set text fill color (white) with given opacity
    renderer.fill(255, alpha)

    # Special handling for the placeholder Surah (index 0)
    if surahIndex == 0:
        words = surahName.split(" ")
        renderer.text_font(surahFont)
        renderer.text(words[0], centerX, centerY - textYOffset)
        renderer.text(words[1], centerX, centerY + textYOffset)
    # Handling for actual Surahs (index > 0)
    else:
        if surahIndex == 109:
            renderer.text_font(surahFontSpecial)
            renderer.text("سُورَةُ", centerX, centerY - textYOffset)
            renderer.text(surahName, centerX + 10, centerY + textYOffset)
        else:
            renderer.text_font(surahFont)
            renderer.text("سُورَةُ", centerX, centerY - textYOffset)
            renderer.text(surahName, centerX, centerY + textYOffset)

    if blur:
        renderer.apply_filter(exporter.BLUR, 7)


# --- Py5 Setup ---
def settings() -> None:
    """
    Py5 sketch configuration function (called first).
    Sets canvas size and rendering mode based on the 'isHeadless' flag.

    This function is called first in a Py5 sketch. It sets the sketch's canvas size and rendering mode based on the 'isHeadless' flag. If 'isHeadless' is True, the sketch uses the HIDDEN renderer for batch processing without a display window. Otherwise, it uses the JAVA2D renderer for standard interactive display.
    """
    global sketchWidth, sketchHeight

    if isHeadless:
        # Use HIDDEN renderer for batch processing without a display window
        py5.size(sketchWidth, sketchHeight, py5.HIDDEN)
    else:
        # Use JAVA2D renderer for standard interactive display
        py5.size(sketchWidth, sketchHeight, py5.JAVA2D)


def setup() -> None:
    """
    Py5 initialisation function (called once after settings).
    Loads font, creates the off-screen exporter, and handles mode-specific logic.
    """
    global \
        exporter, \
        fontPath, \
        fontSize, \
        fontSizeSpecial, \
        surahFont, \
        surahFontSpecial, \
        surahIndex

    py5.frame_rate(144)  # High frame rate for smooth animation (interactive mode)
    surahFont = py5.create_font(fontPath, fontSize)
    surahFontSpecial = py5.create_font(fontPath, fontSizeSpecial)
    # Create a separate, off-screen graphics context for saving images
    exporter = py5.create_graphics(sketchWidth, sketchHeight, py5.JAVA2D)

    # Headless mode: Loop through all Surahs, save the image, and immediately exit.
    if isHeadless:
        currentSurah = surahs[0]
        filename = f"{drawablesPath}/surah_name_blurred.png"
        saveFrame(currentSurah, filename, True)

        for index in range(len(surahs)):
            # Update index before saving to ensure correct filename and content
            surahIndex = index
            currentSurah = surahs[surahIndex]
            filename = (
                f"{drawablesPath}/surah_{surahIndex:03d}.png"
                if surahIndex != 0
                else f"{drawablesPath}/surah_name.png"
            )
            saveFrame(currentSurah, filename)

        # Terminate the sketch after all frames are generated
        py5.exit_sketch()

    # Interactive mode: Start API data loading in a background thread
    else:
        threading.Thread(target=getSurahs, daemon=True).start()


# --- Main Draw Loop ---
def draw() -> None:
    """
    Py5 draw function (called continuously to render the animation).

    This function handles the animation loop and draws the current Surah content
    on the screen at the appropriate opacity level.
    """

    global surahIndex, previousTimestamp

    # If data is loading, draw the loading indicator and skip animation logic
    if loading:
        loadingIndicator()
        return

    currentTimestamp = py5.millis()
    elapsedTime = currentTimestamp - previousTimestamp
    currentSurah = surahs[surahIndex]

    # 1. Fade in phase: alpha goes from 0 to 255
    if elapsedTime < fadeInDuration:
        alpha = py5.remap(elapsedTime, 0, fadeInDuration, 0, 255)
        alpha = py5.constrain(alpha, 0, 255)
    # 2. Full opacity phase: alpha is 255
    elif elapsedTime < fadeInDuration + onScreenDuration:
        alpha = 255
    # 3. Fade out phase: alpha goes from 255 to 0
    elif elapsedTime < cycleDuration:
        fadeOutStart = fadeInDuration + onScreenDuration
        alpha = py5.remap(elapsedTime - fadeOutStart, 0, fadeOutDuration, 255, 0)
        alpha = py5.constrain(alpha, 0, 255)
    # 4. Transition to next surah (cycle complete)
    else:
        # Save the fully visible frame from the previous cycle
        if surahIndex == 0:
            filename = f"{drawablesPath}/surah_name_blurred.png"
            saveFrame(currentSurah, filename, True)

            filename = f"{drawablesPath}/surah_name.png"
            saveFrame(currentSurah, filename)
        else:
            filename = f"{drawablesPath}/surah_{surahIndex:03d}.png"
            saveFrame(currentSurah, filename)

        # Advance the cycle timestamp and index
        previousTimestamp += cycleDuration
        surahIndex += 1
        alpha = 0  # Reset alpha to 0 to start the fade-in of the next Surah

        # Check for end of Surah list
        if surahIndex >= len(surahs):
            py5.no_loop()  # Stop the draw loop
            print("All Surahs displayed.")
            return

    if surahIndex != 0:
        # Draw the content of the current Surah with the calculated alpha
        drawSurahContent(currentSurah, py5, alpha)
    else:
        loadingIndicator()


def generate() -> None:
    """
    Runs the Py5 sketch in interactive mode.

    This function runs the Py5 sketch in interactive mode, which means it has a display window.
    """
    py5.run_sketch(
        sketch_functions={
            "settings": settings,
            "setup": setup,
            "draw": draw,  # Include draw for animation loop
        }
    )


def generateHeadless() -> None:
    """
    Runs the Py5 sketch in headless mode.

    This function runs the Py5 sketch in headless mode, which means it does not have a display window.
    """
    py5.run_sketch(
        sketch_functions={
            "settings": settings,
            "setup": setup,  # The image generation logic is completed entirely within setup()
        }
    )


def main() -> None:
   """Runs the Py5 sketch.

   Depending on the value of the `isHeadless` flag, it runs the sketch in interactive mode (default) or in headless mode.
   """
   # Execute the appropriate Py5 run function based on the command-line argument.
   generateHeadless() if isHeadless else generate()


if __name__ == "__main__":
   main()
