package com.megabyte6.connect4.controller;

import static com.megabyte6.connect4.util.Range.range;

import java.util.stream.Stream;

import com.megabyte6.connect4.App;
import com.megabyte6.connect4.model.Game;
import com.megabyte6.connect4.model.GamePiece;

import javafx.beans.binding.DoubleBinding;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Dimension2D;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

public class GameController {

    private Game game = new Game("John", "James");

    @FXML
    private AnchorPane root;

    @FXML
    private Pane markerContainer;

    private GamePiece marker;
    private int markerPositionIndex = game.getColumnCount() / 2;
    private final DoubleBinding[] markerBindings = new DoubleBinding[game.getColumnCount()];
    private final Line[] columnSeparators = new Line[game.getColumnCount() - 1];

    @FXML
    private Pane gameBoard;

    @FXML
    private void initialize() {
        // Initialize gameBoard width and height.
        StackPane gameBoardContainer = (StackPane) gameBoard.getParent();

        gameBoardContainer.widthProperty().addListener((observable, oldValue, newValue) -> {
            handleWindowSizeChanged(new Dimension2D(
                    newValue.doubleValue(), gameBoardContainer.getHeight()));
        });
        gameBoardContainer.heightProperty().addListener((observable, oldValue, newValue) -> {
            handleWindowSizeChanged(new Dimension2D(
                    gameBoardContainer.getWidth(), newValue.doubleValue()));
        });

        // Draw horizontal grid lines.
        for (int i : range(game.getRowCount() + 1)) {
            double multiplier = ((double) i) / game.getRowCount();
            DoubleBinding y = gameBoard.heightProperty().multiply(multiplier);

            Line line = new Line();
            line.setStroke(Color.WHITE);
            line.startYProperty().bind(y);
            line.endXProperty().bind(gameBoard.widthProperty());
            line.endYProperty().bind(y);

            gameBoard.getChildren().add(line);
        }
        // Draw vertical grid lines.
        Line[] verticalLines = new Line[game.getColumnCount()];
        for (int i : range(game.getColumnCount())) {
            double multiplier = ((double) i) / game.getColumnCount();
            DoubleBinding x = gameBoard.widthProperty().multiply(multiplier);

            Line line = new Line();
            line.setStroke(Color.WHITE);
            line.startXProperty().bind(x);
            line.endXProperty().bind(x);
            line.endYProperty().bind(gameBoard.heightProperty());

            gameBoard.getChildren().add(line);

            verticalLines[i] = line;
        }

        // Populate columnSeparators.
        for (int i : range(columnSeparators.length)) {
            columnSeparators[i] = verticalLines[i + 1];
        }

        // Create GamePieces.
        DoubleBinding cellSizeBinding = gameBoard.heightProperty().divide(game.getRowCount());
        final int border = 5;
        DoubleBinding radiusBinding = cellSizeBinding.divide(2).subtract(border);
        for (double col : range(game.getColumnCount())) {
            double xMultiplier = ((double) col) / game.getColumnCount();
            DoubleBinding xOffset = cellSizeBinding.divide(2);

            DoubleBinding xBinding = gameBoard.widthProperty()
                    .multiply(xMultiplier).add(xOffset);

            for (double row : range(game.getRowCount())) {
                double yMultiplier = ((double) row) / game.getRowCount();
                DoubleBinding yOffset = cellSizeBinding.divide(2);

                DoubleBinding yBinding = gameBoard.heightProperty()
                        .multiply(yMultiplier).add(yOffset);

                GamePiece blankPiece = new GamePiece();
                blankPiece.layoutXProperty().bind(xBinding);
                blankPiece.layoutYProperty().bind(yBinding);
                blankPiece.radiusProperty().bind(radiusBinding);
                blankPiece.setFill(App.BACKGROUND_COLOR);
                blankPiece.setStroke(Color.WHITE);

                game.setGamePiece(blankPiece, (int) col, (int) row);
            }
        }

        // Draw GamePieces.
        Stream.of(game.getGameBoard())
                .flatMap(Stream::of)
                .filter(gamePiece -> gamePiece != null)
                .forEach(gamePiece -> gameBoard.getChildren().add(gamePiece));

        // Initialize marker container.
        markerContainer.maxWidthProperty().bind(gameBoard.widthProperty());

        // Define marker locations
        DoubleBinding offset = cellSizeBinding.divide(2);
        for (int column : range(game.getColumnCount())) {
            double multiplier = ((double) column) / game.getColumnCount();

            markerBindings[column] = markerContainer.widthProperty()
                    .multiply(multiplier).add(offset);
        }

        // Initialize marker.
        marker = new GamePiece();
        marker.layoutXProperty().bind(markerBindings[markerPositionIndex]);
        marker.layoutYProperty().bind(markerContainer.heightProperty().divide(2));
        marker.radiusProperty().bind(radiusBinding);
        marker.setFill(game.getCurrentPlayer().getColor());

        markerContainer.getChildren().add(marker);

        // Set up key listeners.
        markerContainer.setOnMouseMoved(event -> updateMarkerPosition(event.getSceneX()));
        gameBoard.setOnMouseMoved(event -> updateMarkerPosition(event.getSceneX()));
    }

    // TODO : Check if this can be optimized.
    private void updateMarkerPosition(double mouseXPosition) {
        int mouseColumn = -1;

        for (int i : range(columnSeparators.length)) {
            Line line = columnSeparators[i];
            double linePosition = line.localToScene(line.getBoundsInLocal()).getCenterX();

            if (mouseXPosition < linePosition) {
                mouseColumn = i;
                break;
            }
        }

        // If mouseColumn is still -1, the mouse must be in the last column.
        if (mouseColumn == -1)
            mouseColumn = game.getColumnCount() - 1;

        // If the marker is already there, don't move it.
        if (mouseColumn == markerPositionIndex)
            return;

        moveMarkerToIndex(mouseColumn);
    }

    private void moveMarkerToIndex(int index) {
        if (index < 0 || index >= markerBindings.length)
            return;
        markerPositionIndex = index;
        updateMarkerBindings();
    }

    private void updateMarkerBindings() {
        marker.layoutXProperty().bind(markerBindings[markerPositionIndex]);
    }

    // Update gameBoard size because the GridPane doesn't resize automatically
    // when circles are added.
    private void handleWindowSizeChanged(Dimension2D containerDimensions) {
        final double size = Math.min(
                containerDimensions.getWidth() / game.getColumnCount(),
                containerDimensions.getHeight() / game.getRowCount());
        gameBoard.setMaxSize(size * game.getColumnCount(), size * game.getRowCount());
    }

    @FXML
    private void handleBackButton(ActionEvent event) {
        game.moveHistoryPointerBack();
    }

    @FXML
    private void handleForwardButton(ActionEvent event) {
        game.moveHistoryPointerForward();
    }

    @FXML
    private void handleCurrentMoveButton(ActionEvent event) {
        while (!game.historyPointerIsAtLatestMove()) {
            game.moveHistoryPointerForward();
        }
    }

}
