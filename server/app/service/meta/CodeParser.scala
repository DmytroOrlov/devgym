package service.meta

import scala.meta._

object CodeParser {
  private val ls = System.getProperty("line.separator")
  val lb = Term.Name(ls)

  def getSolutionTemplate(code: String): String = {
    val parsed = code.parse[Stat].get
    parsed.transform {
      case q"..$mods def $name[..$tparams](...$paramss): $tpe = $expr" =>
        q"..$mods def $name[..$tparams](...$paramss): $tpe = {$lb}$lb $lb"
    }.syntax
      //remove existing line brakes from the passed code string
      .replace(s"$ls$ls", ls)
  }
}
