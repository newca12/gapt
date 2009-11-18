/*
 * SimpleHOLParser.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package at.logic.parsing.language.simple

import org.specs._
import org.specs.runner._
import at.logic.language.hol.HigherOrderLogic._
import at.logic.language.lambda.TypedLambdaCalculus._
import at.logic.language.hol.LogicSymbols.ConstantStringSymbol
import at.logic.language.lambda.Symbols.VariableStringSymbol
import at.logic.language.lambda.Types.TAImplicitConverters._
import at.logic.language.lambda.Types._
import at.logic.language.lambda.Symbols.SymbolImplicitConverters._
import at.logic.parsing.readers.StringReader

class MyParser(input: String) extends StringReader(input) with SimpleHOLParser

class SimpleHOLParserTest extends Specification with JUnit {
    "SimpleHOLParser" should {
        val var1 = Var[HOL](new VariableStringSymbol("x1"), i->(i->i))
        "parse correctly a variable" in {
            (new MyParser("x1: (i -> (i -> i))").getTerm()) must beEqual (var1)
        }
        val const1 = Var[HOL](new ConstantStringSymbol("c1"), i->i)
        "parse correctly an constant" in {    
            (new MyParser("c1: (i -> i)").getTerm()) must beEqual (const1)
        }
        val var2 = Var[HOL](new VariableStringSymbol("x2"), i)
        val atom1 = Atom(new ConstantStringSymbol("a"),var1::var2::const1::Nil)
        "parse correctly an atom" in {  
            (new MyParser("a(x1: (i -> (i -> i)), x2: i, c1: (i -> i))").getTerm()) must beEqual (atom1)
        }
        val var3 = Var[HOL](new VariableStringSymbol("x3"), o).asInstanceOf[Formula[HOL]]
        "parse correctly a formula variable" in {
            (new MyParser("x3: o").getTerm()) must beLike {case x: Formula[_] => true}
        }
        "parse correctly a formula constant" in {
            (new MyParser("c: o").getTerm()) must beLike {case x: Formula[_] => true}
        }
        val and1 = And(atom1, var3)
        "parse correctly an and" in {
            (new MyParser("And a(x1: (i -> (i -> i)), x2: i, c1: (i -> i)) x3: o").getTerm()) must beEqual (and1)
        }
        val or1 = Or(atom1, var3)
        "parse correctly an or" in {
            (new MyParser("Or a(x1: (i -> (i -> i)), x2: i, c1: (i -> i)) x3: o").getTerm()) must beEqual (or1)
        }
        val imp1 = Imp(atom1, var3)
        "parse correctly an imp" in {
            (new MyParser("Imp a(x1: (i -> (i -> i)), x2: i, c1: (i -> i)) x3: o").getTerm()) must beEqual (imp1)
        }
        val neg1 = Neg(atom1)
        "parse correctly an neg" in {
            (new MyParser("Neg a(x1: (i -> (i -> i)), x2: i, c1: (i -> i))").getTerm()) must beEqual (neg1)
        }
    }
    
}