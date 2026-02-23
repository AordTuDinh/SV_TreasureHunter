#!/bin/bash

# Cấu hình
SERVER=root@103.28.37.28
KEY=~/.ssh/id_rsa
REMOTE_DIR=/root/idleNinja/game

# 1. Đồng bộ thư mục target
rsync -avz -e "ssh -i $KEY" ./target/ $SERVER:$REMOTE_DIR/target/

# 2. Upload file pom.xml
scp -i $KEY pom.xml $SERVER:$REMOTE_DIR/

echo "✅ Deploy completed!"
