package at.logic.gapt.proofs.resolution.algorithms

import at.logic.gapt.language.fol.{ FOLFormula, FOLSubstitution, FOLEquation }
import at.logic.gapt.proofs.lk.algorithms.subsumption.StillmanSubsumptionAlgorithmFOL
import at.logic.gapt.proofs.lk.base.FSequent
import at.logic.gapt.proofs.resolution.FClause
import at.logic.gapt.proofs.resolution.robinson._
import at.logic.gapt.provers.atp.SearchDerivation
import at.logic.gapt.utils.logging.Logger

import scala.collection.immutable.HashMap

/**
 *  Sometimes, we have a resolution refutation R of a set of clauses C
 *  and want a refutation R' of a set C' such that C implies C'.
 *
 *  This algorithm tries to obtain R' by trying to replace clauses c
 *  from C in R by derivations of C from C' in the following way:
 *
 *  - If c is in C' or c is an instance of reflexivity, do nothing.
 *  - If c is subsumed by some c' in C', derive c from c' by factoring.
 *  - Otherwise, try to derive c from C' by paramodulation and symmetry (prover9 often needs
 *    this, and the check is usually fast),
 *  - Otherwise, try to derive c from C' by propositional resolution.
 *
 *  If none of this works, we issue a warning and keep the clause c. If no warning is issued
 *  and the algorithm terminates, the result is the desired R'.
 *
 *  In general, if R is a derivation of a clause c, the result R' of fixDerivation(R)
 *  is a derivation of a subclause of c.
 */

object fixDerivation extends Logger {
  private def getSymmetryMap( to: Tuple2[Seq[FOLFormula], Seq[FOLFormula]], from: Tuple2[Seq[FOLFormula], Seq[FOLFormula]] ) = {
    var err = false
    def createMap( from: Seq[FOLFormula], to: Seq[FOLFormula] ) = {
      ( from zip from.indices ).foldLeft( HashMap[Int, Int]() ) {
        case ( map, ( from_f, from_i ) ) => {
          val to_i = to.indexWhere( to_f => ( from_f == to_f ) || ( ( from_f, to_f ) match {
            case ( FOLEquation( from_l, from_r ), FOLEquation( to_l, to_r ) ) if from_l == to_r && from_r == to_l => true
            case _ => false
          } ) )
          if ( to_i != -1 )
            map + ( ( from_i, to_i ) )
          else {
            err = true
            map
          }
        }
      }
    }
    val neg_map = createMap( from._1, to._1 )
    val pos_map = createMap( from._2, to._2 )
    if ( err )
      None
    else
      Some( ( neg_map, pos_map ) )
  }
  private def convertSequent( seq: FSequent ) =
    ( seq.antecedent.map( f => f.asInstanceOf[FOLFormula] ), seq.succedent.map( f => f.asInstanceOf[FOLFormula] ) )
  def canDeriveBySymmetry( to: FClause, from: FSequent ): Boolean =
    canDeriveBySymmetry( convertSequent( to.toFSequent ), convertSequent( from ) )
  def canDeriveBySymmetry( to: Tuple2[Seq[FOLFormula], Seq[FOLFormula]], from: Tuple2[Seq[FOLFormula], Seq[FOLFormula]] ): Boolean = getSymmetryMap( to, from ) match {
    case Some( _ ) => true
    case None      => false
  }
  private def applySymm( p: RobinsonResolutionProof, f: FOLFormula, pos: Boolean ) =
    {
      val ( left, right ) = f match {
        case FOLEquation( l, r ) => ( l, r )
      }
      val newe = FOLEquation( right, left )
      val refl = FOLEquation( left, left )
      val s = FOLSubstitution()
      if ( pos ) {
        val irefl = InitialClause( Nil, refl :: Nil )
        Paramodulation( p, irefl, f, refl, newe, s, pos )
      } else {
        val init = InitialClause( newe :: Nil, newe :: Nil )
        val init2 = InitialClause( newe :: Nil, newe :: Nil )
        val eq1 = Paramodulation( init, p, newe, f, refl, s, pos )
        val eq2 = Paramodulation( init2, eq1, newe, refl, newe, s, pos )
        Factor( eq2, newe, 3, pos, s )
      }
    }
  private def deriveBySymmetry( to: FClause, from: FSequent ) = {
    trace( "deriving " + to + " from " + from + " by symmetry" )
    val my_to = convertSequent( to.toFSequent )
    val my_from = convertSequent( from )
    val ( neg_map, pos_map ) = getSymmetryMap( my_to, my_from ).get
    val init = InitialClause( from.antecedent.map( _.asInstanceOf[FOLFormula] ), from.succedent.map( _.asInstanceOf[FOLFormula] ) )

    var my_from_s = ( List[FOLFormula](), List[FOLFormula]() )
    var neg_map_s = HashMap[Int, Int]()
    var pos_map_s = HashMap[Int, Int]()

    // add symmetry derivations
    val s_neg = neg_map.keySet.foldLeft( init )( ( p, i ) => {
      val f = my_from._1( i )
      val to_i = neg_map( i )
      neg_map_s = neg_map_s + ( my_from_s._1.size -> to_i )
      f match {
        case FOLEquation( _, _ ) if my_to._1( to_i ) != f => {
          my_from_s = ( my_from_s._1 :+ my_to._1( to_i ), my_from_s._2 )
          applySymm( p, f, false )
        }
        case _ => {
          my_from_s = ( my_from_s._1 :+ f, my_from_s._2 )
          p
        }
      }
    } )
    val s_pos = pos_map.keySet.foldLeft( s_neg )( ( p, i ) => {
      val f = my_from._2( i )
      val to_i = pos_map( i )
      pos_map_s = pos_map_s + ( my_from_s._2.size -> to_i )
      f match {
        case FOLEquation( _, _ ) if my_to._2( to_i ) != f => {
          my_from_s = ( my_from_s._1, my_from_s._2 :+ my_to._2( to_i ) )
          applySymm( p, f, true )
        }
        case _ => {
          my_from_s = ( my_from_s._1, my_from_s._2 :+ f )
          p
        }
      }
    } )

    assert( to.isSubClauseOf( s_pos.root.toFClause ) )

    // contract some formulas if the maps are not injective
    val c_neg = neg_map_s.values.toSeq.distinct.foldLeft( s_pos )( ( p, i ) => {
      val indices = neg_map_s.filterKeys( k => neg_map_s( k ) == i ).keySet
      val form = my_from_s._1( indices.head )

      if ( indices.size > 1 )
        Factor( p, form, indices.size, false, FOLSubstitution() )
      else
        p
    } )

    pos_map_s.values.toSeq.distinct.foldLeft( c_neg )( ( p, i ) => {
      val indices = pos_map_s.filterKeys( k => pos_map_s( k ) == i ).keySet
      val form = my_from_s._2( indices.head )
      if ( indices.size > 1 )
        Factor( p, form, indices.size, true, FOLSubstitution() )
      else
        p
    } )
  }

  private val subsumption_alg = StillmanSubsumptionAlgorithmFOL
  def canDeriveByFactor( to: FClause, from: FSequent ) =
    subsumption_alg.subsumes( from, to.toFSequent )
  def deriveByFactor( to: FClause, from: FSequent ): RobinsonResolutionProof =
    {
      trace( "deriving " + to + " from " + from + " by factoring" )
      val init = InitialClause( from.antecedent.map( _.asInstanceOf[FOLFormula] ), from.succedent.map( _.asInstanceOf[FOLFormula] ) )
      deriveByFactor( to, init )
    }
  def deriveByFactor( to: FClause, from: RobinsonResolutionProof ): RobinsonResolutionProof =
    {
      val from_c = FSequent( from.root.antecedent.map( _.formula ), from.root.succedent.map( _.formula ) )
      val s = subsumption_alg.subsumes_by( from_c, to.toFSequent ).get
      val from_s = FClause( from_c.antecedent.map( s( _ ) ), from_c.succedent.map( s( _ ) ) )
      // make a first Factor inference that does not contract, but applies
      // the FOLSubstitution
      val first = if ( !from_c.antecedent.isEmpty )
        Factor( from, from_c.antecedent.head, 1, false, s )
      else
        Factor( from, from_c.succedent.head, 1, true, s )
      val proof = from_s.neg.toSet.foldLeft( first )( ( proof, atom ) => {
        val cnt = from_s.neg.count( _ == atom ) - to.neg.count( _ == atom ) + 1
        Factor( proof, atom, cnt, false, FOLSubstitution() )
      } )
      from_s.pos.toSet.foldLeft( proof )( ( proof, atom ) => {
        val cnt = from_s.pos.count( _ == atom ) - to.pos.count( _ == atom ) + 1
        Factor( proof, atom, cnt, true, FOLSubstitution() )
      } )
    }
  private def isReflexivity( c: FClause ) =
    c.pos.exists( a => a match {
      case FOLEquation( x, y ) if x == y => true
      case _                             => false
    } )
  private def isTautology( c: FClause ) = c.pos.exists( a => c.neg.exists( b => a == b ) )
  // NOTE: What if the symmetric clause found is a tautology?
  private def handleInitialClause( cls: FClause, cs: Seq[FSequent] ) = {
    val cls_sequent = FSequent(
      cls.neg.map( f => f.asInstanceOf[FOLFormula] ),
      cls.pos.map( f => f.asInstanceOf[FOLFormula] ) )
    if ( cs.contains( cls_sequent ) || isReflexivity( cls ) || isTautology( cls ) ) InitialClause( cls )
    else
      cs.find( c => canDeriveByFactor( cls, c ) ) match {
        case Some( c ) => deriveByFactor( cls, c )
        case None => cs.find( c => canDeriveBySymmetry( cls, c ) ) match {
          case Some( c ) => deriveBySymmetry( cls, c )
          case None => SearchDerivation( cs, cls_sequent, true ) match {
            case Some( d ) => {
              val ret = d.asInstanceOf[RobinsonResolutionProof]
              if ( ret.root.toFClause != cls ) {
                if ( canDeriveByFactor( cls, FSequent( ret.root.antecedent.map( _.formula ), ret.root.succedent.map( _.formula ) ) ) )
                  deriveByFactor( cls, ret )
                else
                  InitialClause( cls )
              } else
                ret
            }
            case None => {
              warn( "Could not derive " + cls + " from " + cs + " by symmetry or propositional resolution" )
              InitialClause( cls )
            }
          }
        }
      }
  }
  def apply( p: RobinsonResolutionProof, cs: Seq[FSequent] ): RobinsonResolutionProof = {
    rec( p )( cs )
  }
  // The inductive invariant is that if we had previously a derivation of a clause c,
  // we will now have a derivation of a subclause of c. Hence we have to drop some parts
  // of the derivation.
  private def rec( p: RobinsonResolutionProof )( implicit cs: Seq[FSequent] ): RobinsonResolutionProof = {
    var fac = false
    val res = p match {
      case InitialClause( cls ) => handleInitialClause( cls.toFClause, cs )
      case Factor( r, par, a, s ) => {
        fac = true
        a match {
          case lit1 :: Nil => {
            val rp = rec( par )
            val form = lit1.head.formula
            val pos = par.root.succedent.contains( lit1.head )
            val cnt_ = if ( pos ) rp.root.succedent.filter( _.formula == form ).size - p.root.succedent.filter( _.formula == form ).size + 1
            else rp.root.antecedent.filter( _.formula == form ).size - p.root.antecedent.filter( _.formula == form ).size + 1
            val cnt = if ( cnt_ > 0 ) cnt_ else 0
            if ( cnt == 0 )
              rp
            else
              Factor( rp, form, cnt, pos, s )
          }
          case lit1 :: lit2 :: Nil => {
            val rp = rec( p )
            val form_left = lit1.head.formula
            val form_right = lit2.head.formula
            val cnt_left_ = rp.root.antecedent.filter( _.formula == form_left ).size - p.root.antecedent.filter( _.formula == form_left ).size
            val cnt_right_ = rp.root.succedent.filter( _.formula == form_right ).size - p.root.succedent.filter( _.formula == form_right ).size
            val cnt_left = if ( cnt_left_ > 0 ) cnt_left_ else 0
            val cnt_right = if ( cnt_right_ > 0 ) cnt_right_ else 0
            if ( cnt_left == 0 && cnt_right == 0 )
              rp
            else
              Factor( rp, form_left, cnt_left, form_right, cnt_right, s )
          }
          case _ => throw new Exception( "Factor rule for " + p.root + " does not have one or two primary formulas!" )
        }
      }
      case Variant( r, p, s ) => Variant( rec( p ), s )
      case Resolution( r, p1, p2, a1, a2, s ) => {
        val rp1 = rec( p1 )
        val rp2 = rec( p2 )
        if ( !rp1.root.succedent.exists( _.formula == a1.formula ) )
          rp1
        else if ( !rp2.root.antecedent.exists( _.formula == a2.formula ) )
          rp2
        else
          Resolution( rp1, rp2, a1.formula.asInstanceOf[FOLFormula], a2.formula.asInstanceOf[FOLFormula], s )
      }
      case Paramodulation( r, p1, p2, a1, a2, p, s ) => {
        val rp1 = rec( p1 )
        val rp2 = rec( p2 )
        val right = p2.root.succedent.contains( a2 )
        if ( !rp1.root.succedent.exists( _.formula == a1.formula ) )
          rp1
        else if ( right && !rp2.root.succedent.exists( _.formula == a2.formula ) )
          rp2
        else if ( !right && !rp2.root.antecedent.exists( _.formula == a2.formula ) )
          rp2
        else
          Paramodulation( rp1, rp2, a1.formula.asInstanceOf[FOLFormula], a2.formula.asInstanceOf[FOLFormula], p.formula.asInstanceOf[FOLFormula], s, right )
      }
      // this case is applicable only if the proof is an instance of RobinsonProofWithInstance
      case Instance( _, p, s ) => Instance( rec( p ), s )
    }
    assert( res.root.toFClause.isSubClauseOf( p.root.toFClause ), "res.root.toFClause: " + res.root.toFClause + "\np.root.toFClause: " + p.root.toFClause + "\np.rule: " + p.rule )
    res
  }
}
