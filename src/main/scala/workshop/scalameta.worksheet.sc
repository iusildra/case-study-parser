import scala.meta.Source
import scala.meta.Stat
import scala.meta.*
import scala.meta.parsers.*

val field = "obj.field"
field.parse[Stat].get.structure
Term.Select(
  qual = Term.Name("obj"),
  name = Term.Name("field")
)


val method = "obj.method(a, b)"
method.parse[Stat].get.structure
Term.Apply(
  fun = Term.Select(
    Term.Name("obj"),
    Term.Name("method")
  ),
  argClause = Term.ArgClause(
    values = List(
      Term.Name("a"),
      Term.Name("b")
    ),
    mod = None
  )
)


val module = "object Module { val x = 42 }"
module.parse[Source].get.structure
Source(
  stats = List(
    Defn.Object(
      mods = Nil,
      name = Term.Name("Module"),
      templ = Template(
        early = Nil,
        inits = Nil,
        self = Self(
          Name(""),
          None
        ),
        stats = List(
          Defn.Val(
            mods = Nil,
            pats = List(Pat.Var(Term.Name("x"))),
            decltpe = None,
            rhs = Lit.Int(42)
          )
        ),
        derives = Nil
      )
    )
  )
)
