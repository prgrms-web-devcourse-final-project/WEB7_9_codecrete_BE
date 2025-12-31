package com.back.web7_9_codecrete_be.global.musicbrainz;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

// MusicBrainzClient - Facade 패턴으로 분리된 클래스들을 통합
@Slf4j
@Component
@RequiredArgsConstructor
public class MusicBrainzClient {

    private final MusicBrainzSearchClient searchClient;
    private final MusicBrainzEntityClient entityClient;
    private final MusicBrainzRealNameClient realNameClient;

    // ========== 검색 관련 메서드 ==========

    public Optional<ArtistInfo> searchArtist(String artistName) {
        Optional<MusicBrainzEntityClient.ArtistInfo> result = searchClient.searchArtist(artistName);
        return result.map(this::convertArtistInfo);
    }

    public Optional<String> searchMbidBySpotifyUrl(String spotifyId) {
        return searchClient.searchMbidBySpotifyUrl(spotifyId);
    }

    // ========== Entity 관련 메서드 ==========

    public Optional<ArtistInfo> getArtistByMbid(String mbid) {
        Optional<MusicBrainzEntityClient.ArtistInfo> result = entityClient.getArtistByMbid(mbid);
        return result.map(this::convertArtistInfo);
    }

    public Optional<Boolean> isEnded(String mbid) {
        return entityClient.isEnded(mbid);
    }

    public Optional<Boolean> isSubunitOrProjectGroup(String mbid) {
        return entityClient.isSubunitOrProjectGroup(mbid);
    }

    // ========== 실명 조회 관련 메서드 ==========

    public List<AliasCandidate> collectAliasCandidates(String mbid) {
        List<MusicBrainzRealNameClient.AliasCandidate> candidates = realNameClient.collectAliasCandidates(mbid);
        return candidates.stream()
                .map(c -> new AliasCandidate(c.getName(), c.getType(), c.isPrimary(), c.getLocale()))
                .toList();
    }

    public Optional<String> getRealNameByMbid(String mbid, String stageName) {
        return realNameClient.getRealNameByMbid(mbid, stageName);
    }

    // ========== 변환 메서드 ==========

    private ArtistInfo convertArtistInfo(MusicBrainzEntityClient.ArtistInfo info) {
        return new ArtistInfo(
                info.getMbid(),
                info.getName(),
                info.getNameKo(),
                info.getArtistGroup(),
                info.getArtistType()
        );
    }

    // ========== 내부 클래스 (하위 호환성) ==========

    /**
     * ArtistInfo 클래스 (하위 호환성)
     */
    public static class ArtistInfo {
        private final String mbid;
        private final String name;
        private final String nameKo;
        private final String artistGroup;
        private final String artistType;

        public ArtistInfo(String mbid, String name, String nameKo, String artistGroup, String artistType) {
            this.mbid = mbid;
            this.name = name;
            this.nameKo = nameKo;
            this.artistGroup = artistGroup;
            this.artistType = artistType;
        }

        public String getMbid() {
            return mbid;
        }

        public String getName() {
            return name;
        }

        public String getNameKo() {
            return nameKo;
        }

        public String getArtistGroup() {
            return artistGroup;
        }

        public String getArtistType() {
            return artistType;
        }
    }

    /**
     * AliasCandidate 클래스 (하위 호환성)
     */
    public static class AliasCandidate {
        private final String name;
        private final String type;
        private final boolean primary;
        private final String locale;
        
        public AliasCandidate(String name, String type, boolean primary, String locale) {
            this.name = name;
            this.type = type;
            this.primary = primary;
            this.locale = locale;
        }
        
        public String getName() { return name; }
        public String getType() { return type; }
        public boolean isPrimary() { return primary; }
        public String getLocale() { return locale; }
    }
}
