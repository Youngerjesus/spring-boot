# Spring Boot 

https://spring.io/projects/spring-boot

***

## Docker Image

스프링 부트에서 자동으로 도커 이미지를 만들때 효율적으로 만들기 위해 계층형으로 레이어를 나눈다. 어떻게 나눠졌는지 보고 싶으면 `$ jar -xf [JAR_FILE_NAME] ` 을 통해서 보면 된다. layer.idx 가 생긴게 볼 수 있다. 

##### layer.idx

```
 "dependencies":
  - "BOOT-INF/lib/"
- "spring-boot-loader":
  - "org/"
- "snapshot-dependencies":
- "application":
  - "BOOT-INF/classes/"
  - "BOOT-INF/classpath.idx"
  - "BOOT-INF/layers.idx"
  - "META-INF/"
```

- 계층을 자세히 보면 dependencies가 둘로 나누어졌다. `dependencies` 와 `snapshot-dependencies` 이렇게. 
- `snapshot-dependencies` 는 정규 배포버전이 아닌 라이브러리를 말한다.  
- `application` 계층은 우리가 직성한 애플리케이션의 코드와 리소스를 말한다. 
- `spring-boot-loader` 는 스프링 부트 JAR 살행하기 위해 들어있는 패키지를 말한다. 


다음과 같이 설정을하면 메이븐에서 패키징을 할 때 자동으로 도커 이미지를 효율적으로 만들어준다. 

##### pom.xml

```xml
<plugins>
		<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
				<configuration>
					<layers>
						<enabled> true </enabled>
					</layers>
				</configuration>
				<executions>
					<execution>
						<goals>
							<goal>build-image</goal>
						</goals>
					</execution>
				</executions>
			</plugin>
</plugins>
```

이를 수동 도커파일로 하기 위해서는 Dockerfile로 작성해야 한다. 파일은 다음과 같다. 

```dockerfile
FROM openjdk:11.0.8-jdk-slim AS builder
WORKDIR source
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM openjdk:11.0.8-jre-slim
WORKDIR application
COPY --from=builder source/dependencies ./
COPY --from=builder source/spring-boot-loader ./
COPY --from=builder source/snapshot-dependencies ./
COPY --from=builder source/application ./

ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
```

***

## graceful shutdown

애플리케이션 서버 종료시 새로운 요청은 막고 기존에 처리중인 요청은 완전히 처리한 이후에 서버를 종료하는 기능

`server.shutdown=graceful` 를 통해 할 수 있다. 서블릿 기반 MVC와 웹 플럭스 모두 지원한다. 

graceful shutdown 이 작동하면 톰캣은 새 요청을 이제 받지 않고 요청이 오면 503 에러를 보낸다(service unavailable) 그리고 기존의 요청을 마무리 하는데 진행한다. 

`spring.lifecycle.timeout-per-shutdown-phase:20s` 를 통해 graceful shutdown 에 타임아웃을 설정할 수 있다. 

***

## Liveness 와 Readiness

Liveness 는 애플리케이션이 살아있는지 상태를 말한다. 상태가 비정상적이라면 종료하고 다시 재가동 해야한다.
  - `LivenessState.CORRECT` : 정상적인 상태를 말한다.
  - `LivenessState.BROKEN` : 문제가 있는 상태를 말한다.
Readiness 는 요청을 받을 수 있는 상태인지 말한다. 트래픽이 많아서 Refusing 상태가 된다면 준비가 될 때까지 요청을 받을 수 없다.
  - `ReadinessState.ACCEPTIONG_TRAFFIC` : 요청을 받을 수 있는 상태를 말한다. 
  - `ReadinessState.REFUSING_TRAFFIC` : 요청을 받을 수 없는 상태를 말한다. 
애플리케이션 내부에서 상태를 조회하기 위해선 `ApplicationAvailability` 를 빈으로 주입받아서 가져오면 된다. 

애플리케이션 외부에서 상태를 조회하기 위해선 Actulator 를 사용해야 한다. spring boot actuator 의존성을 추가하고 `management.endpoint.health.probes.enabled=true` 설정을 줘야한다. 그러면 actuator/health/liveness 에서 liveness 를 조회할 수 있고 actuator/health/readiness 에서 readiness 상태를 조회할 수 있다. 

애플리케이션 상태를 변경하기 위해서는 `AvailabilityChangeEvent.publish()` 메소드와 `ApplicationEventPublisher` 통해 가능하다. 

애플리케이션의 상태가 바뀌면 호출되는 리스너를 만들수도 있다. 
- 주의할 점은 요청을 받아서 어플리케이션의 상태를 바꾸게 했다면 이 바꾸는 스레드와 요청의 상태 변경을 감지해서 호출하는 스레드는 동일한 스레드다. 이게 싫다면 비동기로 처리하는 @Async 에노테이션을 붙이면 된다. 

***

## Spring Boot 와 쿠버네티스 연동 

쿠버네티스의 Liveness probe 는 특정 횟수 (기본값 3회) 이상 Liveness 상태가 안좋은 경우라면 해당 어플리케이션을 재시작한다. 

쿠버네티스 Readiness probe 는 Readiness 상태가 좋지 않은 경우 해당 pod로 요청을 보내지 않는다. 

쿠버네티스 pod 에 접속하기 위해서는 `kubectl exec --stdin --tty [POD_NAME] -- /bin/bash` 을 통해서 가능하다. 

`spring.config.activate.on-cloud.platform=kubernetes` 설정을 통해 쿠버네티스에 배포됐을때 설정파일이 작동하도록 할 수 있다. 
 - 지원하는 클라우드 플랫폼은 다음과같다.
 - Kubernetes
 - Cloud Foundary
 - Herorku
 - SAP
 - NONE

***

## spring.profiles

스프링 부트 2.4에서 spring.config.activate.on-profile 로 좀 더 직관적으로 바꼈다. 

***

## spring.profiles.include

추가로 읽어들일 프로파일을 정하는 것.

원래는 spring.profiles.active 와 spring.profiles.inclue 로 설정한 프로파일을 같이 쓸 수 있었는데 이게 겹치는 설정에 대해서 뭐가 우선순위인지 몰라서 이제는 같이 쓸 수 없게 됐다. 프로파일은 기본적으로 적용되는 default 만 같이 사용이 가능하다. 

spring.config.activate.on-profile 을 사용하면 이제 추가적인 프로파일을 사용할 수 없다. 

***

## 설정 파일 추가 

spring.config.import 를 통해 프로파일이 아니라 설정 파일을 읽어들일 수 있다. 이는 spring.config.activate.on-profile 과 같이 사용하는게 가능하다. 

***

## Configuration Tree

spring.config.import 값으로 configtree 를 이용할 수 있다. configtree 는 파일이나 디렉토리를 읽어서 설정파일로 등록해주는 방법이다.  

configtree 값이 없다면 에러가 나지만 설정을 optional 하게 쓸 수 도 있다. 있으면 쓰고 없으면 안쓰는. 

***

## 스프링 부트 쿠버네티스 ConfigMap

쿠버네티스가 지원하는 여러 Volume 중 하나이다. 

ConfigMap 은 pod 의 설정 정보들을 포함하고 있는 디렉토리를 만들어준다고 생각하면 된다. 이를 이용해서 특정 환경에 종속적인 값들을 컨테이너와 분리할 수 있다. 
  - Dev 설정, Production 설정을 분리해서 사용하는게 가능해진다. 

컨테이너는 Pod에 마운트 되어있는 ConfigMap 볼륨에 들어있는 설정을 참조해서 사용하는게 가능하다. 

어떻게 연동을 하는가는 쿠버네티스 ConfigMap 에 들어있는 설정을 스프링 부트 어플리케이션에서 @ConfigurationProperties 또는 Environment 를 통해 접근하는게 가능하다. 
- 이후 @ConfigurationPropertiesScan 을 등롥해줘야하고 자바 설정 파일에서는 @ConstructorBinding 을 해서 Immutable 하게 쓰는걸 추천한다. 
  

