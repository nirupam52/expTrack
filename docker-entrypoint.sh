#!/bin/sh
set -eu

# Docker can mount a root-owned volume over this directory.
chown -h exptrack:exptrack /data
# SQLite can create journal and write-ahead-log side files.
find /data -maxdepth 1 -type f -name 'exptrack.db*' -exec chown -h exptrack:exptrack {} \;

# The application must not run as root.
exec su-exec exptrack "$@"
