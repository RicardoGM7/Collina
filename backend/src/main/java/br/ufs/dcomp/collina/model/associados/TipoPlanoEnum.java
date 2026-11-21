package br.ufs.dcomp.collina.model.associados;

public enum TipoPlanoEnum {
    BASICO(CategoriaAssociadoEnum.CONTRIBUINTE),
    PLUS(CategoriaAssociadoEnum.CONTRIBUINTE),
    PREMIUM(CategoriaAssociadoEnum.CONTRIBUINTE),
    PROPRIETARIO(CategoriaAssociadoEnum.PROPRIETARIO);

    private final CategoriaAssociadoEnum categoriaPermitida;

    TipoPlanoEnum(CategoriaAssociadoEnum categoriaPermitida) {
        this.categoriaPermitida = categoriaPermitida;
    }

    public CategoriaAssociadoEnum getCategoriaPermitida() {
        return categoriaPermitida;
    }
}
