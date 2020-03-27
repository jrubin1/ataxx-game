package ataxx;


/** A Player that receives its moves from its Game's getMoveCmnd method.
 *  @author Josh Rubin
 */
class Manual extends Player {

    /** A Player that will play MYCOLOR on GAME, taking its moves from
     *  GAME. */
    Manual(Game game, PieceColor myColor) {
        super(game, myColor);
    }

    @Override
    Move myMove() {
        while (true) {
            try {
                String[] operandArr = game().getMoveCmnd
                        (myColor().toString() + ":").operands();
                Move move1 = Move.move(operandArr[0].charAt(0),
                        operandArr[1].charAt(0),
                        operandArr[2].charAt(0),
                        operandArr[3].charAt(0));
                return move1;
            } catch (AssertionError illegalMove) {
                game().reportError("that move is illegal");
            }
        }
    }

}

