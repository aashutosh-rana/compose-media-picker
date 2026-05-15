# Releasing

This project publishes to Maven Central via the [Vanniktech maven-publish-plugin] and the
[Sonatype Central Portal]. Releases are **tag-driven** — `main` always carries the next
`-SNAPSHOT`, and the release workflow derives the real version from the git tag.

[Vanniktech maven-publish-plugin]: https://github.com/vanniktech/gradle-maven-publish-plugin
[Sonatype Central Portal]: https://central.sonatype.com

## One-time setup

1. **Sonatype Central Portal account**.
   - Sign in at <https://central.sonatype.com>.
   - **Namespaces → Add a Namespace → `io.github.aashutosh-rana`**. Verification is
     automatic for `io.github.<github-username>` namespaces — the portal redirects you
     through GitHub OAuth, no DNS records needed.
   - **View Account → Generate User Token**. Save both halves — these become
     `OSSRH_USERNAME` (the token name) and `OSSRH_PASSWORD` (the token secret) in CI.

2. **GPG signing key**.

   ```bash
   gpg --gen-key                                # RSA 4096, no expiry, with a passphrase
   gpg --list-secret-keys --keyid-format=long   # capture the long key ID
   gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
   gpg --keyserver keys.openpgp.org   --send-keys <KEY_ID>
   gpg --armor --export-secret-keys <KEY_ID>    # paste into the SIGNING_KEY secret below
   ```

3. **GitHub secrets**. In <https://github.com/aashutosh-rana/compose-media-picker>
   → **Settings → Secrets and variables → Actions → New repository secret**, add:

   | Secret | Value |
   |---|---|
   | `OSSRH_USERNAME` | Central Portal user-token name |
   | `OSSRH_PASSWORD` | Central Portal user-token secret |
   | `SIGNING_KEY` | Full ASCII-armored private key (output of `gpg --armor --export-secret-keys`) |
   | `SIGNING_PASSWORD` | GPG key passphrase |

## Per-release ritual

1. **Verify the snapshot is green** — every push to `main` runs `publish-snapshot.yml`
   and uploads `<version>-SNAPSHOT` to Maven Central's snapshot repo. Check the latest
   green run before tagging.
2. **Update `CHANGELOG.md`**: rename `## [Unreleased]` to `## [X.Y.Z] - YYYY-MM-DD`,
   add a fresh empty `## [Unreleased]` above it.
3. **Regenerate the API dump** if the public surface changed:

   ```bash
   ./gradlew :media-picker:apiDump
   ```

   Commit the updated `media-picker/api/*.api` files.
4. **Local smoke test**:

   ```bash
   ./gradlew :media-picker:check :media-picker:apiCheck detekt ktlintCheck
   ./gradlew :media-picker:publishToMavenLocal --no-configuration-cache
   ```

   Confirm `~/.m2/repository/io/github/aashutosh-rana/compose-media-picker*/<version>-SNAPSHOT/`
   contains all targets (`compose-media-picker` + `-android`, `-desktop`, `-iosx64`,
   `-iosarm64`, `-iossimulatorarm64`, `-wasm-js`).
5. **Push the changelog** to `main`.
6. **Tag the release**:

   ```bash
   git tag vX.Y.Z
   git push origin vX.Y.Z
   ```

   This fires `.github/workflows/publish-release.yml`: derives `VERSION_NAME=X.Y.Z`
   from the tag, publishes signed artifacts to Maven Central (Vanniktech auto-releases
   the staging repo), and pushes Dokka HTML to the `gh-pages` branch.

   Artifacts are searchable at
   <https://repo1.maven.org/maven2/io/github/aashutosh-rana/compose-media-picker/>
   after ~15 minutes.
7. **Bump main to the next `-SNAPSHOT`**:

   ```bash
   # edit gradle.properties: VERSION_NAME=<next>-SNAPSHOT
   git commit -am "Prepare next development version"
   git push
   ```

## Coordinates

```kotlin
implementation("io.github.aashutosh-rana:compose-media-picker:<version>")
```

Compose Multiplatform consumers pick the platform-specific variants
(`-android`, `-desktop`, `-iosx64`, …) automatically — there is nothing extra to declare.
