package service.meta

import scala.meta._

object CodeParser {
//  val code =
//    """class Main extends SolutionTrait {
//          def someMethod = println(1)
//
//          def anotherMethod(i: Int, a: Array): Int = {
//            1
//          }
//
//        }""".parse[Stat].get

  val cr = Term.Name("\r")
  val lb = Term.Name("\n")

  def getSolutionTemplate(code: String): String = {
    val parsed = code.parse[Stat].get
    parsed.transform {
      case q"..$mods def $name[..$tparams](...$paramss): $tpe = $expr" =>
        q"..$mods def $name[..$tparams](...$paramss): $tpe = { $cr} $cr $lb"
    }.syntax
  }
}
