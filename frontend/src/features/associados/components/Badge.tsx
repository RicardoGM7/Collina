interface BadgeProps {
  texto: string
  variante: 'verde' | 'amarelo' | 'cinza' | 'azul'
}

export default function Badge({ texto, variante }: BadgeProps) {
  return <span className={`as-badge as-badge-${variante}`}>{texto}</span>
}

export function varianteSituacao(situacao: 'ATIVO' | 'SUSPENSO' | 'INATIVO'): BadgeProps['variante'] {
  if (situacao === 'ATIVO') return 'verde'
  if (situacao === 'SUSPENSO') return 'amarelo'
  return 'cinza'
}
