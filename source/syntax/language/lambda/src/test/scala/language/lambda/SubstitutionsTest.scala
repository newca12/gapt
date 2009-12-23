/*
 * SubstitutionsTest.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package at.logic.language.lambda

import org.specs._
import org.specs.runner._


import types._
import symbols._
import symbols.ImplicitConverters._
import typedLambdaCalculus._
import substitutions._
import types.Definitions._
import substitutions.ImplicitConverters._

class SubstitutionsTest extends SpecificationWithJUnit {
  level = Info  // sets the printing of extra information (level can be: Debug, Info, Warning, Error)
  "Substitutions" should {
    "make implicit conversion from pair to Substitution" in {
      val v = LambdaVar("v", i); val x = LambdaVar("x", i); val f = LambdaVar("f", i -> i)
      val e = App(f, x)
      val sigma: Substitution = (v,e)
      val eta = (v,e)
      ( Substitution(eta) ) must beEqual ( sigma )
    }
    "make implicit conversion from Substitution to pair" in {
      val v = LambdaVar("v", i); val x = LambdaVar("x", i); val f = LambdaVar("f", i -> i)
      val e = App(f, x)
      val eta = (v,e)
      val sigma = Substitution(eta)
      ( sigma: Tuple2[Var, LambdaExpression] ) must beEqual ( eta )
    }
    "substitute correctly when Substitution is applied (1)" in {
      val v = LambdaVar("v", i); val x = LambdaVar("x", i); val f = LambdaVar("f", i -> i)
      val e = App(f, x)
      val d = (v,e)
      val sigma: Substitution = Substitution(d)
      ( e ) must beEqual ( sigma(v) )
    }
    "substitute correctly when Substitution is applied (2)" in {
      val v = LambdaVar("v", i); val x = LambdaVar("x", i); val f = LambdaVar("f", i -> i)
      val e = App(f, x)
      val d = (v,e)
      val sigma: Substitution = d
      val expression = App(f, v)
      ( App(f, App(f, x)) ) must beEqual ( sigma(expression) )
    }
    "substitute correctly when Substitution is applied (3)" in {
      val v = LambdaVar("v", i); val x = LambdaVar("x", i); val f = LambdaVar("f", i -> i)
      val y = LambdaVar("y", i)
      val e = App(f, x)
      val sigma: Substitution = (v,e)
      val expression = Abs(y, App(f, v))
      ( Abs(y,App(f, App(f, x))) ) must beEqual ( sigma(expression) )
    }
            /*"substitute correctly when SingleSubstitution is applied, renaming bound variables (1)" in {
                val v = LambdaVar("v", i); val x = LambdaVar("x", i); val f = LambdaVar("f", i -> i)
                val e = App(f, x)
                val sigma: SingleSubstitution = (v,e)
                val exp1 = Abs(x, App(f, v))
                val exp2 = sigma(exp1)
                debug(exp2.toString)
                val exp3 = Abs(x,App(f, App(f, x)))
                val isDifferent = !(exp2==exp3)
                ( isDifferent ) must beEqual ( true )
            }
            "substitute correctly when SingleSubstitution is applied, renaming bound variables (2)" in {
                val v = LambdaVar("v", i); val x = LambdaVar("x", i); val f = LambdaVar("f", i -> i)
                val e = App(f, x)
                val sigma: SingleSubstitution = (v,e)
                val exp1 = Abs(f, App(f, v))
                val exp2 = sigma(exp1)
                debug(exp2.toString)
                val exp3 = Abs(f,App(f, App(f, x)))
                val isDifferent = !(exp2==exp3)
                ( isDifferent ) must beEqual ( true )
            }*/
    "concatenate/compose 2 Substitutions correctly" in {
      val v = LambdaVar("v", i); val x = LambdaVar("x", i); val f = LambdaVar("f", i -> i)
      val e = App(f, x)
      val sigma: Substitution = (v,e)
      val sigma1 = sigma::(Substitution())
      val sigma2 = sigma::sigma::(Substitution())
      val sigma3 = sigma1:::sigma1
      ( sigma2 ) must beEqual ( sigma3 )
    }
    "substitute correctly when Substitution is applied" in {
      val v = LambdaVar("v", i) 
      val x = LambdaVar("x", i)
      val f = LambdaVar("f", i -> i)
      val e = App(f, v)
      val sigma1: Substitution = (v,x)
      ( sigma1(e) ) must beEqual ( App(f,x) )
    }
  }
  "Substitution with regard to de-Bruijn indices" should {
    "not substitute for bound variables in (\\x.fx)x with sub {x |-> gy}" in {
      val term1 = App(Abs(LambdaVar("x",i), App(LambdaVar("f",i->i),LambdaVar("x",i))),LambdaVar("x",i))
      val sub: Substitution = (LambdaVar("x",i), App(LambdaVar("g",i->i),LambdaVar("y",i)))
      val term2 = App(Abs(LambdaVar("x",i), App(LambdaVar("f",i->i),LambdaVar("x",i))),App(LambdaVar("g",i->i),LambdaVar("y",i)))
      (sub(term1)) must beEqual (term2)
    }
    "not substitute for bound variables in (\\x.fx)x with sub {x |-> (\\x.fx)c}" in {
      "- 1" in {
        val term1 = App(Abs(LambdaVar("x",i), App(LambdaVar("f",i->i),LambdaVar("x",i))),LambdaVar("x",i))
        val sub: Substitution = (LambdaVar("x",i), App(Abs(LambdaVar("x",i), App(LambdaVar("f",i->i),LambdaVar("x",i))),LambdaVar("c",i)))
        val term2 = App(Abs(LambdaVar("x",i), App(LambdaVar("f",i->i),LambdaVar("x",i))),App(Abs(LambdaVar("x",i), App(LambdaVar("f",i->i),LambdaVar("x",i))),LambdaVar("c",i)))
        (sub(term1)) must beEqual (term2)
      }
      "- 2" in {
        val term1 = App(Abs(LambdaVar("x",i), App(LambdaVar("f",i->i),LambdaVar("x",i))),LambdaVar("x",i))
        val sub: Substitution = (LambdaVar("x",i), App(Abs(LambdaVar("x",i), App(LambdaVar("f",i->i),LambdaVar("x",i))),LambdaVar("c",i)))
        val term2 = App(Abs(LambdaVar("z",i), App(LambdaVar("f",i->i),LambdaVar("z",i))),App(Abs(LambdaVar("w",i), App(LambdaVar("f",i->i),LambdaVar("w",i))),LambdaVar("c",i)))
        (sub(term1)) must beEqual (term2)
      }
    }
    "recompute correctly indices when substituting a term with bound variables into the scope of other bound variables" in {
      val term1 = Abs(LambdaVar("x",i), App(LambdaVar("F",i->i),LambdaVar("x",i)))
      val sub: Substitution = (LambdaVar("F",i->i), Abs(LambdaVar("x",i), App(LambdaVar("f",i->i),LambdaVar("x",i))))
      val term2 = Abs(LambdaVar("x",i), App(Abs(LambdaVar("x",i), App(LambdaVar("f",i->i),LambdaVar("x",i))),LambdaVar("x",i)))
      (sub(term1)) must beEqual (term2)
    }
  }
}