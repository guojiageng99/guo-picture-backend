#!/usr/bin/env bash
# Ubuntu 单节点 Elasticsearch 7.x：仅监听 127.0.0.1:9200，供 SSH 隧道转发到本机开发。
# 用法：上传到服务器后执行
#   chmod +x install-elasticsearch-ubuntu-single-node.sh
#   sudo ./install-elasticsearch-ubuntu-single-node.sh
set -euo pipefail

if [[ "$(id -u)" -ne 0 ]]; then
  echo "请使用 sudo 运行"
  exit 1
fi

if ! command -v curl >/dev/null 2>&1; then
  apt-get update -qq
  apt-get install -y -qq curl gnupg
fi

if [[ ! -f /usr/share/keyrings/elasticsearch-keyring.gpg ]]; then
  curl -fsSL https://artifacts.elastic.co/GPG-KEY-elasticsearch \
    | gpg --dearmor -o /usr/share/keyrings/elasticsearch-keyring.gpg
fi

if [[ ! -f /etc/apt/sources.list.d/elastic-7.x.list ]]; then
  echo "deb [signed-by=/usr/share/keyrings/elasticsearch-keyring.gpg] https://artifacts.elastic.co/packages/7.x/apt stable main" \
    > /etc/apt/sources.list.d/elastic-7.x.list
fi

apt-get update -qq
# 7.x 源安装当前最新的 7.17.x，与 Spring Boot 2.7 客户端匹配
apt-get install -y elasticsearch

CONF="/etc/elasticsearch/elasticsearch.yml"
if ! grep -q "guo-picture-single-node-marker" "$CONF" 2>/dev/null; then
  cat >> "$CONF" << 'EOF'

# --- guo-picture-single-node-marker（云图库开发，仅本机访问）---
network.host: 127.0.0.1
http.port: 9200
discovery.type: single-node
cluster.name: guo-picture-es
node.name: guo-picture-node-1
xpack.security.enabled: false
# ---
EOF
fi

# 小内存机器可取消注释下面两行（约 512MB 堆）
# mkdir -p /etc/elasticsearch/jvm.options.d
# echo '-Xms512m' > /etc/elasticsearch/jvm.options.d/heap.options
# echo '-Xmx512m' >> /etc/elasticsearch/jvm.options.d/heap.options

systemctl daemon-reload
systemctl enable elasticsearch
systemctl restart elasticsearch

sleep 3
if curl -sS "http://127.0.0.1:9200" >/dev/null; then
  echo "Elasticsearch 已启动，本机自检: curl http://127.0.0.1:9200"
else
  echo "请查看日志: journalctl -u elasticsearch -n 50 --no-pager"
  exit 1
fi

echo ""
echo "下一步：在你电脑上用一条 SSH 同时转发 MySQL / Redis / ES（示例）："
echo "  ssh -L 3307:127.0.0.1:3306 -L 6380:127.0.0.1:6379 -L 9200:127.0.0.1:9200 ubuntu@你的服务器IP -N"
echo "本地 application-local.yml 中 elasticsearch.uris 使用 http://127.0.0.1:9200"
