package io.github.nvcex.android;

import androidx.core.util.Consumer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class NvcexServerApi {
    private final String url;
    private final String username;
    private final String password;

    private final OkHttpClient client;

    public NvcexServerApi(String url, String username, String password)
    {
        this.url = url;
        this.username = username;
        this.password = password;
        this.client = new OkHttpClient();
    }

    private Request.Builder requestBuilder(String command, Consumer<HttpUrl.Builder> buildUrl)
    {
        var uriBuilder = HttpUrl.parse(url).resolve(command).newBuilder();
        if (buildUrl != null) {
            buildUrl.accept(uriBuilder);
        }
        var builder = new Request.Builder()
                .url(uriBuilder.build());
        if (!username.isEmpty() && !password.isEmpty()) {
            builder.header("Authorization", Credentials.basic(username, password));
        }
        return builder;
    }

    private Request.Builder requestBuilder(String command)
    {
        return requestBuilder(command, null);
    }

    public List<String> scenarios() throws IOException
    {
        var req = requestBuilder("scenarios")
                .get()
                .build();
        try (var res = client.newCall(req).execute()) {
            var mapper = new ObjectMapper();
            return mapper.readValue(res.body().string(), new TypeReference<List<String>>() {});
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TtsRequest
    {
        public final String scenario_name;
        public final String text;
        public final byte[] data;

        @JsonCreator
        public TtsRequest(String scenario, String text, byte[] body)
        {
            this.scenario_name = scenario;
            this.text = text;
            this.data = body;
        }
    }

    public byte[] tts(String scenario, String text, byte[] data) throws IOException
    {
        var mapper = new ObjectMapper();
        byte[] json = mapper.writeValueAsBytes(new TtsRequest(scenario, text, data));

        var req = requestBuilder("tts", null)
                .post(RequestBody.create(json, MediaType.parse("application/json")))
                .build();
        try (var res = client.newCall(req).execute()) {
            return res.body().bytes();
        }
    }

}
