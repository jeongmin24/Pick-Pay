# Pick_Pay
비주얼 &amp; 소셜 스마트 오더 솔루션

## 로컬 실행환경
1. 로컬 DB 연결
- paymanagement > src/main/resource > application.properties
```
spring.datasource.username={DBname}
spring.datasource.password={DBpass}
```
2. API base URL 연결
- payclient > gradle > gradle.properties
- my_computer_baseurl → ipconfig 
```
BASE_URL=http://{my_computer_baseurl}:8080/
```
## git 브랜치 관리
- 원격 ←→ 로컬 브랜치 연결
```
git branch --set-upstream-to=origin/<원격브랜치> <로컬브랜치>
```