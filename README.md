# haproxy
How to rote traffic in docker by using haproxy

## flow

```
                                 /
                                 /web1**  +------+
                               +--------->| app1 |
 +---------+    +----------+   |          +------+
 | Browser |<-->| HA proxy |<--+
 +---------+    +----------+   | /web2**  +------+
                               +--------->| app2 |
                                          +------+
```

## build run and test

_build_

```bash
./mvnw -f app/pom.xml
```

_run_

```bash
docker-compose up --build --force-recreate --remove-orphans
```

_test_

```bash
http :80/web1-hostname
{
    "hostname": "27f78c8df6da"
}

http :80/web2-hostname
{
    "hostname": "01a9b26cbcd8"
}

http :80/hostn
{
    "hostname": "27f78c8df6da"
}
```

_tear down_

```bash
docker-compose down -v
```
