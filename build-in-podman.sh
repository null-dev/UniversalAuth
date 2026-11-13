#!/usr/bin/env bash
# Builds signed release APKs into ./dist. AGP 7.1 / Gradle 7.2 need JDK 11, hence the container.
set -euo pipefail

cd "$(dirname "$0")"
CACHE="${XDG_CACHE_HOME:-$HOME/.cache}/universalauth-build"
mkdir -p "$CACHE/sdk" "$CACHE/gradle" dist

proxy="${HTTPS_PROXY:-${https_proxy:-}}"
proxy_host="" proxy_port=""
if [[ -n "$proxy" ]]; then
    hp="${proxy#*://}"; hp="${hp%%/*}"; hp="${hp##*@}"
    proxy_host="${hp%%:*}"; proxy_port="${hp##*:}"
fi

podman run --rm \
    -v "$PWD":/src:Z -v "$CACHE/sdk":/sdk:Z -v "$CACHE/gradle":/gradle:Z \
    -e HTTPS_PROXY="$proxy" -e HTTP_PROXY="$proxy" -e NO_PROXY="${NO_PROXY:-}" \
    -e PROXY_HOST="$proxy_host" -e PROXY_PORT="$proxy_port" \
    -e ANDROID_SDK_ROOT=/sdk -e GRADLE_USER_HOME=/gradle \
    -w /src docker.io/library/eclipse-temurin:11-jdk bash -euo pipefail -c '
        sdk_proxy=()
        if [[ -n "$PROXY_HOST" ]]; then
            sdk_proxy=(--proxy=http --proxy_host="$PROXY_HOST" --proxy_port="$PROXY_PORT")
            printf "systemProp.%s.proxyHost=%s\nsystemProp.%s.proxyPort=%s\n" \
                https "$PROXY_HOST" https "$PROXY_PORT" http "$PROXY_HOST" http "$PROXY_PORT" \
                > /gradle/gradle.properties
        fi
        if [[ ! -x /sdk/cmdline-tools/latest/bin/sdkmanager ]]; then
            apt-get -qq update >/dev/null && DEBIAN_FRONTEND=noninteractive apt-get -qq install -y unzip >/dev/null
            curl -fsSL -o /tmp/clt.zip https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
            mkdir -p /sdk/cmdline-tools && unzip -q /tmp/clt.zip -d /sdk/cmdline-tools
            mv /sdk/cmdline-tools/cmdline-tools /sdk/cmdline-tools/latest
        fi
        if [[ ! -d /sdk/platforms/android-31 || ! -d /sdk/platforms/android-32 || ! -d /sdk/build-tools/30.0.3 ]]; then
            { yes || true; } | /sdk/cmdline-tools/latest/bin/sdkmanager "${sdk_proxy[@]}" --licenses >/dev/null
            /sdk/cmdline-tools/latest/bin/sdkmanager "${sdk_proxy[@]}" \
                "platforms;android-31" "platforms;android-32" "build-tools;30.0.3" >/dev/null
        fi
        ./gradlew --no-daemon -q :faceunlock:assembleRelease :universalauth-xposed:assembleRelease
        cp faceunlock/build/outputs/apk/release/faceunlock-release.apk dist/UniversalAuth-FaceUnlock.apk
        cp universalauth-xposed/build/outputs/apk/release/universalauth-xposed-release.apk dist/UniversalAuth-Xposed.apk
    '
ls -la dist
