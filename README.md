## GAPT: General Architecture for Proof Theory 
[![Build Status](https://travis-ci.org/newca12/gapt.svg?branch=code-coverage)](https://travis-ci.org/newca12/gapt) [![Coverage Status](https://coveralls.io/repos/newca12/gapt/badge.svg?branch=code-coverage)](https://coveralls.io/r/newca12/gapt?branch=code-coverage) [![Ohloh](http://www.openhub.net/p/gapt/widgets/project_thin_badge.gif)](https://www.openhub.net/p/gapt)

GAPT is a proof theory framework developed primarily at the Vienna University
of Technology. GAPT contains data structures, algorithms, parsers and other
components common in proof theory and automated deduction. In contrast to
automated and interactive theorem provers whose focus is the construction of
proofs, GAPT concentrates on the transformation and further processing of
proofs.

Website: http://www.logic.at/gapt  
Contact: gapt@logic.at

Requirements:
* Unix-compatible environment
* Java 1.7 or later

See https://github.com/gapt/gapt/wiki/External-software for details.

Compilation and running:
* sbt assembly
* The scripts cli.sh, gui.sh, atp.sh look for the assembled jar and start the
   respective interface of gapt.

See https://github.com/gapt/gapt/wiki/Compiling-and-running-from-source for
details.

License:
GAPT is free software licensed under the GNU General Public License Version 3.
See the file COPYING for details.
