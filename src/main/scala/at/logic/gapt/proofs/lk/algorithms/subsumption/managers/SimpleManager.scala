/*
 * SimpleManager.scala
 *
 */

package at.logic.gapt.proofs.lk.algorithms.subsumption.managers

import at.logic.gapt.utils.patterns.listeners.ListenerManager
import at.logic.gapt.proofs.lk.algorithms.subsumption._
import at.logic.gapt.proofs.lk.base.FSequent

class SimpleManager( listener: ListenerManager[SubsumptionDSEvent],
                     sbsmpAlg: SubsumptionAlgorithm,
                     iterator: () => Iterator[FSequent], // get the current iterator in every application
                     exists: ( FSequent => Boolean ) => Boolean,
                     remove: FSequent => Unit ) extends SubsumptionManager( listener, sbsmpAlg, iterator, exists, remove ) {
  init
  protected def addSequent( s: FSequent ) = ()
  protected def removeSequent( s: FSequent ) = ()

  def forwardSubsumption( s: FSequent ) = exists( ( s2: FSequent ) => sbsmpAlg.subsumes( s2, s ) )
  def backwardSubsumption( s: FSequent ) = iterator().foreach( s2 => if ( sbsmpAlg.subsumes( s, s2 ) ) remove( s2 ) )
}
