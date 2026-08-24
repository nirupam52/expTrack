# Future development

## CI container image validation

- Review PR #11: restore the release-image build only when GitHub Actions capacity and cost are approved. The job is deferred to avoid creating container images now.

## CI pull request events

- Review PR #11: decide if CI must run when a pull request is reopened. The workflow now uses GitHub's default events (`opened`, `synchronize`, and `reopened`). Restore `types: [opened, synchronize]` if reopened pull requests must not start CI.
