# Hansung Authorization Server

## 프로젝트 소개
이 프로젝트는 한성대학교 캡스톤 디자인 작품의 일환으로, **OIDC(OpenID Connect) 프로토콜의 핵심 구성 요소인 인가 서버**를 구현한 것입니다. **Spring Authorization Server** 프레임워크를 기반으로 개발되었으며, OIDC 프로토콜에서 **OpenID Provider** 역할과 OAuth2 프로토콜에서 **Authorization Server** 역할을 수행합니다.

이 프로젝트의 주요 목적은 **한성대학교 학생들을 위한 안전하고 효율적인 통합 인증 시스템을 구축하는 것**입니다. 이를 통해 다양한 학내 서비스에 대한 접근을 간소화하고, **학생 정보의 보안을 강화**하고자 합니다. 또한, 이 프로젝트는 실제 기업에서 널리 사용되는 **최신 인증 기술을 학습하고 적용**하는 교육적 의의도 가지고 있습니다.

## 개발 기간
- **2023/03/20 ~ 2023/06/01**

## 개발 환경
- 개발 언어: **`Java 17`**
- IDE: `IntelliJ IDEA`
- 프레임워크: **`Spring Authorization Server`**, **`Spring Boot 3`**
- 데이터베이스: `MySQL`
- 빌드 도구: `Gradle`

## OIDC(OpenID Connect) 적용
이 프로젝트는 **OpenID Connect(OIDC) 프로토콜**을 구현하였습니다. OIDC는 **OAuth 2.0**을 기반으로 한 인증 계층으로, 사용자 인증을 안전하고 표준화된 방식으로 처리합니다. 

### OIDC 프로토콜의 주요 특징
- **ID Token 발급**: **JWT(JSON Web Token)** 형식으로 사용자 정보를 안전하게 전달
- **사용자 정보 엔드포인트**: 추가적인 사용자 정보 제공
- **다양한 클라이언트 유형 지원**: SPA, 웹, 모바일 등

이를 통해 본 프로젝트는 한성대학교 학생들의 인증 정보를 안전하게 관리하고, 다양한 클라이언트 애플리케이션에서 이를 활용할 수 있도록 지원합니다.

## 프로젝트 주요 기능
1. **OIDC 프로토콜 지원**
2. **커스텀 ID Token 및 Access Token Claim**
   - ID Token: 이름, 학번, 트랙, 프로필 사진, 권한
   - Access Token: 권한
3. **사용자 인증 및 권한 관리**
4. **클라이언트 애플리케이션 등록 및 관리**
5. **커스텀 로그인 페이지**
6. **로그아웃 엔드포인트 지원**

## 프로젝트 구조
```
com.hansung.hansungauthorizationserver
├── config
│   ├── CORSCustomizer.java
│   └── SecurityConfig.java
├── entity
│   ├── Client.java
│   ├── GrantType.java
│   └── User.java
├── repository
│   ├── ClientRepository.java
│   └── UserRepository.java
├── security
│   └── SecurityUser.java
├── service
│   ├── CustomClientService.java
│   ├── CustomUserDetailsService.java
│   └── OidcUserInfoService.java
└── HansungAuthorizationServerApplication.java
```

## 개인 역할 및 주요 기여 사항
이 [프로젝트](https://github.com/HansungCapstoneDesign)는 4인 팀 프로젝트로 진행하였으며 인가 서버의 경우 제가 대부분의 구현을 담당했습니다. 주요 기여 사항은 다음과 같습니다.

1. **프로젝트 아키텍처 설계 및 구현**
   - Spring Authorization Server 기반 전체 시스템 아키텍처 설계 및 구현
   - `SecurityConfig` 클래스에서 OIDC 및 OAuth2 관련 설정 구성
2. **OIDC 프로토콜 적용**
   - ID Token 생성 및 커스터마이징 로직 구현
   - `OidcUserInfoService`를 통한 사용자 정보 엔드포인트 개발
3. **사용자 인증 시스템 구현**
   - `CustomUserDetailsService`를 통한 데이터베이스 기반 사용자 인증 시스템 구현
   - `SecurityUser` 클래스 개발 및 Spring Security 통합
4. **클라이언트 관리 시스템 개발**
   - `CustomClientService`를 통한 클라이언트 애플리케이션 등록 및 관리 기능 구현
   - `Client` 엔티티 설계 및 JPA를 이용한 데이터베이스 연동
5. **토큰 커스터마이징**
   - 프로젝트 요구사항에 맞는 ID Token 및 Access Token 클레임 커스터마이징
6. **보안 강화**
   - CORS 설정을 통한 클라이언트-서버 간 안전한 통신 구현
   - JWT 키 관리 로직 구현을 통한 토큰 보안 강화
7. **추가 기능 구현**
   - 커스텀 로그인 페이지 설계 및 구현
   - 프레임워크 기본 미지원 로그아웃 엔드포인트 추가 구현

## 느낀점 및 배운점
이 프로젝트를 통해 정말 많은 것을 배우고 성장할 수 있었습니다.

- **OIDC와 OAuth2 프로토콜**에 대해 이해하게 되었고, 실제로 이를 구현해보며 인증/인가 시스템의 **복잡성과 중요성**을 체감했습니다.
- **Spring Authorization Server**를 활용해 실제 동작하는 시스템을 만들어내는 과정에서 큰 **성취감**을 느꼈습니다. 특히 프레임워크에서 지원하지 않는 **로그아웃 기능**을 직접 구현하며 **문제 해결 능력**을 키울 수 있었습니다.
- 팀 프로젝트를 이끌어가며 기술적인 결정을 내리고 팀원들과 소통하는 과정에서 **커뮤니케이션 스킬**도 많이 향상되었습니다.

이 경험을 통해 단순히 코드를 작성하는 것을 넘어, **전체 시스템을 설계하고 구현**하는 과정의 즐거움과 어려움을 동시에 느낄 수 있었습니다.

## 원본 프로젝트 링크
[Organization 리포지토리 링크](https://github.com/HansungCapstoneDesign/Capstone-Design-Authorization-Server)
