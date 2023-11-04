import parser.Failure
import parser.Success
import parser.Parser
import parser.Parser._

val typeParamSepParser = Parser.string(": ").orElse(Parser.string(":"))
val paramSepParser = Parser.string(", ").orElse(Parser.string(","))

val identParser = Parser.alphas.oneOrMore.combineAll
val paramParser = identParser.and(typeParamSepParser).and(identParser)

val paramListParser = paramParser
  .and(paramSepParser)
  .zeroOrMore
  .combineAll
  .and(paramParser)

val methodParser =
  Parser
    .string("def ")
    .and(identParser)
    .and(char('('))
    .and(paramListParser)
    .and(char(')'))
    .and(typeParamSepParser)
    .and(identParser)

methodParser.parse("def foo(x: Int, y: Int): Int") match
  case Success(res, _, _) =>
    res
  case f: Failure => "nop"

