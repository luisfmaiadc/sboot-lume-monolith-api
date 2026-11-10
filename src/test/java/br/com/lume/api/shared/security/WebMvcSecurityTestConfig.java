package br.com.lume.api.shared.security;

import br.com.lume.api.catalogo.mapper.CatalogoMapper;
import br.com.lume.api.estabelecimento.mapper.EstabelecimentoMapper;
import br.com.lume.api.identidade.mapper.UsuarioMapper;
import br.com.lume.api.ocorrencia.mapper.OcorrenciaMapper;
import br.com.lume.api.shared.config.CorsProperties;
import br.com.lume.api.shared.config.JwtProperties;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.mapstruct.factory.Mappers;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
@EnableConfigurationProperties({CorsProperties.class, JwtProperties.class})
class WebMvcSecurityTestConfig {

    static final Instant FIXED_INSTANT = Instant.parse("2026-11-09T10:00:00Z");
    static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

    @Bean
    Clock clock() {
        return FIXED_CLOCK;
    }

    @Bean
    CatalogoMapper catalogoMapper() {
        return Mappers.getMapper(CatalogoMapper.class);
    }

    @Bean
    UsuarioMapper usuarioMapper() {
        return Mappers.getMapper(UsuarioMapper.class);
    }

    @Bean
    EstabelecimentoMapper estabelecimentoMapper() {
        return Mappers.getMapper(EstabelecimentoMapper.class);
    }

    @Bean
    OcorrenciaMapper ocorrenciaMapper() {
        return Mappers.getMapper(OcorrenciaMapper.class);
    }
}
