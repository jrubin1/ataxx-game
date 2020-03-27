package ataxx;

/* Author: P. N. Hilfinger */

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.function.Consumer;

import static ataxx.PieceColor.*;
import static ataxx.Game.State.*;
import static ataxx.Command.Type.*;
import static ataxx.GameException.error;

/** Controls the play of the game.
 *  @author Josh Rubin
 */
class Game {

    /** States of play. */
    static enum State {
        SETUP, PLAYING, FINISHED;
    }

    /** A new Game, using BOARD to play on, reading initially from
     *  BASESOURCE and using REPORTER for error and informational messages. */
    Game(Board board, CommandSource baseSource, Reporter reporter) {
        _inputs.addSource(baseSource);
        _board = board;
        _reporter = reporter;
    }

    /** Run a session of Ataxx gaming.  Use an AtaxxGUI iff USEGUI. */
    void process(boolean useGUI) {
        Player red, blue;
        _state = SETUP;
        red = new Manual(this, RED);
        blue = new AI(this, BLUE);
        redAuto = false;
        blueAuto = true;
        GameLoop:
        while (true) {
            doClear(null);
            SetupLoop:
            while (_state == SETUP) {
                doCommand();
            }
            if (redAuto) {
                red = new AI(this, RED);
            }
            if (!redAuto) {
                red = new Manual(this, RED);
            }
            if (blueAuto) {
                blue = new AI(this, BLUE);
            }
            if (!blueAuto) {
                blue = new Manual(this, BLUE);
            }
            _state = PLAYING;
            while (_state != SETUP && !_board.gameOver()) {
                try {
                    Move move = null;
                    if (board().whoseMove() == RED) {
                        move = red.myMove();
                    } else if (board().whoseMove() == BLUE) {
                        move = blue.myMove();
                    } else {
                        reportError("whoseMove error");
                    }

                    if (_state == PLAYING) {
                        _board.makeMove(move);
                    }
                } catch (AssertionError illegalMove) {
                    reportError("that move is illegal");
                }
            }
            if (_state != SETUP) {
                reportWinner();
            }

            if (_state == PLAYING) {
                _state = FINISHED;
            }

            while (_state == FINISHED) {
                doCommand();
            }
        }

    }

    /** Return a view of my game board that should not be modified by
     *  the caller. */
    Board board() {
        return _board;
    }

    /** Perform the next command from our input source. */
    void doCommand() {
        try {
            Command cmnd =
                Command.parseCommand(_inputs.getLine("ataxx: "));
            _commands.get(cmnd.commandType()).accept(cmnd.operands());
        } catch (GameException excp) {
            _reporter.errMsg(excp.getMessage());
        } catch (AssertionError illegalMove) {
            reportError("that move is illegal");
        }
    }

    /** Read and execute commands until encountering a move or until
     *  the game leaves playing state due to one of the commands. Return
     *  the terminating move command, or null if the game first drops out
     *  of playing mode. If appropriate to the current input source, use
     *  PROMPT to prompt for input. */
    Command getMoveCmnd(String prompt) {
        while (_state == PLAYING) {
            try {
                Command cmnd = Command.parseCommand(_inputs.getLine(prompt));
                if (cmnd.commandType() == PIECEMOVE) {
                    return cmnd;
                } else {
                    _commands.get(cmnd.commandType()).accept(cmnd.operands());
                }
            } catch (GameException excp) {
                _reporter.errMsg(excp.getMessage());
            } catch (AssertionError illegalMove) {
                reportError("that move is illegal");
            }
        }
        return null;
    }

    /** Return random integer between 0 (inclusive) and MAX>0 (exclusive). */
    int nextRandom(int max) {
        return _randoms.nextInt(max);
    }

    /** Report a move, using a message formed from FORMAT and ARGS as
     *  for String.format. */
    void reportMove(String format, Object... args) {
        _reporter.moveMsg(format, args);
    }

    /** Report an error, using a message formed from FORMAT and ARGS as
     *  for String.format. */
    void reportError(String format, Object... args) {
        _reporter.errMsg(format, args);
    }

    /* Command Processors */

    /** Perform the command 'auto OPERANDS[0]'. */
    void doAuto(String[] operands) {

        if (operands[0].equalsIgnoreCase("Red")) {
            redAuto = true;
        } else if (operands[0].equalsIgnoreCase("Blue")) {
            blueAuto = true;
        } else {
            reportError("wrong format for auto command");
        }
    }

    /** Perform a 'help' command. */
    void doHelp(String[] unused) {
        InputStream helpIn =
            Game.class.getClassLoader().getResourceAsStream("ataxx/help.txt");
        if (helpIn == null) {
            System.err.println("No help available.");
        } else {
            try {
                BufferedReader r
                    = new BufferedReader(new InputStreamReader(helpIn));
                while (true) {
                    String line = r.readLine();
                    if (line == null) {
                        break;
                    }
                    System.out.println(line);
                }
                r.close();
            } catch (IOException e) {
                /* Ignore IOException */
            }
        }
    }

    /** Perform the command 'load OPERANDS[0]'. */
    void doLoad(String[] operands) {
        try {
            FileReader reader = new FileReader(operands[0]);
            ReaderSource readerSource = new ReaderSource(reader, false);
            _inputs.addSource(readerSource);
        } catch (IOException e) {
            throw error("Cannot open file %s", operands[0]);
        }
    }

    /** Perform the command 'manual OPERANDS[0]'. */
    void doManual(String[] operands) {
        if (operands[0].equalsIgnoreCase("red")) {
            redAuto = false;
        } else if (operands[0].equalsIgnoreCase("blue")) {
            blueAuto = false;
        } else {
            reportError("wrong format for manual command");
        }
    }

    /** Exit the program. */
    void doQuit(String[] unused) {
        System.exit(0);
    }

    /** Perform the command 'start'. */
    void doStart(String[] unused) {
        checkState("start", SETUP);
        _state = PLAYING;
    }

    /** Perform the move OPERANDS[0]. */
    void doMove(String[] operands) {

        board().makeMove(operands[0].charAt(0), operands[1].charAt(0),
                operands[2].charAt(0), operands[3].charAt(0));
    }

    /** Cause current player to pass. */
    void doPass(String[] unused) {

        board().pass();
    }

    /** Perform the command 'clear'. */
    void doClear(String[] unused) {

        _board = new Board();
    }

    /** Perform the command 'dump'. */
    void doDump(String[] unused) {

        StringBuilder sb = new StringBuilder("===");
        sb.append(System.lineSeparator());
        sb.append(board().toString());
        sb.append(System.lineSeparator());
        sb.append("===");
        reportMove(sb.toString());
    }

    /** Execute 'seed OPERANDS[0]' command, where the operand is a string
     *  of decimal digits. Silently substitutes another value if
     *  too large. */
    void doSeed(String[] operands) {

        checkState("seed", SETUP);
        long seed = Integer.parseInt(operands[0]);
    }

    /** Execute the command 'block OPERANDS[0]'. */
    void doBlock(String[] operands) {

        board().setBlock(operands[0]);
    }

    /** Execute the artificial 'error' command. */
    void doError(String[] unused) {
        throw error("Command not understood");
    }

    /** Report the outcome of the current game. */
    void reportWinner() {
        String msg;
        if (board().bluePieces() > board().redPieces()) {
            msg = "Blue wins.";
        } else if (board().bluePieces() == board().redPieces()) {
            msg = "Draw.";
        } else {
            msg = "Red wins.";
        }

        _reporter.outcomeMsg(msg);
    }

    /** Check that game is currently in one of the states STATES, assuming
     *  CMND is the command to be executed. */
    private void checkState(Command cmnd, State... states) {
        for (State s : states) {
            if (s == _state) {
                return;
            }
        }
        throw error("'%s' command is not allowed now.", cmnd.commandType());
    }

    /** Check that game is currently in one of the states STATES, using
     *  CMND in error messages as the name of the command to be executed. */
    private void checkState(String cmnd, State... states) {
        for (State s : states) {
            if (s == _state) {
                return;
            }
        }
        throw error("'%s' command is not allowed now.", cmnd);
    }

    /** Mapping of command types to methods that process them. */
    private final HashMap<Command.Type, Consumer<String[]>> _commands =
        new HashMap<>();

    {
        _commands.put(AUTO, this::doAuto);
        _commands.put(BLOCK, this::doBlock);
        _commands.put(CLEAR, this::doClear);
        _commands.put(DUMP, this::doDump);
        _commands.put(HELP, this::doHelp);
        _commands.put(MANUAL, this::doManual);
        _commands.put(PASS, this::doPass);
        _commands.put(PIECEMOVE, this::doMove);
        _commands.put(SEED, this::doSeed);
        _commands.put(START, this::doStart);
        _commands.put(LOAD, this::doLoad);
        _commands.put(QUIT, this::doQuit);
        _commands.put(ERROR, this::doError);
        _commands.put(EOF, this::doQuit);
    }

    /** Input source. */
    private final CommandSources _inputs = new CommandSources();

    /** My board. */
    private Board _board;
    /** Current game state. */
    private State _state;
    /** Used to send messages to the user. */
    private Reporter _reporter;
    /** Source of pseudo-random numbers (used by AIs). */
    private Random _randoms = new Random();

    /** Boolean to keep track of whether red is Auto. */
    private boolean redAuto;

    /** Boolean to keep track of whether blue is Auto. */
    private boolean blueAuto;
}
