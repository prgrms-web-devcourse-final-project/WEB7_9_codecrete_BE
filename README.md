<p align="center">
  <img width="114" height="71" alt="ncb" src="https://github.com/user-attachments/assets/f6463190-fd04-4722-97fe-d0db4080927e" />
</p>

<h1 align="center">🎵 내 콘서트를 부탁해 (NCB) - Backend</h1>

<p align="center">
예매부터 뒤풀이까지, 공연 관람의 하루를 관리하는 올인원 플랫폼
</p>

<p align="center">
<b>프로젝트 기간</b> 2025.12.04 ~ 2026.01.13
</p>

---

# 📑 목차

- [기획 의도](#기획-의도)
- [프로젝트 개요](#프로젝트-개요)
- [주요 기능](#주요-기능)
- [시스템 아키텍처](#시스템-아키텍처)
- [ERD](#erd)
- [기술 스택](#기술-스택)
- [프로젝트 구조](#프로젝트-구조)
- [팀원소개](#백엔드-팀)
- [Docker 실행 방법](#docker-실행-방법)

---
<a name="기획-의도"></a>
# 💡 기획 의도

<img width="1920" height="1080" alt="기획 의도" src="https://github.com/user-attachments/assets/92b94aa5-6ca3-4be5-acdd-509b6c7c8116" />

---
<a name="프로젝트-개요"></a>
# 📌 프로젝트 개요

<img width="1920" height="1080" alt="프로젝트 개요" src="https://github.com/user-attachments/assets/94c2d2ab-a6fd-465c-a728-d001ed5a8e79" />

---
<a name="주요-기능"></a>
# 🚀 주요 기능

## 공연 탐색

- Kopis API 기반 공연 데이터 조회
- 공연 일자 / 조회수 기준 정렬
- 무한 스크롤 기반 공연 목록 제공
- 공연 검색 기능

## 검색 자동완성

- Redis 기반 자동완성 검색
- 공연 제목 역인덱스 구조 활용
- 조회수 기반 검색 결과 노출

## 아티스트 정보 제공

외부 API를 활용하여 아티스트 정보를 수집하고 제공합니다.

사용 API

- Spotify API
- Wikidata API
- MusicBrainz API

제공 정보

- 아티스트 소개
- 인기 트랙
- 인기 앨범
- 팔로워 수 / 인기도
- 유사 아티스트

Spotify와 연동되어 **앨범 클릭 시 Spotify 스트리밍 페이지로 이동**합니다.

## 실시간 채팅

WebSocket + STOMP 기반 채팅 기능

- 실시간 채팅 메시지 송수신
- 채팅 메시지 저장 및 조회
- 채팅방 접속자 수 관리
- 채팅방 참여자 관리

Redis 활용

- Redis Stream → 메시지 저장
- Redis Set → 접속자 관리

## 공연 일정 플래너

공연 당일 일정을 관리하고 공유할 수 있는 기능

- 공연 일정 생성 및 관리
- 장소 기반 일정 계획
- 공유 링크 생성
- UUID 기반 공유 토큰
- 1일 후 자동 만료

## 권한 관리

플래너 참여자 권한 관리

권한 유형

- OWNER
- EDITOR
- VIEWER

각 권한에 따라 조회 / 수정 / 삭제 권한이 다르게 적용됩니다.

## 지도 기반 장소 탐색

외부 지도 API 활용

사용 API

- Kakao Map API
- Kakao Mobility API
- TMAP API

제공 기능

- 공연장 주변 카페 / 맛집 검색
- 대중교통 길찾기
- 도보 / 차량 이동 시간 계산

---
<a name="시스템-아키텍처"></a>
# 🏗 시스템 아키텍처

<img width="1920" height="1080" alt="Architecture" src="https://github.com/user-attachments/assets/a7513e8e-dfe1-4a9d-a506-58761133fe73" />

---
<a name="erd"></a>
# 🗄 ERD

<p align="center">
 <img width="1920" height="1080" alt="ERD" src="https://github.com/user-attachments/assets/ae116025-2fc4-44e7-9f7b-14a27589dd48" />
</p>

🔗 **[ERD 전체 보기 (ERDCloud)](https://www.erdcloud.com/d/XxaBXzaoBeJ3ptdef)**

본 프로젝트는 공연 정보 조회, 공연 일정 플래너, 커뮤니티, 채팅 기능을 중심으로 구성된 도메인 구조를 기반으로 데이터 모델을 설계하였습니다.

### 주요 도메인

- **Users** : 사용자 정보 및 인증 관리
- **Concerts** : 공연 정보 및 공연 조회 데이터
- **Artists** : 아티스트 정보 및 외부 API 연동 데이터
- **Plans** : 공연 일정 플래너 및 참여자 관리
- **Community** : 공연 관련 게시글 및 댓글
- **Chats** : 실시간 채팅 및 채팅 메시지 관리
- **Location** : 공연장 주변 장소 및 지도 정보

ERD는 서비스의 핵심 도메인을 중심으로 관계형 데이터 구조를 설계하였으며  
공연 일정 관리와 사용자 간 상호작용을 고려한 데이터 관계를 기반으로 구성되었습니다.

---
<a name="기술-스택"></a>
# 🛠 기술 스택

## Backend

![Java](https://img.shields.io/badge/Java-17-007396?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.0-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=for-the-badge)
![Hibernate](https://img.shields.io/badge/Hibernate-59666C?style=for-the-badge&logo=hibernate&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-Authentication-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)
![WebSocket](https://img.shields.io/badge/WebSocket-STOMP-010101?style=for-the-badge)

## Database

![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)

## Infrastructure

![AWS EC2](https://img.shields.io/badge/AWS_EC2-FF9900?style=for-the-badge&logo=amazon-ec2&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)

## DevOps

![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)

## Collaboration

![Slack](https://img.shields.io/badge/Slack-4A154B?style=for-the-badge&logo=slack&logoColor=white)
![Notion](https://img.shields.io/badge/Notion-000000?style=for-the-badge&logo=notion&logoColor=white)
![Discord](https://img.shields.io/badge/Discord-5865F2?style=for-the-badge&logo=discord&logoColor=white)
![ZEP](https://img.shields.io/badge/ZEP-6F3AFF?style=for-the-badge)

## External API

![Kopis API](https://img.shields.io/badge/Kopis_API-000000?style=for-the-badge)
![Spotify API](https://img.shields.io/badge/Spotify_API-1DB954?style=for-the-badge&logo=spotify&logoColor=white)
![Kakao Map](https://img.shields.io/badge/Kakao_Map_API-FFCD00?style=for-the-badge)
![TMAP API](https://img.shields.io/badge/TMAP_API-FF4F00?style=for-the-badge)

---
<a name="프로젝트-구조"></a>
# 📂 프로젝트 구조

```text
src
├─ main
│  ├─ java
│  │  └─ com.back.web7_9_codecrete_be
│  │     ├─ domain
│  │     │  ├─ auth
│  │     │  ├─ users
│  │     │  ├─ concerts
│  │     │  ├─ artists
│  │     │  ├─ community
│  │     │  ├─ chats
│  │     │  ├─ plans
│  │     │  └─ location
│  │     ├─ global
│  │     │  ├─ config
│  │     │  ├─ security
│  │     │  ├─ error
│  │     │  ├─ redis
│  │     │  ├─ scheduler
│  │     │  └─ websocket
│  │     └─ Web79CodecreteBeApplication
│  └─ resources
│     └─ application.yml
```

---
<a name="백엔드-팀"></a>
# 👥 팀원 소개

<img width="855" height="484" src="https://github.com/user-attachments/assets/db33fbb4-8823-4c77-830d-cb7fc1e871cb" />

---
<a name="docker-실행-방법"></a>
# 🐳 Docker 실행 방법

이 프로젝트는 **MySQL 8.0**과 **Redis 7.2**를 Docker Compose로 실행합니다.

## 1️⃣ 환경 변수 설정

`.env` 파일 생성

```
MYSQL_PORT=3306
MYSQL_ROOT_PASSWORD=root
MYSQL_DATABASE=concert
MYSQL_USER=user
MYSQL_PASSWORD=password

REDIS_PORT=6379
```

## 2️⃣ Docker 실행

```
docker compose up -d
```

## 3️⃣ 컨테이너 확인

```
docker ps
```

| Container | Service |
|---|---|
| concert-mysql | MySQL 8.0 |
| concert-redis | Redis 7.2 |

## 4️⃣ 종료

```
docker compose down
```
