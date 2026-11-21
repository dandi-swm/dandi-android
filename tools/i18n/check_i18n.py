#!/usr/bin/env python3
"""다국어 리소스와 화면 코드의 한국어 흔적을 검사한다. CI(pr-check)에서 돌고, 로컬에서는 저장소 루트에서 실행한다.

    python3 tools/i18n/check_i18n.py

검사
1. 문자열 키: 모듈마다 values(영어, 기본) / values-ko / values-ja의 번역할 문자열 이름이 같은지.
2. 서식 인자: 같은 이름의 문자열끼리 %1$s, %2$d 같은 인자가 같은지. 하나라도 다르면 그 언어에서 앱이 죽거나 값이 빠진다.
3. 일본어 리소스의 한글: values-ja/*.xml, raw-ja/* 에 한글이 없는지(주석 제외).
4. 화면 코드의 한글 문자열: presentation 모듈과 app의 src/main Kotlin 문자열 리터럴에 한글이 없는지.
   빼는 것: 주석, @Preview 함수 본문, 이름이 preview/Preview로 시작하는 선언, 로그·예외 메시지(Log.w(TAG, "…"), error("…") 등),
   `// i18n-ignore`가 붙은 줄(언어별 표기 표, debug 전용 문구 등).
   화면 문구는 문자열 리소스로 옮긴다.

실패하면 위치를 모두 찍고 1로 끝난다. 표준 라이브러리만 쓴다.
"""
from __future__ import annotations

import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
LOCALES = ("values-ko", "values-ja")
HANGUL = re.compile(r"[가-힣ㄱ-ㅎㅏ-ㅣ]")
FORMAT_ARG = re.compile(r"%(?:(\d+)\$)?[-#+ 0,(]*\d*(?:\.\d+)?([sdfxXc])")
SKIP_DIRS = {"build", ".git", ".gradle", ".idea", "node_modules"}
IGNORE_MARK = "i18n-ignore"


def source_dirs(pattern: str) -> list[Path]:
    return sorted(p for p in ROOT.glob(pattern) if not SKIP_DIRS.intersection(p.relative_to(ROOT).parts))


def rel(path: Path) -> str:
    return str(path.relative_to(ROOT))


# ---------------------------------------------------------------- 리소스

def load_strings(values_dir: Path) -> dict[str, str]:
    """번역할 string / plurals / string-array를 이름 → 글자(여러 항목은 이어 붙임)로 읽는다."""
    out: dict[str, str] = {}
    for xml in sorted(values_dir.glob("*.xml")):
        root = ET.parse(xml).getroot()
        for el in root:
            if el.tag not in ("string", "plurals", "string-array") or el.get("translatable") == "false":
                continue
            out[el.get("name")] = "\n".join("".join(item.itertext()) for item in el) if len(el) else "".join(el.itertext())
    return out


def format_args(text: str) -> list[str]:
    text = text.replace("%%", "")
    args = []
    for index, (position, kind) in enumerate(FORMAT_ARG.findall(text), start=1):
        args.append(f"{position or index}${kind}")
    return sorted(set(args))


def check_resources(errors: list[str]) -> None:
    for base_dir in source_dirs("**/src/main/res/values"):
        base = load_strings(base_dir)
        if not base:
            continue
        for locale in LOCALES:
            locale_dir = base_dir.with_name(locale)
            if not locale_dir.is_dir():
                errors.append(f"{rel(base_dir)}: {locale} 폴더가 없다")
                continue
            translated = load_strings(locale_dir)
            for name in sorted(base.keys() - translated.keys()):
                errors.append(f"{rel(locale_dir)}: '{name}' 번역이 없다")
            for name in sorted(translated.keys() - base.keys()):
                errors.append(f"{rel(locale_dir)}: '{name}'가 기본 values에 없다")
            for name in sorted(base.keys() & translated.keys()):
                if format_args(base[name]) != format_args(translated[name]):
                    errors.append(
                        f"{rel(locale_dir)}: '{name}' 서식 인자가 다르다 "
                        f"(기본 {format_args(base[name])}, {locale} {format_args(translated[name])})"
                    )


def check_japanese_hangul(errors: list[str]) -> None:
    files = [f for d in source_dirs("**/src/main/res/values-ja") for f in sorted(d.glob("*.xml"))]
    files += [f for d in source_dirs("**/src/main/res/raw-ja") for f in sorted(d.iterdir())]
    for path in files:
        text = re.sub(r"<!--.*?-->", lambda m: "\n" * m.group(0).count("\n"), path.read_text(encoding="utf-8"), flags=re.S)
        for number, line in enumerate(text.splitlines(), start=1):
            if HANGUL.search(line):
                errors.append(f"{rel(path)}:{number}: 일본어 리소스에 한글이 있다: {line.strip()[:80]}")


# ---------------------------------------------------------------- Kotlin

TOKEN = re.compile(
    r'(?P<line_comment>//[^\n]*)'
    r'|(?P<block_comment>/\*.*?\*/)'
    r'|(?P<raw>"""(?P<raw_body>.*?)"""(?!"))'
    r'|(?P<string>"(?P<body>(?:\\.|\$\{(?:[^{}"]|"(?:\\.|[^"\\\n])*"|\{[^{}]*\})*\}|[^"\\\n])*)")'
    r"|(?P<char>'(?:\\.|[^'\\\n])')"
    r'|(?P<open>\{)|(?P<close>\})',
    re.S,
)
PREVIEW_ANNOTATION = re.compile(r"@Preview\b|@\w*Previews\b")
PREVIEW_DECLARATION = re.compile(r"\b(?:fun|val|var|object|class)\s+(?:[\w.<>]+\.)?[Pp]review\w*")
# 로그·개발자 예외 메시지 자리. 같은 줄에서 문자열 앞에 오면 화면 문구가 아니다.
DEVELOPER_MESSAGE = re.compile(r"\b(?:Log\.[vdiwe]|Timber\.\w+|error|require|check|requireNotNull|checkNotNull|\w*Exception)\($")


def excluded_ranges(source: str, blanked: str) -> list[tuple[int, int]]:
    """@Preview 함수와 이름이 Preview로 시작하는 선언의 본문 범위. [blanked]는 주석·문자열을 지운 같은 길이의 글이다."""
    starts = [m.end() for m in PREVIEW_ANNOTATION.finditer(blanked)]
    starts += [m.start() for m in PREVIEW_DECLARATION.finditer(blanked)]
    ranges = []
    for start in starts:
        decl = re.compile(r"\b(?:fun|val|var|object|class)\b").search(blanked, start)
        if not decl:
            continue
        end = declaration_end(blanked, decl.end())
        ranges.append((start, end))
    return ranges


def declaration_end(blanked: str, position: int) -> int:
    """선언 머리 뒤에서 본문 끝을 찾는다. 괄호를 따라가다 처음 여는 중괄호의 짝, 없으면 다음 빈 줄까지."""
    depth = 0
    index = position
    while index < len(blanked):
        char = blanked[index]
        if char in "([":
            depth += 1
        elif char in ")]":
            depth -= 1
        elif char == "{" and depth == 0:
            return matching_brace(blanked, index)
        elif blanked.startswith("\n\n", index) and depth == 0:
            return index
        index += 1
    return len(blanked)


def matching_brace(blanked: str, open_index: int) -> int:
    depth = 0
    for index in range(open_index, len(blanked)):
        if blanked[index] == "{":
            depth += 1
        elif blanked[index] == "}":
            depth -= 1
            if depth == 0:
                return index + 1
    return len(blanked)


def kotlin_hangul_literals(path: Path) -> list[tuple[int, str]]:
    source = path.read_text(encoding="utf-8")
    if not HANGUL.search(source):
        return []
    literals = []
    blanked = list(source)
    for match in TOKEN.finditer(source):
        kind = match.lastgroup
        if kind in ("open", "close"):
            continue
        body = match.group("raw_body") if kind == "raw" else match.group("body") if kind == "string" else None
        if body is not None:
            literals.append((match.start(), body))
        for index in range(match.start(), match.end()):
            if blanked[index] != "\n":
                blanked[index] = " "
    blanked_text = "".join(blanked)
    ranges = excluded_ranges(source, blanked_text)
    lines = source.splitlines()
    found = []
    for offset, body in literals:
        if not HANGUL.search(body) or any(start <= offset < end for start, end in ranges):
            continue
        number = source.count("\n", 0, offset) + 1
        if IGNORE_MARK in lines[number - 1] or is_developer_message(source, offset):
            continue
        found.append((number, body))
    return found


def is_developer_message(source: str, offset: int) -> bool:
    """[offset]의 문자열이 로그·예외 메시지 호출의 인자인지(같은 줄, 첫 인자 또는 태그 뒤 인자)."""
    line_start = source.rfind("\n", 0, offset) + 1
    before = re.sub(r"\s*(?:\w+\s*,\s*)?$", "", source[line_start:offset])
    return DEVELOPER_MESSAGE.search(before) is not None


def check_kotlin(errors: list[str]) -> None:
    roots = source_dirs("*/presentation/src/main") + source_dirs("app/src/main")
    for root in roots:
        for path in sorted(root.rglob("*.kt")):
            for number, body in kotlin_hangul_literals(path):
                errors.append(f"{rel(path)}:{number}: 화면 코드에 한글 문자열이 있다(문자열 리소스로 옮긴다): \"{body[:60]}\"")


def main() -> int:
    errors: list[str] = []
    check_resources(errors)
    check_japanese_hangul(errors)
    check_kotlin(errors)
    if errors:
        print("\n".join(errors))
        print(f"\n다국어 검사 실패: {len(errors)}건. 이 파일 맨 위 설명을 본다.", file=sys.stderr)
        return 1
    print("다국어 검사 통과")
    return 0


if __name__ == "__main__":
    sys.exit(main())
