package ataxx;

import org.junit.Test;
import static org.junit.Assert.*;

/** Tests of the Board class.
 *  @author
 */
public class BoardTest {

    private static final String[]
        GAME1 = { "a7-b7", "a1-a2",
                  "a7-a6", "a2-a3",
                  "a6-a5", "a3-a4" };

    private static void makeMoves(Board b, String[] moves) {
        for (String s : moves) {
            b.makeMove(s.charAt(0), s.charAt(1),
                       s.charAt(3), s.charAt(4));
        }
    }

    @Test public void testUndo() {
        Board b0 = new Board();
        Board b1 = new Board(b0);
        makeMoves(b0, GAME1);
        assertEquals(6, b0.bluePieces());
        assertEquals(4, b0.redPieces());
        Board b2 = new Board(b0);
        for (int i = 0; i < GAME1.length; i += 1) {
            b0.undo();
        }
        assertEquals(2, b0.bluePieces());
        assertEquals(2, b0.redPieces());
        assertEquals("failed to return to start", b1, b0);
        makeMoves(b0, GAME1);
        assertEquals("second pass failed to reach same position", b2, b0);
    }

    @Test public void testCopy() {
        Board b0 = new Board();
        Board b1 = new Board(b0);
        makeMoves(b0, GAME1);
        assertNotEquals(b0, b1);
    }

    @Test public void testToString() {
        Board b0 = new Board();
        makeMoves(b0, GAME1);
    }

    @Test public void testBlockAndTestGameOver() {
        Board b0 = new Board();
        b0.setBlock('d', '4');
        b0.setBlock('b', '2');
        b0.setBlock('b', '7');
        b0.setBlock('f', '5');
        b0.setBlock('e', '2');
        b0.setBlock('a', '6');
        b0.setBlock('a', '5');
        b0.setBlock('a', '4');
        b0.setBlock('b', '4');
        b0.setBlock('c', '4');
        b0.setBlock('c', '5');
        b0.setBlock('c', '7');
        b0.setBlock('d', '3');
        b0.setBlock('d', '2');
        assertEquals(b0.gameOver(), true);

    }

}
