/*
 * SkolemSymbols.scala
 */

package at.logic.gapt.language.hol

import at.logic.gapt.language.lambda.symbols._
import at.logic.gapt.utils.ds.streams.Definitions._

trait TSkolemSymbol

object TypeSynonyms {
  type SkolemSymbol = SymbolA with TSkolemSymbol
}

import at.logic.gapt.language.hol.TypeSynonyms._

/* The idea of SkolemSymbolFactory is to provide
     a singleton for access to the (global) Skolem symbols.
     SkolemSymbolFactory provides
     (1) single Skolem symbols, and
     (2) streams of Skolem symbols.

     Every Skolem symbol is only returned once in both
     cases.

     This is realized by keeping a stream s of Skolem
     symbols internally, and upon request returning a stream
     consisting of the even indices of s, while keeping
     the odd indices of s.
   */

object SkolemSymbolFactory {
  private def skolem_symbol_stream_from( n: Int ): Stream[SkolemSymbol] =
    Stream.cons( new StringSymbol( "s_{" + n + "}" ) with TSkolemSymbol, skolem_symbol_stream_from( n + 1 ) )

  private var skolem_symbol_stream = skolem_symbol_stream_from( 0 )

  // This method resets the internal state of the factory.
  // WARNING: uniqueness of Skolem Symbols is now not guaranteed anymore
  // (since Skolem Symbols returned before the reset call may now
  // be returned again)
  //
  // Hence, this function should only be used for testing.

  def reset = { skolem_symbol_stream = skolem_symbol_stream_from( 0 ) }

  def getSkolemSymbols = {
    val stream = even( skolem_symbol_stream )
    skolem_symbol_stream = odd( skolem_symbol_stream )
    stream
  }

  def getSkolemSymbol = {
    val sym = skolem_symbol_stream.head
    skolem_symbol_stream = skolem_symbol_stream.tail
    sym
  }
}
