package service.meta

import org.scalatest.{FlatSpec, Matchers}

import scala.io.Source

class CodeParserTest extends FlatSpec with Matchers {

  it should "return empty method bodies" in {
    //given
    val code = Source.fromInputStream(getClass.getResourceAsStream("solution.txt")).mkString
    val expectedTemplate = Source.fromInputStream(getClass.getResourceAsStream("expected_template.txt")).mkString

    //when
    val template = CodeParser.getSolutionTemplate(code)
    //then
    template should be(expectedTemplate)
  }

}
