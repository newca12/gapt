package at.logic.gapt.proofs.algorithms.ceres

import at.logic.gapt.proofs.lk.algorithms.subsumption.StillmanSubsumptionAlgorithmHOL
import at.logic.gapt.proofs.lk._
import at.logic.gapt.proofs.lksk.TypeSynonyms.Label
import at.logic.gapt.proofs.lksk.algorithms.applySubstitution
import at.logic.gapt.proofs.occurrences.FormulaOccurrence
import at.logic.gapt.language.hol.{ HOLFormula, HOLEquation }
import at.logic.gapt.language.lambda.types.Ti
import at.logic.gapt.proofs.algorithms.ceres.struct.Struct
import at.logic.gapt.utils.dssupport.ListSupport._
import at.logic.gapt.proofs.lk.base.{ Sequent, LKProof }
import at.logic.gapt.proofs.lksk
import at.logic.gapt.proofs.lksk.{ Axiom => LKSKAxiom, _ }
import at.logic.gapt.proofs.resolution.ral._

/**
 * Created by marty on 10/6/14.
 */
object ceres_omega extends ceres_omega
class ceres_omega {
  import at.logic.gapt.formats.llk.LLKFormatter._

  private def check_es( s: LabelledSequent, c: LabelledSequent, es: LabelledSequent ): LabelledSequent = {

    s
  }

  /**
   * Applies the CERES_omega method to a proof.
   * @param projections This is the set of projections to use. A projection to reflexvity is generated if needed.
   * @param ralproof The R_al proof to use as skeleton.
   * @param es The end-sequent of the original proof.
   * @param struct The struct of the original proof. (unused at the moment)
   * @return a pair of an LKProof with atomic cuts only and of the subsequent of its root which corresponds the Ral sequent
   */
  def apply( projections: Set[LKProof], ralproof: RalResolutionProof[LabelledSequent], es: LabelledSequent, struct: Struct ): ( LKProof, LabelledSequent ) = ralproof match {
    //reflexivity as initial rule
    case InitialSequent( s @ LabelledSequent( Nil, List( LabelledFormulaOccurrence( HOLEquation( x, y ), anc, label ) ) ) ) if ( x == y ) && ( x.exptype == Ti ) =>

      val rule = LKSKAxiom( s.toFSequent, ( List(), List( label ) ) )
      val reflexivity_occ = rule.root.succedent( 0 ).asInstanceOf[LabelledFormulaOccurrence]
      val weakened_left = es.l_antecedent.foldLeft( rule )( ( r, fo ) => lksk.WeakeningLeftRule( r, fo.formula, fo.skolem_label ) )
      val weakened_right = es.l_succedent.foldLeft( weakened_left )( ( r, fo ) => lksk.WeakeningRightRule( r, fo.formula, fo.skolem_label ) )
      val reflexivity_successor = pickFOWithAncestor( sequentToLabelledSequent( weakened_right.root ).l_succedent, reflexivity_occ )
      val clause = LabelledSequent( Nil, List( reflexivity_successor ) )

      require( weakened_right.root.occurrences.size == es.occurrences.size + clause.occurrences.size, "The size of the generated end-sequent " + rule.root + " is not the size of the end-sequent " + es + " + the size of the clause " + clause )
      require( ( clause.occurrences diff weakened_right.root.occurrences ).isEmpty )

      ( weakened_right, clause )

    case InitialSequent( root ) =>
      val candidates = projections.toList.flatMap( x => {
        val pes = filterEndsequent( sequentToLabelledSequent( x.root ), es, struct )
        StillmanSubsumptionAlgorithmHOL.subsumes_by( pes.toFSequent, root.toFSequent ) match {
          case None        => Nil
          case Some( sub ) => List( ( x, sub ) )
        }
      } )

      candidates match {
        case ( proof, sub ) :: _ =>
          val subproof = applySubstitution( proof, sub )._1
          val clause = filterEndsequent( sequentToLabelledSequent( subproof.root ), es, struct )
          val tocontract = LabelledSequent(
            diffModuloOccurrence( clause.l_antecedent, root.l_antecedent ),
            diffModuloOccurrence( clause.l_succedent, root.l_succedent ) )
          val acontr = tocontract.l_antecedent.foldLeft( subproof )( ( p, occ ) =>
            p.root.antecedent.find( x => x != occ && x.formula == occ.formula && x.asInstanceOf[LabelledFormulaOccurrence].skolem_label == occ.skolem_label ) match {
              case Some( c ) =>
                ContractionLeftRule( p, occ, c )
              case None => throw new Exception( "Could not find an element to contract for " + f( occ ) + " in " + f( root ) )
            } )
          val scontr = tocontract.l_succedent.foldLeft( acontr )( ( p, occ ) =>
            p.root.succedent.find( x => x != occ && x.formula == occ.formula && x.asInstanceOf[LabelledFormulaOccurrence].skolem_label == occ.skolem_label ) match {
              case Some( c ) =>
                ContractionRightRule( p, occ, c )
              case None => throw new Exception( "Could not find an element to contract for " + f( occ ) + " in " + f( root ) )
            } )
          val nclause = filterEndsequent( sequentToLabelledSequent( scontr.root ), es, struct )

          require( scontr.root.syntacticMultisetEquals( nclause compose es ), "The root " + f( scontr.root ) + " must consist of the clause " + f( nclause ) + " plus the end-sequent " + f( es ) )
          require( scontr.root.occurrences.size == es.occurrences.size + nclause.occurrences.size, "The size of the generated end-sequent " + f( root ) + " is not the size of the end-sequent " + f( es ) + " + the size of the clause " + nclause )

          require( ( nclause.occurrences diff scontr.root.occurrences ).isEmpty )
          ( scontr, nclause )
        case Nil =>
          throw new Exception( "Could not find a projection for the clause " + f( root ) + " in " + projections.map( x => filterEndsequent( sequentToLabelledSequent( x.root ), es, struct ) ).map( f( _ ) ).mkString( "\n" ) )
      }

    case Cut( root, parent1, parent2, p1occs, p2occs ) =>
      val ( lkparent1, clause1 ) = ceres_omega( projections, parent1, es, struct )
      val ( lkparent2, clause2 ) = ceres_omega( projections, parent2, es, struct )
      require( ( clause1.occurrences diff lkparent1.root.occurrences ).isEmpty )
      require( ( clause2.occurrences diff lkparent2.root.occurrences ).isEmpty )
      val leftcutformulas = p1occs.foldLeft( List[LabelledFormulaOccurrence]() )( ( list, fo ) => findAuxByFormulaAndLabel( fo.asInstanceOf[LabelledFormulaOccurrence], clause1.l_succedent, list ) :: list ).reverse
      val rightcutformulas = p2occs.foldLeft( List[LabelledFormulaOccurrence]() )( ( list, fo ) => findAuxByFormulaAndLabel( fo.asInstanceOf[LabelledFormulaOccurrence], clause2.l_antecedent, list ) :: list ).reverse
      val ( c1, caux1, c2, caux2 ) = ( leftcutformulas, rightcutformulas ) match {
        case ( x :: xs, y :: ys ) => ( x, xs, y, ys )
        case ( Nil, _ )           => throw new Exception( "Could not find the cut formula " + p1occs( 0 ).formula + " in left parent " + lkparent1.root )
        case ( _, Nil )           => throw new Exception( "Could not find the cut formula " + p2occs( 0 ).formula + " in right parent " + lkparent2.root )
      }

      val cleft = caux1.foldLeft( lkparent1 )( ( proof, occ ) =>
        ContractionRightRule( proof, pickFOWithAncestor( proof.root.succedent, c1 ), pickFOWithAncestor( proof.root.succedent, occ ) ) )
      val cright = caux2.foldLeft( lkparent2 )( ( proof, occ ) =>
        ContractionLeftRule( proof, pickFOWithAncestor( proof.root.antecedent, c2 ), pickFOWithAncestor( proof.root.antecedent, occ ) ) )

      val cutfleft = pickFOWithAncestor( cleft.root.succedent, c1 ).asInstanceOf[LabelledFormulaOccurrence]
      val cutfright = pickFOWithAncestor( cright.root.antecedent, c2 ).asInstanceOf[LabelledFormulaOccurrence]

      require( cutfleft.formula == cutfright.formula,
        "Found the wrong cut formulas:\n" + cutfleft.formula + "\n and\n" + cutfright.formula )
      //      require(cutfleft.formula syntaxEquals  cutfright.formula,
      //        "Cut formulas are alpha equal, but not syntax:\n"+cutfleft.formula+"\n and\n"+cutfright.formula)
      require( cutfleft.skolem_label == cutfright.skolem_label,
        "Found the wrong cut labels:\n" + cutfleft.skolem_label + "\n and\n" + cutfright.skolem_label )
      val rule = CutRule( cleft, cright, cutfleft, cutfright )
      val crule = contractEndsequent( rule, es )
      val nclauses = filterByAncestor( crule.root, clause1 compose clause2 )
      require( nclauses.toFSequent multiSetEquals root.toFSequent, "We tracked the clauses wrong:\n calculated clause: " + f( nclauses ) + "\n real clause: " + f( root ) )
      require( crule.root.occurrences.size == es.occurrences.size + nclauses.occurrences.size, "The size of the generated end-sequent " + rule.root + " is not the size of the end-sequent " + es + " + the size of the clause " + nclauses )
      ( crule, nclauses )

    case AFactorF( root, parent, contr, aux, _ ) =>
      val ( lkparent, clause1 ) = ceres_omega( projections, parent, es, struct )
      require( ( clause1.occurrences diff lkparent.root.occurrences ).isEmpty )
      aux.length match {
        case 0 => ( lkparent, clause1 ) //trivial, skipping factor inference
        case 1 =>
          val c1 = findAuxByFormulaAndLabel( contr, clause1.l_antecedent, Nil )
          val c2 = findAuxByFormulaAndLabel( contr, clause1.l_antecedent, c1 :: Nil )
          val rule = ContractionLeftRule( lkparent, c1, c2 )
          val nclauses = filterByAncestor( rule.root, clause1 )
          require( nclauses.toFSequent multiSetEquals root.toFSequent, "We tracked the clauses wrong:\n calculated clause: " + f( nclauses ) + "\n real clause: " + f( root ) )
          require( rule.root.occurrences.size == es.occurrences.size + nclauses.occurrences.size, "The size of the generated end-sequent " + rule.root + " is not the size of the end-sequent " + es + " + the size of the clause " + nclauses )
          ( rule, nclauses )
        case _ => throw new Exception( "Factor of more than two literals not supported yet!" )
      }

    case AFactorT( root, parent, contr, aux, _ ) =>
      val ( lkparent, clause1 ) = ceres_omega( projections, parent, es, struct )
      require( ( clause1.occurrences diff lkparent.root.occurrences ).isEmpty )
      aux.length match {
        //        case 0 => throw new Exception("At least one auxiliary formula is necessary for a factor rule!")
        case 1 =>
          val c1 = findAuxByFormulaAndLabel( contr, clause1.l_succedent, Nil )
          val c2 = findAuxByFormulaAndLabel( contr, clause1.l_succedent, c1 :: Nil )
          val rule = ContractionRightRule( lkparent, c1, c2 )
          val nclauses = filterByAncestor( rule.root, clause1 )
          require( nclauses.toFSequent multiSetEquals root.toFSequent, "We tracked the clauses wrong:\n calculated clause: " + f( nclauses ) + "\n real clause: " + f( root ) )
          require( rule.root.occurrences.size == es.occurrences.size + nclauses.occurrences.size, "The size of the generated end-sequent " + root + " is not the size of the end-sequent " + es + " + the size of the clause " + nclauses )
          ( rule, nclauses )
        case 0 => ( lkparent, clause1 ) //trivial, skipping factor inference
        case _ => throw new Exception( "Factor of more than two literals not supported yet!" )
      }

    case ParaF( root, parent1, parent2, p1occ, p2occ, principial, flipped ) =>
      val ( lkparent1, clause1 ) = ceres_omega( projections, parent1, es, struct )
      val ( lkparent2, clause2 ) = ceres_omega( projections, parent2, es, struct )
      require( ( clause1.occurrences diff lkparent1.root.occurrences ).isEmpty )
      require( ( clause2.occurrences diff lkparent2.root.occurrences ).isEmpty )
      val eqn: FormulaOccurrence = findAuxByFormulaAndLabel( p1occ, clause1.l_succedent, Nil )
      val modulant: FormulaOccurrence = findAuxByFormulaAndLabel( p2occ.asInstanceOf[LabelledFormulaOccurrence], clause2.l_antecedent, Nil )
      val rule = EquationLeftMacroRule( lkparent1, lkparent2, eqn, modulant, principial.formula )
      val crule = contractEndsequent( rule, es )
      val nclauses = filterByAncestor( crule.root, clause1 compose clause2 )
      require( nclauses.toFSequent multiSetEquals root.toFSequent, "We tracked the clauses wrong:\n calculated clause: " + f( nclauses ) + "\n real clause: " + f( root ) )
      require( crule.root.occurrences.size == es.occurrences.size + nclauses.occurrences.size, "The size of the generated end-sequent " + rule.root + " is not the size of the end-sequent " + es + " + the size of the clause " + nclauses )
      ( crule, nclauses )

    case ParaT( root, parent1, parent2, p1occ, p2occ, principial, flipped ) =>
      val ( lkparent1, clause1 ) = ceres_omega( projections, parent1, es, struct )
      val ( lkparent2, clause2 ) = ceres_omega( projections, parent2, es, struct )
      require( ( clause1.occurrences diff lkparent1.root.occurrences ).isEmpty )
      require( ( clause2.occurrences diff lkparent2.root.occurrences ).isEmpty )
      val eqn: FormulaOccurrence = findAuxByFormulaAndLabel( p1occ, clause1.l_succedent, Nil )
      val modulant: FormulaOccurrence = findAuxByFormulaAndLabel( p2occ.asInstanceOf[LabelledFormulaOccurrence], clause2.l_succedent, Nil )
      val rule = EquationRightMacroRule( lkparent1, lkparent2, eqn, modulant, principial.formula )
      val crule = contractEndsequent( rule, es )
      val nclauses = filterByAncestor( crule.root, clause1 compose clause2 )
      require( nclauses.toFSequent multiSetEquals root.toFSequent, "We tracked the clauses wrong:\n calculated clause: " + f( nclauses ) + "\n real clause: " + f( root ) )
      require( crule.root.occurrences.size == es.occurrences.size + nclauses.occurrences.size, "The size of the generated end-sequent " + rule.root + " is not the size of the end-sequent " + es + " + the size of the clause " + nclauses )
      ( crule, nclauses )

    case Sub( root, parent, sub ) =>
      val ( lkparent, clauses ) = ceres_omega( projections, parent, es, struct )
      require( ( clauses.occurrences diff lkparent.root.occurrences ).isEmpty )
      val ( rule, mapping ) = applySubstitution( lkparent, sub )

      //val axiomformulas = rule.leaves.flatMap( _.vertex.occurrences )
      val lruleroot = sequentToLabelledSequent( rule.root )

      /* find the sub-sequent of the substituted proof which was introduced only by axioms */
      //val axiomancestoroccs_a = lruleroot.l_antecedent.filter( x => firstAncestors( x ).forall( y => axiomformulas.contains( y ) ) )
      //val axiomancestoroccs_s = lruleroot.l_succedent.filter( x => firstAncestors( x ).forall( y => axiomformulas.contains( y ) ) )

      /* for each element in the root, find a matching literal with axiom ancestor in the derived end-sequent */
      //val nclauses_a = root.l_antecedent.foldLeft[List[LabelledFormulaOccurrence]]( List() )( ( list, fo ) => {
      //  findAuxByFormulaAndLabel( fo.formula, fo.skolem_label, axiomancestoroccs_a, list ) :: list
      //} )
      //val nclauses_s = root.l_succedent.foldLeft[List[LabelledFormulaOccurrence]]( List() )( ( list, fo ) => {
      //  findAuxByFormulaAndLabel( fo.formula, fo.skolem_label, axiomancestoroccs_s, list ) :: list
      //} )

      require( clauses.l_antecedent.filterNot( mapping.contains ).isEmpty, "Could not find a mapping for: " + clauses.l_antecedent.filterNot( mapping.contains ) + " in " + lruleroot.l_antecedent )
      require( clauses.l_succedent.filterNot( mapping.contains ).isEmpty, "Could not find a mapping for: " + clauses.l_succedent.filterNot( mapping.contains ) + " in " + lruleroot.l_succedent )
      val nclauses = LabelledSequent( clauses.l_antecedent.map( mapping ), clauses.l_succedent.map( mapping ) )

      //val nclauses = LabelledSequent( nclauses_a.reverse, nclauses_s.reverse )

      require( nclauses.toFSequent multiSetEquals root.toFSequent, "We tracked the clauses wrong:\n calculated clause: " + f( nclauses ) + "\n real clause: " + f( root ) + " parent rule " + parent.rule )

      val rootsize = rule.root.occurrences.size
      val essize = es.occurrences.size
      val nclausessize = nclauses.occurrences.size
      require( rootsize == essize + nclausessize, "The size of the generated end-sequent " + root + " is not the size of the end-sequent " +
        es + " + the size of the clause " + nclauses + "(" + rootsize + " != " + essize + "+" + nclausessize + ")" )

      ( rule, nclauses )

    case _ => throw new Exception( "Unhandled case: " + ralproof )
  }

  def filterByAncestor( sequent: Sequent, anc: LabelledSequent ): LabelledSequent = {
    try {
      val root = sequentToLabelledSequent( sequent )
      LabelledSequent(
        root.l_antecedent.filter( x => anc.l_antecedent.exists( y => tranAncestors( x ).contains( y ) ) ),
        root.l_succedent.filter( x => anc.l_succedent.exists( y => tranAncestors( x ).contains( y ) ) ) )
    } catch {
      case e: Exception =>
        throw new Exception( "Could not filter " + sequent + " by ancestors " + anc + ": " + e.getMessage, e )
    }
  }

  /**
   * Finds an occurrence in candidates - exclusion_list, which has the same formula and label as aux.
   * @return the first occurrence in candidates which matches
   */
  def findAuxByFormulaAndLabel( aux: LabelledFormulaOccurrence,
                                candidates: Seq[LabelledFormulaOccurrence],
                                exclusion_list: Seq[LabelledFormulaOccurrence] ): LabelledFormulaOccurrence = try {
    findAux( candidates, x => x.skolem_label == aux.skolem_label && x.formula == aux.formula, exclusion_list )
  } catch {
    case e: IllegalArgumentException =>
      throw new Exception( "Could not find a candidate for " + aux + " in " + candidates.mkString( "[", ", ", "]" ) + exclusion_list.mkString( " ignoring: ", ", ", "." ) )
  }

  /**
   * Finds an occurrence in candidates - exclusion_list, which has the same formula and label as aux.
   * @return the first occurrence in candidates which matches
   */
  def findAuxByFormulaAndLabel( formula: HOLFormula,
                                skolem_label: Label,
                                candidates: Seq[LabelledFormulaOccurrence],
                                exclusion_list: Seq[LabelledFormulaOccurrence] ): LabelledFormulaOccurrence = try {
    findAux( candidates, x => x.skolem_label == skolem_label && x.formula == formula, exclusion_list )
  } catch {
    case e: IllegalArgumentException =>
      throw new Exception( "Could not find a candidate for " + formula + " with label " + skolem_label + " in " + candidates.mkString( "[", ", ", "]" ) + exclusion_list.mkString( " ignoring: ", ", ", "." ) )
  }

  /**
   * Finds the first element in candidates - exclucsion_list fulfilling the predicate pred. Throws an IllegalArgumentException,
   * if none is found.
   * @param candidates the list of candidates to choose from
   * @param pred a predicate on formula occurrences
   * @param exclusion_list no candidate in exclusion_list will match
   * @throws IllegalArgumentException if no candidate fulfills pred.
   * @return the first element of candidates which fulfills the criteria
   */
  def findAux( candidates: Seq[LabelledFormulaOccurrence],
               pred: LabelledFormulaOccurrence => Boolean,
               exclusion_list: Seq[LabelledFormulaOccurrence] ): LabelledFormulaOccurrence =
    candidates.diff( exclusion_list ).filter( pred ).toList match {
      case List( fo ) => fo
      case l @ ( fo :: _ ) =>
        //println("warning: multiple matching formulas"+ l.mkString(": ",", ","." ))
        fo
      case Nil => throw new IllegalArgumentException( "Could not find matching aux formula!" )
    }

  /**
   * After an application of a binary rule, end-sequent material might be duplicated. This method adds contractions
   * for every end-sequent formula.
   * @param p a proof with an end-sequent of the form: es x es x C (where C is some additional content)
   * @param es the end-sequent material which occurs twice
   * @return a proof with an end-sequent of the form: es x C
   */
  def contractEndsequent( p: LKProof, es: LabelledSequent ): LKProof = {
    val contr_left = es.l_antecedent.foldLeft( p )( ( rp, fo ) => {
      sequentToLabelledSequent( rp.root ).l_antecedent.find( x =>
        x.formula == fo.formula && x.skolem_label == fo.skolem_label ) match {
        case Some( occ1 ) =>
          sequentToLabelledSequent( rp.root ).l_antecedent.find( x =>
            occ1 != x && x.formula == fo.formula && x.skolem_label == fo.skolem_label ) match {
            case Some( occ2 ) =>
              ContractionLeftRule( rp, occ1, occ2 )
            case None =>
              println( "Warning: During contraction of the end-sequent, could not find a second antecedent occurrence of " + fo + " in " + rp.root )
              rp

          }

        case None =>
          throw new Exception( "During contraction of the end-sequent, could not find an antecedent occurrence of " + fo + " in " + rp.root )
      }
    } )
    val contr_right = es.l_succedent.foldLeft( contr_left )( ( rp, fo ) => {
      sequentToLabelledSequent( rp.root ).l_succedent.find( x =>
        x.formula == fo.formula && x.skolem_label == fo.skolem_label ) match {
        case Some( occ1 ) =>
          sequentToLabelledSequent( rp.root ).l_succedent.find( x =>
            occ1 != x && x.formula == fo.formula && x.skolem_label == fo.skolem_label ) match {
            case Some( occ2 ) =>
              ContractionRightRule( rp, occ1, occ2 )
            case None =>
              println( "Warning: During contraction of the end-sequent, could not find a second succeedent occurrence of " + fo + " in " + rp.root )
              rp
          }

        case None =>
          throw new Exception( "During contraction of the end-sequent, could not find a succedent occurrence of " + fo + " in " + rp.root )
      }
    } )

    contr_right
  }

  /* TODO: this might not work if we have atom formulas in the end-sequent. then a formula which comes from a weakining
     might remain and in case of subsequent substitutions, the formula decomposition steps could fail, since they expect
     an introduction rule A :- A */
  def filterEndsequent( root: LabelledSequent, es: LabelledSequent, struct: Struct ) = LabelledSequent(
    es.antecedent.foldLeft( root.l_antecedent.toList )( ( list, fo ) =>
      removeFirstWhere( list, ( x: LabelledFormulaOccurrence ) => fo.formula == x.formula ) ),
    es.succedent.foldLeft( root.l_succedent.toList )( ( list, fo ) =>
      removeFirstWhere( list, ( x: LabelledFormulaOccurrence ) => fo.formula == x.formula ) ) )

  /**
   * Computes the reflexive, transitive closure of the ancestor relation, ie. all ancestors.
   * @param fo a formula occurrence
   * @return the list of all ancestors
   */
  def tranAncestors( fo: FormulaOccurrence ): List[FormulaOccurrence] = {
    fo :: fo.parents.toList.flatMap( x => tranAncestors( x ) )
  }

  /**
   * Computes the reflexive, transitive closure of the ancestor relation, ie. all ancestors.
   * @param fo a formula occurrence
   * @return the list of all ancestors
   */
  def tranAncestors( fo: LabelledFormulaOccurrence ): List[LabelledFormulaOccurrence] = {
    fo :: fo.parents.flatMap( x => tranAncestors( x ) )
  }

  /**
   * Computes the list of earliest ancestors of the formula occurrence. I.e. we calculate
   * the least elements of all ancestors of the occurrence with regard to the ancestorship relation.
   * @param fo a formula occurrence
   * @return the list of first ancestors
   */
  def firstAncestors( fo: FormulaOccurrence ): List[FormulaOccurrence] = {
    if ( fo.parents.isEmpty )
      List( fo )
    else
      fo.parents.toList.flatMap( x => firstAncestors( x ) )
  }

  /**
   * Computes the list of earliest ancestors of the formula occurrence. I.e. we calculate
   * the least elements of all ancestors of the occurrence with regard to the ancestorship relation.
   * @param fo a formula occurrence
   * @return the list of first ancestors
   */
  def firstAncestors( fo: LabelledFormulaOccurrence ): List[LabelledFormulaOccurrence] = {
    if ( fo.parents.isEmpty )
      List( fo )
    else
      fo.parents.toList.flatMap( x => firstAncestors( x ) )
  }

  def pickFOWithAncestor( l: Seq[FormulaOccurrence], anc: FormulaOccurrence ) = l.filter( x => tranAncestors( x ).contains( anc ) ).toList match {
    case List( a ) => a
    case l @ ( a :: _ ) =>
      //println("warning: multiple matching formulas for "+anc+ l.mkString(": ",", ","." ))
      a
    case Nil => throw new Exception( "Could not find any occurrence with ancestor " + anc + " in " + l )
  }

  def pickFOWithAncestor( l: Seq[LabelledFormulaOccurrence], anc: LabelledFormulaOccurrence ) = l.filter( x => tranAncestors( x ).contains( anc ) ).toList match {
    case List( a ) => a
    case l @ ( a :: _ ) =>
      //println("warning: multiple matching formulas for "+anc+ l.mkString(": ",", ","." ))
      a
    case Nil => throw new Exception( "Could not find any occurrence with ancestor " + anc + " in " + l )
  }

  //def pickFOwhere( l : Seq[LabelledFormulaOccurrence], prop : LabelledFormulaOccurrence => Boolean, blacklist : List[LabelledFormulaOccurrence]) =

  def diffModuloOccurrence( from: Seq[LabelledFormulaOccurrence], what: Seq[LabelledFormulaOccurrence] ) = {
    what.foldLeft( from.toList )( ( l, occ ) => removeFirstWhere( l, ( x: LabelledFormulaOccurrence ) => x.formula == occ.formula && x.skolem_label == occ.skolem_label ) )
  }

}
