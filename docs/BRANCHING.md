# Branching

This fork carries a Nissan Teana (2021 2.0T, China) port on top of upstream DiPlay
(shihabal3amri/DiPlay). The branch set follows a light git-flow adapted to a fork that keeps
tracking its parent:

| Branch | Cut from | Purpose | Merged into |
| --- | --- | --- | --- |
| `main` | upstream `main` | Mirror of upstream plus accepted Nissan-port merges; the only branch release APKs are tagged from | — |
| `develop` | `main` | Integration line of the Nissan port; upstream syncs land here first and soak before moving to `main` | `main` |
| `feat/*` | `develop` | One branch per feature, e.g. `feat/nissan-teana-adaptation` | `develop` |
| `hotfix` | `main` | Long-lived hotfix base; a concrete fix cuts `hotfix/<issue>` from it when `main` needs a fix outside the develop cycle | `main` and `develop` |

## Conventions

- `feat/*` names use kebab-case and start with the area when it helps: `feat/nissan-wheel-keys`,
  `feat/nissan-hotspot-guide`, `feat/launcher-integration`.
- A `hotfix/<issue>` branch is created from `hotfix` (which tracks `main`) only when a fix is
  actually needed; it is merged back into both `main` and `develop` and then deleted.
- Commits follow the upstream style: imperative subject, body explaining the measured
  behaviour when the change is car-specific.

## Syncing with upstream

```bash
git fetch upstream
git switch main && git merge --ff-only upstream/main && git push origin main
git switch develop && git merge main && git push origin develop
```

Resolve conflicts on `develop`, never rewrite `main` history away from upstream.

## Rules carried over from upstream

- Every vehicle-specific behaviour stays gated behind a head-unit check
  (`GeelyHeadUnit`, `NissanHeadUnit`, DiLink generation detection) so untested units keep
  stock behaviour.
- New car-specific code ships with a unit test that pins the gate (see
  `GeelyClusterDisplayTest`, `NissanHeadUnitTest`).
- Claims in docs state what was measured and where; unverified behaviour is labelled as such.
