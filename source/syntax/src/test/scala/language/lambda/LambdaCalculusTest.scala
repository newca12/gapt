/*
 * LambdaCalculusTest.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package at.logic.language.lambda

import org.specs._
import org.specs.runner._


import Types._
import Symbols._
import Symbols.SymbolImplicitConverters._
import TypedLambdaCalculus._



class LambdaCalculusTest extends Specification with JUnit {
  "TypedLambdaCalculus" should {
    "make implicit conversion from String to Name" in {
        (Var[Lambda]("p",i) ) must beEqual (Var[Lambda]("p", i ))
    }
    "export lambda expressions to strings correctly (1)" in {
        val exp = Var[Lambda]("P", i)
        (exportLambdaExpressionToString(exp)) must beEqual ("P")
    }
    "export lambda expressions to strings correctly (2)" in {
        val exp = App(Var[Lambda]("P", i -> o), Var[Lambda]("x",i))
        (exportLambdaExpressionToString(exp)) must beEqual ("(P x)")
    }
    "export lambda expressions to strings correctly (3)" in {
        val exp1 = Var[Lambda]("x",i)
        val exp2 = App(Var[Lambda]("P", i -> o), exp1)
        val exp3 = Abs(exp1,exp2)
        (exportLambdaExpressionToString(exp3)) must beEqual ("\\x.(P x)")
    }
    "create N-ary abstractions (AbsN) correctly" in {
        val v1 = Var[Lambda]("x",i)
        val v2 = Var[Lambda]("y",i)
        val f = Var[Lambda]("f",i -> (i -> o))
        ( AbsN(v1::v2::Nil, f) match {
            case Abs(v1,Abs(v2,f)) => true
            case _ => false
            }) must beEqual ( true )
    }
    "create N-ary applications (AppN) correctly" in {
        val v1 = Var[Lambda]("x",i)
        val v2 = Var[Lambda]("y",i)
        val f = Var[Lambda]("f",i -> (i -> o))
        ( AppN(f, List(v1,v2)) match {
            case App(App(f, v1), v2) => true
            case _ => false
            }) must beEqual ( true )
    }

  }
}