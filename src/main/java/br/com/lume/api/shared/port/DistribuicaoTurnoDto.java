package br.com.lume.api.shared.port;

public record DistribuicaoTurnoDto(
        int madrugada,
        int manha,
        int tarde,
        int noite
) {}
