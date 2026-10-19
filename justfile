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
