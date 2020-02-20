# сисюрити... [![CI](https://github.com/daggerok/yet-another-oauth-spring-security-example/workflows/CI/badge.svg)](https://github.com/daggerok/yet-another-oauth-spring-security-example/actions)
oauth2 client and auth-server

Status: IN PROGRESS (oauth2-auth-server)

```bash
rm -rf oauth2-auth-server/src/main/resources/.oauth2.keystore
keytool -genkey -noprompt -alias oauth2-app -storetype PKCS12 -keyalg RSA -keysize 4096 -validity 3650 \
  -dname "CN=oauth2-auth-server-127-0-0-1.nip.io, OU=TEST, O=DAGGEROK, L=Odessa, S=OD, C=UA" \
  -storepass .oauth2.keystore.password -keypass .oauth2.keystore.password \
  -keystore oauth2-auth-server/src/main/resources/.oauth2.keystore

./mvnw clean resources:resources package
java -jar oauth2-auth-server/target/*.jar &
java -jar oauth2-client/target/*.jar &

# wait for ports: 8000, 8080
```

* open in your browser: http://127.0.0.1:8080/
* it will redirect you to: http://oauth2-auth-server-127-0-0-1.nip.io:8000/login
* enter `max / passwd` as `login / password`
* it will redirect you back to http://127.0.0.1:8080/oauth/authorize
* click Approve ....and that shit doesn't work...

## resources

* https://www.youtube.com/watch?v=gSUeGi4sLlA
* https://www.youtube.com/watch?v=EoK5a99Bmjc
* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/2.2.4.RELEASE/maven-plugin/)
* [Spring Security](https://docs.spring.io/spring-boot/docs/2.2.4.RELEASE/reference/htmlsingle/#boot-features-security)
* [OAuth2 Resource Server](https://docs.spring.io/spring-boot/docs/2.2.4.RELEASE/reference/htmlsingle/#boot-features-security-oauth2-server)
* [Securing a Web Application](https://spring.io/guides/gs/securing-web/)
* [Spring Boot and OAuth2](https://spring.io/guides/tutorials/spring-boot-oauth2/)
* [Authenticating a User with LDAP](https://spring.io/guides/gs/authenticating-ldap/)
* [Spring Web](https://docs.spring.io/spring-boot/docs/2.2.4.RELEASE/reference/htmlsingle/#boot-features-developing-web-applications)
* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/bookmarks/)
