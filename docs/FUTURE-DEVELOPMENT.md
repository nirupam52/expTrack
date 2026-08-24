# Future development

## CI pull request events

- Review PR #11: decide if CI must run when a pull request is reopened. The workflow now uses GitHub's default events (`opened`, `synchronize`, and `reopened`). Restore `types: [opened, synchronize]` if reopened pull requests must not start CI.
