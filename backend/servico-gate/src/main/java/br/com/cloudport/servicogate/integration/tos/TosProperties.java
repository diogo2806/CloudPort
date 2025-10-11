package br.com.cloudport.servicogate.integration.tos;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.convert.DurationUnit;

@ConfigurationProperties(prefix = "cloudport.tos")
public class TosProperties {

    @NestedConfigurationProperty
    private final ApiProperties api = new ApiProperties();

    @NestedConfigurationProperty
    private final RetryProperties retry = new RetryProperties();

    @NestedConfigurationProperty
    private final CacheProperties cache = new CacheProperties();

    public ApiProperties getApi() {
        return api;
    }

    public RetryProperties getRetry() {
        return retry;
    }

    public CacheProperties getCache() {
        return cache;
    }

    public static class ApiProperties {

        private String baseUrl;

        @DurationUnit(ChronoUnit.MILLIS)
        private Duration timeout = Duration.ofSeconds(5);

        private String bookingPath = "/tos/bookings/{bookingNumber}";

        private String containerStatusPath = "/tos/containers/{containerNumber}/status";

        private String customsReleasePath = "/tos/containers/{containerNumber}/customs";

        private String username;

        private String password;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public String getBookingPath() {
            return bookingPath;
        }

        public void setBookingPath(String bookingPath) {
            this.bookingPath = bookingPath;
        }

        public String getContainerStatusPath() {
            return containerStatusPath;
        }

        public void setContainerStatusPath(String containerStatusPath) {
            this.containerStatusPath = containerStatusPath;
        }

        public String getCustomsReleasePath() {
            return customsReleasePath;
        }

        public void setCustomsReleasePath(String customsReleasePath) {
            this.customsReleasePath = customsReleasePath;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class RetryProperties {

        private int maxAttempts = 3;

        @DurationUnit(ChronoUnit.MILLIS)
        private Duration initialInterval = Duration.ofMillis(500);

        private double multiplier = 2.0;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public Duration getInitialInterval() {
            return initialInterval;
        }

        public void setInitialInterval(Duration initialInterval) {
            this.initialInterval = initialInterval;
        }

        public double getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(double multiplier) {
            this.multiplier = multiplier;
        }
    }

    public static class CacheProperties {

        private long maxSize = 500L;

        private Duration ttl = Duration.ofMinutes(5);

        public long getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(long maxSize) {
            this.maxSize = maxSize;
        }

        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }
    }
}
