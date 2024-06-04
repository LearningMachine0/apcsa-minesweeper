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
     * true for mine, false for empty
     */
    private boolean[][] mines;

    /**
     * Numerical representation of the board state<br>
     * unopened -2<br>
     * flagged  -1<br>
     * no mine  0<br>
     * numMines >0 (adjacent)
     */
    private byte[][] board;

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
     * The char[][] array which stores the characters to be drawn
     */
    private char[][] boardChars;

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
    private String scoreFileName = "minesweeper_scores.xml";

    class CoordPoint {
        final int x, y;

        public CoordPoint(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    public Minesweeper() {
        this.cursorX = 0;
        this.cursorY = 0;
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        // Immediately exit if user pressed "q"
        if (e.getKeyCode() == NativeKeyEvent.VC_Q) {
            System.exit(0);
        }

        if (isRunning) {
            switch (e.getKeyCode()) {
                // Mine cell selection
                // If the new selection is out of bounds, don't change it
                case NativeKeyEvent.VC_UP:
                    if (cursorY < sizeY) {
                        this.cursorY++;
                    }
                    break;
                case NativeKeyEvent.VC_DOWN:
                    if (cursorY > 0) {
                        this.cursorY--;
                    }
                    break;
                case NativeKeyEvent.VC_RIGHT:
                    if (cursorX < sizeX) {
                        this.cursorX++;
                    }
                    break;
                case NativeKeyEvent.VC_LEFT:
                    if (cursorX > 0) {
                        this.cursorX--;
                    }
                    break;

                // Mine cell selection
                // VC_D for open, VC_F for flag
                case NativeKeyEvent.VC_D:
                    handleCellOpen(new CoordPoint(cursorX, cursorY));
                    break;
                case NativeKeyEvent.VC_F:
                    handleCellFlag(new CoordPoint(cursorX, cursorY));
                    break;

                // Ignore all other keypresses
                default:
                    break;
            }
            drawGame();
        } else {
            return;
        }
    }
    
    /**
     * Checks if the selected cell is a mine or not, then handles accordingly
     * 
     * @param coord 
     */
    private void handleCellOpen(CoordPoint coord) {
        if (mines[coord.y][coord.x]) {
            // Selection is a mine
            this.isRunning = false;
            this.endTimeMillis = System.currentTimeMillis();
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
    private void handleCellFlag(CoordPoint coord) {
        if (board[coord.y][coord.x] == -2) {
            this.board[coord.y][coord.x] = -1;
            this.numFlagsPlaced++;
            if (mines[coord.y][coord.x]) {
                this.numFlagged++;
            }
            if (numFlagged == difficulty.numMines && numFlagged == numFlagsPlaced) {
                // Game won
                this.isRunning = false;
                this.gameWon = true;
                long timeTaken = endTimeMillis - startTimeMillis;
                writeScore(timeTaken, dateTimeStart);
                drawGame();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException exc) {
                    System.exit(1);
                }
                System.exit(0);
            }
        }
    }

    /**
     * Fill the mines array with n mines (set element to true), randomly spaced.
     * The number of mines is determined by the difficulty level
     *
     * @param n
     */
    private void fillBoard(int n) {
        Set<CoordPoint> points = new HashSet<>();

        // Have no repeating points
        while(points.size() != n) {
            points.add(
                new CoordPoint(
                    (int)(Math.random() * mines[0].length),
                    (int)(Math.random() * mines.length)
                )
            );
        }

        for (CoordPoint p : points) {
            this.mines[p.y][p.x] = true;
        }
    }

    /**
     * Counts the number of adjacent mines at CoordPoint cell
     * If the point passed is a mine, -1 is returned
     *
     * @param c the mine coordinate to check
     */
    public byte countAdjacentMines(CoordPoint c) {
        if (mines[c.y][c.x])
            return -1;

        byte numMines = 0;
        for (int y = c.y - 1; y <= c.y + 1; y++) {
            for (int x = c.x - 1; x <= c.x + 1; x++) {
                // Check bounds
                if (x < 0 || x >= sizeX || y < 0 || y >= sizeY) {
                    continue;
                }
                if (mines[y][x]) {
                    numMines++;
                }
            }
        }
        return numMines;
    }

    /**
     * Update the board status with the selected coordinate. Assumes the
     * coordinate is not a mine
     *
     * @param coord the coordinate
     */
    private void updateBoard(CoordPoint coord) throws IllegalArgumentException {
        int x = coord.x, y = coord.y;

        // Check if coordinate is actually unopened. If not, raise exception
        if (board[y][x] != -2)
            throw new IllegalArgumentException();

        // Recursively open the surrounding cells until the perimeter cells have
        // adjacent mine cells
        openCell(coord);
    }

    /**
     * Checks cell coord and write the adjacent mine number if there is one.
     * Recursively check the adjacent cells.
     *
     * @param coord
     */
    private void openCell(CoordPoint coord) {
        // Return if cell is already opened or flagged
        if (board[coord.y][coord.x] != -2) return;

        byte numMines = countAdjacentMines(coord);
        if (numMines != 0) {
            this.board[coord.y][coord.x] = numMines;
            return;
        }

        for (int y = coord.y - 1; y <= coord.y + 1; y++) {
            for (int x = coord.x - 1; x <= coord.x + 1; x++) {
                if (x < 0 || x >= sizeX || y < 0 || y >= sizeY)
                    continue;
                openCell(new CoordPoint(x, y));
            }
        }
    }
    
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
    
    /**
     * Shows the last (up to) 5 plays. 
     */
    private void showScores() throws ParserConfigurationException, SAXException, IOException {
        ArrayList<String> scoresStrings = new ArrayList<>();
        ArrayList<Score> scores = new ArrayList<>();

        File scoreFile = new File(scoreFileName);
        if (!scoreFile.exists()) {
            Screen.addComponent(new LTextComponent(1, 1, 1, "Score file does not exist."));
            try {
                Screen.refresh();
                Thread.sleep(1000);
            } catch (IOException | InterruptedException ex) {
                System.exit(1);
            }
            System.exit(1);
        }
        
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(scoreFile);
        NodeList nodeList = document.getElementsByTagName("score");
        
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                scores.add(
                    new Score(
                            ZonedDateTime.parse(element.getElementsByTagName("datetime").item(0).getTextContent(), DateTimeFormatter.ISO_ZONED_DATE_TIME),
                            Difficulty.getDifficulty(element.getElementsByTagName("difficulty").item(0).getTextContent()),
                            Long.parseLong(element.getElementsByTagName("timetaken").item(0).getTextContent())
                    )
                );
            }
        }
        
        scores.sort((score1, score2) -> score1.dateTime.compareTo(score2.dateTime));
        
        for (int s = scores.size() - 1; s >= scores.size() - 5 && s >= 0; s--) {
            Score score = scores.get(s);
            scoresStrings.add(
                    String.format(
                            "%s - %s, %d sec",
                            score.dateTime.format(DateTimeFormatter.ISO_DATE),
                            score.difficulty, score.timeTaken / 1000
                    )
            );
        }
        
        // Show scores
        int y = 1;
        for (String s : scoresStrings) {
            Screen.addComponent(new LTextComponent(1, y, 1, s));
            y++;
        }
        Screen.refresh();
    }

    /**
     * Writes the score (time in millis) and date to the scores.xml file.
     */
    private void writeScore(long duration, ZonedDateTime datetime) {
        try {
            File scoreFile = new File(scoreFileName);
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document;
            Element root;
            if (scoreFile.exists()) {
                document = documentBuilder.parse(scoreFile);
                document.getDocumentElement().normalize();
                root = document.getDocumentElement();
            } else {
                document = documentBuilder.newDocument();
                root = document.createElement("scores");
                document.appendChild(root);
            }
            Element score = document.createElement("score");

            Element dateTime = document.createElement("datetime");
            dateTime.appendChild(document.createTextNode(datetime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)));
            score.appendChild(dateTime);

            Element timeTaken = document.createElement("timetaken");
            timeTaken.appendChild(document.createTextNode("" + duration));
            score.appendChild(timeTaken);
            
            Element difficulty = document.createElement("difficulty");
            difficulty.appendChild(document.createTextNode(difficulty.toString()));
            score.appendChild(difficulty);

            root.appendChild(score);

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            DOMSource domSource = new DOMSource(document);
            StreamResult streamResult = new StreamResult(scoreFile);
            transformer.transform(domSource, streamResult);
        } catch (IOException | ParserConfigurationException | TransformerException | DOMException | SAXException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Creates the board to be drawn and updates boardScreen<br>
     * If showMines is true, all unflagged mines are shown<br>
     * The background for the board is white
     * <br><br>
     * <b>Colours</b><br>
     * Names are relative to the names under SGR<br>
     * Numbers:<br>
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
     * - flag : 'F' : FGRED<br>
     * - mine : '%' : FGBLACK<br>
     * - cursor : UNDERLINE<br>
     *
     * @param showMines the flag to show mines
     */
    private void drawBoard(boolean showMines) {
        char[][] drawnBoard = new char[sizeY][sizeX];

        for (int y = 0; y < sizeY; y++) {
            boardScreen.addDisplayAttr(SGR.BGWHITE, y, 0, sizeX);
            for (int x = 0; x < sizeX; x++) {
                if (board[y][x] == -1) {
                    drawnBoard[y][x] = 'F';
                    boardScreen.addDisplayAttr(SGR.FGRED, y, x, x + 1);
                } else if (board[y][x] > 0) {
                    // Convert int to char by adding '0' offset and casting
                    drawnBoard[y][x] = (char)(board[y][x] + '0');
                    SGR FGColour;
                    switch (board[y][x]) {
                        case 1:
                            FGColour = SGR.FGBRIGHTBLUE;
                            break;
                        case 2:
                            FGColour = SGR.FGGREEN;
                            break;
                        case 3:
                            FGColour = SGR.FGBRIGHTRED;
                            break;
                        case 4:
                            FGColour = SGR.FGBLUE;
                            break;
                        case 5:
                            FGColour = SGR.FGRED;
                            break;
                        case 6:
                            FGColour = SGR.FGCYAN;
                            break;
                        case 7:
                            FGColour = SGR.FGBLACK;
                            break;
                        case 8:
                            FGColour = SGR.FGGREY;
                            break;
                        default:
                            FGColour = SGR.FGBLACK;
                            break;
                    }
                    boardScreen.addDisplayAttr(FGColour, y, x, x + 1);
                } else if (showMines && mines[x][y]) {
                    drawnBoard[y][x] = '%';
                    boardScreen.addDisplayAttr(SGR.FGRED, y, x, x + 1);
                }
            }
        }
        // Insert cursor position
        boardScreen.addDisplayAttr(SGR.UNDERLINE, cursorY, cursorX, cursorX + 1);

        this.boardChars = drawnBoard;
//        debugBoard();
        // Update the boardScreen component
        this.boardScreen.setArray(boardChars);
    }

    // for debugging
    private void debugBoard() {
        for (int y = 0; y < sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                System.out.print(this.boardChars[y][x]);
            }
            System.out.println();
        }
        System.exit(0);
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
            Screen.addComponent(new LTextComponent(2, sizeY + 3, 1, String.format("Time taken: %d sec", timeTaken / 1000)));
            if (gameWon) {
                Screen.addComponent(new LTextComponent(2, sizeY + 4, 1, "Game Won"));
            }
        }
        try {
            Screen.refresh();
        } catch (IOException exc) {
            exc.printStackTrace();
            System.exit(1);
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
//        Screen.removeComponent(gameSelect);
        Screen.clrscr();
        Screen.clearComponents();
        if (selections[selectInt].equals("View scores")) {
            showScores();
        } else if (selections[selectInt].equals("Exit")) {
            System.exit(0);
        }

        // Start the game - init the boards/status vars and draw
        this.difficulty = Difficulty.getDifficulty(selections[selectInt]);
        this.sizeX = difficulty.sizeX;
        this.sizeY = difficulty.sizeY;
        this.mines = new boolean[sizeY][sizeX];
        this.board = new byte[sizeY][sizeX];
        fillBoard(difficulty.numMines);
        this.boardChars = new char[sizeY][sizeX];
        this.boardScreen = new CharArrayComponent(2, 2, sizeX, sizeY, 1, boardChars);
        this.numFlagsPlaced = this.numFlagged = 0;

        // flagsLeftComponent text will be set when the game starts
        this.flagsLeftComponent = new LTextComponent(2, sizeY + 3, 1, "");

        Screen.addComponent(boardScreen);
        Screen.addComponent(flagsLeftComponent);
        drawBoard(false);

        // Start the timer
        this.startTimeMillis = System.currentTimeMillis();
        this.dateTimeStart = ZonedDateTime.now();
        this.isRunning = true;
        this.gameWon = false;
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
    }
}
