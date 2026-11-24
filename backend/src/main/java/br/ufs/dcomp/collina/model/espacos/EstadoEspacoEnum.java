package br.ufs.dcomp.collina.model.espacos;

// RF-ESP01: estado operacional do espaço. Somente espaços ATIVO aceitam novas reservas.
public enum EstadoEspacoEnum {
    ATIVO,
    EM_MANUTENCAO,
    INATIVO;
}
