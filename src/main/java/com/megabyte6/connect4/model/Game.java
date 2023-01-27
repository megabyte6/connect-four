package com.megabyte6.connect4.model;

import static com.megabyte6.connect4.util.Range.range;

import java.util.LinkedList;

import com.megabyte6.connect4.App;
import com.megabyte6.connect4.util.tuple.Triplet;
import com.megabyte6.connect4.util.tuple.Tuple;

import javafx.scene.paint.Color;

public class Game {

    private boolean active = true;
    private boolean gameOver = false;

    private final Player player1;
    private final Player player2;

    private Player currentPlayer;

    // [column][row]
    private final GamePiece[][] gameBoard = new GamePiece[7][6];

    private int selectedColumn = 3;

    // Player, Column, Row
    private LinkedList<Triplet<Player, Integer, Integer>> moveHistory = new LinkedList<>();
    private int historyPointer = -1;

    public Game(String player1Name, String player2Name) {
        player1 = new Player(player2Name, Color.YELLOW);
        player2 = new Player(player1Name, Color.RED);

        currentPlayer = player1;
    }

    public void swapTurns() {
        currentPlayer = currentPlayer == player1
                ? player2
                : player1;
    }

    public int findNextFreeRow(int columnIndex) {
        final GamePiece[] column = getGameBoardColumn(columnIndex);

        for (int i : range(column.length - 1, -1)) {
            if (column[i].getOwner().equals(Player.NONE))
                return i;
        }

        return -1;
    }

    public boolean getActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void gameOver() {
        gameOver = true;
        active = false;
    }

    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public void setSelectedColumn(int index) {
        if (!active || index < 0 || index >= gameBoard.length)
            return;
        selectedColumn = index;
    }

    public int getSelectedColumn() {
        return selectedColumn;
    }

    public GamePiece[][] getGameBoard() {
        return gameBoard;
    }

    public GamePiece[] getGameBoardColumn(int columnNumber) {
        return gameBoard[columnNumber];
    }

    public GamePiece[] getGameBoardRow(int rowIndex) {
        GamePiece[] row = new GamePiece[gameBoard.length];
        for (int i : range(gameBoard.length)) {
            row[i] = gameBoard[i][rowIndex];
        }

        return row;
    }

    public int getColumnCount() {
        return gameBoard.length;
    }

    public int getRowCount() {
        return gameBoard[0].length;
    }

    public GamePiece getGamePiece(int columnIndex, int rowIndex) {
        return gameBoard[columnIndex][rowIndex];
    }

    public void setGamePiece(GamePiece gamePiece, int columnIndex, int rowIndex) {
        gameBoard[columnIndex][rowIndex] = gamePiece;
    }

    public void addMoveToHistory(Player player, int column, int row) {
        moveHistory.add(Tuple.of(player, column, row));
        historyPointer = moveHistory.size() - 1;
    }

    public void moveHistoryPointerBack() {
        // Check if the user is already at the beginning.
        if (historyPointer == -1)
            return;

        active = false;

        var selectedAction = moveHistory.get(historyPointer);
        GamePiece selectedGamePiece = getGamePiece(selectedAction.b(), selectedAction.c());
        selectedGamePiece.setOwner(Player.NONE);
        selectedGamePiece.setFill(App.BACKGROUND_COLOR);

        historyPointer--;
    }

    public void moveHistoryPointerForward() {
        // Check if the user is already at the end.
        if (historyPointerIsAtLatestMove())
            return;

        historyPointer++;

        var selectedAction = moveHistory.get(historyPointer);
        GamePiece selectedGamePiece = getGamePiece(selectedAction.b(), selectedAction.c());
        selectedGamePiece.setOwner(selectedAction.a());

        if (historyPointerIsAtLatestMove())
            active = true;
    }

    public boolean historyPointerIsAtLatestMove() {
        return historyPointer == moveHistory.size() - 1;
    }

}
