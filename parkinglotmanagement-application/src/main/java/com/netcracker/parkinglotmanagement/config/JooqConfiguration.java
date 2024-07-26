package com.netcracker.parkinglotmanagement.config;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.RenderNameStyle;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

@Configuration
public class JooqConfiguration {
    @Bean("connectionProvider")
    @DependsOn("transactionAwareDataSource")
    @Qualifier("connectionProvider")
    public DataSourceConnectionProvider connectionProvider(@Autowired @Qualifier("transactionAwareDataSource") TransactionAwareDataSourceProxy transactionAwareDataSourceProxy) {
        return new DataSourceConnectionProvider(transactionAwareDataSourceProxy);
    }

    @Bean("jooqConfig")
    @Primary
    @DependsOn("connectionProvider")
    @Qualifier("jooqConfig")
    public org.jooq.Configuration configuration(
            @Autowired @Qualifier("connectionProvider") DataSourceConnectionProvider dataSourceConnectionProvider
    ) {
        DefaultConfiguration conf = new DefaultConfiguration();
        conf.set(dataSourceConnectionProvider);
        conf.set(SQLDialect.POSTGRES);
        return conf;
    }


    @Bean("dslContext")
    @DependsOn("jooqConfig")
    @Qualifier("dslContext")
    public DSLContext dsl(@Autowired @Qualifier("jooqConfig") org.jooq.Configuration configuration) {
        DSLContext context = new DefaultDSLContext(configuration);
        context.settings().setRenderNameStyle(RenderNameStyle.LOWER);
        context.settings().withReturnAllOnUpdatableRecord(true);
        return context;
    }
}
