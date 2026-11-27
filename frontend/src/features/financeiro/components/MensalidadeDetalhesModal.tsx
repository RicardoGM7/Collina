import { Modal } from '../../../components/Modal/Modal'
import type { Mensalidade, Pagamento } from '../../../types/financeiro'
import { formatCurrency, formatDateFull } from '../../../utils/formatters'
import { formatarDataBr } from '../../associados/associadoUtils'
import { formatarCompetencia, obterGatewayId } from '../mensalidadesUtils'
import { StatusMensalidadeBadge } from './StatusMensalidadeBadge'
import { StatusPagamentoBadge } from './StatusPagamentoBadge'
import { ValorMonetario } from './ValorMonetario'
import './mensalidadesShared.css'

interface MensalidadeDetalhesModalProps {
  mensalidade: Mensalidade | null
  pagamentos?: Pagamento[]
  carregandoPagamentos?: boolean
  onClose: () => void
}

export function MensalidadeDetalhesModal({
  mensalidade,
  pagamentos,
  carregandoPagamentos = false,
  onClose,
}: MensalidadeDetalhesModalProps) {
  if (!mensalidade) return null

  return (
    <Modal
      isOpen={mensalidade !== null}
      onClose={onClose}
      title={`Mensalidade #${mensalidade.id}`}
      subtitle={`Competência ${formatarCompetencia(mensalidade.competencia)}`}
      headerBadge={<StatusMensalidadeBadge status={mensalidade.status} />}
      maxWidth={540}
    >
      <div className="mens-modal-body">
        <div className="mens-detalhes-grid">
          <div className="mens-detalhe-item">
            <span>Competência</span>
            <strong>{formatarCompetencia(mensalidade.competencia)}</strong>
          </div>
          <div className="mens-detalhe-item">
            <span>Vencimento</span>
            <strong>{formatarDataBr(mensalidade.vencimento)}</strong>
          </div>
          <div className="mens-detalhe-item">
            <span>{mensalidade.status === 'EM_ABERTO' ? 'Total até agora' : 'Valor total'}</span>
            <strong>
              <ValorMonetario
                valor={mensalidade.valor}
                riscado={mensalidade.status === 'CANCELADA'}
              />
            </strong>
          </div>
          {mensalidade.valorBase !== undefined && (
            <div className="mens-detalhe-item">
              <span>Valor base</span>
              <strong>{formatCurrency(mensalidade.valorBase)}</strong>
            </div>
          )}
        </div>

        {mensalidade.gastosAvulsos && mensalidade.gastosAvulsos.length > 0 && (
          <div className="mens-detalhes-bloco">
            <h4>Gastos avulsos</h4>
            <ul className="mens-detalhes-lista">
              {mensalidade.gastosAvulsos.map((gasto) => (
                <li key={gasto.id}>
                  <span>{gasto.descricao}</span>
                  <strong>{formatCurrency(gasto.valor)}</strong>
                </li>
              ))}
            </ul>
          </div>
        )}

        {pagamentos !== undefined && (
          <div className="mens-detalhes-bloco">
            <h4>Pagamentos vinculados</h4>
            {carregandoPagamentos ? (
              <p className="mens-field-hint">Carregando pagamentos...</p>
            ) : pagamentos.length === 0 ? (
              <p className="mens-field-hint">Nenhum pagamento registrado para esta mensalidade.</p>
            ) : (
              <ul className="mens-detalhes-lista">
                {pagamentos.map((pag) => (
                  <li key={pag.id}>
                    <div>
                      <strong>{pag.formaPagamento}</strong>
                      <span className="mens-field-hint">
                        {' '}
                        · {pag.dataPagamento ? formatDateFull(pag.dataPagamento) : 'Sem data'}
                        {obterGatewayId(pag) ? ` · Ref: ${obterGatewayId(pag)}` : ''}
                      </span>
                    </div>
                    <div className="mens-detalhes-pag-dir">
                      <StatusPagamentoBadge status={pag.status} />
                      <ValorMonetario
                        valor={pag.valor}
                        riscado={pag.status === 'RECUSADO' || pag.status === 'EXPIRADO'}
                      />
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}

        <div className="mens-modal-actions">
          <button type="button" className="mens-btn-secondary" onClick={onClose}>
            Fechar
          </button>
        </div>
      </div>
    </Modal>
  )
}
