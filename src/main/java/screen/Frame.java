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
 * A class representing the characters to display along with any display
 * attributes. This is used by Components and Screen.render to represent the
 * rendered result. The display attributes are stored in a separate array to
 * prevent formatting from getting mixed up.
 * 
 * @author kaiyu
 */
public class Frame {
    private char[][] frame;
    // Each element is an ArrayList<SGR> because a character can have multiple
    // display attributes
    private ArrayList<SGR>[][] displayAttr;
    
    /**
     * Components should create the arrays on their own and pass them into the
     * constructor as arguments. Frame data is not meant to be modified here.
     * The starting coordinates and size are stored in the component object and
     * not here.
     * 
     * @param frame
     * @param displayAttr 
     */
    public Frame(char[][] frame, ArrayList<SGR>[][] displayAttr) {
        this.frame = frame;
        this.displayAttr = displayAttr;
    }
    
    /**
     * 
     * @param r
     * @param c
     * @return 
     */
    public char getFrameCharacter(int r, int c) {
        return frame[r][c];
    }
    
    /**
     * 
     * @param r
     * @param c
     * @return 
     */
    public ArrayList<SGR> getFrameCharacterAttr(int r, int c) {
        return displayAttr[r][c];
    }
}
