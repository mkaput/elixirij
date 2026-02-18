# Maintainer Instructions

## Release Runbook

### Hard Requirement: Release Tag Style

- Use plain SemVer without a `v` prefix: `X.Y.Z` or `X.Y.Z-rc.1`.
- Release tag must match `pluginVersion` exactly.
- Enforced by CI:
  - `Build` workflow blocks draft release creation when `pluginVersion` is not in required format.
  - `Release` workflow blocks publish when tag format is invalid or tag does not match `pluginVersion`.

### Steps

1. Update `pluginVersion` in `gradle.properties` to the target SemVer version.
2. Update `CHANGELOG.md` under `## [Unreleased]` with release-ready notes.
3. Merge to `main`.
4. Wait for the `Build` workflow to finish; it creates a draft GitHub release for the current version.
5. Open the draft release, review/edit notes, mark as prerelease when applicable, then publish it.
6. Confirm the `Release` workflow succeeds (signing + Marketplace publish + release asset upload).
7. If created, merge the changelog update PR opened by the `Release` workflow.

## Quality Gates Before Merge/Release

Run locally before merging release-affecting changes:

- `./gradlew check`
- `./gradlew verifyPlugin`

For release readiness, also run:

- `./gradlew buildPlugin`

Then verify:

- `CHANGELOG.md` has accurate `Unreleased` notes.
- `pluginVersion` is intentional and valid SemVer.
- If using a pre-release suffix (for example `1.2.0-alpha.1`), ensure the channel is intentional.

## Failure Recovery

- If the `Release` workflow fails after the release is published, fix the issue (usually secrets/config) and rerun the
  workflow for the same release tag before creating a new version.
