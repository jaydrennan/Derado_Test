package com.derado.k8sdashboard.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class KubernetesClientConfig implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(KubernetesClientConfig.class);

    @Bean
    @Profile("incluster")
    public ApiClient inClusterApiClient() throws IOException {
        ApiClient client = ClientBuilder.cluster().build();
        io.kubernetes.client.openapi.Configuration.setDefaultApiClient(client);
        return client;
    }

    @Bean
    @Profile("!incluster")
    public ApiClient defaultApiClient() throws IOException {
        ApiClient client = Config.defaultClient();
        io.kubernetes.client.openapi.Configuration.setDefaultApiClient(client);
        return client;
    }

    /**
     * Called after ALL singleton beans are fully initialized.
     * Replaces the strict Gson with a lenient one that uses reflection-based
     * deserialization (ignoring unknown fields from newer K8s API versions).
     */
    @Override
    public void afterSingletonsInstantiated() {
        log.info("Configuring lenient JSON deserialization for K8s API compatibility");

        Gson lenientGson = new GsonBuilder()
                .disableHtmlEscaping()
                .registerTypeAdapter(OffsetDateTime.class, new TypeAdapter<OffsetDateTime>() {
                    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

                    @Override
                    public void write(JsonWriter out, OffsetDateTime value) throws IOException {
                        if (value == null) { out.nullValue(); } else { out.value(formatter.format(value)); }
                    }

                    @Override
                    public OffsetDateTime read(JsonReader in) throws IOException {
                        if (in.peek() == JsonToken.NULL) { in.nextNull(); return null; }
                        return OffsetDateTime.parse(in.nextString(), formatter);
                    }
                })
                .registerTypeAdapter(LocalDate.class, new TypeAdapter<LocalDate>() {
                    @Override
                    public void write(JsonWriter out, LocalDate value) throws IOException {
                        if (value == null) { out.nullValue(); } else { out.value(value.toString()); }
                    }

                    @Override
                    public LocalDate read(JsonReader in) throws IOException {
                        if (in.peek() == JsonToken.NULL) { in.nextNull(); return null; }
                        return LocalDate.parse(in.nextString());
                    }
                })
                .registerTypeAdapter(byte[].class, new JSON.ByteArrayAdapter())
                .registerTypeAdapter(Quantity.class, new TypeAdapter<Quantity>() {
                    @Override
                    public void write(JsonWriter out, Quantity value) throws IOException {
                        if (value == null) { out.nullValue(); } else { out.value(value.toSuffixedString()); }
                    }

                    @Override
                    public Quantity read(JsonReader in) throws IOException {
                        if (in.peek() == JsonToken.NULL) { in.nextNull(); return null; }
                        return Quantity.fromString(in.nextString());
                    }
                })
                .create();

        JSON.setGson(lenientGson);
        log.info("Lenient JSON deserialization configured - static Gson replaced");
    }
}
