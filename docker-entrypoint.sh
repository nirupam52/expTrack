#!/bin/sh
set -eu

chown -h exptrack:exptrack /data
find /data -maxdepth 1 -type f -name 'exptrack.db*' -exec chown -h exptrack:exptrack {} \;

exec su-exec exptrack "$@"
