/*
 * TermNormalizerTest.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package at.logic.algorithms.normalization

import org.specs._
import org.specs.runner._
import org.specs.matcher.Matcher

import at.logic.language.hol.propositions._
import at.logic.language.lambda.symbols.ImplicitConverters._
import at.logic.language.hol.logicSymbols._
import at.logic.language.lambda.types._
import scala.collection.immutable.EmptySet
import at.logic.parsing.readers.StringReader
import at.logic.parsing.language.simple.SimpleHOLParser
import scala.collection.mutable.Map

private class MyParser(input: String) extends StringReader(input) with SimpleHOLParser

class TermNormalizerTest extends SpecificationWithJUnit {
  val f1a = new MyParser("And P(z:(i->i)) Q(b:(i->i))").getTerm().asInstanceOf[Formula]
  val f1b = new MyParser("And P(x_1:(i->i)) Q(b:(i->i))").getTerm().asInstanceOf[Formula]
  val f2a = new MyParser("And P(f(x:i, y:i, a:i):(i->i), z:(i->i)) Q(Neg T(x:i, a:i, b:(i->i), g(x:i):i), Forall x1: (i -> (i -> i)) a(x1: (i -> (i -> i)), x: i, c1: (i -> i)))").getTerm().asInstanceOf[Formula]
  val f2b = new MyParser("And P(f(x_1:i, x_2:i, a:i):(i->i), x_3:(i->i)) Q(Neg T(x_1:i, a:i, b:(i->i), g(x_1:i):i), Forall x_4: (i -> (i -> i)) a(x_4: (i -> (i -> i)), x_1: i, c1: (i -> i)))").getTerm().asInstanceOf[Formula]

  "TermNormalizer" should {
    "normalize correctly the terms:" in {
      "1" in {
        var id = 0
        TermNormalizer(f1a,Map(), {id = id + 1; id}) must beEqual(f1b)
      }
      "2" in {
        var id = 0
        TermNormalizer(f2a,Map(), {id = id + 1; id}) must beEqual(f2b)
      }
    }
  }
}