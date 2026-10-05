"""Generate README demos: python make_demo_gifs.py VIDEO --ffmpeg FFMPEG."""
import argparse
import subprocess
from pathlib import Path

# name, start seconds, duration seconds, two simultaneous phone screens
CLIPS = [
    ("login", 0, 8, False),
    ("review-ai", 8, 22, False),
    ("nfc-cart", 36, 8, False),
    ("personal-receipt", 56, 11, False),
    ("signup", 68, 21, True),
    ("group-invite", 89, 21, True),
    ("shared-cart", 110, 14, True),
    ("group-chat", 124, 10, True),
    ("pickup-roulette", 134, 15, True),
    ("split-payment", 149, 25, True),
    ("group-receipt", 174, 5, True),
]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("video", type=Path)
    parser.add_argument("--ffmpeg", default="ffmpeg")
    parser.add_argument("--only", help="Regenerate a single clip by name")
    args = parser.parse_args()
    output = Path(__file__).resolve().parents[1] / "images"
    output.mkdir(parents=True, exist_ok=True)
    for name, start, duration, paired in CLIPS:
        if args.only and name != args.only:
            continue
        crop = "crop=1320:1080:310:0,scale=660:-1" if paired else "crop=520:1080:700:0,scale=300:-1"
        filters = (
            f"trim=start={start}:end={start + duration},setpts=(PTS-STARTPTS)/1.5,{crop},fps=10,split[a][b];"
            "[a]palettegen=stats_mode=diff[p];"
            "[b][p]paletteuse=dither=sierra2_4a:diff_mode=rectangle"
        )
        subprocess.run([
            args.ffmpeg, "-hide_banner", "-loglevel", "error", "-y",
            "-i", str(args.video),
            "-filter_complex", filters, "-an", "-loop", "0",
            str(output / f"{name}.gif"),
        ], check=True)
        print(f"Created {name}.gif", flush=True)


if __name__ == "__main__":
    main()
