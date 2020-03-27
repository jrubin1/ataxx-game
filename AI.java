package ataxx;

import static ataxx.PieceColor.*;
import static java.lang.Math.min;
import static java.lang.Math.max;

/** A Player that computes its own moves.
 *  @author Josh Rubin
 */
class AI extends Player {

    /** Maximum minimax search depth before going to static evaluation. */
    private static final int MAX_DEPTH = 4;
    /** A position magnitude indicating a win (for red if positive, blue
     *  if negative). */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 1;
    /** A magnitude greater than a normal value. */
    private static final int INFTY = Integer.MAX_VALUE;

    /** A new AI for GAME that will play MYCOLOR. */
    AI(Game game, PieceColor myColor) {
        super(game, myColor);
    }

    @Override
    Move myMove() {
        if (!board().canMove(myColor())) {
            game().reportMove("%s passes.", myColor().toString());
            return Move.pass();
        }
        Move move = findMove();
        Object[] arr = new Object[]{myColor().toString(), move.toString()};
        game().reportMove("%s moves %s.", arr);
        return move;
    }

    /** Return a move for me from the current position, assuming there
     *  is a move. */
    private Move findMove() {
        Board b = new Board(board());
        if (myColor() == RED) {
            findMove(b, MAX_DEPTH, true, 1, -INFTY, INFTY);
        } else {
            findMove(b, MAX_DEPTH, true, -1, -INFTY, INFTY);
        }
        return _lastFoundMove;
    }

    /** Used to communicate best moves found by findMove, when asked for. */
    private Move _lastFoundMove;

    /** Find a move from position BOARD and return its value, recording
     *  the move found in _lastFoundMove iff SAVEMOVE. The move
     *  should have maximal value or have value >= BETA if SENSE==1,
     *  and minimal value or value <= ALPHA if SENSE==-1. Searches up to
     *  DEPTH levels before using a static estimate. */
    private int findMove(Board board, int depth, boolean saveMove, int sense,
                         int alpha, int beta) {
        if (depth == 0 || board.gameOver()) {
            return staticScore(board);
        }
        int bestSoFar;  Move move;
        bestSoFar = -sense * INFTY;
        for (char c = 'a'; c <= 'g'; c++) {
            for (char r = '1'; r <= '7'; r++) {
                if (board.get(c, r) == board.whoseMove()) {
                    outerloop:
                    for (int c1 = -2; c1 <= 2; c1++) {
                        for (int r1 = -2; r1 <= 2; r1++) {
                            move = Move.move(c, r,
                                    (char) (c + c1), (char) (r + r1));
                            if ((!board().canMove(board().whoseMove()))) {
                                move = Move.PASS;
                            }
                            if (move != null && (char) (c + c1) >= 'a'
                                    && (char) (c + c1) <= 'g' && (char)
                                    (r + r1) <= '7' && (char) (r + r1)
                                    >= '1' && board.legalMove(move)) {
                                board.makeMove(move);
                                int response = findMove(board, depth - 1,
                                        false, -1 * sense, alpha, beta);
                                if (sense == 1) {
                                    if (response >= bestSoFar) {
                                        bestSoFar = response;
                                        if (saveMove) {
                                            _lastFoundMove = move;
                                        }
                                        alpha = max(alpha, response);
                                        if (beta <= alpha) {
                                            board.undo();
                                            break outerloop;
                                        }
                                    }
                                } else {
                                    if (response <= bestSoFar) {
                                        bestSoFar = response;
                                        if (saveMove) {
                                            _lastFoundMove = move;
                                        }
                                        beta = min(beta, response);
                                        if (beta <= alpha) {
                                            board.undo();
                                            break outerloop;
                                        }
                                    }
                                }
                                board.undo();
                            }
                        }
                    }
                }
            }
        }
        return bestSoFar;
    }

    /** Return a heuristic value for BOARD. */
    private int staticScore(Board board) {
        return board.redPieces() - board.bluePieces();
    }
}
