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
 * Represents the difficulty levels as described by the Wikipedia page
 * https://en.wikipedia.org/wiki/Minesweeper_(video_game)
 * 
 * @author kaiyu
 */
public enum Difficulty {
    BEGINNER(9, 9, 10),
    INTERMEDIATE(16, 16, 40),
    EXPERT(30, 16, 99);
    
    final int sizeX, sizeY, numMines;
    
    private Difficulty(int x, int y, int n) {
        this.sizeX = x;
        this.sizeY = y;
        this.numMines = n;
    }
    
    /**
     * Returns the Difficulty enum that matches the String given
     * 
     * @param d the difficulty String
     * @return the Difficulty enum or null if none found
     */
    public static Difficulty getDifficulty(String d) {
        d = d.toUpperCase();
        switch (d) {
            case "BEGINNER": return Difficulty.BEGINNER;
            case "INTERMEDIATE": return Difficulty.INTERMEDIATE;
            case "EXPERT": return Difficulty.EXPERT;
            default: return null;
        }
    }
}
