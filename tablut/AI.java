package tablut;

import java.util.List;

import static java.lang.Math.*;

import static tablut.Square.sq;
import static tablut.Piece.*;

/** A Player that automatically generates moves.
 *  @author ANUJS
 */
class AI extends Player {

    /** A position-score magnitude indicating a win (for white if positive,
     *  black if negative). */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 20;
    /** A position-score magnitude indicating a forced win in a subsequent
     *  move.  This differs from WINNING_VALUE to avoid putting off wins. */
    private static final int WILL_WIN_VALUE = Integer.MAX_VALUE - 40;
    /** A magnitude greater than a normal value. */
    private static final int INFTY = Integer.MAX_VALUE;

    /** A new AI with no piece or controller (intended to produce
     *  a template). */
    AI() {
        this(null, null);
    }

    /** A new AI playing PIECE under control of CONTROLLER. */
    AI(Piece piece, Controller controller) {
        super(piece, controller);
    }

    @Override
    Player create(Piece piece, Controller controller) {
        return new AI(piece, controller);
    }

    @Override
    String myMove() {
        String s = findMove().toString();
        return s;
    }

    @Override
    boolean isManual() {
        return false;
    }

    /** Return a move for me from the current position, assuming there
     *  is a move. */
    private Move findMove() {
        Board b = new Board(board());
        _lastFoundMove = null;
        findMove(b, maxDepth(b), true, (b.turn() == WHITE ? 1 : -1),
                -INFTY, INFTY);
        return _lastFoundMove;
    }

    /** The move found by the last call to one of the ...FindMove methods
     *  below. */
    private Move _lastFoundMove;

    /** Find a move from position BOARD and return its value, recording
     *  the move found in _lastFoundMove iff SAVEMOVE. The move
     *  should have maximal value or have value > BETA if SENSE==1,
     *  and minimal value or value < ALPHA if SENSE==-1. Searches up to
     *  DEPTH levels.  Searching at level 0 simply returns a static estimate
     *  of the board value and does not set _lastMoveFound. */
    private int findMove(Board board, int depth, boolean saveMove,
                         int sense, int alpha, int beta) {
        assert Math.abs(sense) == 1;
        if (depth == 0 || board.winner() != null) {
            return staticScore(board);
        } else if (sense == 1) {
            int bestSoFar = -INFTY;
            for (Move M : board.legalMoves(WHITE)) {
                Board next = new Board(board);
                next.makeMove(M);
                int response =
                        findMove(next, depth - 1, false, -1, alpha, beta);
                if (response >= bestSoFar) {
                    _lastFoundMove = M;
                    bestSoFar = response;
                    alpha = max(alpha, bestSoFar);
                    if (beta <= alpha) {
                        break;
                    }
                }
            }
            return bestSoFar;
        } else {
            int bestSoFar = INFTY;
            for (Move M : board.legalMoves(BLACK)) {
                Board next = new Board(board);
                next.makeMove(M);
                int response =
                        findMove(next, depth - 1, false, 1, alpha, beta);
                if (response <= bestSoFar) {
                    _lastFoundMove = M;
                    bestSoFar = response;
                    beta = min(beta, bestSoFar);
                    if (beta <= alpha) {
                        break;
                    }
                }
            }
            return bestSoFar;
        }
    }


    /** Return a heuristically determined maximum search depth
     *  based on characteristics of BOARD. */
    private static int maxDepth(Board board) {
        return 2;
    }

    /** Return a heuristic value for BOARD. */
    private int staticScore(Board board) {
        List<String> encodedlist = board.getEncoded();
        String lastboard = encodedlist.get(encodedlist.size() - 1);
        int blackPieces = 0;
        int whitePieces = 0;
        for (int i = 1; i < lastboard.length(); i++) {
            switch (lastboard.charAt(i)) {
            case 'B': blackPieces++;
                break;
            case 'W': whitePieces++;
                break;
            default:
                break;
            }
        }

        Square king = board.kingPosition();

        if (king == null) {
            return -INFTY;
        }
        if (king.isEdge()) {
            return INFTY;
        }

        int unconqueredKing = 0;
        if (board.isLegalMove(king, sq(king.col(), 0))) {
            unconqueredKing++;
        }
        if (board.isLegalMove(king, sq(king.col(), 8))) {
            unconqueredKing++;
        }
        if (board.isLegalMove(king, sq(0, king.row()))) {
            unconqueredKing++;
        }
        if (board.isLegalMove(king, sq(8, king.row()))) {
            unconqueredKing++;
        }
        if (unconqueredKing > 0) {
            if (board.turn() == BLACK) {
                return WILL_WIN_VALUE;
            } else {
                return WINNING_VALUE;
            }
        }
        return (-blackPieces + whitePieces);
    }
}
