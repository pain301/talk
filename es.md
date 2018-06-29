```sh
wget https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-6.2.3.tar.gz
tar -xzf elasticsearch-6.2.3.tar.gz
cd elasticsearch-6.2.3/

./elasticsearch -Ecluster.name=pain -Enode.name=node-1
```

```sh
sudo sysctl -w vm.max_map_count=262144
ulimit -n 65536

vi /etc/security/limits.conf
elasticsearch  -  nofile  65536
```

插件
```sh
./elasticsearch-plugin install https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v6.2.3/elasticsearch-analysis-ik-6.2.3.zip
```

```sh
curl -X GET "localhost:9200/_cat/health?v"
curl -X GET "localhost:9200/_cat/nodes?v"
```

### index
select
```sh
curl -X GET "localhost:9200/_cat/indices?v"
```
create
```sh
curl -X PUT "localhost:9200/accounts?pretty"
```
delete
```sh
curl -X DELETE "localhost:9200/accounts?pretty"
```

### type
select
```sh
curl -X GET 'localhost:9200/_mapping?pretty'
```
create
```sh
curl -X PUT 'localhost:9200/accounts?pretty' -H 'Content-Type: application/json' -d '
{
  "mappings": {
    "se": {
      "properties": {
        "name": {
          "type": "text",
          "analyzer": "ik_max_word",
          "search_analyzer": "ik_max_word"
        },
        "age": {
          "type": "long"
        },
        "desc": {
          "type": "text",
          "analyzer": "ik_max_word",
          "search_analyzer": "ik_max_word"
        }
      }
    }
  }
}'
```

### document
create
```sh
curl -X PUT "localhost:9200/accounts/se/1?pretty" -H 'Content-Type: application/json' -d'
{
  "name": "John Doe",
  "age": 20,
  "desc": "系统架构"
}
'
```
```sh
# 随机生成 id
curl -X POST "localhost:9200/accounts/se?pretty" -H 'Content-Type: application/json' -d'
{
  "name": "Jojo"
}
'
```
update
```sh
curl -X POST "localhost:9200/accounts/se/2/_update?pretty" -H 'Content-Type: application/json' -d'
{
  "doc": { "name": "Jane Doe", "age": 20 }
}
'
```
```sh
curl -X POST "localhost:9200/accounts/se/2/_update?pretty" -H 'Content-Type: application/json' -d'
{
  "script" : "ctx._source.age += 5"
}
'
```
select
```sh
curl -X GET "localhost:9200/accounts/se/1?pretty"
```
delete
```sh
curl -X DELETE "localhost:9200/accounts/se/2?pretty"
```

### search
```sh
curl -X GET 'localhost:9200/accounts/se/_search?pretty'
```
```sh
curl -X GET 'localhost:9200/accounts/se/_search?pretty' -H 'Content-Type: application/json' -d '
{
  "query": {
    "match": {
      "desc": "银杏"
    }
  },
  "from": 0,
  "size": 5
}'
```
```sh
curl 'localhost:9200/accounts/se/_search?pretty' -H 'Content-Type: application/json' -d '
{
  "query": {
    "match": {
      "desc": "系统 钱"
    }
  }
}'
```
```sh
curl 'localhost:9200/accounts/se/_search?pretty' -H 'Content-Type: application/json' -d '
{
  "query": {
    "bool": {
      "must": [
        { "match": { "desc": "管理" } },
        { "match": { "desc": "系统" } }
      ]
    }
  }
}'
```
