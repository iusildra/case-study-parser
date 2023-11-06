# Workshop

## 1. Introduction

### 1.1 Parsers & ADT

Un parser est un programme qui analyse un texte pour en déterminer la structure grammaticale. Généralement, on prend une chaîne de caractère et on retourne une structure de données représentant le texte (qui sera un type de donnée algébrique, ou ADT en anglais).

Un ADT est un type "composite", c'est à dire qu'il est formé par la combinaison d'autre types. Un exemple d'ADT est la liste

```scala
type List[A] = Nil | Cons[A]
```

Pour un exemple d'ADT plus complet, on peut prendre un interpréteur basique d'expression:

```scala
sealed trait Tree:
  def `type`: Type

sealed trait NonEvaluable extends Tree
sealed trait Evaluable extends Tree

case class ThisTree(`type`: Type) extends Evaluable
case class LiteralTree(value: AnyVal) extends Evaluable

case class LocalVarTree(variable: LocalVariable) extends Evaluable
case class FieldTree(field: Field, owner: Tree) extends Evaluable
case class MethodTree(method: Method, args: Seq[Evaluable], owner: Tree) extends Evaluable

case class ModuleTree(`type`: ClassType) extends Evaluable
case class ClassTree (`type`: ClassType) extends NonEvaluable

case class IfTree(p: Evaluable, thenp: Evaluable, elsep: Evaluable) extends Evaluable
```

On a donc le type général `Tree`, qui est soit evaluable, soit non evaluable (on ne peut pas évaluer une classe, par exemple). Un attribut appartient à un module ou une classe, qui peut être retourné par un accès direct à sa référence, ou bien par le résultat d'une méthode. Même chose pour la méthode avec ses arguments un if peut être représenté par 3 champs, la condition, les 2 branches, plus éventuellement le type de retour

- Par example, dans un cas très simple où on veut obtenir un field, on selectionne le field d'un objet
- Dans un cas un peu moins simple, avec un méthode à 2 arguments, on selectionne la méthode d'une object et on a sa liste d'arguments
- Enfin, dans un cas plus complet, avec la déclaration d'un module, on a beaucoup plus d'informations: les modifiers (private...), son nom, ses superclasse/traits, et son body (qui est une liste de déclarations). Dans ce body, on a une définition de value, avec aucun modifier, un nom, aucun type déclaré, et une expression (qui est un arbre, j'aurai pu mettre un bloc de code à la place et ça aurait quand même fonctionné)

Un parser a donc besoin d'une structure de donnée pour représenter la chaîne de caractères, et l'ADT permet d'apporter une bonne modularité. Et comme un parser peut échouer, on a besoin d'un type représentant le résultat.

```scala
sealed trait Tree
/* branches & leaves */

sealed trait Result[A]
case class Success[A](value: A) extends Result[A]
case class Failure(message: String) extends Result[Nothing]

sealed trait Parser[A]:
  def parse(input: String): Result[A]

class SourceParser extends Parser[Tree]:
  def parse(input: String): Result[Tree] = ???
```

Voici le code de base pour un parser.

Alors maintenant, vous pourriez me dire pourquoi s'embêter à utiliser un autre ADT pour représenter le résultat et ne pas renvoyer directement la structure ou jeter une exception/renvoyer un null à la place ? Ça fait du code en plus, il y a du boxing, etc...

Pour le null, c'est tout simplement parce que renvoyer une valeur qui peut être nulle, c'est la porte ouverte aux NullPointerExceptions inattendues. On peut faire des nullity checks partout, mais si on en oublie un... Et puis, ce n'est pas très expressif, on ne sait pas pourquoi on a obtenu un null, alors qu'en fonction de la raison on voudrait éventuellement recouvrer une valeur par défaut.

Si on utilise des exceptions, ce n'est pas forcément mieux, parce que déjà c'est pas pratique à écrire et il y a des impacts importants sur les performances (de l'ordre du x300). Entre autre parce que la JVM a besoin de collecter la stack d'appel et c'est ce qui rend l'instanciation de l'exception si chère.

| Benchmark            | Mode | Cnt | Score  | Error   | Units |
| -------------------- | ---- | --- | ------ | ------- | ----- |
| No exceptions        | avgt | 10  | 0.046  | ± 0.003 | ms/op |
| Throw & catch        | avgt | 10  | 16.268 | ± 0.239 | ms/op |
| Throw & no catch     | avgt | 10  | 17.874 | ± 3.199 | ms/op |
| Throw w/o stacktrace | avgt | 10  | 1.174  | ± 0.014 | ms/op |

(source: [Baeldung Java Exceptions Performance](https://www.baeldung.com/java-exceptions-performance))

Ce qui dit ce benchmark, c'est que jetter une exception est 300x plus lent que de ne pas en jeter. Mais maintenant on pourrait se dire que c'est peut-être le try/catch et non pas l'exception le problème. Et bien non, parce que même si on ne jette pas l'exception, on retrouve des performances similaire à un try/catch, ce qui montre bien que le problème vient de l'instanciation de l'exception.

En revanche, si on jette ce qu'on appelle une "fast-exception" où on ne collecte pas les informations de la stack d'appel, on a des performances "seulement" 25x pire que sans exception.

Voilà donc pourquoi on va utiliser un ADT pour représenter le résultat, et non pas une exception ou un null.

### 1.2 Parser combinator

Un parser combinator est un parser qui est construit à partir d'autres parsers (d'où le nom). L'avantage de cette approche est qu'on peut construire des petits parsers unitaires (très facile à tester) et les combiner pour construire des parsers plus complexes.

Par exemple, si on prend le code suivant

```scala
package lulu

object Main {
  val x = 1
  val y = 2

  val adder = (a: Int, b: Int): Int = a + b

  def main(args: Array[String]): Unit =
    println(foo(x, y))

  def foo(x: Int, y: Int) = x + y
}
```

Qu'est ce que vous verrez comme parsers ? Personnellement, je vois:

| Parser    | Description                      | Example                               |
| --------- | -------------------------------- | ------------------------------------- |
| `name`    | Un nom                           | `x`, `y`, `main`, `foo`               |
| `literal` | Une valeur litérale              | `1`, `2`                              |
| `param`   | Un paramètre de méthode/fonction | `a: Int`, `args: Array[String]`       |
| `method`  | Une définition de méthode        | `def main(args: Array[String]) = ???` |
| `func`    | Une définition de fonction       | `val adder = (a: Int, b: Int) = ???`  |
| `val`     | Une définition de valeur         | `val x = 1`                           |
| `class`   | Une définition de classe         | `object Main`                         |
| `source`  | Un fichier source                |                                       |

![parser structure](name.png)

Grosso modo, ici j'ai voulu représenter la structure du parser. D'ailleurs, j'aurai dû couper `BodyParser` en plusieurs. Donc par exemple pour définir le parser d'une méthode, on va avoir besoin d'un parser de paramètres, d'un parser de body qui contient lui même les parsers de définitions de valeurs, de fonctions, etc...

### 1.3 Réification

On va partir sur une implémentation par réification (c.a.d. concrétiser une notion abstraite, ici on va transformer les parsers en objets). Dans l'idée on divise nos méthodes en 3 catégories:

- les constructeurs: ils permettent d'obtenir une valeur contenue dans notre algèbre à partir d'une valeur qui n'en fait pas partie.
- les combinateurs: ils permettent de combiner des parsers pour en créer de nouveaux plus généraux.
- les interpréteurs: ils permettent d'obtenir une valeur à partir de l'ADT qu'on vient de créer avec les constructeurs & combinateurs

Par exemple

```scala
trait Parser[A]:
  def parse(input: String): Result[A]     // interpréteur
  def and(parser: Parser[A]): Parser[A]   // combinateur
object Parser:
  def string(str: String): Parser[String] // constructeur
```

Ensuite, on va reifier les constructeurs et combinateurs dans notre ADT, pour enfin pouvoir implémenter l'interpréteur récursivement. Pour chaque méthode, on va donc avoir une `final case class` dont:

- les paramètres sont les mêmes que la méthode qu'on veut réifier
- le type étendu est le même que celui que retourne la méthode qu'on veut réifier

Pour parser un if

```scala
case class IfTree(p: Predicate, thenp: Tree, elsep: Tree)

case class IfParser(p: Parser[Predicate], thenp: Parser[Tree], elsep: Parser[Tree]) extends Parser[IfTree]

```

## 2. Design avec les type classes

### 2.0 Introduction

Une type class est une façon de découpler le coeur d'une classe de ses comportements. Plutôt que de définir les comportements dans la classe, ce qui rendrait le code trop rigide et peu modulable, on définit ces comportements dans une classe à part, et on les implémente au besoins. Si on compare les 2 approches, on a:

- La façon "habituelle" en Java: définir les comportements dans une interface et étendre les classes avec ces interfaces. Problème si on fait ça pour toutes les comportement, on se retrouve avec des classes énormes et un code très rigide (besoin d'un nouveau comportement => étendre des classes, réutilisation de comportements unitaires compliquée). Un autre problème est qu'on peut combiner un chien avec un oiseau puisque `combine` prend un `Animal`... je plains l'oiseau

```scala
trait Encoder:
  def encode(): String
trait Decoder[+A]:
  def decode(s: String): A
trait Combiner[-A]:
  def combine(b: A): String

abstract class Animal extends Encoder with Decoder[Animal] with Combiner[Animal]
case class Dog() extends Animal:
  override def encode(): String
  override def decode(s: String): Dog
  override def combine(b: Animal): String
case class Cat() extends Animal:
  override def encode(): String
  override def decode(s: String): Cat
  override def combine(b: Animal): String
case class Bird() extends Animal:
  override def encode(): String
  override def decode(s: String): Bird
  override def combine(b: Animal): String
```

- Vous implémentez les comportements dans des classes à part, en gardant le coeur de la logique métier dans la classe et vous passez ces implémentations en paramètre. C'est mieux, on découple les comportements et on peut facilement changer d'implémentation. Problème, on aimerait bénéficier de l'expressivité de la première forme et pouvoir appeler `cat.encode()` directement

```scala
trait Encoder[-A]:
  def encode(a: A): String
trait Decoder[+A]:
  def decode(s: String): A
trait Combiner[-A]:
  def combine(a: A, b: A): String

abstract class Animal
case class Dog() extends Animal
case class Cat() extends Animal
case class Bird() extends Animal

object Client:
  val catEncoder: Encoder[Cat] = ???
  val cat = Cat()
  catEncoder.encode(cat)
```

Dans le premier cas, la difficulté est surtout de maintenir le code et de le faire évoluer, tandis que dans le 2e cas, la difficulté est surtout dans l'exploration du code (on n'aura pas `catEncoder` dans les suggestions de l'IDE). Scala propose une solution permettant de combiner les 2 approches pour avoir le meilleur des 2 mondes: les implicits et extensions methods.

```scala
extension [A](a: A)
  def encode(using encoder: Encoder[A]): String = encoder.encode(a)
  def combine(b: A)(using combiner: Combiner[A]): String = combiner.combine(a, b)

given Encoder[Cat] with
  def encode(a: Cat): String = "A cat"
given Encoder[Animal] with
  def encode(a: Animal): String = "An animal"

aCat.encode // "A cat"
aDog.encode // "An animal"
```

Cette solution nous permet de garder l'aspect "dev-friendly" de l'héritage, en s'affranchisant de la rigité inhérente. L'avantage des implicites ici est aussi de ne pas polluer le code avec des paramètres lié au contexte qui ne vont pas changer en cours de route.

### 2.1 Définition d'une TC

L'implémentation d'une type class se fait en 4 étapes

1. Définir le contrat de la type class avec un trait

```scala
trait Honker[T]:
  def honk: String
```

2. Définir les méthodes dépendantes de ce comportement, et ne pas oublier de déclarer le paramètre de la TC en tant qu'implicite sous peine d'avoir à l'écrire partout

```scala
def hooonk[T](t: T)(using honker: Honker[T]): String = honker.honk
```

3. Définir les instances de la type class

```scala
given Honker[Car] with
  def honk: String = "honk honk"
given Honker[Boat] with
  def honk: String = "hoooooonk"
```

4. (optionnel) Définir les extensions methods

```scala
extension [T](t: T)(using honker: Honker[T])
  def honk: String = honker.honk
```

2 petits exercices pour vous. Le premier est d'implémenter une type class `JsonEncoder` qui permet de transformer un objet en JSON, et une class `Person(name: String, age: Int, address: String, town: String)`, je vous laisse 5-10min pour le faire
Le deuxième est de créer une type class `JsonEncoder` pour une liste (et donc ici une liste de personne). Donc comme vous avez défini une instance de `JsonEncoder` pour `Person`, vous devez le faire pour `List`, je vous laisse 5min pour le faire

Donc voici le rendu final, on a besoin de déclarer une instance de `JsonEncoder` pour une liste de personne, mais comme on a déjà déclaré une instance de `JsonEncoder` pour `Person`, elle est automatiquement utilisée par le compilateur. Si on veut rendre cette instance disponible pour une liste de n'importe quelle type, il faut modifier le given de `JsonEncoder[List[Person]]` pour le rendre générique.

Je n'ai pas encore sauvegardé, est ce que ce code compile? Oui/non et pourquoi ?
On rajoute donc une contrainte à ce given: il doit avoir une instance de `JsonEncoder[A]` dans le scope. Donc on le rajoute en tant que paramètre implicite. Mais comme on a définit plus haut une méthode d'extension (comme en Swift) qui permet de pas avoir besoin d'appeler directement l'instance, on peut raccourcir encore plus le code.

A titre personnel, j'adore les type-class (qui ont eu de bonnes améliorations de perf avec Scala 3), on peut écrire un code très expressif et facilement maintenable. Si on reste sur notre `JsonEncoder`, et qu'on veut encoder un type un peu plus complexe, par exemple une `List[Option[T]]`

```scala
// Type de bases
case class Person(name: String, age: Int, address: String, town: String)
trait JsonEncoder[-T]:
  def encode(t: T): String

// Extension methods par commodité
extension [T](t: T)(using encoder: JsonEncoder[T])
  def encode: String = encoder.encode(t)

// Given instances
given JsonEncoder[Person] with
  def encode(p: Person): String = s"""{"name": "${p.name}", "age": ${p.age}, "address": "${p.address}", "town": "${p.town}"}"""

given [T: JsonEncoder]: JsonEncoder[Option[T]] with
  def encode(o: Option[T]): String =
    o match
      case Some(t) => t.encode
      case None => "null"

given [A: JsonEncoder]: JsonEncoder[List[A]] with
  def encode(list: List[A]): String =
    list.map(_.toJson).mkString("[", ",", "]")

// Test
val list = List(Some(Person("John", 42, "1 rue de la paix", "Paris")), None)
list.encode // "[{"name": "John", "age": 42, "address": "1 rue de la paix", "town": "Paris"},null]"
```

### 2.2 Functor

On est bon pour les petits rappels sur les bases des TC en Scala. Maintenant imaginez que vous avez une API qui peut renvoyer divers containers (des listes, des options, des map...) et que vous voulez les transformer.

```scala
def transformOption[A, B](option: Option[A])(f: A => B): Option[B] = option.map(f)
def transformTry[A, B](t: Try[A])(f: A => B): Try[B] = t.map(f)
def transformIO[A, B](io: IO[A])(f: A => B): IO[B] = io.map(f)
// ...
```

Pour abstraire un peu tout ça, on a le Functor, un TC. L'interface du functor est très simple, c'est juste une méthode `map`. Le `Functor` est une type class abstrayant la notion de transformation de valeur dans un container (une list, une option...).

```scala
trait Functor[F[_]]:
  def map[A, B](fa: F[A])(f: A => B): F[B] // => map[A, B](fa: F[A], f: A => B): F[B]
```

Certains (si ce n'est tous) se demandent "c'est quoi cette chose?"

1. D'abord on a ce truc bizarre `F[_]` qui signifie que `Functor` prend un type générique qui est lui-même générique (on appelle aussi ça des Higher-Kinded Types). On peut donc par exemple avoir `Functor[List]`, `Functor[Array]`, mais pas `Functor[Int]`. Dans la suite j'appelerai ce type `F[_]` un container, juste parce qu'il contient d'autres types
2. La méthode `map` est un peu différente de celle dont on a l'habitude. Elle prend un container et va appliquer la fonction à ce container, alors qu'habituellement on n'a besoin de passer que la function en paramètre. Par exemple, si on veut appliquer une fonction à une liste, on fait `list.map(f)`, alors que là on fait `functor.map(list)(f)`. C'est un peu plus verbeux, mais il y a moyen de le rendre moins verbeux et plus expressif, on verra ça plus tard

Avec le `Functor`, on peut définir une méthode générique qui va fonctionner pour tous les containers

```scala
def transform[F[_], A, B](container: F[A])(f: A => B)(using functor: Functor[F]): F[B] = functor.map(container)(f)
```

Et encore mieux, en utilisant les extensions methods, on peut rendre le code encore plus expressif et moins verbeux

```scala
extension [T](fa: F[T])(using functor: Functor[F])
  def fmap[B](f: T => B): F[B] = functor.map(fa)(f)
```

Ainsi, vous pourrez continuer à utiliser votre API avec n'importe quel container (à condition de fournir les `given` correspondants). Et encore plus fort, vous pouvez changer de container sans rien casser en changeant le `given` correspondant (ici on est sur des abstractions très haut niveau qui sont utiles pour les librairies, mais qui rendraient le code très verbeux pour un usage courant)

### 2.3 Monoid

Si on veut combiner 2 valeurs, on peut utiliser un monoid, et réellement on le fait tous les jours, dès qu'on fait `1+3` ou même `"abc"+"def"`. C'est est une structure algébrique avec une opération de combinaison et un élément neutre (0 pour les `int`, `""` pour les string...). Si on ne dispose que de l'opération de combinaison, on appelle ça un semigroup. Les cas d'usages des monoids, est d'appliquer des réductions à un jeu de données, en abstrayant la structure de donnée utilisées

```scala
trait Monoid[A] {
  def combine(a1: A, a2: A): A
  def empty: A
}

extension [A](fa: Seq[A])(using monoid: Monoid[A])
  def combineAll = fa.foldLeft(monoid.empty)(monoid.combine)

List(1, 2, 3).combineAll        // 6
Vector(1, 2, 3).combineAll      // 6
Array("a", "b", "c").combineAll // "abc"
```

Bon, c'est bien joli, mais quel est l'intérêt pour des parsers? Pouvoir combiner 2 parsers permet de faire 2 choses:

- Essayer de parser avec le premier parser, et si ça échoue, essayer avec le deuxième. C'est juste un ou logique. En Scala on appelle l'alternative `orElse` par convention
- Parser un élément avec le premier parser, puis avec le deuxième et combiner les résultats. C'est un et logique

Le premier est obligatoire si on veut construire un parser général à base de briques plus petites, le deuxième est plus optionnel et on ne va pas le couvrir car je suis assez limité en temps.

Maintenant, vous allez pouvoir implémenter le `orElse`, je vous laisse 5min pour le faire

### 2.4.0 Semigroupals (optionnel)

And not semigroups (which only define a combine method)

Les semigroupals définissent une méthode `product` qui permet de combiner 2 valeurs dans un container.

```scala
trait Semigroupal[F[_]]:
  def product[A, B](fa: F[A], fb: F[B]): F[(A, B)]
```

Il existe 2 types de combinaisons différents:

- Les combinaisons monadiques (où le container est une monade) où l'implémentation se fait à base à map/flatMap. Ça donne un produit cartésien, mais si une des deux valeurs est vide (`Nil`, `None`...) alors le résultat est vide. Et si les 2 valeurs sont vides, alors seule la première valeur est conservée
- Les combinaisons applicatives (où le container est une applicative (on la voit juste après)) où l'implémentation se fait différemment (elle est hyper abstraite, ça ne servirait à rien de la montrer et surtout je ne l'ai que partiellement comprise) et même si les 2 valeurs sont vides, on les combine quand même. Ca permet d'accumuler les erreurs par example plutôt que de s'arrêter à la première

```scala
val aTupledList  = Semigroupal[List].product(
  List(1, 2),
  List(3, 4)
) // List((1, 3), (1, 4), (2, 3), (2, 4))
val aTupledList2 = Semigroupal[List].product(
  List(1, 2),
  Nil
) // List()

type ErrorsOr[A] = Validated[List[String], A]
val validated = Semigroupal[ErrorsOr].product(
  Validated.invalid(List("Badness")),
  Validated.invalid(List("Fail"))
) // Invalid(List("Badness", "Fail"))
val validated2 = Semigroupal[ErrorsOr].product(
  Validated.invalid(List("Badness")),
  Validated.valid(47),
) // Invalid(List("Badness"))
```

### 2.4.1 Applicative

L'applicative est une extension du `Functor` (qui définie la méthode `map`) et définit les méthodes `pure` et `ap` (pour applicative, le nom est pourri). La méthode `pure` permet de créer un container à partir d'une valeur, et la méthode `ap` permet de définir la méthode `product` du `Semigroupal`. Cette méthode fait de la magie noire, mais en gros, elle permet de combiner 2 containers en un seul, en appliquant la fonction contenue dans le premier container à la valeur contenue dans le deuxième container.

| fa  | fb  | f                | ap(fa)(a => b => (a,b)) |
| --- | --- | ---------------- | ----------------------- |
| 1   | 2   | 1 => b => (1, b) | (1,2)                   |
| 1   | 3   | 1 => b => (1, b) | (1,3)                   |
| 2   | 2   | 2 => b => (2, b) | (2,2)                   |
| 2   | 3   | 2 => b => (2, b) | (2,3)                   |

Bon, cette sorcellerie mise à part, quel est l'intérêt des applicatives ? Grâce à `product` on peut produire des séquences de parsers pour les utiliser "d'un seul bloc".`pure` est moins utile car elle renvoit toujours la valeur fournie en input, mais le reste dans certains cas.

### 2.5 Monad

Un monad est une autre structure algébrique permettant de chaîner des opérations. La différence avec le `Functor` est que le `Functor` permet de faire des transformations (il prend une valeur et la transforme en une autre), alors que le `Monad` permet de chaîner les transformations (il prend le résultat d'une transformation et effectue une nouvelle transformation).

```scala
// Parse "number: 100" or "string: 'hello'" and return the value
val fieldParser: Parser[String]   = ???   // parse <type>: and returns <type>
val intParser: Parser[Int]        = ???
val stringParser: Parser[String]  = ???

fieldParser.flatMap {
  case "number" => intParser
  case "string" => stringParser
}
```

Par ailleurs, le `Monad` possède aussi une méthode `pure` et `product`.

Je vous laisse 5-10min pour implémenter `flatMap`

### 2.6 Kleene Star

C'est un nom bien compliqué pour une fonctionnalité très simple: répéter 0 ou plusieurs fois (même signification que l'étoile en regex). Pourquoi on en a besoin ? Si on a un nombre (composé d'un nombre indéterminé de chiffres), on aimerait pouvoir le parser correctement sans avoir à écrire un parser pour chaque nombre de chiffres possible. Et c'est là qu'intervient la Kleene Star.

## X. Inspirations

Merci à Noel Welsh pour son livre [Creative Scala](https://www.creativescala.org) dont je me suis grandement inspiré pour la structure de cet exercice (avec son accord préalable)
