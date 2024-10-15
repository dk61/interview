# Forex-proxy
Proxy app to get information from OneFrame 
# How to run locally
## Local env configuration
To run locally you're required to have pre-installed OneFrame service. 
To have it locally you're required to run following command:
```shell
docker-compose -f local-env-compose.yaml up
```
After service up you're able to run scala app for real testing
## Local run (sbt)
To run application locally you're required to go to root of the directory and execute following command
```shell
sbt run
```
## Test running
To run tests not required any docker, only run next command:
```shell
sbt test
```
# Run (Dockerfile)
To run in production prepared docker file and compose file
```shell
docker-compose -f app-compose.yaml up
```
