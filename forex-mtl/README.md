# Forex-proxy
Proxy app to get information from OneFrame
# Application environments
Application able to work in next envs:
- local
- prod
- docker

To set it required to define environment variable APP_ENV.
In case if env will not be set automatically will be chosen local. 
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
## Local run (docker)
To run full application locally it's okey to run next command:
```shell
docker-compose -f local-compose.yaml up
```
## Test running
To run tests not required any docker, only run next command:
```shell
sbt test
```
# Run (Dockerfile)
To run in production prepared docker file and compose file
```shell
docker-compose -f prod-compose.yaml up
```
