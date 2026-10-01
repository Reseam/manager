#!/usr/bin/env bash
set -euo pipefail

version=$1
destination=$2
release=${version/+/%2B}
archive="OpenJDK21U-jdk_x64_windows_hotspot_${version/+/_}.zip"
url="https://github.com/adoptium/temurin21-binaries/releases/download/jdk-$release/$archive"
staging="$destination.download"
trap 'rm -rf "$staging"' EXIT
mkdir -p "$staging"
curl -fL --retry 3 -o "$staging/$archive" "$url"
curl -fL --retry 3 -o "$staging/checksum" "$url.sha256.txt"
(cd "$staging"; sha256sum --check checksum)
unzip -q "$staging/$archive" -d "$staging/unpacked"
rm -rf "$destination"
mkdir -p "$(dirname "$destination")"
mv "$staging/unpacked/jdk-$version" "$destination"
