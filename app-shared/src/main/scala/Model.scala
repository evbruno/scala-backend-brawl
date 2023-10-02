package etc.rinha.shared

import java.util.UUID

case class PessoaIn(            // FIXME validar regras
  apelido: String,              // obrigatório, único, string de até 32 caracteres.
  nome: String,                 // obrigatório, string de até 100 caracteres.
  nascimento: String,           // obrigatório, string para data no formato AAAA-MM-DD (ano, mês, dia).
  stack: Option[Seq[String]]    // opcional, vetor de string com cada elemento sendo obrigatório e de até 32 caracteres.
) {
  assert(apelido.length <= 32, "apelido#length > 32")
  assert(nome.length <= 100, "nome#length > 100")
  assert(nascimento.length == 10, "nascimento != AAAA-MM-DD") // FIXME regex?
  assert(
    stack.isEmpty || stack.get.forall(_.length < 32),
    s"stack inválida: ${stack.mkString}")
}

case class PessoaOut(
  id: UUID,
  apelido: String,
  nome: String,
  nascimento: String,
  stack: Seq[String] = Nil
)

object PessoaOut {
  def apply(id: UUID, p: PessoaIn): PessoaOut =
    PessoaOut(
      id,
      p.apelido,
      p.nome,
      p.nascimento,
      p.stack.getOrElse(Nil)
  )
}

case class ApelidoEmUso(p: PessoaIn)
  extends RuntimeException(s"Duplicado: ${p.apelido}")
