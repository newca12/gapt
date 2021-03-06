/* *************************************************************************** *
   n-Tape Proof script: loads an instance of the n-Tape proof, extracts the
   characteristic sequent set and exports it to tptp.
   Usage:                                                                       
     (start cli in gapt/source directory or adjust the filename variable below command)     
     :load examples/hol-tape/ntape-inst.scala                               
     prooftool(acnf)                                                            
                                                                                
 * *************************************************************************** */

/* adjust filename to load a different example */
val filename = "tape3-4c.llk"  
//val filename = "examples/hol-tape/tape3-3.llk"  

/* begin of proof script  */

import at.logic.gapt.language.fol.algorithms.recreateWithFactory
import at.logic.gapt.language.hol._

import at.logic.gapt.language.fol.algorithms.undoHol2Fol
import at.logic.gapt.algorithms.hlk.HybridLatexParser
import at.logic.gapt.algorithms.rewriting.DefinitionElimination
import at.logic.gapt.proofs.lk.algorithms.{AtomicExpansion, subsumedClausesRemovalHOL, regularize}
import at.logic.gapt.proofs.lksk.sequentToLabelledSequent
import at.logic.gapt.proofs.resolution.algorithms.RobinsonToRal


import at.logic.gapt.provers.prover9._
import at.logic.gapt.proofs.algorithms.ceres.clauseSets._
import at.logic.gapt.proofs.algorithms.ceres.projections.Projections
import at.logic.gapt.proofs.algorithms.ceres.struct.StructCreators

import at.logic.gapt.proofs.algorithms.ceres.ceres_omega
import at.logic.gapt.proofs.algorithms.herbrandExtraction.lksk.extractLKSKExpansionSequent
import at.logic.gapt.proofs.algorithms.skolemization.lksk.LKtoLKskc

 def show(s:String) = println("\n\n+++++++++ "+s+" ++++++++++\n")

 show("Loading file")
 val pdb = loadLLK(filename)

 show("Eliminating definitions, expanding tautological axioms")
 val elp = AtomicExpansion(DefinitionElimination(pdb.Definitions, regularize(pdb.proof("TAPEPROOF"))))

 show("Skolemizing")
 val selp = LKtoLKskc(elp)

 show("Extracting struct")
 val struct = StructCreators.extract(selp, x => containsQuantifier(x) || freeHOVariables(x).nonEmpty)

 show("Computing clause set")
 //val cl = AlternativeStandardClauseSet(struct)
 val cl = SimpleStandardClauseSet(struct)
 //val cl = structToClausesList(struct).map(_.toFSequent)

 show("simplifying clause set")
 val fs = FSequent(Nil, List(parseLLKFormula("var x :i; x = x")))
 val rcl = subsumedClausesRemovalHOL(fs::(cl.toList))
 val tcl = deleteTautologies(rcl)

 show("exporting to THF and Tex")
 exportTHF(tcl.sorted(FSequentOrdering), filename+".tptp")
 exportLatex(tcl.sorted(FSequentOrdering), filename+".tex")

 show("statistics")
 println("Clause set:"+cl.size)
 println("Reduced clause set:"+tcl.size)
