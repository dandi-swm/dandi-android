"""냐미 스프라이트를 앱 에셋으로 내보낸다.

상황 표(app_states.json)와 체형별 원본 매니페스트(states_<체형>/<폴더>/<폴더>.json)로
체형마다 animations.json을 만들고, 쓰는 시트만 무손실 WebP로 바꿔 앱 에셋 폴더에 넣는다.
체형 폴더 이름은 고양이 조회 API의 weight 값(LEAN … PLUMP)을 그대로 쓴다.
원본에 없는 동작은 그 체형에서만 빠진다. 검수에서 떨어진 체형은 상황 표의 skip에 적어 뺀다.

    python3 -I tools/cat-assets/export_app_cats.py <원본 폴더> [앱 에셋 폴더]

원본 폴더 예: ~/Downloads/nyummy-cat-states
앱 에셋 폴더 기본값: cat/data/src/main/assets/cats
필요한 도구: cwebp (brew install webp)
"""
import json
import os
import shutil
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(os.path.dirname(HERE))

# 원본 체형 폴더 → 고양이 조회 API의 weight
BODIES = {"m2": "LEAN", "m1": "SLIM", "0": "NORMAL", "p1": "CHUBBY", "p2": "PLUMP"}


def load_motions(states_dir):
    """(폴더, 동작) → (프레임 규격, 클립 목록)"""
    motions = {}
    for folder in sorted(os.listdir(states_dir)):
        manifest = os.path.join(states_dir, folder, f"{folder}.json")
        if not os.path.isfile(manifest):
            continue
        data = json.load(open(manifest))
        for group in data["animation"]:
            name = group[0]["src"].split("/")[-2]
            motions[(folder, name)] = (data["frame"], group)
    return motions


def export_body(src_root, out_root, body, weight, table):
    states_dir = os.path.join(src_root, f"states_{body}")
    motions = load_motions(states_dir)
    out_dir = os.path.join(out_root, weight)
    shutil.rmtree(out_dir, ignore_errors=True)
    os.makedirs(out_dir)

    sheets = {}  # 원본 src → 앱 src
    animations = []
    missing = []
    skipped = []
    for state, rows in table.items():
        if state.startswith("_"):
            continue
        frame = None
        entries = []
        for row in rows:
            key = tuple(row["motion"].split("/"))
            if weight in row.get("skip", []):
                skipped.append(f"{state}:{row['motion']}")
                continue
            if key not in motions:
                missing.append(f"{state}:{row['motion']}")
                continue
            motion_frame, group = motions[key]
            assert frame in (None, motion_frame), f"{state}: 프레임 규격이 다른 동작이 섞임 {row['motion']}"
            frame = motion_frame
            clips = []
            for i, clip in enumerate(group):
                # 원본 src는 states_<체형>/<폴더>/<동작>/파일.png, 앱 src는 <폴더>/<동작>/파일.webp
                app_src = clip["src"].split("/", 1)[1].rsplit(".", 1)[0] + ".webp"
                sheets[clip["src"]] = app_src
                out = {"src": app_src, "frames": clip["frames"]}
                if clip.get("loop"):
                    out["loop"] = True
                    if row.get("hold") and 0 < i < len(group) - 1:
                        out["loopTimes"] = row["hold"]
                clips.append(out)
            entries.append({"name": key[1], "weight": row["weight"], "clips": clips})
        names = [e["name"] for e in entries]
        assert len(names) == len(set(names)), f"{state}: 동작 이름이 겹침 {names}"
        if entries:
            animations.append({"state": state, "frame": frame, "motions": entries})

    for src, app_src in sheets.items():
        png = os.path.join(src_root, src)
        assert os.path.isfile(png), png
        webp = os.path.join(out_dir, app_src)
        os.makedirs(os.path.dirname(webp), exist_ok=True)
        subprocess.run(["cwebp", "-quiet", "-lossless", "-z", "6", "-exact", png, "-o", webp], check=True)

    with open(os.path.join(out_dir, "animations.json"), "w") as f:
        json.dump({"animations": animations}, f, ensure_ascii=False, indent=1)

    size = sum(os.path.getsize(os.path.join(d, n)) for d, _, ns in os.walk(out_dir) for n in ns)
    counts = " ".join(f"{a['state']}={len(a['motions'])}" for a in animations)
    print(f"{weight:7} 시트 {len(sheets):3}장 {size / 1e6:.2f}MB | {counts}")
    if missing:
        print(f"        원본에 없어 뺀 동작 {len(missing)}개: {', '.join(missing)}")
    if skipped:
        print(f"        검수에서 떨어져 뺀 동작 {len(skipped)}개: {', '.join(skipped)}")


def main():
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    src_root = os.path.expanduser(sys.argv[1])
    out_root = sys.argv[2] if len(sys.argv) > 2 else os.path.join(REPO, "cat/data/src/main/assets/cats")
    table = json.load(open(os.path.join(HERE, "app_states.json")))
    for body, weight in BODIES.items():
        export_body(src_root, out_root, body, weight, table)


if __name__ == "__main__":
    main()
