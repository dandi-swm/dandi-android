require "fileutils"
require "fastlane_core"

# 출시 노트: fastlane/release-notes/<버전 이름>/<로케일>.txt (본문 그대로, 태그 없음).
RELEASE_NOTES_DIR = File.expand_path("release-notes", __dir__)
# Play 출시 노트 한도(로케일마다, 유니코드 글자 수)
RELEASE_NOTES_MAX_CHARS = 500
RELEASE_NOTES_LOCALE_PATTERN = /\A[a-z]{2}(-[A-Z]{2})?\z/

# 버전 폴더의 출시 노트를 검사해 { 로케일 => 본문(앞뒤 공백 제거) } 로 돌려준다. 문제가 있으면 멈춘다.
def load_release_notes(version_name, base_dir = RELEASE_NOTES_DIR)
  dir = File.join(base_dir, version_name)
  files = Dir.exist?(dir) ? Dir.glob(File.join(dir, "*.txt")).select { |p| File.file?(p) }.sort : []
  if files.empty?
    FastlaneCore::UI.user_error!("출시 노트가 없습니다. #{dir}/<로케일>.txt (예: ko-KR.txt) 를 만들어 주세요.")
  end

  files.each_with_object({}) do |path, notes|
    locale = File.basename(path, ".txt")
    unless locale.match?(RELEASE_NOTES_LOCALE_PATTERN)
      FastlaneCore::UI.user_error!("출시 노트 파일 이름이 로케일 코드(xx-XX 또는 xx)가 아닙니다: #{path}")
    end
    # BOM 은 떼고, 줄바꿈은 LF 로 맞춰 센다(올리는 본문도 같은 값).
    raw = File.read(path, encoding: "BOM|UTF-8")
    unless raw.valid_encoding?
      FastlaneCore::UI.user_error!("출시 노트가 UTF-8 이 아닙니다: #{path}")
    end
    text = raw.gsub("\r\n", "\n").strip
    FastlaneCore::UI.user_error!("출시 노트가 비어 있습니다: #{path}") if text.empty?
    if text.length > RELEASE_NOTES_MAX_CHARS
      FastlaneCore::UI.user_error!("출시 노트가 #{RELEASE_NOTES_MAX_CHARS}자를 넘습니다: #{path} (#{text.length}자)")
    end
    notes[locale] = text
  end
end

# supply 가 읽는 <dir>/<로케일>/changelogs/<versionCode 또는 default>.txt 구조를 만든다.
# supply 는 <versionCode>.txt 가 없으면 같은 폴더의 default.txt 를 쓴다.
def write_supply_changelogs(notes, dir, version_code)
  file_name = version_code ? "#{version_code}.txt" : "default.txt"
  notes.each do |locale, text|
    changelogs = File.join(dir, locale, "changelogs")
    FileUtils.mkdir_p(changelogs)
    File.write(File.join(changelogs, file_name), text, encoding: "UTF-8")
  end
end
