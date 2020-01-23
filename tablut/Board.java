package tablut;


import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;
import java.util.Arrays;


import static tablut.Piece.*;
import static tablut.Square.*;
import static tablut.Move.mv;


/**
 * The state of a Tablut Game.
 *
 * @author ANUJ
 */
class Board {

    /**
     * The number of squares on a side of the board.
     */
    static final int SIZE = 9;

    /**
     * The throne (or castle) square and its four surrounding squares..
     */
    static final Square THRONE = sq(4, 4),
            NTHRONE = sq(4, 5),
            STHRONE = sq(4, 3),
            WTHRONE = sq(3, 4),
            ETHRONE = sq(5, 4);

    /**
     * .
     */
    private ArrayList<Square> throneArray =
            new ArrayList<Square>(Arrays.
                    asList(THRONE, NTHRONE, ETHRONE, STHRONE, WTHRONE));


    /**
     * Initial positions of attackers.
     */
    static final Square[] INITIAL_ATTACKERS = {
            sq(0, 3), sq(0, 4), sq(0, 5), sq(1, 4),
            sq(8, 3), sq(8, 4), sq(8, 5), sq(7, 4),
            sq(3, 0), sq(4, 0), sq(5, 0), sq(4, 1),
            sq(3, 8), sq(4, 8), sq(5, 8), sq(4, 7)
    };

    /**
     * Initial positions of defenders of the king.
     */
    static final Square[] INITIAL_DEFENDERS = {
        NTHRONE, ETHRONE, STHRONE, WTHRONE,
            sq(4, 6), sq(4, 2), sq(2, 4), sq(6, 4)
    };

    /**
     * Initializes a game board with SIZE squares on a side in the
     * initial position.
     */
    Board() {
        init();
    }

    /**
     * Initializes a copy of MODEL.
     */
    Board(Board model) {
        copy(model);
    }

    /**
     * Copies MODEL into me.
     */
    void copy(Board model) {
        if (model == this) {
            return;
        }
        init();
        _encoded.remove(0);
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                _board[i][j] = model._board[i][j];
            }
        }
        _moveCount = model._moveCount;
        _turn = model._turn;
        _winner = model._winner;
        _repeated = model._repeated;
        _kingPosition = model.kingPosition();
        _moveLimit = model.moveLimit();
        for (int i = 0; i < model._encoded.size(); i++) {
            _encoded.add(model._encoded.get(i));
        }
    }

    /**
     * Clears the board to the initial position.
     */
    void init() {
        _board = new Piece[SIZE][SIZE];
        for (Square i : INITIAL_ATTACKERS) {
            _board[i.col()][i.row()] = BLACK;
        }
        for (Square i : INITIAL_DEFENDERS) {
            _board[i.col()][i.row()] = WHITE;
        }
        _board[THRONE.col()][THRONE.row()] = KING;
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (_board[i][j] == null) {
                    _board[i][j] = EMPTY;
                }
            }
        }
        _moveCount = 0;
        _turn = BLACK;
        _winner = null;
        _repeated = false;
        _kingPosition = THRONE;
        _encoded = new ArrayList<String>();
        _encoded.add(encodedBoard());
    }

    /**
     * Set the move limit to LIM.
     *
     * @param n It is an error if 2*LIM <= moveCount().
     */
    void setMoveLimit(int n) {
        if (2 * n <= moveCount()) {
            throw new IllegalArgumentException("Illegal Limit");
        }
        _moveLimit = n;
    }

    /**
     * Return a Piece representing whose move it is (WHITE or BLACK).
     */
    Piece turn() {
        return _turn;
    }

    /**
     * Return the winner in the current position, or null if there is no winner
     * yet.
     */
    Piece winner() {
        return this._winner;
    }

    /**
     * Returns true iff this is a win due to a repeated position.
     */
    boolean repeatedPosition() {
        return _repeated;
    }

    /**
     * Record current position and set winner() next mover if the current
     * position is a repeat.
     */
    private void checkRepeated() {
        String encoded = encodedBoard();
        if (_encoded.contains(encoded)) {
            _winner = turn();
            _repeated = true;
        }
        _encoded.add(encoded);
    }

    /**
     * Return the number of moves since the initial position that have not been
     * undone.
     */
    int moveCount() {
        return _moveCount;
    }

    /**
     * Return location of the king.
     */
    Square kingPosition() {
        return _kingPosition;
    }

    /**
     * Return the contents the square at S.
     */
    final Piece get(Square s) {
        return get(s.col(), s.row());
    }

    /**
     * Return the contents of the square at (COL, ROW), where
     * 0 <= COL, ROW <= 9.
     */
    final Piece get(int col, int row) {
        return _board[col][row];
    }

    /**
     * Return the contents of the square at COL ROW.
     */
    final Piece get(char col, char row) {
        return get(col - 'a', row - '1');
    }

    /**
     * Set square S to P.
     */
    final void put(Piece p, Square s) {
        _board[s.col()][s.row()] = p;
    }

    /**
     * Set square S to P and record for undoing.
     */
    final void revPut(Piece p, Square s) {
        put(p, s);
        checkRepeated();
    }

    /**
     * Set square COL ROW to P.
     */
    final void put(Piece p, char col, char row) {
        put(p, sq(col - 'a', row - '1'));
    }

    /**
     * Return true iff FROM - TO is an unblocked rook move on the current
     * board.  For this to be true, FROM-TO must be a rook move and the
     * squares along it, other than FROM, must be empty.
     */
    boolean isUnblockedMove(Square from, Square to) {
        if ((to.row() > SIZE && to.col() > SIZE)
                || (from.row() > SIZE && from.col() > SIZE)) {
            throw new IllegalArgumentException(
                    "Square row or column out of range.");
        }
        int dir = from.direction(to);
        for (Square i : ROOK_SQUARES[from.index()][dir]) {
            if (get(i) != EMPTY) {
                return false;
            }
            if (i == to) {
                break;
            }
        }
        return true;
    }

    /**
     * Return true iff FROM is a valid starting square for a move.
     */
    boolean isLegal(Square from) {
        return get(from).side() == _turn;
    }

    /**
     * Return true iff FROM-TO is a valid move.
     */
    boolean isLegal(Square from, Square to) {
        if (isLegal(from)) {
            if (to == THRONE) {
                if (get(from) != KING) {
                    return false;
                }
                return true;
            }
            return isUnblockedMove(from, to);
        }
        return false;
    }

    /**
     * Return true iff FROM-TO is a valid move irrespective of colour.
     */
    boolean isLegalMove(Square from, Square to) {
        if (to == THRONE) {
            if (get(from) != KING) {
                return false;
            }
        }
        return isUnblockedMove(from, to);
    }

    /**
     * Return true iff MOVE is a legal move in the current
     * position.
     */
    boolean isLegal(Move move) {
        return isLegal(move.from(), move.to());
    }

    /**
     * Move FROM-TO, assuming this is a legal move.
     */
    void makeMove(Square from, Square to) {
        if (_kingPosition != null && _kingPosition.isEdge()) {
            _winner = WHITE;
        }
        if (isLegal(from, to)) {
            put(get(from), to);
            put(EMPTY, from);
            if (get(to) == KING) {
                _kingPosition = to;
            }
        }
        if (_kingPosition != null && _kingPosition.isEdge()) {
            _winner = WHITE;
        }
        _moveCount++;
        _turn = ((_turn == WHITE) ? BLACK : WHITE);
        int count = 0;
        for (int i = 0; i < 4; i++) {
            if (ROOK_SQUARES[to.index()][i].size() >= 2) {
                capture(to, ROOK_SQUARES[to.index()][i].get(1));
            }
        }
        checkRepeated();
        boolean setWinner = false;
        if (_repeated) {
            setWinner = true;
        }
        if (setWinner && _winner == null) {
            _winner = ((_turn == WHITE) ? BLACK : WHITE);
        }
    }

    /**
     * Move according to MOVE, assuming it is a legal move.
     */
    void makeMove(Move move) {
        makeMove(move.from(), move.to());
    }

    /**
     * Capture the piece between SQ0 and SQ2, assuming a piece just moved to
     * SQ0 and the necessary conditions are satisfied.
     */
    private void capture(Square sq0, Square sq2) {
        int dir = sq0.direction(sq2);
        Square sq1 = ROOK_SQUARES[sq0.index()][dir].get(0);
        if (get(sq1) != EMPTY) {
            Piece side0 = get(sq0).side(), side2 = get(sq2).side(),
                    side1 = get(sq1);
            if (side0 != side1.side()) {
                if (!(side1 == KING && throneArray.contains(sq1))) {
                    if (ishostile(get(sq1), sq2)) {
                        put(EMPTY, sq1);
                        if (side1 == KING) {
                            _winner = BLACK;
                            _kingPosition = null;
                        }
                    }
                } else {
                    if (throneArray.contains(sq1)) {
                        int count = 0;
                        for (int i = 0; i < 4; i++) {
                            if (ishostile(WHITE,
                                    ROOK_SQUARES[sq1.index()][i].get(0))) {
                                count++;
                            }
                        }
                        if (count == 4) {
                            put(EMPTY, sq1);
                            _kingPosition = null;
                            _winner = BLACK;
                        }
                    }
                }
            }
        }
    }

    /**
     * There is @param center
     * and @param sq
     * and @return boolean.
     */
    private boolean ishostile(Piece center, Square sq) {
        if (get(sq) == EMPTY) {
            if (sq == THRONE) {
                return true;
            }
            return false;
        } else if (center == WHITE && sq == THRONE) {
            int count = 0;
            for (int i = 0; i < 4; i++) {
                if (get(ROOK_SQUARES[sq.index()][i].get(0)) == BLACK) {
                    count++;
                }
                if (count == 3) {
                    return true;
                }
            }
            if (count >= 3) {
                return true;
            }
            return false;
        } else {
            return center.opponent() == get(sq).side();
        }
    }

    /**
     * Undo one move.  Has no effect on the initial board.
     */
    void undo() {
        if (_moveCount > 0) {
            undoPosition();
            decode(_encoded.get(_encoded.size() - 1));
            _moveCount--;
            _turn = _turn.opponent();

        }
    }

    /**
     * Remove record of current position in the set of positions encountered,
     * unless it is a repeated position or we are at the first move.
     */
    private void undoPosition() {
        if (_moveCount > 0 || !_repeated) {
            _encoded.remove(encodedBoard());
        }
    }

    /**
     * Clear the undo stack and board-position counts. Does not modify the
     * current position or win status.
     */
    void clearUndo() {
        _encoded = new ArrayList<String>();
        _moveCount = 0;
        _repeated = false;
    }

    /**
     * and @param s and.
     */
    void decode(String s) {
        for (int i = 1; i < s.length(); i++) {
            int row = (i - 1) / SIZE;
            int column = (i - 1) % SIZE;
            switch (s.charAt(i)) {
            case 'K':
                _board[column][row] = KING;
                break;
            case '-':
                _board[column][row] = EMPTY;
                break;
            case 'W':
                _board[column][row] = WHITE;
                break;
            case 'B':
                _board[column][row] = BLACK;
                break;
            default:
            }
        }
    }

    /**
     * Return a new mutable list of all legal moves on the current board for
     * SIDE (ignoring whose turn it is at the moment).
     */
    List<Move> legalMoves(Piece side) {
        List<Move> sideMove = new ArrayList<Move>();
        HashSet<Square> locations = pieceLocations(side);
        for (Square from : locations) {
            for (int dir = 0; dir < 4; dir++) {
                for (Square sq : ROOK_SQUARES[from.index()][dir]) {
                    if (get(sq) != EMPTY) {
                        break;
                    }
                    Move move = mv(from, sq);
                    if (move != null && isLegal(move)) {
                        sideMove.add(move);
                    }
                }
            }
        }
        return sideMove;
    }

    /**
     * Return true iff SIDE has a legal move.
     */
    boolean hasMove(Piece side) {
        return legalMoves(side).size() > 0;
    }

    @Override
    public String toString() {
        return toString(true);
    }

    /**
     * Return a text representation of this Board.  If COORDINATES, then row
     * and column designations are included along the left and bottom sides.
     */
    String toString(boolean coordinates) {
        Formatter out = new Formatter();
        for (int r = SIZE - 1; r >= 0; r -= 1) {
            if (coordinates) {
                out.format("%2d", r + 1);
            } else {
                out.format("  ");
            }
            for (int c = 0; c < SIZE; c += 1) {
                out.format(" %s", get(c, r));
            }
            out.format("%n");
        }
        if (coordinates) {
            out.format("  ");
            for (char c = 'a'; c <= 'i'; c += 1) {
                out.format(" %c", c);
            }
            out.format("%n");
        }
        return out.toString();
    }

    /**
     * Return the locations of all pieces on SIDE.
     */
    private HashSet<Square> pieceLocations(Piece side) {
        assert side != EMPTY;
        HashSet<Square> pieceLoc = new HashSet<Square>();
        for (Square s : SQUARE_LIST) {
            if (get(s.col(), s.row()).side() == side) {
                pieceLoc.add(s);
            }
        }
        return pieceLoc;
    }

    /**
     * Return the contents of _board in the order of SQUARE_LIST as a sequence
     * of characters: the toString values of the current turn and Pieces.
     */
    String encodedBoard() {
        char[] result = new char[Square.SQUARE_LIST.size() + 1];
        result[0] = turn().toString().charAt(0);
        for (Square sq : SQUARE_LIST) {
            Piece colour = get(sq);
            String word = colour.toString();
            char first = word.charAt(0);
            result[sq.index() + 1] = first;
        }
        return new String(result);
    }

    /**
     * Piece whose turn it is (WHITE or BLACK).
     */
    private Piece _turn;
    /**
     * Cached value of winner on this board, or null if it has not been
     * computed.
     */
    private Piece _winner;
    /**
     * Number of (still undone) moves since initial position.
     */
    private int _moveCount;
    /**
     * True when current board is a repeated position (ending the game).
     */
    private boolean _repeated;

    /**
     * and @return Piece[][].
     */
    public Piece[][] getBoard() {
        return _board;
    }

    /**
     * and @return int.
     */
    int moveLimit() {
        return _moveLimit;
    }

    /**
     * .
     */
    private Piece[][] _board;

    /**
     * and @return List<String>.
     */

    public List<String> getEncoded() {
        return _encoded;
    }

    /**
     * .
     */
    private List<String> _encoded;
    /**
     * .
     */
    private Square _kingPosition;
    /**
     * .
     */
    private int _moveLimit;

}
