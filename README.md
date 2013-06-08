ATurtleYak
==========

Turtle Graphics in the Yak's varient of Logo Language.

Download the app:  http://yak.net/repos/tmp/ATurtleYak.apk

What's New
==========

Index variables i, j, k automatically created in loops.
i is the index of the outermost loop;  j the second outer-most loop, etc.
(This means that a single i is no longer a roman numberal).

Binary operators + - * /.   They can be used where N appears below.  Execution is strictly left to right (no precedence).


c N -- color (n is RGB decimal;  i.e. 0=black 999=white 900=red 90=green 9=blue

u -- pen up

d -- pen down

f N -- go forward N steps (the only way to draw)

l N -- go left n degrees

r N -- go right n degrees

( code... ) N  -- repeat code... N times

Separate all tokens by white space!   Even "(" and ")" and "+" etc.

Numbers can be decimal or roman numerals (except i is an index variable).

