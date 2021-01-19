package com.demandware.carbonj.service.engine;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConfigServerUtil {

    private static Logger log = LoggerFactory.getLogger( ConfigServerUtil.class );

    private volatile List<RelayRule> relayRules;

    // TODO-SRH: pull this from config server
    private volatile List<RelayRule> auditRules = Collections.EMPTY_LIST;

    private final RestTemplate restTemplate;

    private final String registrationUrl;

    private final String metricPrefix;

    private final MetricRegistry metricRegistry;

    private final Meter registrationSuccess;

    private final Meter registrationFailure;

    public ConfigServerUtil(RestTemplate restTemplate, String configServerBaseUrl, String metricPrefix,
                            MetricRegistry metricRegistry) {
        this.restTemplate = restTemplate;
        this.registrationUrl = String.format("%s/rest/v1/relays/register", configServerBaseUrl);
        this.metricPrefix = metricPrefix;
        this.metricRegistry = metricRegistry;
        this.registrationFailure = metricRegistry.meter(MetricRegistry.name("configServer", "registration", "failed"));
        this.registrationSuccess = metricRegistry.meter(MetricRegistry.name("configServer", "registration", "success"));
        log.info("Config server registry initialised. Registration URL {}", registrationUrl);
        register();
    }

    public synchronized void register() {
        try {
            final ResponseEntity<Registration> res = restTemplate.postForEntity(registrationUrl, getRegistration(),
                    Registration.class);
            if (res.getStatusCode().value() >= 200 && res.getStatusCode().value() < 300) {
                Registration registration = res.getBody();
                this.relayRules = Stream.concat(registration.getSpecificRelayRules().stream(),
                        registration.getGenericRelayRules().stream()).collect(Collectors.toList());
                registrationSuccess.mark();
                log.info("Config server registration success. Registration: {}", registration);
            } else {
                log.error("Config server registration failed. URL: {}, Response status code: {}", registrationUrl,
                        res.getStatusCodeValue());
                registrationFailure.mark();
            }
        } catch (Exception e) {
            log.error("Unexpected error during config server registration. URL: " + registrationUrl, e);
            registrationFailure.mark();
        }
    }

    public List<RelayRule> getRelayRules() {
        return relayRules;
    }

    public List<RelayRule> getAuditRules() {
        return auditRules;
    }

    private Registration getRegistration() {
        // TODO
        // metric prefix
        // host
        // avgMetricVolume
        // env
        // infrastructure
        return new Registration(metricPrefix, "sholavanall-ltm.internal.salesforce.com", 100, "prd", "1P");
    }

    public static class Registration {

        private String metricPrefix;

        private String host;

        private long avgMetricVolume;

        private String env;

        private String infrastructure;

        private List<RelayRule> specificRelayRules;

        private List<RelayRule> genericRelayRules;

        public Registration() {
        }

        public Registration(String metricPrefix, String host, long avgMetricVolume, String env, String infrastructure) {
            this.metricPrefix = metricPrefix;
            this.host = host;
            this.avgMetricVolume = avgMetricVolume;
            this.env = env;
            this.infrastructure = infrastructure;
            this.specificRelayRules = Collections.EMPTY_LIST;
            this.genericRelayRules = Collections.EMPTY_LIST;
        }

        public String getMetricPrefix() {
            return metricPrefix;
        }

        public void setMetricPrefix(String metricPrefix) {
            this.metricPrefix = metricPrefix;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public long getAvgMetricVolume() {
            return avgMetricVolume;
        }

        public void setAvgMetricVolume(long avgMetricVolume) {
            this.avgMetricVolume = avgMetricVolume;
        }

        public String getEnv() {
            return env;
        }

        public void setEnv(String env) {
            this.env = env;
        }

        public String getInfrastructure() {
            return infrastructure;
        }

        public void setInfrastructure(String infrastructure) {
            this.infrastructure = infrastructure;
        }

        public List<RelayRule> getSpecificRelayRules() {
            return specificRelayRules;
        }

        public void setSpecificRelayRules(List<RelayRule> specificRelayRules) {
            this.specificRelayRules = specificRelayRules;
        }

        public List<RelayRule> getGenericRelayRules() {
            return genericRelayRules;
        }

        public void setGenericRelayRules(List<RelayRule> genericRelayRules) {
            this.genericRelayRules = genericRelayRules;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Registration that = (Registration) o;
            return avgMetricVolume == that.avgMetricVolume &&
                    Objects.equals(metricPrefix, that.metricPrefix) &&
                    Objects.equals(host, that.host) &&
                    Objects.equals(env, that.env) &&
                    Objects.equals(infrastructure, that.infrastructure) &&
                    Objects.equals(specificRelayRules, that.specificRelayRules) &&
                    Objects.equals(genericRelayRules, that.genericRelayRules);
        }

        @Override
        public int hashCode() {
            return Objects.hash(metricPrefix, host, avgMetricVolume, env, infrastructure, specificRelayRules,
                    genericRelayRules);
        }

        @Override
        public String toString() {
            return "Registration{" +
                    "metricPrefix='" + metricPrefix + '\'' +
                    ", host='" + host + '\'' +
                    ", avgMetricVolume=" + avgMetricVolume +
                    ", env='" + env + '\'' +
                    ", infrastructure='" + infrastructure + '\'' +
                    ", specificRelayRules=" + specificRelayRules +
                    ", genericRelayRules=" + genericRelayRules +
                    '}';
        }
    }

    public static class RelayRule {

        private String regex;

        private String destination;

        public RelayRule() {
        }

        public RelayRule(String regex, String destination) {
            this.regex = regex;
            this.destination = destination;
        }

        public String getRegex() {
            return regex;
        }

        public void setRegex(String regex) {
            this.regex = regex;
        }

        public String getDestination() {
            return destination;
        }

        public void setDestination(String destination) {
            this.destination = destination;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RelayRule relayRule = (RelayRule) o;
            return Objects.equals(regex, relayRule.regex) &&
                    Objects.equals(destination, relayRule.destination);
        }

        @Override
        public int hashCode() {
            return Objects.hash(regex, destination);
        }

        @Override
        public String toString() {
            return "RelayRule{" +
                    "regex='" + regex + '\'' +
                    ", destination='" + destination + '\'' +
                    '}';
        }
    }
}
