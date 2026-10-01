#!/usr/bin/env bash
set -euo pipefail

version=$1
platform=$2
destination=$3
release=${version/+/%2B}
case $platform in
  windows) archive="OpenJDK21U-jdk_x64_windows_hotspot_${version/+/_}.zip" ;;
  linux) archive="OpenJDK21U-jdk_x64_linux_hotspot_${version/+/_}.tar.gz" ;;
esac
url="https://github.com/adoptium/temurin21-binaries/releases/download/jdk-$release/$archive"
staging="$destination.download"
trap 'rm -rf "$staging"' EXIT
mkdir -p "$staging/unpacked"
curl -fL --retry 3 -o "$staging/$archive" "$url"
curl -fL --retry 3 -o "$staging/checksum" "$url.sha256.txt"
(cd "$staging"; sha256sum --check checksum)
case $archive in
  *.zip) unzip -q "$staging/$archive" -d "$staging/unpacked" ;;
  *) tar -xzf "$staging/$archive" -C "$staging/unpacked" ;;
esac
rm -rf "$destination"
mkdir -p "$(dirname "$destination")"
mv "$staging/unpacked/jdk-$version" "$destination"
