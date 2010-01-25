/*
 * SimpleResolutionParserTest.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package at.logic.parsing.calculi.simple

import org.specs._
import org.specs.runner._
import at.logic.language.hol.propositions._
import at.logic.language.hol.quantifiers._
import at.logic.language.hol.propositions.TypeSynonyms._
import at.logic.language.hol.propositions.Definitions._
import at.logic.language.hol.propositions.ImplicitConverters._
import at.logic.language.lambda.typedLambdaCalculus._
import at.logic.language.fol.{FOLVar, FOLConst, Atom => FOLAtom, Function => FOLFunction}
import at.logic.language.hol.logicSymbols.ConstantStringSymbol
import at.logic.language.lambda.symbols.VariableStringSymbol
import at.logic.language.lambda.types.ImplicitConverters._
import at.logic.language.lambda.types.Definitions._
import at.logic.language.lambda.types._
import at.logic.language.lambda.symbols.ImplicitConverters._
import at.logic.parsing.readers.StringReader
import at.logic.calculi.resolution.base._
import at.logic.language.hol.propositions.Definitions._
import at.logic.language.hol.propositions.ImplicitConverters._

class SimpleResolutionParserTest extends SpecificationWithJUnit {
  private class MyParser(input: String) extends StringReader(input) with SimpleResolutionParserHOL
  private class MyParser2(input: String) extends StringReader(input) with SimpleResolutionParserFOL

  val pa = Atom(ConstantStringSymbol("p"),Var(ConstantStringSymbol("a"), i, hol)::Nil)
  val pfx = Atom(ConstantStringSymbol("p"),Function(ConstantStringSymbol("f"), Var(VariableStringSymbol("x"), i, hol)::Nil,i)::Nil)
  val px = Atom(ConstantStringSymbol("p"),Var(VariableStringSymbol("x"), i, hol)::Nil)
  val pffa = Atom(ConstantStringSymbol("p"),Function(ConstantStringSymbol("f"),Function(ConstantStringSymbol("f"), Var(ConstantStringSymbol("a"), i, hol)::Nil,i)::Nil, i)::Nil)
  val pa_fol = FOLAtom(ConstantStringSymbol("P"),FOLConst(ConstantStringSymbol("a"))::Nil)
  val pfx_fol = FOLAtom(ConstantStringSymbol("P"),FOLFunction(ConstantStringSymbol("f"), FOLVar(VariableStringSymbol("x"))::Nil)::Nil)
  val px_fol = FOLAtom(ConstantStringSymbol("P"),FOLVar(VariableStringSymbol("x"))::Nil)
  val pffa_fol = FOLAtom(ConstantStringSymbol("P"),FOLFunction(ConstantStringSymbol("f"),FOLFunction(ConstantStringSymbol("f"), FOLConst(ConstantStringSymbol("a"))::Nil)::Nil)::Nil)
  "SimpleResolutionParser" should {
    "return an empty clause when given ." in {
      (new MyParser(".").getClauseList) must beEqual (Clause(Nil,Nil)::Nil)
    }
    "return an empty list when given nothing" in {
      (new MyParser("").getClauseList) must beEqual (Nil)
    }
    "return the correct clause of p(a)." in {
      (new MyParser("p(a:i).").getClauseList) must beEqual (Clause(Nil,pa::Nil)::Nil)
    }
    "return the correct clause of p(f(x))." in {
      (new MyParser("p(f(x:i):i).").getClauseList) must beEqual (Clause(Nil,pfx::Nil)::Nil)
    }
    "return the correct clause of -p(x)." in {
      (new MyParser("-p(x:i).").getClauseList) must beEqual (Clause(px::Nil,Nil)::Nil)
    }
    "return the correct clause of -p(x) | p(f(x))" in {
      (new MyParser("-p(x:i) | p(f(x:i):i).").getClauseList) must beEqual (Clause(px::Nil,pfx::Nil)::Nil)
    }
    "return the correct clauses for p(a). -p(x) | p(f(x)). -p(f(f(a)))." in {
      (new MyParser("p(a:i). -p(x:i) | p(f(x:i):i). -p(f(f(a:i):i):i).").getClauseList) must beEqual (Clause(Nil,pa::Nil)::Clause(px::Nil,pfx::Nil)::Clause(pffa::Nil,Nil)::Nil)
    }
    "return the correct clause of -p(x). fol" in {
      (new MyParser2("-P(x).").getClauseList) must beEqual (Clause(px_fol::Nil,Nil)::Nil)
    }
    "return the correct clauses for p(a). -p(x) | p(f(x)). -p(f(f(a))). in fol" in {
      (new MyParser2("P(a). -P(x) | P(f(x)). -P(f(f(a))).").getClauseList) must beEqual (Clause(Nil,pa_fol::Nil)::Clause(px_fol::Nil,pfx_fol::Nil)::Clause(pffa_fol::Nil,Nil)::Nil)
    }
  }
}