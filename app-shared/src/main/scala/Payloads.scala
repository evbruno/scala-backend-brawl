package etc.rinha.shared

import java.util.UUID

object Payloads {

  private val rand = scala.util.Random

  def randomPessoaIn(prefix: String, id: Option[UUID] = None): (UUID, PessoaIn) =
    (
      id.getOrElse(UUID.randomUUID()),
      PessoaIn(
        randomApelido(prefix),
        "a name whatever",
        "1234-56-78",
        randomStack()
      )
    )

  def randomApelido(prefix: String, max: Int = 32): String = {
    val tailSize = max - prefix.length
    assert(tailSize > 1)

    val suffix = (1 to rand.nextInt(tailSize) + 1)
      .map(_ => rand.nextPrintableChar())
      .mkString
    prefix + suffix
  }

  def randomStack(): Option[List[String]] =
    if (rand.nextBoolean()) None
    else {
      val m = stacks.size / 2
      val a = rand.nextInt(m)
      val b = rand.nextInt(m) + 1
      Some(rand.shuffle(stacks.drop(a).take(b)))
    }

  private val stacks = List(
    "Postgres", "Swift", "Kotlin", "R", "C#", "Go", "Perl", "MySQL", "Clojure",
    "CSS", "Scala", "C", "HTML", "R", "Swift", "CSS", "Java", "Javascript",
    "SQL", "MySQL", "MySQL"
  )

}
