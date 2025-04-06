#!/bin/bash

# Create producer_videos directories (1-20)
for i in {1..20}; do
  mkdir -p "producer_videos$i"
  # Copy videos if there is a source directory
  if [ -d "producer_videos" ]; then
    cp -r producer_videos/* "producer_videos$i/" 2>/dev/null || true
  fi
done