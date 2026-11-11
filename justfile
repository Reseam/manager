export GRADLE_OPTS := "-Xmx1g"

gradle := "./gradlew --no-daemon -Dorg.gradle.jvmargs=-Xmx1536m"
engine := `sed -n 's/^reseam = "\(.*\)"/\1/p' gradle/libs.versions.toml`
reseam := "build/engine/reseam"

check:
    {{ gradle }} :composeApp:assembleDebug :composeApp:compileKotlinJvm -PmanagerVersion=0.0.1

release tag:
    curl -fsSL --create-dirs -o {{ reseam }} https://cdn.reseam.app/engine/v{{ engine }}/reseam-linux-x64
    chmod +x {{ reseam }}
    echo "$ANDROID_KEYSTORE_B64" | base64 -d > build/keystore.jks
    RESEAM_KEYSTORE="$PWD/build/keystore.jks" {{ gradle }} :composeApp:stageRelease -PmanagerVersion={{ tag }}
    {{ reseam }} release manager composeApp/build/release --tag {{ tag }} --name "Reseam Manager" --author Reseam \
        --download-url https://reseam.app/download/ --to s3://reseam-releases/manager --keep 3 --forge-release
    # The GitHub mirror gets the same files, as a second place to download them from.
    git push "https://x-access-token:$MIRROR_RELEASE_TOKEN@github.com/Reseam/manager.git" refs/tags/{{ tag }}
    GITHUB_SERVER_URL=https://github.com GITHUB_API_URL=https://api.github.com RELEASE_TOKEN="$MIRROR_RELEASE_TOKEN" \
        {{ reseam }} release files composeApp/build/release --tag {{ tag }}
