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
package screen;

import java.util.ArrayList;

/**
 *
 * @author kaiyu
 * 
 * Component base class representing all components to be rendered on screen
 * Components are things such as widgets, text areas, etc
 * Not meant to be used on its own
 */
abstract class Component {
    // Starting coordinates for the component
    // Are relative to the screen (x is column, y is line, 1-based counting)
    // When setting values, only a minimum value check is done (>= 1)
    private int startX, startY;
    
    // Size of component in each dimension
    private int sizeX, sizeY;
    
    /**
     * Layer to be drawn on.
     * Works like the CSS z-index property, where the highest number has
     * most precedence
     */
    private int layer;
    
    /**
     * 2 dimensional array of ArrayList&lt;SGR&gt; for the display attributes.
     */
    ArrayList<SGR>[][] displayAttr;
    
    /**
     * Default constructor. Used for components that can only set the values
     * after processing
     */
    public Component() {
        this.startX = 1;
        this.startY = 1;
        this.sizeX = 0;
        this.sizeY = 0;
    }
    
    /**
     * Constructors for components should have the argument order of:
     * startX, startY, sizeX, sizeY, layer, [component specific arguments]
     * 
     * @param startX
     * @param startY
     * @param sizeX
     * @param sizeY
     * @param layer
     * @throws IndexOutOfBoundsException 
     */
    public Component(int startX, int startY, int sizeX, int sizeY, int layer) throws IndexOutOfBoundsException {
        if (startX < 1 || startY < 1)
            throw new IndexOutOfBoundsException("Starting coords need to be 1-based");
        this.startX = startX;
        this.startY = startY;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.layer = layer;
        
        this.displayAttr = new ArrayList[sizeY][sizeX];
    }
    
    public int getStartX() {
        return startX;
    }

    public void setStartX(int startX) throws IndexOutOfBoundsException {
        if (startX < 1)
            throw new IndexOutOfBoundsException("startX out of bounds");
        this.startX = startX;
    }
 
    public int getStartY() {
        return startY;
    }

    public void setStartY(int startY) throws IndexOutOfBoundsException {
        if (startY < 1)
            throw new IndexOutOfBoundsException("startY out of bounds");
        this.startY = startY;
    }

    public int getSizeX() {
        return sizeX;
    }

    // Everytime the size is changed, initialise a new displayAttr array. The
    // Component is responsible for repopulating attributes.
    public void setSizeX(int sizeX) {
        this.displayAttr = new ArrayList[this.sizeY][sizeX];
        this.sizeX = sizeX;
    }

    public int getSizeY() {
        return sizeY;
    }

    public void setSizeY(int sizeY) {
        this.displayAttr = new ArrayList[sizeY][this.sizeX];
        this.sizeY = sizeY;
    }

    public int getLayer() {
        return layer;
    }

    public void setLayer(int layer) {
        this.layer = layer;
    }
    
    /**
     * Add a display attribute for the range of characters. If the indexes are
     * out of bounds, an IndexOutOfBoundsException is thrown. <br>
     * 
     * Display attributes are handled by this class, while the display array is
     * handled by the component themselves.
     * 
     * @param attr      the attribute
     * @param row       the row which the character is in
     * @param startCol  the column to start the attribute (inclusive)
     * @param endCol    the column to end the attribute (exclusive)
     * 
     * @throws IndexOutOfBoundsException
     */
    public void addDisplayAttr(SGR attr, int row, int startCol, int endCol) throws IndexOutOfBoundsException {
        // Check bounds
        if (row < 0 || row >= sizeY
            || startCol < 0 || startCol > sizeX
            || endCol < 0 || endCol > sizeX) {
            throw new IndexOutOfBoundsException();
        }
        
        for (int col = startCol; col < endCol; col++) {
            if (displayAttr[row][col] == null)
                this.displayAttr[row][col] = new ArrayList();
            this.displayAttr[row][col].add(attr);
        }
    }
    
    /**
     * Gets the ArrayList&lt;SGR&gt; at the given row and column
     * 
     * @param r the row
     * @param c the column
     * @return 
     */
    public ArrayList<SGR> getDisplayAttr(int r, int c) {
        return displayAttr[r][c];
    }
    
    /**
     * Clears the display attributes by initializing a new array.
     * This should be called at the start of refresh function if the component
     * is managing the display attributes by themselves.
     */
    public void clearAttr() {
        this.displayAttr = new ArrayList[sizeY][sizeX];
    }
    
    /**
     * Clears the display attributes for the specified row and column
     * 
     * @param r the row
     * @param c the column
     */
    public void clearAttr(int r, int c) {
        this.displayAttr[r][c] = new ArrayList<SGR>();
    }

    /**
     * Returns the rendered component as a 2 dimensional array of
     * DisplayCharacter. Start and end coordinates are stored in the component.
     * 
     * @return a 2 dimensional array of DisplayCharacter
     */
    public abstract DisplayCharacter[][] render();
}
