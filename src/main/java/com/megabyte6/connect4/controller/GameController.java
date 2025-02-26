package com.megabyte6.connect4.controller;

import static com.megabyte6.connect4.util.Range.range;
import static javafx.util.Duration.millis;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import com.megabyte6.connect4.App;
import com.megabyte6.connect4.controller.dialog.ConfirmController;
import com.megabyte6.connect4.model.Game;
import com.megabyte6.connect4.model.GamePiece;
import com.megabyte6.connect4.model.Player;
import com.megabyte6.connect4.util.Position;
import com.megabyte6.connect4.util.SceneManager;
import com.megabyte6.connect4.util.WinChecker;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Dimension2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

public class GameController implements Controller {

    private final Game game = new Game(
            App.getPlayer1(), App.getPlayer2(),
            App.getSettings().getColumnCount(), App.getSettings().getRowCount());

    @FXML
    private AnchorPane root;

    @FXML
    private Pane markerContainer;
    private GamePiece marker;
    private final DoubleBinding[] markerBindings = new DoubleBinding[game.getColumnCount()];

    @FXML
    private Pane gameBoard;

    @FXML
    private Label player1Score;
    @FXML
    private Label currentTurn;
    @FXML
    private Label timerLabel;
    @FXML
    private Label player2Score;

    @FXML
    private void initialize() {
        // If player 1 won last game, make player 2 the starting player.
        if (App.getWinner().equals(App.getPlayer1())
                && game.getCurrentPlayer().equals(App.getPlayer1()))
            game.swapTurns();

        App.setWinner(Player.NONE.get());

        // Initialize gameBoard width and height.
        final StackPane gameBoardContainer = (StackPane) gameBoard.getParent();

        gameBoardContainer.widthProperty().addListener((observable, oldValue, newValue) -> handleWindowSizeChanged(
                new Dimension2D(newValue.doubleValue(), gameBoardContainer.getHeight())));
        gameBoardContainer.heightProperty().addListener((observable, oldValue, newValue) -> handleWindowSizeChanged(
                new Dimension2D(gameBoardContainer.getWidth(), newValue.doubleValue())));

        // Give the board a color.
        gameBoard.setBackground(Background.fill(App.getSettings().getBoardColor()));

        // Draw horizontal grid lines.
        for (int i : range(game.getRowCount() + 1)) {
            final double multiplier = ((double) i) / game.getRowCount();
            final DoubleBinding y = gameBoard.heightProperty().multiply(multiplier);

            final Line line = new Line();
            line.setStroke(App.getSettings().getLineColor());
            line.startYProperty().bind(y);
            line.endXProperty().bind(gameBoard.widthProperty());
            line.endYProperty().bind(y);

            gameBoard.getChildren().add(line);
        }
        // Draw vertical grid lines.
        for (int i : range(game.getColumnCount() + 1)) {
            final double multiplier = ((double) i) / game.getColumnCount();
            final DoubleBinding x = gameBoard.widthProperty().multiply(multiplier);

            final Line line = new Line();
            line.setStroke(App.getSettings().getLineColor());
            line.startXProperty().bind(x);
            line.endXProperty().bind(x);
            line.endYProperty().bind(gameBoard.heightProperty());

            gameBoard.getChildren().add(line);
        }

        // Create GamePieces.
        final DoubleBinding cellSizeBinding = gameBoard.heightProperty().divide(game.getRowCount());
        final int border = 5;
        final DoubleBinding radiusBinding = cellSizeBinding.divide(2).subtract(border);
        for (int col : range(game.getColumnCount())) {
            final double xMultiplier = ((double) col) / game.getColumnCount();
            final DoubleBinding xOffset = cellSizeBinding.divide(2);

            final DoubleBinding xBinding = gameBoard.widthProperty()
                    .multiply(xMultiplier).add(xOffset);

            for (int row : range(game.getRowCount())) {
                final double yMultiplier = ((double) row) / game.getRowCount();
                final DoubleBinding yOffset = cellSizeBinding.divide(2);

                final DoubleBinding yBinding = gameBoard.heightProperty()
                        .multiply(yMultiplier).add(yOffset);

                final GamePiece blankPiece = new GamePiece();
                blankPiece.layoutXProperty().bind(xBinding);
                blankPiece.layoutYProperty().bind(yBinding);
                blankPiece.radiusProperty().bind(radiusBinding);
                blankPiece.setFill(App.BACKGROUND_COLOR);
                blankPiece.setStroke(App.getSettings().getLineColor());

                game.setGamePiece(blankPiece, col, row);
            }
        }

        // Add obstacles.
        if (App.getSettings().isObstaclesEnabled()) {
            final int numOfObstacles = App.getSettings().getNumOfObstacles();
            for (int i = 0; i < numOfObstacles; i++) {
                // Randomly choose a column that isn't full.
                int column, row;
                do {
                    column = (int) (Math.random() * game.getColumnCount());
                    row = game.findNextFreeRow(column);
                } while (row == -1);

                final GamePiece selectedPiece = game.getGamePiece(column, row);
                selectedPiece.setOwner(Player.OBSTACLE.get());
                selectedPiece.setFill(App.getSettings().getObstacleColor());
            }
        }

        // Draw GamePieces.
        Stream.of(game.getGameBoard())
                .flatMap(Stream::of)
                .filter(Objects::nonNull)
                .forEach(gameBoard.getChildren()::add);

        // Initialize marker container.
        markerContainer.maxWidthProperty().bind(gameBoard.widthProperty());

        // Define marker locations
        final DoubleBinding offset = cellSizeBinding.divide(2);
        for (int column : range(game.getColumnCount())) {
            final double multiplier = ((double) column) / game.getColumnCount();

            markerBindings[column] = markerContainer.widthProperty()
                    .multiply(multiplier).add(offset);
        }

        // Initialize marker.
        marker = new GamePiece();
        marker.layoutXProperty().bind(markerBindings[game.getSelectedColumn()]);
        marker.layoutYProperty().bind(markerContainer.heightProperty().divide(2));
        marker.radiusProperty().bind(radiusBinding);
        marker.setFill(game.getCurrentPlayer().getColor());

        markerContainer.getChildren().add(marker);

        // Initialize labels.
        updatePlayerScoreLabels();
        updateCurrentTurnLabel();
        // Initialize timer.
        if (App.getSettings().isTimerEnabled())
            resetTimer();

        // Set up key listeners.
        markerContainer.setOnMouseMoved(event -> updateMarkerPosition(event.getX()));

        gameBoard.setOnMouseMoved(event -> updateMarkerPosition(event.getX()));

        root.setOnMouseClicked(event -> placePiece(game.getSelectedColumn()));

        root.setOnKeyPressed(event -> {
            if (event.isShortcutDown())
                switch (event.getCode()) {
                    case Q -> handleReturnToStartScreen();
                    case N -> handleNewGame();
                    default -> {
                    }
                }
        });

        root.requestFocus();
    }

    private boolean checkForWin() {
        final var lastMove = game.getLastMove();
        if (lastMove == null)
            return false;
        final Player player = lastMove.a();
        final int column = lastMove.b();
        final int row = lastMove.c();

        final WinChecker winChecker = new WinChecker(game.getGameBoard(), player, new Position(column, row),
                App.getSettings().getWinRequirement(), App.getSettings().isBoardWrappingEnabled());

        if (winChecker.findWinPosition() == null)
            return false;

        App.setWinner(player);
        return true;
    }

    private boolean checkForTie() {
        for (int i : range(game.getColumnCount())) {
            if (game.findNextFreeRow(i) != -1)
                return false;
        }
        return true;
    }

    private void gameWon() {
        game.gameOver();
        App.getWinner().incrementScore();
        updatePlayerScoreLabels();

        showGameFinishedScreen();
    }

    private void gameTie() {
        game.gameOver();
        App.setWinner(Player.NONE.get());

        showGameFinishedScreen();
    }

    private void showGameFinishedScreen() {
        setDisable(true);

        final var loadedData = SceneManager.loadFXMLAndController("GameFinished");
        final Node root = loadedData.a();
        final GameFinishedController controller = (GameFinishedController) loadedData.b();
        controller.setOnClose(() -> setDisable(false));

        SceneManager.addScene(root);
    }

    private void updateMarkerPosition(double mouseXPos) {
        if (game.isPaused())
            return;

        final double columnWidth = gameBoard.getMaxWidth() / game.getColumnCount();
        final int mouseColumn = (int) Math.floor(mouseXPos / columnWidth);

        // If the marker is already there, don't move it.
        if (mouseColumn == game.getSelectedColumn())
            return;

        moveMarkerToIndex(mouseColumn);
    }

    private void moveMarkerToIndex(int index) {
        if (index < 0 || index >= markerBindings.length)
            return;
        game.setSelectedColumn(index);
        updateMarkerBindings();
    }

    private void updateMarkerBindings() {
        marker.layoutXProperty().bind(markerBindings[game.getSelectedColumn()]);
    }

    private void placePiece(int column) {
        if (game.isGameOver() || game.isControlsLocked())
            return;
        if (game.isPaused() && !game.isGameOver()) {
            SceneManager.popup("Please return to the current move.");
            return;
        }

        final int row = game.findNextFreeRow(column);

        // Column is full.
        if (row == -1)
            return;

        // Prevent the user from accidentally dropping their opponent's piece.
        game.setControlsLocked(true);
        App.delay(500, () -> game.setControlsLocked(false));

        game.addMoveToHistory(game.getCurrentPlayer(), column, row);

        final GamePiece selectedPiece = game.getGamePiece(column, row);
        selectedPiece.setOwner(game.getCurrentPlayer());
        selectedPiece.setFill(App.BACKGROUND_COLOR);
        playDroppingAnimation(marker, selectedPiece, game.getCurrentPlayer());

        swapTurns();

        if (checkForWin()) {
            gameWon();
            return;
        }
        if (checkForTie()) {
            gameTie();
            return;
        }
    }

    private void playDroppingAnimation(GamePiece origin, GamePiece destination, Player player) {
        final Bounds initialBounds = origin.localToScene(origin.getBoundsInLocal());
        final double initialX = initialBounds.getCenterX();
        final double initialY = initialBounds.getCenterY();

        final Bounds finalBounds = destination.localToScene(destination.getBoundsInLocal());
        final double finalY = finalBounds.getCenterY();

        final Circle circle = new Circle();
        circle.setFill(origin.getFill());
        circle.setRadius(origin.getRadius());
        circle.setCenterX(initialX);
        root.getChildren().add(circle);

        final DoubleProperty yPos = circle.centerYProperty();

        final List<KeyFrame> keyFrames = new LinkedList<>();
        final double displacement = finalY - initialY;
        final double initialFallTime = (displacement - circle.getRadius()) / 2;
        double time = 0;
        double deltaTime;
        double bounceHeight = displacement;

        // Change the check case of the for loop to change the number of bounces.
        for (int i = 0; i < 2; i++) {
            // Change constant to adjust speed.
            deltaTime = ((i + 1) * bounceHeight / displacement) * initialFallTime;

            keyFrames.add(new KeyFrame(
                    millis(time),
                    new KeyValue(yPos, finalY - bounceHeight, Interpolator.EASE_OUT)));
            time += deltaTime;
            keyFrames.add(new KeyFrame(
                    millis(time),
                    new KeyValue(yPos, finalY, Interpolator.EASE_IN)));
            time += deltaTime;

            // Change constant to change the hight of each bounce.
            bounceHeight = displacement / (i + 8);
        }

        final Timeline timeline = new Timeline();
        timeline.getKeyFrames().addAll(keyFrames);
        timeline.setOnFinished(event -> {
            destination.setFill(player.getColor());
            root.getChildren().remove(circle);
        });
        timeline.play();
    }

    private void swapTurns() {
        game.swapTurns();
        marker.setOwner(game.getCurrentPlayer());

        updateCurrentTurnLabel();
        if (App.getSettings().isTimerEnabled() && !game.isPaused() && !game.isGameOver())
            resetTimer();
    }

    private void resetTimer() {
        game.resetTimer(
                () -> updateTimerLabel(),
                () -> timerTimeout());

        updateTimerLabel();
    }

    private void updateTimerLabel() {
        timerLabel.setText("Time left: " + game.getTimer().getFormattedTime());
    }

    private void timerTimeout() {
        if (App.getSettings().isTimerAutoDrop()) {
            int column = game.getSelectedColumn();
            if (game.findNextFreeRow(column) == -1) {
                final List<Integer> freeColumns = game.findFreeColumns();
                column = freeColumns.get((int) (Math.random() * freeColumns.size()));
                marker.layoutXProperty().bind(markerBindings[column]);
            }

            placePiece(column);
        } else {
            swapTurns();
        }
    }

    private void updatePlayerScoreLabels() {
        final String player1Plural = App.getPlayer1().getScore() == 1 ? "" : "s";
        player1Score.setText(App.getPlayer1().getName() + " won " + App.getPlayer1().getScore()
                + " time" + player1Plural);
        final String player2Plural = App.getPlayer2().getScore() == 1 ? "" : "s";
        player2Score.setText(App.getPlayer2().getName() + " won " + App.getPlayer2().getScore()
                + " time" + player2Plural);
    }

    public void updateCurrentTurnLabel() {
        final String name = game.getCurrentPlayer().getName();
        final String pluralPostfix = name.charAt(name.length() - 1) == 's' ? "'" : "'s";
        currentTurn.setText(name + pluralPostfix + " turn");
    }

    // Update gameBoard size because the GridPane doesn't resize automatically
    // when circles are added.
    private void handleWindowSizeChanged(Dimension2D containerDimensions) {
        if (containerDimensions == null)
            return;
        final double size = Math.min(
                containerDimensions.getWidth() / game.getColumnCount(),
                containerDimensions.getHeight() / game.getRowCount());
        gameBoard.setMaxSize(size * game.getColumnCount(), size * game.getRowCount());
    }

    @FXML
    private void handleBackButton() {
        game.moveHistoryPointerBack();
    }

    @FXML
    private void handleForwardButton() {
        game.moveHistoryPointerForward();
    }

    @FXML
    private void handleCurrentMoveButton() {
        while (!game.historyPointerIsAtLatestMove()) {
            game.moveHistoryPointerForward();
        }
    }

    @FXML
    private void handleReturnToStartScreen() {
        setDisable(true);

        final var loadedData = SceneManager.loadFXMLAndController("dialog/Confirm");
        final Node root = loadedData.a();
        final ConfirmController controller = (ConfirmController) loadedData.b();

        controller.setText("Are you sure you want to leave the game?");
        controller.setOnOk(() -> SceneManager.switchScenes("Start", millis(400)));
        controller.setOnCancel(() -> setDisable(false));

        SceneManager.addScene(root);
    }

    @FXML
    private void handleNewGame() {
        setDisable(true);

        final var loadedData = SceneManager.loadFXMLAndController("dialog/Confirm");
        final Node root = loadedData.a();
        final ConfirmController controller = (ConfirmController) loadedData.b();

        controller.setText("Are you sure you want to reset the game?");
        controller.setOnOk(() -> SceneManager.switchScenes("Game", millis(400)));
        controller.setOnCancel(() -> setDisable(false));

        SceneManager.addScene(root);
    }

    @Override
    public void setDisable(boolean disabled) {
        if (!disabled && !game.isGameOver()) {
            game.setPaused(false);
            if (App.getSettings().isTimerEnabled() && game.getTimer() != null)
                game.getTimer().resume();
        } else if (disabled) {
            game.setPaused(true);
            if (App.getSettings().isTimerEnabled() && game.getTimer() != null)
                game.getTimer().stop();
        }

        root.setDisable(disabled);
        root.setOpacity(disabled ? App.DISABLED_OPACITY : 1);
    }
}
