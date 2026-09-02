// Spring Boot wiring for InvoiceXmlClient.
//
// The default RestClient request factory is fine for low volume. Under real batch load
// (dozens of invoices in flight at once) a pooled Apache HttpClient 5 factory keeps the
// calls genuinely parallel instead of queueing them behind a small connection limit.
//
// Timeouts are set on the connection manager rather than on the Spring factory, which
// works the same across Spring Framework 6.1 and 6.2+.
//
// Change the package to your own.

package com.example.invoicexml;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class InvoiceXmlConfig {

    @Bean
    public InvoiceXmlClient invoiceXmlClient(@Value("${invoicexml.api-key}") String apiKey) {
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(10))
                // Generous on purpose: validation runs the full KoSIT schematron and rendering
                // builds a whole PDF, so those are the slowest calls in the set.
                .setSocketTimeout(Timeout.ofSeconds(60))
                .build();

        PoolingHttpClientConnectionManager pool = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(connectionConfig)
                .setMaxConnTotal(100)
                .setMaxConnPerRoute(100)
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(pool)
                .build();

        var factory = new HttpComponentsClientHttpRequestFactory(httpClient);

        return new InvoiceXmlClient(RestClient.builder().requestFactory(factory), apiKey);
    }
}
