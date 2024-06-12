/*
 * The MIT License
 *
 * Copyright 2024 kaiyu.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.kaiyushe.minesweeper;

/**
 *
 * @author kaiyu
 *
 * AP CSA terminal minesweeper final project
 *
 */

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.time.ZonedDateTime;

// For score file writing/reading
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import screen.*;

public class Minesweeper implements NativeKeyListener {
    // Size of board
    private int sizeX, sizeY;

    /**
     * The current position of the cursor in relation to the board
     * (1, 1) is top left, and follows 2D array coordinates
     */
    private int cursorX, cursorY;

    /**
     * Used by multiple methods to determine how to handle input (such as
     * key presses).
     */
    private boolean isRunning;
    
    /**
     * Used to handle game end.
     */
    private boolean gameWon;

    /**
     * 2D array representing the board cells
     */
    private Cell[][] cells;

    /**
     * Difficulty played
     */
    private Difficulty difficulty;

    /**
     * Number of flags placed. Different from number of successful flags
     * This number is displayed by subtracting from the total number of flags
     * The total number of flags equals the number of mines
     */
    private int numFlagsPlaced;

    /**
     * Number of mines which are actually flagged
     * When this number reaches the number of mines, the game ends (won)
     */
    private int numFlagged;

    /**
     * LTextComponent component to show the number of flags left
     */
    LTextComponent flagsLeftComponent;

    /**
     * The CharArrayComponent component which draws the board.
     */
    private CharArrayComponent boardScreen;

    /**
     * Time at which the game started in millis
     */
    private long startTimeMillis;
    
    /**
     * Time at which the game ended. Ends when the selection is a mine or all of
     * the mines have been found
     */
    private long endTimeMillis;
    
    /**
     * ZonedDateTime at which the game started
     */
    private ZonedDateTime dateTimeStart;
    
    /**
     * Filename for the XML score file
     */
    private final String scoreFileName = "minesweeper_scores.xml";

    class CoordPoint {
        final int x, y;

        CoordPoint(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
    
    /**
     * Class representing one cell of the board
     */
    class Cell {
        /**
         * Set to true if the cell is a mine, false if not
         */
        boolean isMine;
        
        /**
         * Cells are open if they have been dug by the user or opened by
         * openCell(). Flagged cells are not open.
         */
        boolean isOpen;
        
        boolean isFlagged;
        
        // Selected by the user cursor
        boolean isSelected;
        
        /**
         * Number of adjacent mines. If 0, there are no adjacent mines.
         */
        byte numAdjacentMines;
        
        
        Cell(boolean isMine, boolean isOpen, boolean isFlagged, boolean isSelected, byte numAdjacentMines) {
            this.isMine = isMine;
            this.isOpen = isOpen;
            this.isFlagged = isFlagged;
            this.isSelected = isSelected;
            this.numAdjacentMines = numAdjacentMines;
        }
        
        /**
         * No-argument constructor - assumes values are going to be filled later
         */
        Cell() {
            this.isOpen = false;
            this.isFlagged = false;
            this.isSelected = false;
            this.isMine = false;
            this.numAdjacentMines = -1;
        }
        
        /**
         * Returns a char representation of the cell. Used by drawBoard().<br>
         * When showMine is true, '%' is returned if the cell is a mine
         * 
         * @return a char representation of this cell
         */
        char getCellChar(boolean showMine) {
            if (showMine && isMine) {
                return '%';
            }
            
            if (isSelected) {
                return 'S';
            }
            
            if (isFlagged) {
                return 'F';
            }
            
            if (isOpen) {
                return (char)(numAdjacentMines + '0');
            } else {
                return 'C';
            }
        }
        
        /**
         * Cell numbers:<br>
         * - 1 : FGBRIGHTBLUE<br>
         * - 2 : FGGREEN<br>
         * - 3 : FGBRIGHTRED<br>
         * - 4 : FGBLUE<br>
         * - 5 : FGRED<br>
         * - 6 : FGCYAN<br>
         * - 7 : FGBLACK<br>
         * - 8 : FGGREY<br>
         *
         * Objects:<br>
         * - flag : FGRED<br>
         * - mine : FGBLACK<br>
         *
         * Unopened and empty cells don't have display attributes; null is
         * returned.
         * 
         * @return the display attribute associated with the cell
         */
        SGR getCellDisplayAttr() {
            if (isFlagged) {
                return SGR.FGRED;
            }
            if (isOpen) {
                switch (numAdjacentMines) {
                    case 1:
                        return SGR.FGBRIGHTBLUE;
                    case 2:
                        return SGR.FGGREEN;
                    case 3:
                        return SGR.FGBRIGHTRED;
                    case 4:
                        return SGR.FGBLUE;
                    case 5:
                        return SGR.FGRED;
                    case 6:
                        return SGR.FGCYAN;
                    case 7:
                        return SGR.FGBLACK;
                    case 8:
                        return SGR.FGGREY;
                    default:
                        return null;
                }
            } else {
                return null;
            }
        }
    }
    
    private void printAdjacents() {
        for (Cell[] cr : cells) {
            for (Cell c : cr) {
                System.out.print(c.numAdjacentMines + "\t");
            }
            System.out.println();
        }
    }

    public Minesweeper() {
        this.cursorX = 0;
        this.cursorY = 0;
    }
    
    private void exitGame() {
        try {
            GlobalScreen.unregisterNativeHook();
        } catch (NativeHookException ex) {
            System.out.println("Unable to unregister native hook");
        }
        try {
            Screen.clrscr();
        } catch (IOException ex) {
            
        }
        System.out.println(gameWon ? "Game win" : "Game lost");
        System.exit(0);
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        // Immediately exit if user pressed "q"
        if (e.getKeyCode() == NativeKeyEvent.VC_Q) {
            exitGame();
        }

        if (isRunning) {
            switch (e.getKeyCode()) {
                // Mine cell selection
                // If the new selection is out of bounds, don't change it
                // Cursor coordinates follow 2D array coordinates, so up and
                // down are "flipped"
                case NativeKeyEvent.VC_UP:
                    // Decrement cursorY
                    if (cursorY > 0) {
                        this.cursorY--;
                        updateSelectedCell();
                    }
                    break;
                case NativeKeyEvent.VC_DOWN:
                    // Increment cursorY
                    if (cursorY < sizeY - 1) {
                        this.cursorY++;
                        updateSelectedCell();
                    }
                    break;
                case NativeKeyEvent.VC_RIGHT:
                    if (cursorX < sizeX - 1) {
                        this.cursorX++;
                        updateSelectedCell();
                    }
                    break;
                case NativeKeyEvent.VC_LEFT:
                    if (cursorX > 0) {
                        this.cursorX--;
                        updateSelectedCell();
                    }
                    break;

                // Mine cell selection
                // VC_D for open, VC_F for flag
                case NativeKeyEvent.VC_D:
                    try {
                        handleCellOpen(new CoordPoint(cursorX, cursorY));
                    } catch (NativeHookException ex) {
                        
                    }
                    break;

                case NativeKeyEvent.VC_F:
                    try {
                        handleCellFlag(new CoordPoint(cursorX, cursorY));
                    } catch (NativeHookException ex) {
                        
                    }
                    break;

                // Ignore all other keypresses
                default:
                    break;
            }
            drawGame();
//            if (!isRunning) {
//                System.exit(0);
//            }
        }
    }
    
    /**
     * Checks if the selected cell is a mine or not, then handles accordingly
     * 
     * @param coord 
     */
    private void handleCellOpen(CoordPoint coord) throws NativeHookException {
        if (cells[coord.y][coord.x].isMine) {
            // Selection is a mine
            this.isRunning = false;
            GlobalScreen.unregisterNativeHook();
            this.endTimeMillis = System.currentTimeMillis();
            drawGame();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                
            }
            exitGame();
        } else {
            openCell(coord);
        }
    }
    
    /**
     * Set flag cell at cursor coord, then handle num flag attrs. If all mines
     * have been flagged and number of flags used equals number of mines, game
     * ends (user won). This method handles the game won situation and stops
     * the program.
     * 
     * @param coord 
     */    
    private void handleCellFlag(CoordPoint coord) throws NativeHookException {
        if (!cells[coord.y][coord.x].isOpen) {
            if (!cells[coord.y][coord.x].isFlagged) {
                cells[coord.y][coord.x].isFlagged = true;
                this.numFlagsPlaced++;
                if (cells[coord.y][coord.x].isMine) {
                    this.numFlagged++;
                }
            } else {
                cells[coord.y][coord.x].isFlagged = false;
                this.numFlagsPlaced--;
                if (cells[coord.y][coord.x].isMine) {
                    this.numFlagged--;
                }
            }
            if (numFlagged == difficulty.numMines && numFlagged == numFlagsPlaced) {
                // Game won
                this.isRunning = false;
                this.gameWon = true;
                GlobalScreen.unregisterNativeHook();
                this.endTimeMillis = System.currentTimeMillis();
                long timeTaken = endTimeMillis - startTimeMillis;
                writeScore(timeTaken, dateTimeStart);
            }
        }
    }
    
    /**
     * Updates isSelected for the cell the cursor coords point to, and sets
     * false to all other cells
     */
    private void updateSelectedCell() {
        for (int r = 0; r < sizeY; r++) {
            for (int c = 0; c < sizeX; c++) {
                cells[r][c].isSelected = false;
            }
        }
        cells[cursorY][cursorX].isSelected = true;
    }

    /**
     * Initialises the Cell 2D array:<br>
     * - Instantiating all Cell objects<br>
     * - Fill the board with n mines, randomly spaced<br>
     * - Set all adjacent cell numbers<br>
     * - Sets isSelected to true for the selected cell
     * 
     * @param n the number of mines to fill
     */
    private void initBoard(int n) {
        // Initialise all Cell objects
        for (int r = 0; r < sizeY; r++) {
            for (int c = 0; c < sizeX; c++) {
                this.cells[r][c] = new Cell();
            }
        }
        
        // Mine filling
        Set<CoordPoint> mineCoords = new HashSet<>();
        
        while (mineCoords.size() != n) {
            mineCoords.add(
                new CoordPoint(
                    (int)(Math.random() * sizeX),
                    (int)(Math.random() * sizeY)
                )
            );
        }
        
        for (CoordPoint p : mineCoords) {
            cells[p.y][p.x].isMine = true;
        }
        
        // Set adjacent cell numbers
        fillAdjacentMineNumbers();
        
        // Set selected cell
        cells[cursorY][cursorX].isSelected = true;
    }
    
    /**
     * Counts the number of adjacent mines at CoordPoint cell
     * If the point passed is a mine, -1 is returned
     *
     * @param c the mine coordinate to check
     * 
     * @return number of adjacent mines
     */
    private byte countAdjacentMines(CoordPoint coord) {
        Cell cell = cells[coord.y][coord.x];
        if (cell.isMine) {
            return -1;
        }
        
        byte numMines = 0;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                int r = coord.y + dr, c = coord.x + dc;
                if (r >= 0 && r < sizeY && c >= 0 && c < sizeX) {
                    numMines += cells[r][c].isMine ? 1 : 0;
                }
            }
        }
        
        return numMines;
    }

    /**
     * Checks cell coord and write the adjacent mine number if there is one.
     * Recursively check the adjacent cells.
     *
     * @param coord
     */
    private void openCell(CoordPoint coord) {
        Cell cell = cells[coord.y][coord.x];
        
        if (!cell.isMine && !cell.isOpen) {
            cell.isOpen = true;
            if (cell.numAdjacentMines != 0) {
                return;
            }
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    int r = coord.y + dr, c = coord.x + dc;
                    if (r == coord.y && c == coord.x) continue;
                    if (r >= 0 && r < sizeY && c >= 0 && c < sizeX) {
                        openCell(new CoordPoint(c, r));
                    }
                }
            }
        }
    }
    
    /**
     * Sets the given cell's isOpen to true. Does not recursively open.
     * 
     * @param coord 
     */
//    private void openCell(CoordPoint coord) {
//        Cell cell = cells[coord.y][coord.x];
//        cell.isOpen = true;
//    }
    
    class Score {
        final ZonedDateTime dateTime;
        final Difficulty difficulty;
        final long timeTaken;
        
        public Score(ZonedDateTime dateTime, Difficulty difficulty, long timeTaken) {
            this.dateTime = dateTime;
            this.difficulty = difficulty;
            this.timeTaken = timeTaken;
        }
    }
    
    private void showScores() throws IOException {
        File scoreFile = new File(scoreFileName);
        if (scoreFile.exists()) {
            Path filePath = scoreFile.toPath();
            String scoreString = new String(Files.readAllBytes(filePath));
            System.out.println(scoreString);
            System.exit(0);
        } else {
            System.out.println("Score file does not exist");
        }
    }

    /**
     * Writes the score (time in millis) and date to the scores file.
     */
    private void writeScore(long duration, ZonedDateTime datetime) {
        try (FileWriter scoreFile = new FileWriter(scoreFileName, true)) {
            scoreFile.write(
                String.format(
                    "Date: %s\nTime taken: %d sec\n\n",
                    datetime.format(DateTimeFormatter.ISO_DATE_TIME),
                    duration / 1000
                )
            );
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    /**
     * Creates the board to be drawn and updates boardScreen. If showMines is
     * true, all mines are shown.
     * 
     * @param showMines boolean to set whether to show mines or not
     */
    private void drawBoard(boolean showMines) {
        char[][] boardChars = new char[sizeY][sizeX];
        boardScreen.clearAttr();
        
        for (int r = 0; r < sizeY; r++) {
            boardScreen.addDisplayAttr(SGR.BGWHITE, r, 0, sizeX);
            for (int c = 0; c < sizeX; c++) {
                boardChars[r][c] = cells[r][c].getCellChar(showMines);
                if (cells[r][c].getCellDisplayAttr() != null) {
                    boardScreen.addDisplayAttr(cells[r][c].getCellDisplayAttr(), r, c);
                } else {
//                    System.out.println("null displ attr");
                }
            }
        }
        
        // Cursor
        boardScreen.addDisplayAttr(SGR.UNDERLINE, cursorY, cursorX);
        
        boardScreen.setArray(boardChars);
    }
        
    /**
     * Calls drawBoard() and updates the flagsLeft LTextComponent, then calls
     * Screen.refresh(). If ifRunning is false, drawBoard() is called with
     * parameter `true` and time taken is shown.<br>
     * Also displays game end + won behaviour
     */
    private void drawGame() {
        // Only show mines if not isRunning
        drawBoard(!isRunning);
        if (isRunning) {
            flagsLeftComponent.setText(String.format("Flags left: %d", difficulty.numMines - numFlagsPlaced));
        } else {
            Screen.removeComponent(flagsLeftComponent);
            // Show time taken
            long timeTaken = endTimeMillis - startTimeMillis;
            Screen.addComponent(new LTextComponent(1, sizeY + 3, 1, String.format("Time taken: %d sec", timeTaken / 1000)));
            if (gameWon) {
                Screen.addComponent(new LTextComponent(1, sizeY + 4, 1, "Game Won"));
            }
        }
        try {
            Screen.refresh();
        } catch (IOException exc) {
            exc.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Fills the adjacentMineNumbers array
     */
    private void fillAdjacentMineNumbers() {
        for (int r = 0; r < sizeY; r++) {
            for (int c = 0; c < sizeX; c++) {
                if (cells[r][c].isMine) {
                    continue;
                }
                this.cells[r][c].numAdjacentMines = countAdjacentMines(new CoordPoint(c, r));
            }
        }
    }
        
    private void runGame() throws IOException, InterruptedException, ParserConfigurationException, SAXException {
        // Title screen and game choice (choose difficulty/view past scores)
        LTextComponent title = new LTextComponent(2, 2, 1, "Minesweeper");
        String[] selections = {
            "Beginner",
            "Intermediate",
            "Expert",
            "View scores",
            "Exit"
        };
        SelectionComponent gameSelect = new SelectionComponent(2, 3, 1, selections);
        Screen.addComponent(title);
        Screen.addComponent(gameSelect);
        Screen.refresh();
        int selectInt = gameSelect.getChoice();
        Screen.clrscr();
        Screen.clearComponents();
        if (selectInt == 3) {
            showScores();
            System.exit(0);
        } else if (selectInt == 4) {
            System.exit(0);
        }
        
        // Start the game - init the boards/status vars and draw
        this.difficulty = Difficulty.getDifficulty(selections[selectInt]);
        this.sizeX = difficulty.sizeX;
        this.sizeY = difficulty.sizeY;
        
        this.cells = new Cell[sizeY][sizeX];
        initBoard(difficulty.numMines);
        // To debug adjacent cell numbers
//        printAdjacents();
//        exitGame();
        
        // Initially fill boardScreen with an empty char array. Will be changed
        // for each drawBoard() call
        this.boardScreen = new CharArrayComponent(1, 1, sizeX, sizeY, 1, new char[sizeY][sizeX]);
        this.numFlagsPlaced = this.numFlagged = 0;
        
        // flagsLeftComponent text will be set when the game starts
        this.flagsLeftComponent = new LTextComponent(1, sizeY + 2, 1, "");

        Screen.addComponent(boardScreen);
        Screen.addComponent(flagsLeftComponent);

        // Start the game and timer
        this.startTimeMillis = System.currentTimeMillis();
        this.dateTimeStart = ZonedDateTime.now();
        this.isRunning = true;
        this.gameWon = false;
        drawGame();
    }
    
    public static void testScreen() {
        // CharArrayComponent test
        int rows = 10, cols = 10;
        char[][] testCharArray = new char[rows][cols];
        CharArrayComponent ca = new CharArrayComponent(1, 1, rows, cols, 1, testCharArray);
        for (int r = 0; r < rows; r++) {
            ca.addDisplayAttr(SGR.BGWHITE, r, 0, cols);
            for (int c = 0; c < cols; c++) {
                testCharArray[r][c] = (char)((int)(Math.random() * 10) + '0');
                ca.addDisplayAttr(SGR.FGBRIGHTRED, r, c);
            }
        }
        Screen.addComponent(ca);
        try {
            Screen.refresh();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) throws NativeHookException, IOException, InterruptedException, ParserConfigurationException, SAXException {
        GlobalScreen.registerNativeHook();
        Minesweeper game = new Minesweeper();
        GlobalScreen.addNativeKeyListener(game);

        // Size of screen to be used
        int lines, columns;
        try {
            // Because echo is on, leave one line at bottom for any key presses
            // printed out
            lines = Integer.parseInt(System.getenv("LINES")) - 1;
        } catch (NumberFormatException e) {
            lines = 30;
        }
        try {
            columns = Integer.parseInt(System.getenv("COLUMNS"));
        } catch (NumberFormatException e) {
            columns = 50;
        }

        Screen.initscr(lines, columns);

        // Uncomment for debugging
//        System.out.println(String.format("lines %d, columns %d", lines, columns));
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {}

        game.runGame();
//        testScreen();
    }
}
