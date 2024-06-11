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

import java.io.*;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Pattern;

/**
 *
 * @author kaiyu
 * 
 * Class to control the terminal (write/draw characters, receive input, etc)
 * Inspired by the curses library found in C
 * Everything printed onto the terminal is a component, which is rendered layer
 * by layer
 */
public class Screen {
    private static final OutputStream stdout = System.out;
    private static final InputStream stdin = System.in;
    
    // Number of lines/columns usable in the terminal
    private static int numLines;
    private static int numColumns;
    
    /**
     * List of components to be rendered
     * It is expected of the main class to remove any components which do not
     * need to be rendered in the next refresh
     * For components like SelectionComponent, the main class should keep a
     * separate reference to the component, so that it can read it after it has
     * been removed from this list
     */
    private static ArrayList<Component> components;

    /**
     * Initializes the screen by clearing it and moving the cursor to the origin
     * Also initializes attributes
     * Does not return anything
     * 
     * @param nl
     * @param nc
     * @throws java.io.IOException
     */
    public static void initscr(int nl, int nc) throws IOException {
        stdout.write("\033[2J\033[1;1H".getBytes());
        stdout.flush();
        
        components = new ArrayList();
        
        numLines = nl;
        numColumns = nc;
    }
    
    /**
     * Clears the screen and moves the cursor to the origin
     * 
     * @throws java.io.IOException
     */
    public static void clrscr() throws IOException {
        stdout.write("\033[0m\033[2J\033[;H".getBytes());
        stdout.flush();
    }
    
    /**
     * Returns the number of lines in the terminal
     * 
     * @return number of lines
     */
    public static int getNumLines() {
        return 0;
    }
    
    /**
     * Adds a component to `components` list.
     * The components must entirely fit into the screen size
     * 
     * @param c
     * @throws IndexOutOfBoundsException
     */
    public static void addComponent(Component c) throws IndexOutOfBoundsException {
        // Check if the component stays within numLines and numColumns
        if (c.getStartX() + c.getSizeX() > numColumns
                || c.getStartY() + c.getSizeY() > numLines)
            throw new IndexOutOfBoundsException("Component's position exceeds limits");
        
        components.add(c);
    }
    /**
     * Removes a component from `components` list by its index
     * 
     * @param cIndex
     */
    public static void removeComponent(int cIndex) {
        components.remove(cIndex);
    }
    
    /**
     * Remove the exact component from the list by its reference
     * 
     * @param c 
     */
    public static void removeComponent(Component c) throws NoSuchElementException {
        for (int i = 0; i < components.size(); i++) {
            if (components.get(i) == c) {
                components.remove(i);
                return;
            }
        }
        throw new NoSuchElementException();
    }
    
    public static ArrayList<Component> getComponents() {
        return components;
    }
    
    public static void clearComponents() {
        components.clear();
    }
    
    /**
     * Renders components and outputs the result as an array of Strings.
     * Screen.refresh() calls this.
     * 
     * @return a DisplayCharacter[][] array for Screen.refresh() to draw
     */    
    public static DisplayCharacter[][] render() {
        DisplayCharacter[][] result = new DisplayCharacter[numLines][numColumns];
        
        Collections.sort(
            components,
            (Component left, Component right) -> {
                return left.getLayer() - right.getLayer();
            }
        );
        
        for (Component component : components) {
            DisplayCharacter[][] renderedComponent = component.render();
            
            // Here startX and startY is 0-based since we're operating on an
            // array. The start variables in the component is 1-based.
            int startX = component.getStartX() - 1;
            int startY = component.getStartY() - 1;
            int sizeX = component.getSizeX();
            int sizeY = component.getSizeY();
            
            for (int y = 0; y < sizeY; y++) {
                for (int x = 0; x < sizeX; x++) {
                    result[y + startY][x + startX] = renderedComponent[y][x];
                }
            }
        }
        
        // Fill in the empty character cells as to avoid NullPointerException
        for (int y = 0; y < numLines; y++) {
            for (int x = 0; x < numColumns; x++) {
                if (result[y][x] == null) {
//                    System.out.println("null cell detected");
                    result[y][x] = new DisplayCharacter(' ', null);
                }
            }
        }

        return result;
    }
    
    /**
     * Refreshes the screen.
     * This should be done after every keystroke since echo is on
     * 
     * @throws java.io.IOException
     */
    public static void refresh() throws IOException {
        clrscr();
        DisplayCharacter[][] screen = render();
        // Rather than calling stdout.write() everytime, add everything to print
        // to one String variable and print that out at the end. This might also
        // have the positive effect of overwriting any echoed keypresses.
        String toPrint = "";
        
        for (int r = 0; r < numLines; r++) {
            toPrint += String.format("\033[%d;%dH", r + 1, 1);
            for (int c = 0; c < numColumns; c++) {
                DisplayCharacter displayChar = screen[r][c];
                if (displayChar.displayAttr != null) {
                    for (SGR a : displayChar.displayAttr) {
                        if (a != null) {
                            toPrint += a;
                        }
                    }
                }
                // Checks if the character is printable. If not, replace with a
                // space. This also limits the printable characters to only
                // those found on a keyboard.
                // Reference: https://stackoverflow.com/questions/13925454/
                if (Character.isLetterOrDigit(displayChar.character)
                    || Pattern.matches("\\p{Punct}", String.valueOf(displayChar.character))) {
                    toPrint += displayChar.character;
                } else {
                    toPrint += " ";
                }

                // Print the reset control sequence for every character.
                // Although unnecessary for many characters, this is safer than
                // checking if the next character doesn't have the attribute and
                // printing the end attribute
                toPrint += SGR.RESET;
            }
        }
        stdout.write(toPrint.getBytes());
        stdout.flush();
    }
}
