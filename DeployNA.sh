#!/bin/bash

# Cấu hình
SERVER=root@192.168.1.100
KEY=~/.ssh/id_rsa
REMOTE_DIR=/root/game/gameserver

# 1. Đồng bộ thư mục target
rsync -avz -e "ssh -i $KEY" ./target/ $SERVER:$REMOTE_DIR/target/

# 2. Upload file pom.xml
scp -i $KEY pom.xml $SERVER:$REMOTE_DIR/
scp -i $KEY config.json $SERVER:$REMOTE_DIR/
scp -i $KEY log4j2.xml $SERVER:$REMOTE_DIR/
#scp -i $KEY run.sh $SERVER:$REMOTE_DIR/

echo "✅ Deploy completed!"
