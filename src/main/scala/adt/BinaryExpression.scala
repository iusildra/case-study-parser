package adt

sealed trait Expression:
  def +(that: Expression) = Add(this, that)

object Expression {
  def literal(value: Int): Expression = Literal(value)
  def variable(name: String): Expression = Variable(name)
}

final case class Literal(value: Int) extends Expression
final case class Variable(name: String) extends Expression
final case class Add(left: Expression, right: Expression) extends Expression