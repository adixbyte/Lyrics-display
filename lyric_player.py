import json
import re
import time
from urllib.request import urlopen, Request
from urllib.parse import urlencode
from urllib.error import URLError
import os
import serial


arduino = serial.Serial("COM4", 9600, timeout=1)
time.sleep(2)

def parse_lrc(synced_lyrics):
    cues = []

    for lyric_line in synced_lyrics.splitlines():
        timestamps = re.findall(r"\[(\d+):(\d+(?:\.\d+)?)\]", lyric_line)
        words = re.sub(r"\[[^\]]*\]", "", lyric_line).strip()

        if not words:
            continue

        for minutes, seconds in timestamps:
            time_ms = round((int(minutes) * 60 + float(seconds)) * 1000)
            cues.append({"time_ms": time_ms, "text": words})

    return sorted(cues, key=lambda cue: cue["time_ms"])


def fetch_lyrics(title, artist):
    query = urlencode({"q": f"{title} {artist}"})
    url = f"https://lrclib.net/api/search?{query}"
    request = Request(url, headers={"User-Agent": "LyricsDisplayPrototype/1.0"})

    try:
        with urlopen(request, timeout=8) as response:
            results = json.load(response)
    except (URLError, TimeoutError) as error:
        print(f"Could not look up lyrics: {error}")
        return []

    normalize = lambda value: " ".join(value.casefold().split())

    for result in results:
        same_title = normalize(result.get("trackName", "")) == normalize(title)
        same_artist = normalize(result.get("artistName", "")) == normalize(artist)
        synced_lyrics = result.get("syncedLyrics")

        if same_title and same_artist and synced_lyrics:
            print("Found synced lyrics.")
            return parse_lrc(synced_lyrics)

    print("No matching synced lyrics found for this song.")
    return []

"""
It search LRCLIB for a matching song and convert 
its LRC timestamps into the time_ms cues your existing
display() function uses
"""

def display(lyrics, message):
    line = None

    for lyric in lyrics:
        if lyric["time_ms"] <= message["position_ms"]:
            line = lyric["text"] #gets replaced with last line less than equal to message["position_ms"]


    return line


def text(line):
    if line is None:
        return "", ""

    rows = ["", ""]
    current_row=0
    for word in line.split():#gives words in list
        word = word[:16]  # Cut off words longer than one LCD row
        
        for row_number in range(current_row,2): # row_number=0,1(IT DOES NOT GO TO 1 AS LONG AS THE IF BLOCK IS RUNNING,WHEN IF BLOCK DOES NOT goes to 1)
            separator = " " if rows[row_number] else ""#handles the case when there is no string in the row
            space_needed = len(separator) + len(word)

            if len(rows[row_number]) + space_needed <= 16:
                rows[row_number] += separator + word
                current_row = row_number #remain 0 as long as chracter is below 16 
                break #breaks once we add the word in row then moves to next word
        else:
            break  # Both rows are full

       
    return rows[0], rows[1] #returns a tuple



last_rows = None
print("Waiting for playback data from Flask...")

lyrics = []
active_track = None

while True:
    try:
        with urlopen(
            "http://127.0.0.1:8000/now-playing",
            timeout=3
        ) as response:
            latest = json.load(response)["message"]

        if latest is not None:
            track = (latest["title"], latest["artist"])

            # Detect a song change
            if track != active_track:
                active_track = track

                # Show title and artist on the LCD
                title = track[0][:16]
                artist = track[1][:16]

                lcd_message = title.ljust(16) + artist.ljust(16)
                arduino.write((lcd_message + "\n").encode("utf-8"))

                last_rows = None
                lyrics = fetch_lyrics(*track)

                # Skip lyric display during this iteration
                time.sleep(3)
                continue

            # Update lyrics on every cycle
            if lyrics:
                current_line = display(lyrics, latest)
                rows = text(current_line)

                if rows != last_rows:
                    print(rows[0])
                    print(rows[1])
                    print()

                    lcd_message = rows[0].ljust(16) + rows[1].ljust(16)
                    arduino.write((lcd_message + "\n").encode("utf-8"))

                    last_rows = rows

        time.sleep(0.5) #just for effects

    except (URLError, TimeoutError) as error:
        print(f"Could not read playback data from Flask: {error}")
        time.sleep(2)
