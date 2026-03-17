# 🎵 내 콘서트를 부탁해 (NCB) - Backend

> 예매부터 뒤풀이까지, 공연 관람의 하루를 관리하는 올인원 플랫폼

<img width="1920" height="1080" alt="4-기획 의도" src="https://github.com/user-attachments/assets/92b94aa5-6ca3-4be5-acdd-509b6c7c8116" />

---

# 📌 Project Overview

<img width="1920" height="1080" alt="5-프로젝트 개요" src="https://github.com/user-attachments/assets/94c2d2ab-a6fd-465c-a728-d001ed5a8e79" />


# 🚀 Main Features

### 공연 탐색

- Kopis API 기반 공연 데이터 조회
- 공연 일자 / 조회수 기준 정렬
- 무한 스크롤 기반 공연 목록 제공
- 공연 검색 기능

### 검색 자동완성

- Redis 기반 자동완성 검색
- 공연 제목 역인덱스 구조 활용
- 조회수 기반 검색 결과 노출

### 아티스트 정보 제공

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

### 실시간 채팅

WebSocket + STOMP 기반 채팅 기능

- 실시간 채팅 메시지 송수신
- 채팅 메시지 저장 및 조회
- 채팅방 접속자 수 관리
- 채팅방 참여자 관리

Redis를 활용하여

- Redis Stream → 메시지 저장
- Redis Set → 접속자 관리

### 공연 일정 플래너

공연 당일 일정을 관리하고 공유할 수 있는 기능

- 공연 일정 생성 및 관리
- 장소 기반 일정 계획
- 공유 링크 생성
- UUID 기반 공유 토큰
- 1일 후 자동 만료

### 권한 관리

플래너 참여자 권한 관리

권한 유형

- OWNER
- EDITOR
- VIEWER

각 권한에 따라 조회 / 수정 / 삭제 권한이 다르게 적용됩니다.

### 지도 기반 장소 탐색

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

# 🏗 System Architecture
<img width="1920" height="1080" alt="투표 _ 박상아 _ 변수연 _ 최병준 _ 김민석 _ 이혜지 _ 김윤수 Codecrete" src="https://github.com/user-attachments/assets/a7513e8e-dfe1-4a9d-a506-58761133fe73" />


# 🛠 Tech Stack

### Backend
![Java](https://img.shields.io/badge/Java-17-007396?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.0-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=for-the-badge)
![Hibernate](https://img.shields.io/badge/Hibernate-59666C?style=for-the-badge&logo=hibernate&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-Authentication-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)
![WebSocket](https://img.shields.io/badge/WebSocket-STOMP-010101?style=for-the-badge)

### Database
![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)

### Infrastructure
![AWS EC2](https://img.shields.io/badge/AWS_EC2-FF9900?style=for-the-badge&logo=amazon-ec2&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)

### DevOps
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)

### External API
![Kopis API](https://img.shields.io/badge/Kopis_API-000000?style=for-the-badge)
![Spotify API](https://img.shields.io/badge/Spotify_API-1DB954?style=for-the-badge&logo=spotify&logoColor=white)
![Kakao Map](https://img.shields.io/badge/Kakao_Map_API-FFCD00?style=for-the-badge)
![TMAP API](https://img.shields.io/badge/TMAP_API-FF4F00?style=for-the-badge)

---

## 📂 Project Structure

```text
src
├─ main
│  ├─ java
│  │  └─ com.back.web7_9_codecrete_be
│  │     ├─ domain        # 도메인별 비즈니스 로직
│  │     │  ├─ auth
│  │     │  ├─ users
│  │     │  ├─ concerts
│  │     │  ├─ artists
│  │     │  ├─ community
│  │     │  ├─ chats
│  │     │  ├─ plans
│  │     │  └─ location
│  │     ├─ global        # 공통 설정 및 인프라
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

# 👥 Backend Team
<img width="855" height="484" alt="screencapture-miricanvas-v2-ko-design2-139b13ea-a2b5-4b45-a015-0ad51e6e0d34-2026-03-17-17_36_17" src="https://github.com/user-attachments/assets/db33fbb4-8823-4c77-830d-cb7fc1e871cb" />

---
## 🐳 Docker 실행 방법

이 프로젝트는 **MySQL 8.0**과 **Redis 7.2**를 Docker Compose로 실행합니다.  
로컬 개발 환경에서 데이터베이스와 캐시 서버를 쉽게 구성할 수 있습니다.

---

## 1️⃣ 환경 변수 설정

프로젝트 루트에 `.env` 파일을 생성하고 아래 내용을 작성합니다.

```env
MYSQL_PORT=3306
MYSQL_ROOT_PASSWORD=root
MYSQL_DATABASE=concert
MYSQL_USER=user
MYSQL_PASSWORD=password

REDIS_PORT=6379
```

`.env` 파일은 `docker-compose.yml`에서 사용하는 환경 변수입니다.

---

## 2️⃣ Docker 컨테이너 실행

아래 명령어로 MySQL과 Redis 컨테이너를 실행합니다.

```bash
docker compose up -d
```

옵션 설명

| 옵션 | 설명 |
|---|---|
| up | 컨테이너 실행 |
| -d | 백그라운드 실행 |

---

## 3️⃣ 실행 상태 확인

```bash
docker ps
```

정상적으로 실행되면 다음과 같은 컨테이너가 실행됩니다.

| Container | Service |
|---|---|
| concert-mysql | MySQL 8.0 |
| concert-redis | Redis 7.2 |

---

## 4️⃣ 로그 확인

문제가 발생했을 경우 로그를 확인할 수 있습니다.

```bash
docker compose logs
```

특정 서비스 로그 확인

```bash
docker compose logs mysql
docker compose logs redis
```

---

## 5️⃣ 컨테이너 종료

```bash
docker compose down
```

---

## 6️⃣ 데이터 유지 (Volume)

MySQL과 Redis 데이터는 Docker Volume에 저장됩니다.

| Volume | 설명 |
|---|---|
| mysql-data | MySQL 데이터 저장 |
| redis-data | Redis 데이터 저장 |

컨테이너를 삭제해도 데이터는 유지됩니다.

---

## 7️⃣ 포트 정보

| Service | Port |
|---|---|
| MySQL | `${MYSQL_PORT}:3306` |
| Redis | `${REDIS_PORT}:6379` |

---

💡 **Tip**

처음 실행할 때는 Docker 이미지 다운로드로 인해 시간이 조금 걸릴 수 있습니다.
