package com.back.web7_9_codecrete_be.global.spotify;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;

@Component
@RequiredArgsConstructor
public class SpotifyClient {

    private final SpotifyApi spotifyApi;

    public String getAccessToken() {
        try {
            ClientCredentialsRequest request = spotifyApi.clientCredentials().build();
            ClientCredentials credentials = request.execute();
            spotifyApi.setAccessToken(credentials.getAccessToken());
            return credentials.getAccessToken();
        } catch (Exception e) {
            throw new RuntimeException("Spotify 토큰 발급 실패", e);
        }
    }

    public SpotifyApi getAuthorizedApi() {
        if (spotifyApi.getAccessToken() == null) {
            getAccessToken();
        }
        return spotifyApi;
    }
}
