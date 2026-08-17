# ========================================================
# Elasticsearch 8.x + IK 中文分词插件
# IK 是 RAG 混合检索（ik_max_word 分析器）的运行时依赖，
# 未安装时 ES 索引创建会失败并被后端熔断降级。
# ========================================================
ARG ES_VERSION=8.18.8
FROM docker.elastic.co/elasticsearch/elasticsearch:${ES_VERSION}

ARG ES_VERSION
# --batch 跳过交互确认；版本号与 ES 主版本保持一致
RUN bin/elasticsearch-plugin install --batch https://get.infini.cloud/elasticsearch/analysis-ik/${ES_VERSION}
